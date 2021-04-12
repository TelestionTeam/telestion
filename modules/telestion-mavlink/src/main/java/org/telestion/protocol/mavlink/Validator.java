package org.telestion.protocol.mavlink;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;

/**
 *
 *
 * @author Cedric Boes
 * @version 1.0
 */
public abstract class Validator extends AbstractVerticle {

	@Override
	public void start(Promise<Void> startPromise) {
		vertx.eventBus().consumer(getInAddress(), this::handleMessage);
		startPromise.complete();
	}

	@Override
	public void stop(Promise<Void> stopPromise) {
		stopPromise.complete();
	}

	/**
	 * <p>
	 *     Each Mavlink-Validator must be able to handle incoming messages to separate the payload from its header.
	 * </p>
	 * <p>
	 *     It must be noted though that the incoming messages are not checked for validation and might not be
	 *     Mavlink-packages in the first place.
	 * </p>
	 *
	 * @param msg raw message which is unchecked
	 */
	protected abstract void handleMessage(Message<?> msg);

	/**
	 * Getter for {@link #inAddress}.
	 *
	 * @return {@link #inAddress}
	 */
	public String getInAddress() {
		return inAddress;
	}

	/**
	 * Getter for {@link #packetOutAddress}.
	 *
	 * @return {@link #packetOutAddress}
	 */
	public String getPacketOutAddress() {
		return packetOutAddress;
	}

	/**
	 * Getter for {@link #parserInAddress}.
	 *
	 * @return {@link #parserInAddress}
	 */
	public String getParserInAddress() {
		return parserInAddress;
	}

	/**
	 * Creates a new {@link Validator} with the given information.
	 *
	 * @param inAddress {@link #inAddress}
	 * @param packetOutAddress {@link #packetOutAddress}
	 * @param parserInAddress {@link #parserInAddress}
	 */
	public Validator(String inAddress, String packetOutAddress, String parserInAddress) {
		this.inAddress = inAddress;
		this.packetOutAddress = packetOutAddress;
		this.parserInAddress = parserInAddress;
	}

	/**
	 * Address on which this {@link AbstractVerticle verticle} receives its inputs.
	 */
	private final String inAddress;

	/**
	 * Address to which the raw input-packet and the parsing-status (whether it was successful or not) will be
	 * published.
	 */
	private final String packetOutAddress;

	/**
	 * Address to which the raw payload (which is the essential part of the Mavlink-Message) will be sent, to be
	 * received by the payload-parser.
	 */
	private final String parserInAddress;
}
