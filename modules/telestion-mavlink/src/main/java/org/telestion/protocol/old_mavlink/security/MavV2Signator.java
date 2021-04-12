package org.telestion.protocol.old_mavlink.security;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

/**
 * The {@link MavV2Signator} creates a "unique" 7 byte signature for MavlinkV2-Messages in according to the
 * specifications.<br>
 * <br>
 * A signature contains:
 * <ul>
 * <li>1 byte for the linkId</li>
 * <li>6 bytes for the timestamp</li>
 * <li>6 bytes for the actual signature (SHA256 hashed)</li>
 * </ul>
 * <br>
 * <em>This class is designed to be used in a static-context!</em>
 *
 * @author Cedric Boes
 * @version 1.0
 */
public final class MavV2Signator {

	/**
	 * Epoch seconds of 2015/01/01-00:00:00 UTC-Time.
	 */
	private static final long secondJan2015 = OffsetDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();

	/**
	 * There shall be no objects.
	 */
	private MavV2Signator() {
		// There shall be no objects!
	}

	/**
	 * Returns a MAVLink-Timestamp (which is a custom timestamp using 10 microseconds since {@link #secondJan2015} as a
	 * step) of the current time.
	 *
	 * @return MAVLink timestamp of the current time
	 */
	public static byte[] getTimestamp() {
		OffsetDateTime dt = OffsetDateTime.now(ZoneOffset.UTC);
		long time = ((dt.toEpochSecond() - secondJan2015) * 1_000_000 + (int) dt.toInstant().getNano() / 1_000) / 10;

		return new byte[] { (byte) ((time >> 40) & 0xff), (byte) ((time >> 32) & 0xff), (byte) ((time >> 24) & 0xff),
				(byte) ((time >> 16) & 0xff), (byte) ((time >> 8) & 0xff), (byte) (time & 0xff) };
	}

	/**
	 * Creates the raw 6 byte signature (SHA-256 hashed) for the given arguments according to the
	 * MAVLink-specifications.
	 *
	 * @param secretKey key for the SHA-256 hash <em>(&rightarrow; must be exchanged on a secure channel)</em>
	 * @param header    of the MAVLink-Message
	 * @param payload   of the MAVLink-Message
	 * @param crcExtra  for the MAVLink-Message
	 * @param linkId    of the message
	 * @param timestamp for the message
	 * @return first 6 bytes of the SHA-256 hashed signature
	 * @throws NoSuchAlgorithmException at missing massage digest
	 */
	public static byte[] rawSignature(byte[] secretKey, byte[] header, byte[] payload, int crcExtra, short linkId,
			byte[] timestamp) throws NoSuchAlgorithmException {
		ByteBuffer buffer = ByteBuffer
				.allocate(secretKey.length + header.length + payload.length + 3 + timestamp.length);

		buffer.put(secretKey);
		buffer.put(header);
		buffer.put(payload);
		buffer.put((byte) crcExtra);
		buffer.put((byte) (linkId & 0xff));
		buffer.put(timestamp);

		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return Arrays.copyOfRange(digest.digest(buffer.array()), 0, 6);
	}

	/**
	 * Creates the full 13 bytes signature for MAVLinkV2-Messages with a unique timestamp, the hashed signature and the
	 * linkId.
	 *
	 * @param secretKey key for the SHA-256 hash <em>(&rightarrow; must be exchanged on a secure channel)</em>
	 * @param header    of the MAVLink-Message
	 * @param payload   of the MAVLink-Message
	 * @param crcExtra  for the MAVLink-Message
	 * @param linkId    of the message
	 * @return full 13 MAVLink-Signature
	 * @throws NoSuchAlgorithmException at missing massage digest
	 */
	public static byte[] generateSignature(byte[] secretKey, byte[] header, byte[] payload, int crcExtra, short linkId)
			throws NoSuchAlgorithmException {
		byte[] timestamp = getTimestamp();

		ByteBuffer buffer = ByteBuffer.allocate(13);

		buffer.put((byte) (linkId & 0xff));
		buffer.put(timestamp);
		buffer.put(rawSignature(secretKey, header, payload, crcExtra, linkId, timestamp));
		return buffer.array();
	}

}
