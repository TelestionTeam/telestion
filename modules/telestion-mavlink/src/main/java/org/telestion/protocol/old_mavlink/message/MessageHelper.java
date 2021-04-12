package org.telestion.protocol.old_mavlink.message;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.telestion.core.util.UnsafePredicate;
import org.telestion.protocol.old_mavlink.annotation.MavArray;
import org.telestion.protocol.old_mavlink.annotation.MavField;

/**
 * Utility class for {@link MavlinkMessage MavlinkMessages}.<br>
 * At the moment it just supports different length handling.
 *
 * @author Cedric Boes
 * @version 1.0
 */
public final class MessageHelper {

	/**
	 * There shall be no objects. This is a utility class.
	 */
	private MessageHelper() {
	}

	/**
	 * Returns the actual size of a message-object.
	 *
	 * @return actual size
	 */
	public static int length(MavlinkMessage msg) {
		return Arrays.stream(msg.getClass().getRecordComponents())
				.filter(component -> component.isAnnotationPresent(MavField.class))
				.filter(UnsafePredicate.safe(component -> component.getAccessor().invoke(msg) != null))
				.mapToInt(MessageHelper::calcRecordLength).sum();

	}

	/**
	 * Returns the minimum length of this message with all extension excluded.<br>
	 * <br>
	 * <em>Note if {@link #minLength()} and {@link #maxLength()} are equal, there are no extensions!</em>
	 *
	 * @return minimum length of message
	 */
	public static int minLength(Class<? extends MavlinkMessage> msg) {
		return Arrays.stream(msg.getRecordComponents())
				.filter(component -> component.isAnnotationPresent(MavField.class))
				.filter(component -> !component.getAnnotation(MavField.class).extension())
				.mapToInt(MessageHelper::calcRecordLength).sum();
	}

	/**
	 * Returns the maximum length of this message with all extensions in use.<br>
	 * <br>
	 * <em>Note if {@link #minLength()} and {@link #maxLength()} are equal, there are no extensions!</em>
	 *
	 * @return maximum length of message
	 */
	public static int maxLength(Class<? extends MavlinkMessage> msg) {
		return Arrays.stream(msg.getRecordComponents())
				.filter(component -> component.isAnnotationPresent(MavField.class))
				.mapToInt(MessageHelper::calcRecordLength).sum();
	}

	/**
	 * Calculates the length of a {@link RecordComponent} from a {@link MavlinkMessage}.<br>
	 * If the component is an array the size will be calculated with respect to its length.
	 *
	 * @param c RecordComponent for calculation
	 * @return MAVLink-length of the RecordComponent
	 */
	private static int calcRecordLength(RecordComponent c) {
		int multiplier = c.isAnnotationPresent(MavArray.class) ? c.getAnnotation(MavArray.class).length() : 1;
		return multiplier * c.getAnnotation(MavField.class).nativeType().size;
	}
}
