package org.telestion.protocol.old_mavlink.annotation;

import java.lang.annotation.*;
import org.telestion.protocol.old_mavlink.message.MavlinkMessage;

/**
 * An {@link Annotation} providing more information about a MAVLink-Message.
 *
 * @author Cedric Boes
 * @version 1.0
 * @see MavlinkMessage
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MavInfo {
	/**
	 * Equivalent to the message id of the MAVLink-Specifications.
	 *
	 * @return message id
	 */
	public int id();

	/**
	 * The calculated CRC_EXTRA byte for this message.
	 *
	 * @return CRC_EXTRA byte for this message
	 */
	public int crc();
}
