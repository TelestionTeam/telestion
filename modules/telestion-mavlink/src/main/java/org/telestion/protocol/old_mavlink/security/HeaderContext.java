package org.telestion.protocol.old_mavlink.security;

import org.telestion.protocol.old_mavlink.MavlinkParser;
import org.telestion.protocol.old_mavlink.message.MavlinkMessage;
import org.telestion.protocol.old_mavlink.message.internal.RawMavlink;

/**
 * Containing all relevant header-information for the {@link MavlinkParser}.<br>
 * The {@link HeaderContext} is MAVLinkV1- and MAVLinkV2-compatible which means that both formats can put out using
 * this.<br>
 * <br>
 * {@link HeaderContext HeaderContexts} are used to parse a {@link MavlinkMessage} into a {@link RawMavlink
 * RawMavlink-message} again.
 *
 * @author Cedric Boes
 * @version 1.1
 */
public final class HeaderContext {
	/**
	 * Represents the incompatible-flags for the MAVLinkV2-packets.<br>
	 * <br>
	 * <em>Note that implementation discards packet if it does not understand flag.</em>
	 */
	private final short incompFlags;
	/**
	 * Represents the compatible-flags for the MAVLinkV2-packets <em>(currently not in use by official messages)</em>.
	 */
	private final short compFlags;
	/**
	 * ID of system (vehicle) sending the message. Used to differentiate systems on network.<br>
	 * <br>
	 * <em>Note that the broadcast address 0 may not be used in this field as it is an invalid source address.</em>
	 */
	private final short sysId;
	/**
	 * ID of component sending the message. Used to differentiate components in a system (e.g. autopilot and a camera).
	 * Use appropriate values in MAV_COMPONENT.<br>
	 * <br>
	 * <em>Note that the broadcast address MAV_COMP_ID_ALL may not be used in this field as it is an invalid source
	 * address.</em>
	 */
	private final short compId;
	/**
	 * Defines the channel the packets should be designed for.<br>
	 * <br>
	 * <em>Only works with MAVLinkV2 and signing enabled</em>
	 */
	private final short linkId;
	/**
	 * A "unique" id for packages to identify packet loss. Will be incremented for each call of
	 * {@link #getNewPacketSeq()}.
	 */
	private volatile byte seq; // Must be volatile as more than one thread could potentially access this.

	/**
	 * Creates a new {@link HeaderContext} with the given arguments and initializes the sequenceID with 0.
	 */
	public HeaderContext(short incompFlags, short compFlags, short sysId, short compId, short linkId) {
		this.incompFlags = incompFlags;
		this.compFlags = compFlags;
		this.seq = 0;
		this.sysId = sysId;
		this.compId = compId;
		this.linkId = linkId;
	}

	/**
	 * Creates a new pseudo-"unique" packet sequence which will only return after 256 messages.
	 *
	 * @return new "unique" id
	 */
	public synchronized byte getNewPacketSeq() { // Synchronized because more than one thread could potentially access
		return seq++;
	}

	/**
	 * Returns the {@link #incompFlags} for this {@link HeaderContext}.
	 *
	 * @return {@link #incompFlags}
	 */
	public short incompFlags() {
		return incompFlags;
	}

	/**
	 * Returns the {@link #compFlags} for this {@link HeaderContext}.
	 *
	 * @return {@link #compFlags}
	 */
	public short compFlags() {
		return compFlags;
	}

	/**
	 * Returns the {@link #sysId()} for this {@link HeaderContext}.
	 *
	 * @return {@link #sysId}
	 */
	public short sysId() {
		return sysId;
	}

	/**
	 * Returns the {@link #compId} for this {@link HeaderContext}.
	 *
	 * @return {@link #compId}
	 */
	public short compId() {
		return compId;
	}

	/**
	 * Returns the {@link #linkId} for this {@link HeaderContext}.
	 *
	 * @return {@link #linkId}
	 */
	public short linkId() {
		return linkId;
	}

}
