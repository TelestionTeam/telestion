package org.telestion.protocol.old_mavlink;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telestion.api.message.JsonMessage;
import org.telestion.api.config.Config;
import org.telestion.core.message.Address;
import org.telestion.protocol.old_mavlink.annotation.MavArray;
import org.telestion.protocol.old_mavlink.annotation.MavField;
import org.telestion.protocol.old_mavlink.annotation.MavInfo;
import org.telestion.protocol.old_mavlink.annotation.NativeType;
import org.telestion.protocol.old_mavlink.exception.*;
import org.telestion.protocol.old_mavlink.message.MavlinkMessage;
import org.telestion.protocol.old_mavlink.message.MessageHelper;
import org.telestion.protocol.old_mavlink.message.MessageIndex;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlink;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlinkV1;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlinkV2;
import org.telestion.protocol.old_mavlink.security.HeaderContext;
import org.telestion.protocol.old_mavlink.security.MavV2Signator;
import org.telestion.protocol.old_mavlink.security.SecretKeySafe;
import org.telestion.protocol.old_mavlink.security.X25Checksum;

/**
 * A {@link Verticle} converting MAVLink-Message <code>byte[]</code> buffers to {@link MavlinkMessage MavlinkMessages}
 * and vice versa.<br>
 * This is done mostly via reflection.<br>
 * <br>
 * To convert the raw message bytes to a {@link MavlinkMessage} send {@link RawMavlink messages} to the
 * {@link #toMavlinkInAddress}. The result will be published on {@link #toMavlinkOutAddress}.<br>
 * For getting the raw-message from a {@link MavlinkMessage} simply send it as json-message to {@link #toRawInAddressV1}
 * or {@link #toRawInAddressV2} depending on the MAVLink version you want to address. Results will be published on the
 * {@value #toRawOutAddress}.<br>
 * <br>
 * Supported {@link MavlinkMessage messages} must be registered in the {@link MessageIndex} as the index cannot be
 * recognized otherwise. In addition to that they also must meet the requirements in general (e.g. must be records and
 * be annotated in the correct way).<br>
 * <br>
 * A valid {@link HeaderContext} must be given to parse the messages to bytes. As the context is MAVLinkV1- and
 * MAVLinkV2-compatible both output-versions are supported (for unsigned MAVLinkV2-Messages set the incompatible-Flag to
 * <code>0x0</code> otherwise it must be <code>0x1</code>).
 *
 * @author Cedric Boes, Jan von Pichowski
 * @version 1.0
 */
public final class MavlinkParser extends AbstractVerticle {

	/**
	 * All logs of {@link MavlinkParser} will be using this {@link Logger}.
	 */
	private static final Logger logger = LoggerFactory.getLogger(MavlinkParser.class);
	/**
	 * This configuration will be used if not null.
	 */
	private final Configuration forcedConfig;

	/**
	 * {@link HeaderContext HeaderContexts} are required for parsing {@link MavlinkMessage messages} to bytes.
	 */
	private HeaderContext context;

	/**
	 * A {@link SecretKeySafe} containing the password bytes for the Signature-Hash.
	 */
	private SecretKeySafe keySafe;

	/**
	 * Default {@link MavlinkParser} which can only be used for receiving data.<br>
	 * This can only be used for telemetry.
	 */
	public MavlinkParser() {
		this(null, null, null);
	}

	/**
	 * Default {@link MavlinkParser} which can only be used for receiving data.<br>
	 * This can only be used for telemetry.
	 *
	 * @param forcedConfig the forced configuration
	 */
	public MavlinkParser(Configuration forcedConfig) {
		this(null, null, forcedConfig);
	}

	/**
	 * {@link MavlinkParser} using the default {@link #addressIdentifier} and the given {@link HeaderContext} and
	 * {@link SecretKeySafe}.
	 *
	 * @param context      for parsing
	 * @param safe         containing the password for the signature
	 * @param forcedConfig the forced configuration
	 */
	public MavlinkParser(HeaderContext context, SecretKeySafe safe, Configuration forcedConfig) {
		this.context = context;
		this.keySafe = safe;
		this.forcedConfig = forcedConfig;
	}

	/**
	 * {@link Comparator} for the {@link RecordComponent MAVLink-RecordComponents} to bring them into the right format
	 * for MAVLink.
	 *
	 * @param c1 {@link RecordComponent} #1
	 * @param c2 {@link RecordComponent} #2
	 * @return how the sorting algorithm should sort
	 */
	private static int compareRecordComponents(RecordComponent c1, RecordComponent c2) {
		if (!(c1.isAnnotationPresent(MavField.class) && c2.isAnnotationPresent(MavField.class))) {
			// breaks out of method
			throw new AnnotationMissingException("MavField-Annotation is missing for at least one RecordComponent!");
		}

		MavField mf1 = c1.getAnnotation(MavField.class);
		MavField mf2 = c2.getAnnotation(MavField.class);

		if (!(mf1.position() == -1 || mf2.position() == -1)) {
			return mf1.position() - mf2.position();
		}

		if (mf1.extension() == mf2.extension()) {
			return mf2.nativeType().size - mf1.nativeType().size;
		} else {
			return mf1.extension() ? 1 : -1;
		}
	}

	/**
	 * Changes the {@link #context} for this {@link MavlinkParser}.
	 *
	 * @param context with which {@link #context} will be replaced
	 */
	public void changeHeaderContext(HeaderContext context) {
		this.context = context;
	}

	/**
	 * Changes the {@link #keySafe} for this {@link MavlinkParser}.
	 *
	 * @param safe with which {@link #keySafe} will be replaced
	 */
	public void changeKeySafe(SecretKeySafe safe) {
		if (!(this.keySafe == null || this.keySafe.isDeleted())) {
			this.keySafe.deleteKey();
		}
		this.keySafe = safe;
	}

	@Override
	public void start(Promise<Void> startPromise) {
		var config = Config.get(forcedConfig, config(), Configuration.class);

		vertx.eventBus().consumer(config.rawMavSupplierAddr(), msg -> {
			try {
				if (!(JsonMessage.on(RawMavlink.class, msg, m -> interpretMsg(m, config)))) {
					logger.error("Unsupported type sent to {}", msg.address());
					throw new PacketException("Unsupported type sent to Mavlink-Parser!");
				}
			} catch (PacketException | ParsingException | InvalidChecksumException | WrongSignatureException
					| AnnotationMissingException e) {
				// TODO: @Matei log here
			}
		});

		vertx.eventBus().consumer(config.mavV1SupplierAddr(), msg -> {
			if (!JsonMessage.on(MavlinkMessage.class, msg, raw -> {
				byte[] payload = getRaw(raw);

				var seq = context.getNewPacketSeq();

				// If this happens more than once -> the message will no longer be really unique!
				if ((byte) (seq & 0xff) == 0) {
					logger.warn("MessageIds starting at 0 (again)!");
				}

				var bytes = new byte[payload.length + 5];
				bytes[0] = (byte) (payload.length & 0xff);
				bytes[1] = (byte) (seq & 0xff);
				bytes[2] = (byte) (context.compId() & 0xff);
				bytes[3] = (byte) (raw.getId() & 0xff);

				var index = 4;
				for (byte b : payload) {
					bytes[index++] = b;
				}

				bytes[bytes.length - 1] = (byte) raw.getCrc();

				final int checksum = X25Checksum.calculate(bytes);

				byte[] rawBytes = new byte[bytes.length + 2];
				rawBytes[0] = (byte) 0xFE;
				index = 1;

				for (byte b : bytes) {
					rawBytes[index++] = b;
				}

				rawBytes[index - 1] = (byte) (checksum >> 8 & 0xff);
				rawBytes[index] = (byte) (checksum & 0xff);

				vertx.eventBus().send(config.rawMavConsumerAddr(), new RawMavlinkV1(rawBytes).json());
			})) {
				logger.warn("Invalid message sent to Mavlink2RawV1-Parser! (Message-Body: {})", msg.body());
			}
		});

		vertx.eventBus().consumer(config.mavV2SupplierAddr(), msg -> {
			if (!JsonMessage.on(MavlinkMessage.class, msg, raw -> {
				byte[] payload = getRaw(raw);

				var seq = context.getNewPacketSeq();

				// If this happens more than once -> the message will no longer be really unique!
				if ((byte) (seq & 0xff) == 0) {
					logger.warn("MessageIds starting at 0 (again)!");
				}

				ByteBuffer buffer = ByteBuffer.allocate(payload.length + 10);
				buffer.put((byte) (payload.length & 0xff));
				buffer.put((byte) (context.incompFlags() & 0xff));
				buffer.put((byte) (context.compFlags() & 0xff));
				buffer.put((byte) (seq & 0xff));
				buffer.put((byte) (context.sysId() & 0xff));
				buffer.put((byte) (context.compId() & 0xff));
				buffer.put((byte) ((raw.getId() >> 16) & 0xff));
				buffer.put((byte) ((raw.getId() >> 8) & 0xff));
				buffer.put((byte) (raw.getId() & 0xff));
				buffer.put(payload);
				buffer.put((byte) (raw.getCrc() & 0xff));

				final int checksum = X25Checksum.calculate(buffer.array());
				byte[] signature = new byte[0];
				if (context.incompFlags() == 0x01) {
					try {
						signature = MavV2Signator.generateSignature(keySafe.getSecretKey(),
								Arrays.copyOfRange(buffer.array(), 0, 9), payload, raw.getCrc(),
								(short) context.linkId());
					} catch (NoSuchAlgorithmException e) {
						logger.error("Signating packet failed!", e);
						throw new ParsingException("Parsing MAVLink-Message into byte[] failed due to signating!");
					}
				}

				byte[] rawBytes = new byte[buffer.capacity() + 15];
				rawBytes[0] = (byte) 0xFD;
				int index = 1;

				// Streaming of byte[] not possible :(
				for (byte b : buffer.array()) {
					rawBytes[index++] = b;
				}

				rawBytes[index - 1] = (byte) (checksum >> 8 & 0xff);
				rawBytes[index++] = (byte) (checksum & 0xff);

				for (byte b : signature) {
					rawBytes[index++] = b;
				}

				vertx.eventBus().send(config.rawMavConsumerAddr(), new RawMavlinkV2(rawBytes).json());
			})) {
				logger.warn("Invalid message sent to Mavlink2RawV2-Parser! (Message-Body: {})", msg.body());
			}
		});

		startPromise.complete();
	}

	@Override
	public void stop(Promise<Void> stopPromise) {
		changeKeySafe(null);
		stopPromise.complete();
	}

	/**
	 * Casting an Object to the right {@link Number number-object}.
	 *
	 * @param o     {@link Number} as {@link Object}
	 * @param clazz {@link Class} to which the object should be casted
	 * @return casted number
	 */
	private Number toRightNum(Object o, Class<? extends Number> clazz) {
		// TODO: Add char-support
		return switch (clazz.getSimpleName()) {
		case "Byte" -> (Byte) o;
		case "Short" -> (Short) o;
		case "Integer" -> (Integer) o;
		case "Long" -> (Long) o;
		case "Float" -> (Float) o;
		case "Double" -> (Double) o;
		default -> throw new ClassCastException("Unsupported Type " + clazz.getName());
		};
	}

	/**
	 * Parsing a part of the payload at the given index with the help of the given {@link RecordComponent}.
	 *
	 * @param index   at which the parsing starts happening
	 * @param payload of the MAVLink-message
	 * @param c       {@link RecordComponent} which will be used for parsing
	 * @return parsed Object
	 */
	private Object parse(AtomicInteger index, byte[] payload, RecordComponent c) {
		// TODO: Add char-support
		var annotation = c.getAnnotation(MavField.class);

		if (index.get() > payload.length) {
			if (annotation.extension()) {
				return null;
			} else {
				throw new PacketException("Missing bytes which are not an extension!");
			}
		}

		boolean unsigned = annotation.nativeType().unsigned;
		switch (c.getAnnotation(MavField.class).nativeType().size) {
		case 1:
			short s = payload[index.incrementAndGet()];
			return unsigned ? toRightNum(s, Short.class) : toRightNum((byte) s, Byte.class);
		case 2:
			int i = (payload[index.incrementAndGet()] << 8) + payload[index.incrementAndGet()];
			return unsigned ? toRightNum(i, Integer.class) : toRightNum((short) i, Short.class);
		case 4:
			long l = (long) (payload[index.incrementAndGet()]) << 24 + (payload[index.incrementAndGet()]) << 16
					+ (payload[index.incrementAndGet()]) << 8 + payload[index.incrementAndGet()];
			return unsigned ? toRightNum(l, Long.class) : toRightNum((int) l, Integer.class);
		case 8:
			// There is no real support for unsigned longs, yet!
			l = (((long) payload[index.incrementAndGet()]) << 56) + (((long) payload[index.incrementAndGet()]) << 48)
					+ (((long) payload[index.incrementAndGet()]) << 40)
					+ (((long) payload[index.incrementAndGet()]) << 32) + (payload[index.incrementAndGet()] << 24)
					+ (payload[index.incrementAndGet()] << 16) + (payload[index.incrementAndGet()] << 8)
					+ payload[index.incrementAndGet()];
			return toRightNum(l, Long.class);
		default:
			throw new ParsingException("Parsing failed due to unknown Payload-Type!");
		}
	}

	/**
	 * Parses a {@link RawMavlink RawMavlink-message} into a {@link MavlinkMessage} and publishing it on the
	 * {@link #toMavlinkOutAddress} on the vert.x-eventbus.
	 *
	 * @param raw message to parse
	 */
	@SuppressWarnings({ "preview", "rawtypes" })
	private void interpretMsg(RawMavlink raw, Configuration config) {
		// TODO: Add char-support
		Class<? extends MavlinkMessage> mavlinkClass = null;

		int subtract = 1;
		int checksum = 0;
		byte[] load = null;

		if (raw instanceof RawMavlinkV2 v2) {
			mavlinkClass = MessageIndex.get(v2.msgId());

			// Handling according to specifications! Only 0x0 and 0x1 are supported, yet
			if ((v2.incompatFlags() & 0xff) == 0x1) {
				subtract = 14;
			} else if ((v2.incompatFlags() & 0xff) != 0x0) {
				throw new ParsingException("Invalid incompatible flag set! Must either be 0x0 or 0x1!");
			}

			checksum = v2.checksum();
			load = v2.payload().payload();
		} else if (raw instanceof RawMavlinkV1 v1) {
			mavlinkClass = MessageIndex.get(v1.msgId());

			checksum = v1.checksum();
			load = v1.payload().payload();
		} else {
			logger.error("Unsupported MAVLink-Message received on {}!", config.rawMavSupplierAddr());
			throw new PacketException("Unsupported MAVLink-Message received!");
		}

		try {
			var minLength = MessageHelper.minLength(mavlinkClass);
			var maxLength = MessageHelper.maxLength(mavlinkClass);

			if (load.length < minLength || load.length > maxLength) {
				throw new PacketException("Payload length invalid!");
			}
		} catch (IllegalArgumentException | SecurityException e) {
			throw new PacketException(e);
		}

		/*
		 * Check signature and checksum
		 */
		if (mavlinkClass.isAnnotationPresent(MavInfo.class)) {
			MavInfo annotation = mavlinkClass.getAnnotation(MavInfo.class);

			int crcExtra = annotation.crc();
			byte[] buildArray = Arrays.copyOfRange(raw.getRaw(), 1, raw.getRaw().length - subtract);
			buildArray[buildArray.length - 1] = (byte) crcExtra;

			int crc = X25Checksum.calculate(buildArray);

			if (crc != checksum) {
				throw new InvalidChecksumException("Checksum of received MAVLink-Package was invalid!");
			}

			if (raw instanceof RawMavlinkV2 v2 && v2.incompatFlags() == 0x01) {
				try {
					if (MavV2Signator.generateSignature(keySafe.getSecretKey(), Arrays.copyOfRange(v2.getRaw(), 1, 11),
							Arrays.copyOfRange(v2.getRaw(), 12, 12 + v2.payload().payload().length), crcExtra,
							v2.linkId()) == v2.signature()) {
						throw new WrongSignatureException("Signature of received MAVLink-Package was" + "incorrect!");
					}
				} catch (NoSuchAlgorithmException e) {
					throw new WrongSignatureException("An unexpected error occured while checking the signature "
							+ "of the received MAVLink-Package ", e);
				}
			}
		} else {
			throw new AnnotationMissingException("MavInfo-Annotation is missing!");
		}

		RecordComponent[] components = Arrays.stream(mavlinkClass.getRecordComponents())
				.sorted(MavlinkParser::compareRecordComponents).toArray(RecordComponent[]::new);

		/*
		 * Start interpreting
		 */
		try {
			Class[] componentTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);

			final AtomicInteger index = new AtomicInteger(-1);
			final byte[] payload = load;

			Constructor<? extends MavlinkMessage> constructor = mavlinkClass.getConstructor(componentTypes);

			Object[] parameters = Arrays.stream(components)
					.map(c -> c.isAnnotationPresent(MavArray.class)
							? IntStream.range(0, c.getAnnotation(MavArray.class).length())
									.mapToObj(i -> parse(index, payload, c)).toArray(Object[]::new)
							: parse(index, payload, c))
					.toArray(Object[]::new);

			MavlinkMessage m = constructor.newInstance(parameters);

			vertx.eventBus().publish(config.mavConsumerAddr(), m.json());
		} catch (Exception e) {
			throw new ParsingException(e);
		}

	}

	/**
	 * Converts an Object to a raw <code>byte[]</code> array.
	 *
	 * @param c RecordComponent of the Object to parse
	 * @param o Object to covert
	 * @return byte[] representation of the given object
	 * @throws IllegalArgumentException if the accessor of the for the {@link RecordComponent} cannot be invoked
	 * @throws ParsingException         if the type of the {@link RecordComponent} is unknown
	 */
	private byte[] recordToRaw(RecordComponent c, Object o) {
		MavField info = c.getAnnotation(MavField.class);

		NativeType natType = info.nativeType();
		if (natType == NativeType.DOUBLE) {
			o = Double.doubleToLongBits((double) o);
			natType = NativeType.INT_64;
		} else if (natType == NativeType.FLOAT) {
			o = Float.floatToIntBits((float) o);
			natType = NativeType.INT_32;
		}

		// TODO: Add char-support
		Number n = (Number) o;

		return switch (natType) {
		case CHAR -> new byte[] { (byte) (char) o };
		case INT_8, UINT_8 -> new byte[] { (byte) n.byteValue() };
		case UINT_16, INT_16 -> new byte[] { (byte) (((short) n.shortValue() >> 8) & 0xff),
				(byte) ((short) n.shortValue() & 0xff) };
		case UINT_32, INT_32 -> new byte[] { (byte) (((int) n.intValue() >> 24) & 0xff),
				(byte) (((int) n.intValue() >> 16) & 0xff), (byte) (((int) n.intValue() >> 8) & 0xff),
				(byte) ((int) n.intValue() & 0xff) };
		case INT_64, UINT_64 -> new byte[] { (byte) (((long) n.intValue() >> 56) & 0xff),
				(byte) (((long) n.intValue() >> 48) & 0xff), (byte) (((long) n.intValue() >> 40) & 0xff),
				(byte) (((long) n.intValue() >> 32) & 0xff), (byte) (((long) n.intValue() >> 24) & 0xff),
				(byte) (((long) n.intValue() >> 16) & 0xff), (byte) (((long) n.intValue() >> 8) & 0xff),
				(byte) ((long) n.intValue() & 0xff) };
		default -> throw new ParsingException("Unknown datatype " + info.nativeType() + "!");
		};
	}

	/**
	 * Converts a {@link MavlinkMessage} to a raw <code>byte[]</code> array.
	 *
	 * @param mav {@link MavlinkMessage} to convert
	 * @return converted byte[] array
	 * @throws AnnotationMissingException if the {@link MavField MavField-annotation} is missing
	 * @throws ParsingException           if the accessor of the for the {@link RecordComponent} cannot be invoked
	 */
	private byte[] getRaw(MavlinkMessage mav) {
		// TODO: Add char-support
		var components = Arrays.stream(mav.getClass().getRecordComponents())
				.sorted(MavlinkParser::compareRecordComponents).toArray(RecordComponent[]::new);

		var buffer = new byte[MessageHelper.length(mav)];
		var index = 0;
		for (var c : components) {
			Object o;
			try {
				o = c.getAccessor().invoke(mav);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new ParsingException(e);
			}
			if (!c.isAnnotationPresent(MavField.class)) {
				throw new AnnotationMissingException("MavField Annotation is missing!");
			}
			if (c.isAnnotationPresent(MavArray.class)) {
				int count = c.getAnnotation(MavArray.class).length();
				var os = (Object[]) o;
				for (int i = 0; i < count; i++) {
					byte[] bytes = recordToRaw(c, os[i]);
					for (byte b : bytes) {
						buffer[index++] = b;
					}
				}
			} else {
				byte[] bytes = recordToRaw(c, o);
				for (byte b : bytes) {
					buffer[index++] = b;
				}
			}
		}
		return buffer;
	}

	/**
	 * The parser configuration.
	 *
	 * @param rawMavSupplierAddr Address for parsing {@link RawMavlink RawMavlink-message} to a {@link MavlinkMessage}.
	 * @param mavConsumerAddr    Address for parsed {@link RawMavlink RawMavlink-messages}.
	 * @param mavV1SupplierAddr  Address which will be accepting {@link MavlinkMessage messages} and parse them into a
	 *                           {@link RawMavlinkV1} which already contains the header for the given payload and a
	 *                           unique messageId.
	 * @param mavV2SupplierAddr  Address for parsing {@link MavlinkMessage messages} into a {@link RawMavlinkV2} which
	 *                           already contains the header for the given payload, a unique messageId and a signature
	 *                           (if specified).
	 * @param rawMavConsumerAddr Address which consumes the {@link RawMavlink RawMavlink-messages}.
	 */
	@SuppressWarnings("preview")
	public static record Configuration(@JsonProperty String rawMavSupplierAddr, @JsonProperty String mavConsumerAddr,
			@JsonProperty String mavV1SupplierAddr, @JsonProperty String mavV2SupplierAddr,
			@JsonProperty String rawMavConsumerAddr) {

		@SuppressWarnings("unused")
		private Configuration() {
			this(Address.incoming(MavlinkParser.class, "toMavlink"), Address.outgoing(MavlinkParser.class, "toMavlink"),
					Address.incoming(MavlinkParser.class, "toRaw1"), Address.incoming(MavlinkParser.class, "toRaw2"),
					Address.outgoing(MavlinkParser.class, "toRaw"));
		}
	}

}
