package org.telestion.protocol.old_mavlink.security;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SecretKeySafe} for the MAVLink-Signature-Keys. To ensure security the key will be stored in a final byte
 * array which can be overwritten. It can only be accessed by {@link #getSecretKey()}.<br>
 * <br>
 * It's key must be deleted manually after using with {@link #deleteKey()}!<br>
 * <br>
 * Each {@link SecretKeySafe} has a unique ID to identify it in the logs.
 *
 * @author Cedric Boes
 * @version 1.0
 */
public final class SecretKeySafe {

	/**
	 * Is used to create uniqueIds. They are needed to identify each SecretKeySafe in the logger.
	 */
	private static long globalId = 0;

	/**
	 * Id to identify this {@link SecretKeySafe} in the logs.
	 */
	private final long id;
	/**
	 * Logs all important messages for a SecretKeySafe.
	 */
	private final Logger logger = LoggerFactory.getLogger(SecretKeySafe.class);
	/**
	 * The actual secretKey. This is an array to delete all traces of the key in memory after freeing it.
	 */
	private byte[] secretKey;

	/**
	 * Creates a new {@link SecretKeySafe} with a new {@link #secretKey}.<br>
	 * <br>
	 * <em>Keys are final and cannot be changed. This however means after calling {@link #deleteKey()} this
	 * {@link SecretKeySafe} is no longer usable which is a security feature.</em>
	 *
	 * @param secretKey the secret key
	 */
	public SecretKeySafe(byte[] secretKey) {
		this.secretKey = secretKey;
		this.id = getNewId();
		logger.debug("Creating new instance of the SecretKeySafe ({})!", id);
	}

	/**
	 * Creates a new unique id for a {@link SecretKeySafe}.
	 *
	 * @return new unique id
	 */
	private static long getNewId() {
		return globalId++;
	}

	/**
	 * Returns the secretKey saved in this {@link SecretKeySafe}.<br>
	 * <br>
	 * Will return null after {@link #deleteKey()} has been called.
	 *
	 * @return the stored secret key
	 */
	public byte[] getSecretKey() {
		return secretKey;
	}

	/**
	 * Returns the id of this {@link SecretKeySafe}.
	 *
	 * @return {@link #id}
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns if {@link #deleteKey()} has already been called on this object and secretKey has already been deleted.
	 *
	 * @return if secretKey has already been deleted
	 */
	public boolean isDeleted() {
		return secretKey == null;
	}

	/**
	 * Clears the password from memory and runs the {@link System#gc() Garbage-Collector}.<br>
	 * This ensures security for passwords when deleting passwords.<br>
	 * <br>
	 * Will only work if the secretKey is not already <code>null</code>.
	 */
	public void deleteKey() {
		if (!isDeleted()) {
			Arrays.fill(secretKey, (byte) 0x0);

			secretKey = null;
			System.gc();

			logger.debug("Key has been deleted from the SecretKeySafe ({}) successfully. This instance will no longer "
					+ "be usable.", id);
		} else {
			logger.warn("SecretKey has already been deleted from this SecretKeySafe ({})!", id);
		}
	}

}
