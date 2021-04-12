package org.telestion.protocol.mavlink;

/**
 *
 * @param <T>
 */
public interface TypeParser<T> {
	/**
	 *
	 * @param payload
	 * @param arraySize
	 * @return
	 */
	default T parse(byte[] payload, int arraySize) {
		return parse(payload, arraySize, 0);
	}

	/**
	 *
	 * @param payload
	 * @param arraySize
	 * @param offset
	 * @return
	 */
	T parse(byte[] payload, int arraySize, int offset);
}
