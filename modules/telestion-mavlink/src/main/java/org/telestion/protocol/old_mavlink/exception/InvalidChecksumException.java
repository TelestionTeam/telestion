package org.telestion.protocol.old_mavlink.exception;

/**
 * A custom implementation of the {@link RuntimeException}.<br>
 * Indicates that the X.25 checksum of a MAVLink-Message is invalid.
 *
 * @author Cedric Boes
 * @version 1.0
 * @see RuntimeException
 */
public class InvalidChecksumException extends RuntimeException {

	/**
	 * SerialVersion UID for v1.0 of the {@link InvalidChecksumException}.
	 */
	private static final long serialVersionUID = 5920137939040604788L;

	/**
	 * Creates an InvalidChecksumException.
	 *
	 * @see RuntimeException#RuntimeException()
	 */
	public InvalidChecksumException() {
		super();
	}

	/**
	 * Creates an InvalidChecksumException.
	 *
	 * @see RuntimeException#RuntimeException(String)
	 */
	public InvalidChecksumException(String s) {
		super(s);
	}

	/**
	 * Creates an InvalidChecksumException.
	 *
	 * @see RuntimeException#RuntimeException(Throwable)
	 */
	public InvalidChecksumException(Throwable t) {
		super(t);
	}

	/**
	 * Creates an InvalidChecksumException.
	 *
	 * @see RuntimeException#RuntimeException(String, Throwable)
	 */
	public InvalidChecksumException(String s, Throwable t) {
		super(s, t);
	}

	/**
	 * Creates an InvalidChecksumException.
	 *
	 * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
	 */
	public InvalidChecksumException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
