package org.telestion.protocol.mavlink;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telestion.api.message.JsonMessage;
import org.telestion.protocol.mavlink.annotation.MavArray;
import org.telestion.protocol.mavlink.annotation.MavField;
import org.telestion.protocol.mavlink.annotation.NativeType;
import org.telestion.protocol.mavlink.exception.AnnotationMissingException;
import org.telestion.protocol.mavlink.message.internal.ValidatedMavlinkPacket;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Cedric Boes
 * @version 1.0
 */
public final class PayloadParser extends AbstractVerticle {

	@Override
	public void start(Promise<Void> startPromise) {
		vertx.eventBus().consumer(config.inAddress(), msg -> {
			JsonMessage.on(ValidatedMavlinkPacket.class, msg, validatedMavlink -> {
				var clazz = validatedMavlink.clazz();
				var payload = validatedMavlink.payload();
				var components = clazz.getRecordComponents();

				try {
					components = Arrays.stream(components).sorted(PayloadParser::compareRecordComponents)
							.toArray(RecordComponent[]::new);
				} catch(AnnotationMissingException e) {
					logger.error("Parsing packet failed!" +
							"At least one Record-Component of Mavlink-message {} does not have the required " +
							"@MavField annotation!", clazz.getName());
					return;
				}

				try {
					var currentIndex = new AtomicInteger(0);
					var objs = Arrays.stream(components).
							map(c -> {
								var annotation = c.getAnnotation(MavField.class);
								// Extensions are optional and at the end of the parsing process
								if (payload.length - 1 - currentIndex.get() == 0 && annotation.extension()) {
									return null;
								}

								var arrLength = c.isAnnotationPresent(MavArray.class) ?
										c.getAnnotation(MavArray.class).length() : 0;
								var type = c.getAnnotation(MavField.class).nativeType();

								return parser.get(type).parse(payload, arrLength,
										currentIndex.getAndAdd(type.size * (arrLength == 0 ? 1 : arrLength)));
							}).filter(Objects::nonNull).toArray();

					var constructor = clazz.getConstructor(Arrays.stream(components)
							.map(RecordComponent::getType).toArray(Class[]::new));

					var instance = constructor.newInstance(objs);
					vertx.eventBus().publish(getOutAddress(), instance.json());
				} catch(Exception e) {
					logger.error("Parsing packet payload failed due to an unexpected error!", e);
				}
			});
		});
		startPromise.complete();
	}

	@Override
	public void stop(Promise<Void> stopPromise) {
		stopPromise.complete();
	}

	/**
	 * @param inAddress
	 * @param outAddress
	 */
	public record Configuration(@JsonProperty String inAddress,
								@JsonProperty String outAddress) implements JsonMessage {
		/**
		 * Used for reflection!
		 */
		@SuppressWarnings("unused")
		private Configuration() {
			this(null, null);
		}
	}

	/**
	 *
	 * @param inAddress
	 * @param outAddress
	 * @param parser
	 */
	public PayloadParser(String inAddress, String outAddress, HashMap<NativeType, TypeParser<?>> parser) {
		this(new Configuration(inAddress, outAddress), parser);
	}

	/**
	 *
	 * @param config
	 */
	public PayloadParser(Configuration config) {
		this(config, DefaultParsers.LITTLE_ENDIAN);
	}

	/**
	 *
	 * @param config
	 * @param parser
	 */
	public PayloadParser(Configuration config, HashMap<NativeType, TypeParser<?>> parser) {
		this.config = config;
		this.parser = parser;
	}

	/**
	 * Getter for {@link Configuration#inAddress inAddress}.
	 *
	 * @return {@link Configuration#inAddress inAddress}
	 */
	public String getInAddress() {
		return config.inAddress();
	}

	/**
	 * Getter for {@link Configuration#outAddress outAddress}.
	 *
	 * @return {@link Configuration#outAddress outAddress}
	 */
	public String getOutAddress() {
		return config.outAddress();
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
			throw new AnnotationMissingException("@MavField-Annotation is missing for at least one RecordComponent!");
		}

		var mf1 = c1.getAnnotation(MavField.class);
		var mf2 = c2.getAnnotation(MavField.class);

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
	 *
	 */
	private final Logger logger = LoggerFactory.getLogger(PayloadParser.class);

	/**
	 * A configuration which specifies this in and out address for this verticle.<br>
	 * This can also be loaded from a config.
	 */
	private final Configuration config;

	/**
	 *
	 */
	private final HashMap<NativeType, TypeParser<?>> parser;
}
