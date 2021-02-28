package org.telestion.protocol.mavlink;

import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telestion.api.message.JsonMessage;
import org.telestion.protocol.mavlink.annotation.MavInfo;
import org.telestion.protocol.mavlink.dummy.NetPacket;
import org.telestion.protocol.mavlink.message.Mavlink1Information;
import org.telestion.protocol.mavlink.message.RawMavlinkPacket;
import org.telestion.protocol.mavlink.message.internal.ValidatedMavlinkPacket;
import org.telestion.protocol.old_mavlink.security.X25Checksum;

import java.util.Arrays;

/**
 * @author Cedric Boes
 * @version 1.0
 */
public final class ValidatorMavlink1 extends Validator {

	/**
	 *
	 * @param config
	 */
	public ValidatorMavlink1(Config config) {
		super(config);
	}

	/**
	 *
	 * @param inAddress
	 * @param packetOutAddress
	 * @param parserInAddress
	 */
	public ValidatorMavlink1(String inAddress, String packetOutAddress, String parserInAddress) {
		super(inAddress, packetOutAddress, parserInAddress);
	}

	@Override
	public final void handleMessage(Message<?> msg) {
		JsonMessage.on(NetPacket.class, msg, packet -> {
			var raw = packet.raw();

			// Checking raw packet constraints and if the packet is a MAVLinkV1 packet
			if (!(raw != null && raw.length > 7 && raw[0] == (byte) 0xFE)) {
				return;
			}

			logger.debug("MavlinkV1-packet received");

			var length = raw[1];

			// It can be greater if e.g. the packet length is smaller than the raw stream input
			if (raw.length - 8 - length < 0) {
				logger.info("Broken MavlinkV1-packet received!");
				vertx.eventBus().publish(getPacketOutAddress(), new RawMavlinkPacket(raw, false));
				return;
			}

			var seq = raw[2];
			var sysId = raw[3];
			var compId = raw[4];
			var msgId = raw[5];
			var payload = Arrays.copyOfRange(raw, 6, 6 + length);
			var checksum = Arrays.copyOfRange(raw, 6 + length, 6 + length + 2);

			var clazz = MessageIndex.get(msgId);
			if (!clazz.isAnnotationPresent(MavInfo.class)) {
				logger.warn("Annotation missing for {} (MavlinkV1)!", clazz.getName());
				vertx.eventBus().publish(getPacketOutAddress(), new RawMavlinkPacket(raw, false));
				return;
			}
			var annotation = clazz.getAnnotation(MavInfo.class);

			if (X25Checksum.calculate(payload, annotation.crc()) != checksum[0] << 8 + checksum[1]) {
				logger.info("Checksum of received MavlinkV1-packet invalid!");
				vertx.eventBus().publish(getPacketOutAddress(), new RawMavlinkPacket(raw, false));
				return;
			}

			vertx.eventBus().publish(getPacketOutAddress(), new RawMavlinkPacket(raw, true));

			var mavInfo = new Mavlink1Information(seq, sysId, compId);
			vertx.eventBus().publish(getParserInAddress(), new ValidatedMavlinkPacket(payload, clazz, mavInfo));
		});
	}

	/**
	 * Handles all logs for {@link ValidatorMavlink1 this} verticle.
	 */
	private final Logger logger = LoggerFactory.getLogger(ValidatorMavlink2.class);

}
