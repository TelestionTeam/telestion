package org.telestion.protocol.mavlink;

import io.vertx.core.eventbus.EventBus;

/**
 * <p>
 *     A simple Factory which can create and register all necessary modules for a functioning MavlinkAdapter.<br>
 *     Although there is still the possibility of doing the work manually, it is recommended to use this designated
 *     factory.
 * </p>
 * <p>
 *     Part of a correctly working MavlinkAdapter are usually:
 *     <ul>
 *         <li>{@link Validator}</li>
 *     </ul>
 * </p>
 *
 * @author Cedric Boes
 * @version 1.0
 */
public final class MavlinkAdapterFactory {

	/**
	 * <p>
	 *     Configuration with which this factory can create all modules for a functioning MavlinkAdapter.
	 * </p>
	 */
	public final record Configuration() {

	}

	/**
	 * <p>
	 *     Creates all necessary parts for a functioning MavlinkAdapter and registers them on the bus.
	 * </p>
	 * <p>
	 *     Modules which will be created and registered:
	 *     <ul>
	 *         <li>{@link Validator}</li>
	 *     </ul>
	 * </p>
	 *
	 * @param bus {@link EventBus} on which the modules should be registered
	 * @param config which specifies all necessary details for a MavlinkAdapter
	 */
	public static void registerNew(EventBus bus, Configuration config) {

	}

	/**
	 * There shall be no objects of this class!
	 */
	private MavlinkAdapterFactory() {}
}
