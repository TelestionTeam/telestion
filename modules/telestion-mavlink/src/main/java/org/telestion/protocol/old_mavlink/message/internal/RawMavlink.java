package org.telestion.protocol.old_mavlink.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.telestion.api.message.JsonMessage;

/**
 * Superclass for all raw MAVLink-Messages.<br>
 * Each version of MAVLink gets its dedicated RawMavlinkV[version] class which apart from the raw payload already
 * contains the header parsed into a usable format.
 *
 * @author Cedric Boes
 * @version 1.0
 * @see RawMavlinkV1
 * @see RawMavlinkV2
 */
public interface RawMavlink extends JsonMessage {
	/**
	 * Returns a MAVLink-ID which creation is "unique" to each version of MAVLink.<br>
	 * <br>
	 * <em>To ensure it can be sent over the vert.x bus in a JSON-format the {@link JsonProperty annotation} is
	 * required.</em>
	 *
	 * @return
	 */
	@JsonProperty(access = Access.READ_ONLY)
	public String getMavlinkId();

	/**
	 * Returns a the raw payload for each version of MAVLink.<br>
	 * <br>
	 * <em>To ensure it can be sent over the vert.x bus in a JSON-format the {@link JsonProperty annotation} is
	 * required.<br>
	 * This is usually the same but as records cannot be extended it must be overwritten in the classes directly.</em>
	 *
	 * @return
	 */
	@JsonProperty(access = Access.READ_ONLY)
	public byte[] getRaw();
}
