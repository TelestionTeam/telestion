package org.telestion.protocol.mavlink;

import org.telestion.protocol.mavlink.message.MavlinkMessage;

import java.util.HashMap;

/**
 * Handling the linking processes of the MAVLink-message IDs to the right implementation.<br>
 * Before being able to get the messages by id they must be registered here.<br>
 * <br>
 * <em>Note that this class should be used in a static context.</em>
 *
 * @author Cedric Boes
 * @version 1.0
 */
public final class MessageIndex {
	/**
	 * Actual Map for the linking.
	 */
	private static HashMap<Long, Class<? extends MavlinkMessage>> map = new HashMap<>();

	/**
	 * There shall be no objects.
	 */
	private MessageIndex() throws InstantiationException {
		throw new InstantiationException("There shall be no MessageIndex Object!");
	}

	/**
	 * Registers a new item by linking a new id to a class.<br>
	 * Basically {@link HashMap#put(Object, Object)}.<br>
	 * <br>
	 * <em>Note that if an other {@link MavlinkMessage} has already been linked with the given id an will
	 * {@link IllegalArgumentException} be thrown.</em>
	 *
	 * @param id    of the new {@link MavlinkMessage MavlinkMessage-implementation}
	 * @param clazz Class of the {@link MavlinkMessage} which should be linked
	 * @throws IllegalArgumentException if the given id is already in use
	 */
	public static void put(long id, Class<? extends MavlinkMessage> clazz) {
		if (get(id) != null) {
			throw new IllegalArgumentException("Given ID is already in use!");
		}
		map.put(id, clazz);
	}

	/**
	 * Returns if an id already has been linked to a {@link MavlinkMessage}.
	 *
	 * @param id of the msg
	 * @return whether an id is already associated with a {@link MavlinkMessage}
	 */
	public static boolean isRegistered(long id) {
		return map.containsKey(id);
	}

	/**
	 * Returns the {@link MavlinkMessage MavlinkMessageClass} linked to the given id.<br>
	 * Basically {@link HashMap#get(Object)}.<br>
	 * <br>
	 * <em>Note that if the given id is not registered <code>null</code> will be returned.</em>
	 *
	 * @param id of the {@link MavlinkMessage}
	 * @return {@link Class} of the {@link MavlinkMessage} linked to the given id
	 */
	public static Class<? extends MavlinkMessage> get(long id) {
		return map.get(id);
	}
}
