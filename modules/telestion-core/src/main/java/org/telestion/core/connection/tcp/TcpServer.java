package org.telestion.core.connection.tcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telestion.api.config.Config;
import org.telestion.api.message.JsonMessage;
import org.telestion.core.connection.ConnectionData;
import org.telestion.core.util.Tuple;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <b>An implementation of an unencrypted TCP-Server.</b>
 * <p>
 *     Features are:
 *     <ul>
 *         <li>Opening new connections to TCP-Clients</li>
 *         <li>Receiving data from all the open connections</li>
 *         <li>Keep the connections open until either no package gets sent for a certain amount of time (timeout) or
 *         the Client closes the connection</li>
 *         <li>Sending answers back to the Client if the connections are still open</li>
 *     </ul>
 * </p>
 */
public final class TcpServer extends AbstractVerticle {

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		config = Config.get(config, config(), Configuration.class);

		// Unencrypted -> TODO: Implement functionality for encryption
		var serverOptions = new NetServerOptions();
		serverOptions.setHost(config.hostAddress());
		serverOptions.setPort(config.port());
		serverOptions.setIdleTimeout(config.clientTimeout() == TcpTimeouts.NO_RESPONSES
				? (int) TcpTimeouts.NO_TIMEOUT : (int) config.clientTimeout());
		serverOptions.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
		activeCons = new HashMap<>();

		server = vertx.createNetServer(serverOptions);
		server.connectHandler(this::onConnected);
		server.exceptionHandler(handler -> logger.error("Encountered an unexpected error (Config: {})", config.json(),
				handler));
		server.listen(h -> complete(h, startPromise,
				r -> logger.info("Successfully started. Running on {}:{}", config.hostAddress(), r.actualPort())));

		vertx.eventBus().consumer(config.inAddress, raw -> {
			if (!JsonMessage.on(TcpData.class, raw, this::handleDispatchedMsg)) {
				JsonMessage.on(ConnectionData.class, raw, this::handleMsg);
			}
		});

		startPromise.complete();
	}

	@Override
	public void stop(Promise<Void> stopPromise) throws Exception {
		if (server != null) {
			activeCons.values().forEach(net -> net.close(stopPromise));
			logger.info("Closing Server on {}:{}", config.hostAddress(), config.port());
			server.close();
		}
		stopPromise.complete();
	}

	/**
	 * Configuration for this Verticle which can be loaded from a config.<br>
	 * An optional timeout can be specified which is the consecutive time without any packets incoming or outgoing after
	 * which a client will be disconnected. Special timeouts can be found in {@link TcpTimeouts}.
	 *
	 * @param inAddress		address on which the verticle listens on
	 * @param outAddress	address on which the verticle publishes
	 * @param hostAddress	host-address on which the Server-Socket should run
	 * @param port			port on which the Server-Socket should run
	 * @param clientTimeout time until timeout
	 */
	public record Configuration(@JsonProperty String inAddress,
								@JsonProperty String outAddress,
								@JsonProperty String hostAddress,
								@JsonProperty int port,
								@JsonProperty long clientTimeout) implements JsonMessage {

		/**
		 * Used for reflection.
		 */
		@SuppressWarnings("unused")
		private Configuration() {
			this(null, null, null, 0, 0L);
		}

		public Configuration(String inAddress, String outAddress, String hostAddress, int port) {
			this(inAddress, outAddress, hostAddress, port, TcpTimeouts.DEFAULT_TIMEOUT);
		}
	}

	public TcpServer() {
		this(null);
	}

	public TcpServer(Configuration config) {
		if (config != null && (config.hostAddress() == null || config.hostAddress().equals(""))) {
			config = new Configuration(config.inAddress(), config.outAddress(), "localhost", config.port(),
					config.clientTimeout());
		}
		this.config = config;
	}

	public Configuration getConfig() {
		return this.config;
	}

	// Public for own Dispatcher implementations
	public boolean isActiveCon(Tuple<String, Integer> key) {
		return activeCons.containsKey(key);
	}

	// @jvpichowski this is similar to TcpClient#onConnected, do you want to put this into one method?
	private void onConnected(NetSocket netSocket) {
		var ip = netSocket.remoteAddress().hostAddress();
		var port = netSocket.remoteAddress().port();
		var key = new Tuple<>(ip, port);

		logger.info("Connection established with {}:{}", ip, port);
		activeCons.put(key, netSocket);

		var packetIdCounter = new AtomicInteger(0);

		netSocket.handler(buffer -> {
			var packetId = packetIdCounter.getAndIncrement();
			logger.debug("New message received from Client ({}:{}, packetId={})", ip, port, packetId);
			vertx.eventBus().publish(config.outAddress(), new ConnectionData(buffer.getBytes(), new TcpDetails(ip, port,
					packetId)).json());
			if (config.clientTimeout() == TcpTimeouts.NO_RESPONSES) {
				netSocket.close();
			}
		});

		netSocket.exceptionHandler(error -> {
			logger.error("Encountered an unexpected error (Client: {}:{})", ip, port,
					error);
			activeCons.remove(key);
		});

		netSocket.closeHandler(handler -> {
			logger.info("Closing remote connection with {}:{}", ip, port);
			activeCons.remove(key);
		});
	}

	/**
	 * Completes a promise based on the success of a {@link AsyncResult}. If it was successful a handler will be called.
	 *
	 * @param result  the result which is observed
	 * @param promise the promise which will be completed
	 * @param handler the handler which will be executed on a successful result
	 * @param <T>     the type of the result
	 */
	private static <T> void complete(AsyncResult<T> result, Promise<?> promise, Handler<T> handler) {
		if (result.failed()) {
			promise.fail(result.cause());
			return;
		}
		handler.handle(result.result());
		promise.tryComplete();
	}

	private void handleDispatchedMsg(TcpData data) {
		var details = data.details();

		var element = activeCons.get(new Tuple<>(details.ip(), details.port()));

		// Might be useful to send this to the TCP-Client however the config must be varied for this (future update?)
		if (element == null) {
			logger.warn("Requested connection {}:{} is (no longer) available. Packet will be dropped.",
					data.details().ip(), data.details().port());
			return;
		}

		logger.debug("Sending message to {}:{}", details.ip(), details.port());

		if (element.writeQueueFull()) {
			logger.error("Write queue of socket is full ({}:{}). Packet will be dropped.", details.ip(),
					details.port());
			return;
		}

		element.write(Buffer.buffer(data.data()));
	}

	private void handleMsg(ConnectionData data) {
		var details = data.conDetails();
		if (details instanceof TcpDetails tcpDet) {
			handleDispatchedMsg(new TcpData(data.rawData(), tcpDet));
		} else {	// Shouldn't happen due to Dispatcher
			logger.warn("Wrong connection detail type received. Packet will be dropped.");
			// If there will be ever a logger for broken packets, send this
		}
	}

	private Configuration config;
	private NetServer server;
	private HashMap<Tuple<String, Integer>, NetSocket> activeCons;

	private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
}
