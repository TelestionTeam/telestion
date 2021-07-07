package de.wuespace.telestion.services.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.wuespace.telestion.services.message.Address;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.wuespace.telestion.api.message.JsonMessage;
import de.wuespace.telestion.api.config.Config;

/**
 * DataService is a verticle which is the interface to a underlying database implementation.
 * All data requests should come to the DataService and will be parsed and executed.
 * TODO: DataOperations like Integrate, Differentiate, Offset, Sum, ...
 * TODO: MongoDB Queries explanation and implementation in fetchLatestData.
 *
 */
public final class DataService extends AbstractVerticle {
	private final Configuration forcedConfig;
	private Configuration config;

	private final Logger logger = LoggerFactory.getLogger(DataService.class);
	/**
	 * DataService Eventbus Addresses.
	 */
	private final String inSave = Address.incoming(DataService.class, "save");
	private final String inFind = Address.incoming(DataService.class, "find");
	private final String dbSave = Address.incoming(MongoDatabaseService.class, "save");
	private final String dbFind = Address.incoming(MongoDatabaseService.class, "find");

	/**
	 * If this constructor is used, settings have to be specified in the config file.
	 */
	public DataService() {
		this.forcedConfig = null;
	}

	/**
	 * This constructor supplies default options.
	 *
	 * @param dataOperationMap		Map of String->DataOperation for incoming dataRequests
	 */
	public DataService(Map<String, DataOperation> dataOperationMap) {
		this.forcedConfig = new Configuration(dataOperationMap);
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		config = Config.get(forcedConfig, config(), Configuration.class);
		this.registerConsumers();
		startPromise.complete();
	}

	/**
	 * Method to register consumers to the eventbus.
	 */
	private void registerConsumers() {
		vertx.eventBus().consumer(inFind, request -> {
			JsonMessage.on(DataRequest.class, request, req -> {
				this.dataRequestDispatcher(req, res -> {
					if (res.failed()) {
						logger.error(res.cause().getMessage());
						request.fail(-1, res.cause().getMessage());
						return;
					}
					request.reply(res.result());
				});
			});
		});
		vertx.eventBus().consumer(inSave, document -> {
			JsonMessage.on(JsonMessage.class, document, doc -> {
				vertx.eventBus().request(dbSave, doc.json(), res -> {
					if (res.failed()) {
						logger.error(res.cause().getMessage());
						document.fail(-1, res.cause().getMessage());
						return;
					}
					document.reply(res.result().body());
				});
			});
		});
	}

	/**
	 * Parse and dispatch incoming DataRequests.
	 *
	 * @param request			Determines which dataType should be retrieved and if an Operation should be executed.
	 * @param resultHandler		Handles the request to the underlying database. Can be failed or succeeded.
	 */
	private void dataRequestDispatcher(DataRequest request, Handler<AsyncResult<JsonObject>> resultHandler) {
		if (request.operation().isEmpty()) {
			this.fetchLatestData(request.collection(), request.query(), res -> {
				if (res.failed()) {
					resultHandler.handle(Future.failedFuture(res.cause().getMessage()));
					return;
				}
				resultHandler.handle(Future.succeededFuture(res.result()));
			});
		} else {
			var dataOperation = new DataOperation(new JsonObject(), request.operationParams());
			this.fetchLatestData(request.collection(), request.query(), res -> {
				if (res.failed()) {
					resultHandler.handle(Future.failedFuture(res.cause().getMessage()));
					return;
				}
				dataOperation.data().put("data", res.result());
			});
			this.applyManipulation(request.operation(), dataOperation, resultHandler);
		}
	}

	/**
	 * Request data from another verticle and handle the result of the request.
	 *
	 * @param address			Address String of the desired verticle.
	 * @param message			Object to send to the desired verticle.
	 * @param resultHandler		Handles the result of the requested operation.
	 */
	private void requestResultHandler(
			String address, JsonMessage message, Handler<AsyncResult<JsonObject>> resultHandler) {
		JsonObject result = new JsonObject();
		vertx.eventBus().request(address, message, reply -> {
			if (reply.failed()) {
				logger.error(reply.cause().getMessage());
				resultHandler.handle(Future.failedFuture(reply.cause().getMessage()));
				return;
			}
			result.put("data", reply.result().body());
			resultHandler.handle(Future.succeededFuture(result));
		});
	}

	/**
	 * Method to fetch the latest data of a specified data type.
	 *
	 * @param collection		Determines from which collection data should be fetched.
	 * @param query				MongoDB query, can be empty JsonObject if no specific query is needed.
	 * @param resultHandler		Handles the request to the underlying database. Can be failed or succeeded.
	 */
	private void fetchLatestData(String collection, String query,
			Handler<AsyncResult<JsonObject>> resultHandler) {
		DbRequest dbRequest = new DbRequest(collection, query);
		this.requestResultHandler(dbFind, dbRequest, resultHandler);
	}

	/**
	 * Apply data operation to fetched data.
	 *
	 * @param dataOperation		Determines which manipulation should be applied.
	 * @param resultHandler		Handles the request to the data operation verticle. Can be failed or succeeded.
	*/
	private void applyManipulation(String operationAddress, DataOperation dataOperation,
								   Handler<AsyncResult<JsonObject>> resultHandler) {
		this.requestResultHandler(operationAddress, dataOperation, resultHandler);
	}

	private static record Configuration(
			@JsonProperty Map<String, DataOperation> dataOperationMap
	) {
		private Configuration() {
			this(Collections.emptyMap());
		}
	}
}
