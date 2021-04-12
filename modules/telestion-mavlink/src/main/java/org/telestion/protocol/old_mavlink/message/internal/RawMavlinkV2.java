package org.telestion.protocol.old_mavlink.message.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.telestion.protocol.old_mavlink.message.MavlinkMessage;
import org.telestion.protocol.old_mavlink.message.MessageIndex;
import org.telestion.protocol.old_mavlink.message.RawPayload;

/**
 * Implementation of {@link RawMavlink} for MAVLink Version 2.
 *
 * @param len           Length of the payload (array-buffer length).
 * @param incompatFlags Represents the incompatible-flags for the MAVLinkV2-packets.<br>
 *                      <br>
 *                      <em>Note that implementation discards packet if it does not understand flag.</em>
 * @param compatFlags   Represents the compatible-flags for the MAVLinkV2-packets <em>(currently not in use by official
 *                      messages)</em>.
 * @param seq           A "unique" id for packages to identify packet loss. Will be incremented for each packet.<br>
 *                      The parser later must identify if there has occurred any packet loss.
 * @param sysId         ID of system (vehicle) sending the message. Used to differentiate systems on network.<br>
 *                      <br>
 *                      <em>Note that the broadcast address 0 may not be used in this field as it is an invalid source
 *                      address.</em>
 * @param compId        ID of component sending the message. Used to differentiate components in a system (e.g.
 *                      autopilot and a camera). Use appropriate values in MAV_COMPONENT.<br>
 *                      <br>
 *                      <em>Note that the broadcast address MAV_COMP_ID_ALL may not be used in this field as it is an
 *                      invalid source address.</em>
 * @param msgId         Id of the {@link MavlinkMessage}. Must be registered in the {@link MessageIndex}.<br>
 *                      Compared to MAVLinkV1 this allows for 3 (unsigned) bytes of message id.
 * @param payload       Actual MAVLink payload bytes of a message.
 * @param checksum      The X.25 checksum for this message.
 * @param linkId        A linkId for this message (usually a channelId to be robust for multisignal usecases).
 * @param signature     The generated signature for this message (or <code>null</code> if {@link #incompatFlags} !=
 *                      0x1).
 * @author Cedric Boes
 * @version 1.1
 * @implNote Support for MAVLink V2
 * @see MavlinkMessage
 * @see MessageIndex
 */
@SuppressWarnings("preview")
public final record RawMavlinkV2(@JsonProperty short len, @JsonProperty short incompatFlags,
		@JsonProperty short compatFlags, @JsonProperty short seq, @JsonProperty short sysId, @JsonProperty short compId,
		@JsonProperty long msgId, @JsonProperty RawPayload payload, @JsonProperty int checksum,
		@JsonProperty short linkId, @JsonProperty byte[] signature) implements RawMavlink {

	/**
	 * There shall be no default constructor for normal people.<br>
	 * This will only be used by the JSON-parser.
	 */
	@SuppressWarnings("unused")
	private RawMavlinkV2() {
		this((short) 0, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, 0L, null, 0, (short) 0, null);
	}

	/**
	 * Creates a new {@link RawMavlinkV2 object} from a byte representation.<br>
	 * To successfully read this, the starting byte <code>(0xFD)</code> must still be included.
	 *
	 * @param bytes raw MAVLink-message
	 */
	public RawMavlinkV2(byte[] bytes) {
		this((short) bytes[1], (short) bytes[2], (short) bytes[3], (short) bytes[4], (short) bytes[5], (short) bytes[6],
				(long) (bytes[7] << 16) + (bytes[8] << 8) + bytes[9],
				new RawPayload(Arrays.copyOfRange(bytes, 10, bytes[1] + 10)),
				(int) (bytes[bytes[1] + 10] << 8) + bytes[bytes[1] + 11],
				(short) (bytes[2] & 0x1) == 0x1 ? bytes[bytes[1] + 12] : 0,
				(byte[]) ((bytes[2] & 0x1) == 0x1 ? Arrays.copyOfRange(bytes, bytes[1] + 12, bytes[1] + 25)
						: new byte[] {}));
	}

	@Override
	public String getMavlinkId() {
		return sysId + "-" + compId + "v2";
	}

	@Override
	public byte[] getRaw() {
		ByteBuffer raw = ByteBuffer.allocate(12 + payload.payload().length + (incompatFlags == 0x01 ? 13 : 0));

		raw.put((byte) 0xFD);
		raw.put((byte) (len & 0xff));
		raw.put((byte) (incompatFlags & 0xff));
		raw.put((byte) (compatFlags & 0xff));
		raw.put((byte) (seq & 0xff));
		raw.put((byte) (sysId & 0xff));
		raw.put((byte) (compId & 0xff));
		raw.put((byte) ((msgId >> 16) & 0xff));
		raw.put((byte) ((msgId >> 8) & 0xff));
		raw.put((byte) (msgId & 0xff));
		raw.put(payload.payload());
		raw.put((byte) ((checksum >> 8) & 0xff));
		raw.put((byte) (checksum & 0xff));

		if (incompatFlags == 0x01) {
			raw.put(signature);
		}

		return raw.array();
	}

}
