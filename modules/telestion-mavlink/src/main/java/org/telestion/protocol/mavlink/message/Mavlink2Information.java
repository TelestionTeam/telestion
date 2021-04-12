package org.telestion.protocol.mavlink.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @param incompatFlags
 * @param compatFlags
 * @param seqNum
 * @param sysId
 * @param compId
 *
 * @author Cedric Boes
 * @version 1.0
 */
public record Mavlink2Information(@JsonProperty int incompatFlags,
								  @JsonProperty int compatFlags,
								  @JsonProperty int seqNum,
								  @JsonProperty int sysId,
								  @JsonProperty int compId) implements PacketInformation {
	@SuppressWarnings("unused")
	private Mavlink2Information() {
		this(0, 0, 0, 0, 0);
	}
}
