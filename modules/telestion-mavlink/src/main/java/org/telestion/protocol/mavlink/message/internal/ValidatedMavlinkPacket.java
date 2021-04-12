package org.telestion.protocol.mavlink.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.telestion.api.message.JsonMessage;
import org.telestion.protocol.mavlink.message.MavlinkMessage;
import org.telestion.protocol.mavlink.message.PacketInformation;

/**
 * @param payload
 * @param clazz
 * @param info
 *
 * @author Cedric Boes
 * @version 1.0
 */
public record ValidatedMavlinkPacket(@JsonProperty byte[] payload,
									 @JsonProperty Class<? extends MavlinkMessage> clazz,
									 @JsonProperty PacketInformation info) implements JsonMessage {
	/**
	 * Used for reflection!
	 */
	@SuppressWarnings("unused")
	private ValidatedMavlinkPacket() {
		this(null, null, null);
	}
}
