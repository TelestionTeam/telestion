package org.telestion.protocol.old_mavlink.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.telestion.api.message.JsonMessage;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlink;

/**
 * Wrapper for the payload of MAVLink-messages to easily use it in the {@link RawMavlink RawMavlinkV-messages}.
 *
 * @param payload Actual payload bytes of a MAVLink-message.
 * @author Cedric Boes
 * @version 1.0
 */
@SuppressWarnings("preview")
public final record RawPayload(@JsonProperty byte[] payload) implements JsonMessage {

	/**
	 * There shall be no default constructor for normal people.<br>
	 * This will only be used by the JSON-parser.
	 */
	@SuppressWarnings("unused")
	private RawPayload() {
		this(null);
	}
}
