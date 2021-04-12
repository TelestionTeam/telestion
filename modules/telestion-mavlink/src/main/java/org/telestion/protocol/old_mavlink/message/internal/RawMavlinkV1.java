package org.telestion.protocol.old_mavlink.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.telestion.protocol.old_mavlink.message.MavlinkMessage;
import org.telestion.protocol.old_mavlink.message.MessageIndex;
import org.telestion.protocol.old_mavlink.message.RawPayload;

/**
 * Implementation of {@link RawMavlink} for MAVLink Version 1.
 *
 * @param len      Length of the payload (array-buffer length).
 * @param seq      A "unique" id for packages to identify packet loss. Will be incremented for each packet.<br>
 *                 The parser later must identify if there has occurred any packet loss.
 * @param compId   ID of component sending the message. Used to differentiate components in a system (e.g. autopilot and
 *                 a camera). Use appropriate values in MAV_COMPONENT.<br>
 *                 <br>
 *                 <em>Note that the broadcast address MAV_COMP_ID_ALL may not be used in this field as it is an invalid
 *                 source ddress.</em>
 * @param msgId    Id of the {@link MavlinkMessage}. Must be registered in the {@link MessageIndex}.
 * @param payload  Actual MAVLink payload bytes of a message.
 * @param checksum The X.25 checksum for this message.
 * @author Cedric Boes
 * @version 1.0
 * @implNote Support for MAVLink V1
 * @see MavlinkMessage
 * @see MessageIndex
 */
@SuppressWarnings("preview")
public final record RawMavlinkV1(@JsonProperty short len, @JsonProperty short seq, @JsonProperty short compId,
		@JsonProperty short msgId, @JsonProperty RawPayload payload, @JsonProperty int checksum) implements RawMavlink {

	/**
	 * There shall be no default constructor for normal people.<br>
	 * This will only be used by the JSON-parser.
	 */
	@SuppressWarnings("unused")
	private RawMavlinkV1() {
		this((short) 0, (short) 0, (short) 0, (short) 0, null, 0);
	}

	/**
	 * Creates a new {@link RawMavlinkV1 object} from a byte representation.<br>
	 * To successfully read this, the starting byte (0xFE) must still be included.
	 *
	 * @param bytes raw MAVLink-message
	 */
	public RawMavlinkV1(byte[] bytes) {
		this((short) bytes[1], (short) bytes[2], (short) bytes[3], (short) bytes[4],
				new RawPayload(Arrays.copyOfRange(bytes, 5, bytes[1] + 5)),
				(int) (bytes[bytes[1] + 5] << 8) + bytes[bytes[1] + 6]);
	}

	@Override
	public String getMavlinkId() {
		return compId + "v1";
	}

	@Override
	public byte[] getRaw() {
		ByteBuffer raw = ByteBuffer.allocate(7 + payload.payload().length);

		raw.put((byte) 0xFE);
		raw.put((byte) (len & 0xff));
		raw.put((byte) (seq & 0xff));
		raw.put((byte) (compId & 0xff));
		raw.put((byte) (msgId & 0xff));
		raw.put(payload.payload());
		raw.put((byte) ((checksum >> 8) & 0xff));
		raw.put((byte) (checksum & 0xff));

		return raw.array();
	}

}
