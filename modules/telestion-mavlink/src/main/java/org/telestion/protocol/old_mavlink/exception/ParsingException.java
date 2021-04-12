package org.telestion.protocol.old_mavlink.exception;

/**
 * A custom implementation of the {@link RuntimeException}.<br>
 * Indicates that something went wrong in the parsing-process of a MAVLink-Message.
 *
 * @author Cedric Boes
 * @version 1.0
 * @see RuntimeException
 */
public class ParsingException extends RuntimeException {

	/**
	 * SerialVersion UID for v1.0 of this {@link ParsingException}.
	 */
	private static final long serialVersionUID = -7127168456897713093L;

	/**
	 * Creates a ParsingException.
	 *
	 * @see RuntimeException#RuntimeException()
	 */
	public ParsingException() {
		super();
	}

	/**
	 * Creates a ParsingException.
	 *
	 * @see RuntimeException#RuntimeException(String)
	 */
	public ParsingException(String message) {
		super(message);
	}

	/**
	 * Creates a ParsingException.
	 *
	 * @see RuntimeException#RuntimeException(Throwable)
	 */
	public ParsingException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a ParsingException.
	 *
	 * @see RuntimeException#RuntimeException(String, Throwable)
	 */
	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a ParsingException.
	 *
	 * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
	 */
	public ParsingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
