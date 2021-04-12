package org.telestion.protocol.old_mavlink.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.telestion.api.message.JsonMessage;
import org.telestion.protocol.old_mavlink.annotation.MavInfo;
import org.telestion.protocol.old_mavlink.exception.AnnotationMissingException;
import org.telestion.protocol.old_mavlink.security.X25Checksum;

/**
 * Superclass for all MavlinkMessages.<br>
 * Implementations will be created automatically by the xml2record-tool.
 *
 * @author Cedric Boes
 * @version 1.0
 */
public interface MavlinkMessage extends JsonMessage {

	/**
	 * Checks if the necessary {@link MavInfo MavInfo-Annotation} is present.<br>
	 * If so a representing Object will be returned otherwise an {@link AnnotationMissingException} will be thrown.
	 *
	 * @return {@link MavInfo} of this class if present
	 * @throws AnnotationMissingException if {@link MavInfo} is not present
	 * @implNote Should not be overwritten!
	 */
	@JsonProperty(access = Access.READ_ONLY)
	default MavInfo checkAnnotation() {
		if (!this.getClass().isAnnotationPresent(MavInfo.class)) {
			throw new AnnotationMissingException("Required Annotation @MavInfo is missing!");
		} else {
			return this.getClass().getAnnotation(MavInfo.class);
		}
	}

	/**
	 * Returns the CRC_EXTRA-Byte for the {@link MavlinkMessage} declared in the {@link MavInfo}.<br>
	 * It is used for the checksum-calculation based on the {@link X25Checksum} algorithm (also know as
	 * <code>CRC-16-CCITT</code>).<br>
	 * <br>
	 * For more information see:<br>
	 * <a href="https://mavlink.io/en/guide/serialization.html#crc_extra">
	 * https://mavlink.io/en/guide/serialization.html#crc_extra</a>
	 *
	 * @return CRC_EXTRA-Byte for the {@link MavlinkMessage}
	 * @implNote Should not be overwritten!
	 */
	@JsonProperty(access = Access.READ_ONLY)
	default int getCrc() {
		return checkAnnotation().crc();
	}

	/**
	 * Returns the id of the {@link MavlinkMessage} declared in the {@link MavInfo}.<br>
	 * it is used to identify the message which was sent to cast the payload.<br>
	 * <br>
	 * For more information see:<br>
	 * <a href="https://mavlink.io/en/guide/serialization.html">https://mavlink.io/en/guide/serialization.html</a>
	 *
	 * @return id of the {@link MavlinkMessage}
	 * @implNote Should not be overwritten!
	 */
	@JsonProperty(access = Access.READ_ONLY)
	default int getId() {
		return checkAnnotation().id();
	}

}
