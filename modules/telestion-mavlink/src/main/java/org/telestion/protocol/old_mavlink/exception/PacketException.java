package org.telestion.protocol.old_mavlink.exception;

/**
 * A custom implementation of the {@link RuntimeException}.<br>
 * Indicates that something went wrong with the MAVLink-Message-packet (e.g. wrong format).
 *
 * @author Cedric Boes
 * @version 1.0
 * @see RuntimeException
 */
public class PacketException extends RuntimeException {

	/**
	 * SerialVersion UID for v1.0 of this {@link PacketException}.
	 */
	private static final long serialVersionUID = 2256065887188465994L;

	/**
	 * Creates a PacketException.
	 *
	 * @see RuntimeException#RuntimeException()
	 */
	public PacketException() {
		super();
	}

	/**
	 * Creates a PacketException.
	 *
	 * @see RuntimeException#RuntimeException(String)
	 */
	public PacketException(String s) {
		super(s);
	}

	/**
	 * Creates a PacketException.
	 *
	 * @see RuntimeException#RuntimeException(Throwable)
	 */
	public PacketException(Throwable t) {
		super(t);
	}

	/**
	 * Creates a PacketException.
	 *
	 * @see RuntimeException#RuntimeException(String, Throwable)
	 */
	public PacketException(String s, Throwable t) {
		super(s, t);
	}

	/**
	 * Creates a PacketException.
	 *
	 * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
	 */
	public PacketException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
