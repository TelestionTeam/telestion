package org.telestion.protocol.mavlink.security;

/**
 * This class is handling the creation of the X.25 (CRC-16-CCITT) custom checksum for MAVLink.<br>
 * Apart from only hashing the message itself a CRC_EXTRA byte must be added which is unique for each message. Only 2
 * bytes <em>(last 2 bytes)</em> of the final hash will then be used.<br>
 * <br>
 * Implementation is based on the <a href=https://github.com/mavlink/c_library_v2/blob/master/checksum.h>official c
 * implementation</a> for MAVLink.
 *
 * @author Cedric Boes
 * @version 1.0
 */
public final class X25Checksum {

	/**
	 * Initial value for the CRC-Calculation.<br>
	 * This is needed because for each byte of the message the current-checksum gets "reused".
	 */
	public static final int INIT_CRC = 0xffff;

	/**
	 * There shall be no objects.
	 */
	private X25Checksum() {
		/* There shall be no objects! */
	}

	/**
	 * Calculates the CRC X.25-checksum for one byte with regards to the current-checksum.
	 *
	 * @param data       byte which should be added to the checksum
	 * @param currentCrc current checksum which should be extended
	 * @return checksum composed from a prior checksum and a byte of data
	 */
	public static int calculate(int data, int currentCrc) {
		data = data ^ (currentCrc & 0xff);
		data ^= (data << 4) & 0xff;
		data &= 0xff;
		return (((currentCrc >> 8) ^ (data << 8) ^ (data << 3) ^ (data >> 4))) & 0xffff;
	}

	/**
	 * Calculates the CRC X.25-checksum for a whole byte[] array.<br>
	 * <em>Must contain the CRC_EXTRA byte which must be added for each message.</em>
	 *
	 * @param buffer of data to calculate a checksum for
	 * @return checksum for the whole buffer
	 */
	public static int calculate(byte[] buffer) {
		int currentCrc = INIT_CRC;
		for (byte b : buffer) {
			currentCrc = calculate(b, currentCrc);
		}
		return currentCrc & 0xffff;
	}

	/**
	 * Calculates the CRC X.25-checksum for a given payload array and the right crc-extra extra byte.<br>
	 *
	 * @param payload data to calculate the checksum for
	 * @param crc crc-byte which must be added
	 * @return checksum for the payload
	 */
	public static int calculate(byte[] payload, int crc) {
		var buffer = new byte[payload.length + 1];
		System.arraycopy(payload, 0, buffer, 0, payload.length);
		buffer[payload.length] = (byte) crc;
		return calculate(buffer);
	}
}
