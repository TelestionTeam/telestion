package org.telestion.core.connection.tcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telestion.api.config.Config;
import org.telestion.api.message.JsonMessage;
import org.telestion.core.connection.ConnectionData;
import org.telestion.core.util.Tuple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class TcpClient extends AbstractVerticle {

	@Override
	public void start(Promise<Void> startPromise) {
		config = Config.get(config, config(), Configuration.class);

		var options = new NetClientOptions();
		options.setIdleTimeout(config.timeout() == TcpTimeouts.NO_RESPONSES
				? (int) TcpTimeouts.NO_TIMEOUT : (int) config.timeout());
		currentClient = vertx.createNetClient(options);

		activeClients = new HashMap<>();

		vertx.eventBus().consumer(config.inAddress(), raw -> {
			if (!JsonMessage.on(TcpData.class, raw, this::handleDispatchedMsg)) {
				JsonMessage.on(ConnectionData.class, raw, this::handleMsg);
			}
		});

		startPromise.complete();
	}

	@Override
	public void stop(Promise<Void> stopPromise) {
		stopPromise.complete();
	}

	public record Configuration(@JsonProperty String inAddress,
								@JsonProperty String outAddress,
								@JsonProperty long timeout) implements JsonMessage {
		/**
		 * For json
		 */
		@SuppressWarnings("unused")
		private Configuration() {
			this(null, null, 0L);
		}
	}

	public TcpClient() {
		this(null);
	}

	public TcpClient(Configuration config) {
		this.config = config;
	}

	private void handleDispatchedMsg(TcpData tcpData) {
		var details = tcpData.details();

		// Checks if socket already exists, if not connects to Server and if successful sends data asynchronously
		vertx.executeBlocking(handler -> {
			if (!activeClients.containsKey(new Tuple<>(details.ip(), details.port()))) {
				currentClient.connect(details.port(), details.ip(), h -> {
					if (h.succeeded()) {
						onConnected(h.result());
						handler.complete();
					} else {
						logger.error("Failed to create new NetClient with {}:{}:", details.ip(), details.port(),
								h.cause());
						handler.fail(h.cause());
					}
				});
			} else {
				handler.complete();
			}
		}, handler -> {
			if (handler.succeeded()) {
				logger.debug("Sending data to {}:{}", details.ip(), details.port());
				activeClients.get(new Tuple<>(details.ip(), details.port())).write(Buffer.buffer(tcpData.data()));
			} else {
				logger.warn("Due to an error the packet to {}:{} will be dropped", details.ip(), details.port());
			}
		});
	}

	/**
	 * Called when a new socket is opened.
	 *
	 * @param socket the newly connected socket
	 */
	private void onConnected(NetSocket socket) {
		var ip = socket.remoteAddress().host();
		var port = socket.remoteAddress().port();
		var key = new Tuple<>(ip, port);

		activeClients.put(key, socket);

		logger.info("Connection established ({}:{})", ip, port);

		var packetIdCounter = new AtomicInteger(0);

		socket.handler(buffer -> {
			var packetId = packetIdCounter.getAndIncrement();
			logger.debug("New message received from Server ({}:{}, packetId={})", ip, port, packetId);
			vertx.eventBus().publish(config.outAddress(), new ConnectionData(buffer.getBytes(), new TcpDetails(ip, port,
					packetId)).json());
		});

		socket.exceptionHandler(error -> {
			logger.error("Encountered an unexpected error (Server: {}:{})", ip, port,
					error);
			activeClients.remove(key);
		});

		socket.closeHandler(handler -> {
			logger.info("Closing remote connection with Server ({}:{})", ip, port);
			activeClients.remove(key);
		});
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
	private HashMap<Tuple<String, Integer>, NetSocket> activeClients;
	private NetClient currentClient;

	private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);
}
