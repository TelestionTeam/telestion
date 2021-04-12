package org.telestion.protocol.old_mavlink;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telestion.api.message.JsonMessage;
import org.telestion.api.config.Config;
import org.telestion.core.connection.TcpConn;
import org.telestion.core.message.Address;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlink;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlinkV1;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlinkV2;

/**
 * {@link Verticle} which handles incoming MAVLink-Messages (in bytes[]).<br>
 * Incoming messages will be parsed into a {@link RawMavlink} and send to the {@link MavlinkParser}.
 *
 * @author Cedric Boes, Jan von Pichowski
 * @version 1.0
 */
public final class Receiver extends AbstractVerticle {

	/**
	 * All logs of {@link Receiver} will be using this {@link Logger}.
	 */
	private static final Logger logger = LoggerFactory.getLogger(Receiver.class);
	/**
	 * This configuration will be used if not <code>null</code>.
	 */
	private final Configuration forcedConfig;

	/**
	 * Creates a default {@link Receiver} which publishes its data to the specified addresses. The addresses are either
	 * defined by the default configuration or in the config file. The default configuration is only has the
	 * {@link Receiver} as permitted input and output.
	 */
	public Receiver() {
		forcedConfig = null;
	}

	/**
	 * Creates a {@link Receiver} which publishes its data to the given addresses.
	 *
	 * @param dataProviderAddress   the address which provides the data which should be received
	 * @param rawMavConsumerAddress the address which consumes the {@link RawMavlink} message
	 */
	public Receiver(String dataProviderAddress, String rawMavConsumerAddress) {
		forcedConfig = new Configuration(dataProviderAddress, rawMavConsumerAddress);
	}

	@Override
	public void start(Promise<Void> startPromise) {
		var config = Config.get(forcedConfig, config(), Configuration.class);

		vertx.eventBus().consumer(config.tcpDataSupplierAddress(), msg -> {
			if (!JsonMessage.on(TcpConn.Data.class, msg, data -> {
				byte[] bytes = data.data();

				RawMavlink mav = switch (bytes[0]) {
				case (byte) 0xFD -> (bytes.length > 11 ? new RawMavlinkV2(bytes) : null);
				case (byte) 0xFE -> (bytes.length > 7 ? new RawMavlinkV1(bytes) : null);
				default -> null;
				};

				if (mav != null) {
					AddressAssociator.put(mav.getMavlinkId(),
							new AddressPort(data.participant().host(), data.participant().port()));
					vertx.eventBus().send(config.rawMavConsumerAddress(), mav.json());
				} else {
					logger.warn("TCP-Package with unsupported format received.");
				}
			})) {
				// Might cause problems because sender does not get notified.
				logger.error("Unsupported event bus message sent to {}", msg.address());
			}
		});

		startPromise.complete();
	}

	@Override
	public void stop(Promise<Void> stopPromise) {
		stopPromise.complete();
	}

	/**
	 * The configuration of the receiver.
	 *
	 * @param tcpDataSupplierAddress the address which provides the data which should be received
	 * @param rawMavConsumerAddress  the address which consumes the RawMavlink message
	 */
	@SuppressWarnings("preview")
	private static record Configuration(@JsonProperty String tcpDataSupplierAddress,
			@JsonProperty String rawMavConsumerAddress) {

		@SuppressWarnings("unused")
		private Configuration() {
			this(Address.incoming(Receiver.class), Address.outgoing(Receiver.class));
		}
	}
}
