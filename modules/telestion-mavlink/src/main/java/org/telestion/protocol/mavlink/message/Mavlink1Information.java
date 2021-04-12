package org.telestion.protocol.mavlink.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Mavlink1Information(@JsonProperty int seq,
								  @JsonProperty int sysId,
								  @JsonProperty int compId) implements PacketInformation {
	/**
	 * Used for reflection!
	 */
	@SuppressWarnings("unused")
	private Mavlink1Information() {
		this(0, 0, 0);
	}
}
