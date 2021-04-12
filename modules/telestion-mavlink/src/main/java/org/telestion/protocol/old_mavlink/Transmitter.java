package org.telestion.protocol.old_mavlink;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telestion.api.message.JsonMessage;
import org.telestion.api.config.Config;
import org.telestion.core.message.Address;
import org.telestion.protocol.old_mavlink.message.MavlinkMessage;
import org.telestion.protocol.old_mavlink.message.RawPayload;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlink;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlinkV1;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlinkV2;

/**
 * {@link Verticle} which handles outgoing MAVLink-Messages (as {@link RawMavlink}).<br>
 * Outgoing messages will be send to the {@link TcpServer}.
 *
 * @author Cedric Boes, Jan von Pichowski
 * @version 1.0
 */
public final class Transmitter extends AbstractVerticle {

	/**
	 * All logs of {@link Transmitter} will be using this {@link Logger}.
	 */
	private final Logger logger = LoggerFactory.getLogger(Transmitter.class);
	/**
	 * This configuration will be used if not <code>null</code>.
	 */
	private final Configuration forcedConfig;

	/**
	 * Creates a transmitter with the default configuration or the file configuration if available.
	 */
	public Transmitter() {
		forcedConfig = null;
	}

	/**
	 * Creates a transmitter with the given addresses.
	 *
	 * @param rawMavSupplierAddr  {@link RawMavlink RawMavlink-messages} will be send here.
	 * @param tcpDataConsumerAddr Messages (as {@link TcpData}) will be published on this address.
	 */
	public Transmitter(String rawMavSupplierAddr, String tcpDataConsumerAddr) {
		forcedConfig = new Configuration(rawMavSupplierAddr, tcpDataConsumerAddr);
	}

	@Override
	public void start(Promise<Void> startPromise) {
		var config = Config.get(forcedConfig, config(), Configuration.class);

		vertx.eventBus().consumer(config.rawMavSupplierAddr(), msg -> {
			if (!(JsonMessage.on(RawMavlinkV1.class, msg, m -> interpretMsg(m, config.tcpDataConsumerAddr()))
					|| JsonMessage.on(RawMavlinkV2.class, msg, m -> interpretMsg(m, config.tcpDataConsumerAddr())))) {
				logger.error("Unsupported type sent to {}", msg.address());
			}
		});

		startPromise.complete();
	}

	@Override
	public void stop(Promise<Void> stopPromise) {
		stopPromise.complete();
	}

	/**
	 * Converts {@link RawMavlink RawMavlink-messages} to a {@link TcpData TcpData-objects}.
	 *
	 * @param mav {@link MavlinkMessage} to convert
	 */
	private void interpretMsg(RawMavlink mav, String outAddress) {
		vertx.eventBus().send(outAddress, new RawPayload(mav.getRaw()).json());
	}

	/**
	 * The transmitter configuration.
	 *
	 * @param rawMavSupplierAddr  {@link RawMavlink RawMavlink-messages} will be send here.
	 * @param tcpDataConsumerAddr Messages (as {@link TcpData}) will be published on this address.
	 */
	@SuppressWarnings("preview")
	private static record Configuration(@JsonProperty String rawMavSupplierAddr,
			@JsonProperty String tcpDataConsumerAddr) {
		@SuppressWarnings("unused")
		private Configuration() {
			this(Address.incoming(Transmitter.class), Address.outgoing(Transmitter.class));
		}
	}
}
