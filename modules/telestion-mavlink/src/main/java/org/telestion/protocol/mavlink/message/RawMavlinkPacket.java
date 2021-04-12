package org.telestion.protocol.mavlink.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This will be published on the bus, if the {@link org.telestion.protocol.mavlink.Validator Validator} detects
 * that a packet was a MAVLink-packet.<br>
 * The record also contains information about the success of the parsing-process of the validator.
 *
 * @param raw bytes of the {@link org.telestion.protocol.mavlink.dummy.NetPacket NetPacket}
 * @param success if the validation process was successful
 *
 * @author Cedric Boes
 * @version 1.0
 * @see org.telestion.protocol.mavlink.Validator
 */
public record RawMavlinkPacket(@JsonProperty byte[] raw,
							   @JsonProperty boolean success) {
	@SuppressWarnings("unused")
	private RawMavlinkPacket() {
		this(null, false);
	}
}
