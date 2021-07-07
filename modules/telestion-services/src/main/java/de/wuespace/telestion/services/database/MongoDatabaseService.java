package de.wuespace.telestion.services.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.wuespace.telestion.api.message.JsonMessage;
import de.wuespace.telestion.api.config.Config;
import de.wuespace.telestion.services.message.Address;
import io.vertx.core.*;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDatabaseService is a verticle which connects to a local running MongoDB-Database and listens for incoming
 * database requests to process.
 * TODO: Each database implementation (currently only MongoDB) is written in its own DBClient,
 * TODO: but listens to the same address for DBRequests. The address is the interface to the database implementation,
 * TODO: so that the used DB can be replaced easily by spawning another DBClient.
 * Mongo specific:
 * Data is always saved in their exclusive collection which is always named after their Class.name.
 *
 * @author Jan Tischhöfer
 * @version 07-05-2021
 */
public final class MongoDatabaseService extends AbstractVerticle {
	private final Logger logger = LoggerFactory.getLogger(MongoDatabaseService.class);
	private final Configuration forcedConfig;
	private Configuration config;
	private MongoClient client;

	/**
	 * MongoDB Eventbus Addresses.
	 */
	private final String inSave = Address.incoming(MongoDatabaseService.class, "save");
	private final String outSave = Address.outgoing(MongoDatabaseService.class, "save");
	private final String inFind = Address.incoming(MongoDatabaseService.class, "find");
	private final String inAgg = Address.incoming(MongoDatabaseService.class, "aggregate");

	/**
	 * This constructor supplies default options.
	 *
	 * @param dbName			the name of the local running database
	 * @param dbPoolName		the name of the database pool
	 */
	public MongoDatabaseService(String dbName, String dbPoolName) {
		this.forcedConfig = new Configuration(
				new JsonObject().put("db_name", dbName).put("useObjectId", true), dbPoolName);
	}

	/**
	 * If this constructor is used at all, settings have to be specified in the config file.
	 */
	public MongoDatabaseService() {
		this.forcedConfig = null;
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		config = Config.get(forcedConfig, config(), Configuration.class);
		this.client = MongoClient.createShared(vertx, config.dbConfig, config.dbPoolName);
		this.registerConsumers();
		startPromise.complete();
	}

	/**
	 * Method to register consumers to the eventbus.
	 */
	private void registerConsumers() {
		vertx.eventBus().consumer(inSave, document -> {
			JsonMessage.on(JsonMessage.class, document, this::save);
		});
		vertx.eventBus().consumer(inFind, request -> {
			JsonMessage.on(DbRequest.class, request, dbRequest -> {
				this.find(dbRequest, result -> {
					if (result.failed()) {
						request.fail(-1, result.cause().getMessage());
					}
					if (result.succeeded()) {
						request.reply(result.result());
					}
				});
			});
		});
		vertx.eventBus().consumer(inAgg, request -> {
			JsonMessage.on(DbRequest.class, request, dbRequest -> {
				this.aggregate(dbRequest, result -> {
					if (result.failed()) {
						request.fail(-1, result.cause().getMessage());
					}
					if (result.succeeded()) {
						request.reply(result.result());
					}
				});
			});
		});
	}

	/**
	 * Save the received document to the database.
	 * If a MongoDB-ObjectId is specified data will be upserted, meaning if the id does not exist it will be inserted,
	 * otherwise it will be updated. Else it will be inserted with a new id.
	 * Additionally the current date/time is added for future queries regarding date and time.
	 * If the save was successful the database looks for the newly saved document and publishes it to the database
	 * outgoing address concatenated with "/Class.name".
	 * Through this behaviour clients (e.g. GUI) can listen
	 * to the outgoing address of a specific data value and will always be provided with the most recent data.
	 *
	 * @param document a JsonMessage validated through the JsonMessage.on method
	 */
	private void save(JsonMessage document) {
		var object = document.json();
		var dateString = getISO8601StringForDate(new Date());
		object.put("datetime",	new JsonObject().put("$date", dateString));
		client.save(document.className(), object, res -> {
			if (res.failed()) {
				logger.error("DB Save failed: ", res.cause());
				return;
			}
			/*String id = res.result();
			client.find(document.className(), new JsonObject().put("_id", id), rec -> {
				if (rec.failed()) {
					logger.error("DB Find failed: ", rec.cause());
					return;
				}
				DbResponse dbRes = new DbResponse(rec.result());
				vertx.eventBus().publish(outSave.concat("/").concat(document.className()), dbRes.json());
			});*/
		});
	}

	/**
	 * Find the latest entry of the requested data type.
	 *
	 * @param request	{@link de.wuespace.telestion.services.database.DbRequest}
	 * @param handler	result handler, can be failed or succeeded
	 */
	@SuppressWarnings("unused")
	private void findLatest(DbRequest request, Handler<AsyncResult<JsonObject>> handler) {
		client.findWithOptions(request.collection(),
				getJsonQueryFromString(request.query()),
				setFindOptions(request.fields(), List.of("_id"), 1, 0),
				res -> {
					if (res.failed()) {
						logger.error("DB Request failed: ", res.cause());
						handler.handle(Future.failedFuture(res.cause()));
						return;
					}
					var dbRes = new DbResponse(res.result());
					handler.handle(Future.succeededFuture(dbRes.json()));
				});
	}

	/**
	 * Find all requested entries in the MongoDB.
	 *
	 * @param request	query options are defined by {@link de.wuespace.telestion.services.database.DbRequest}.
	 * @param handler	result handler, can be failed or succeeded.
	 */
	private void find(DbRequest request, Handler<AsyncResult<JsonObject>> handler) {
		client.findWithOptions(
				request.collection(),
				getJsonQueryFromString(request.query()),
				setFindOptions(request.fields(), request.sort(), request.limit(), request.skip()),
				res -> {
					if (res.failed()) {
						logger.error("DB Request failed: ", res.cause());
						handler.handle(Future.failedFuture(res.cause()));
						return;
					}
					var dbRes = new DbResponse(res.result());
					handler.handle(Future.succeededFuture(dbRes.json()));
				}
		);
	}

	private void aggregate(DbRequest request, Handler<AsyncResult<JsonObject>> handler) {
		var command = new JsonObject()
				.put("aggregate", request.collection())
				.put("pipeline", new JsonArray());
		command.getJsonArray("pipeline")
				.add(new JsonObject()
						.put("$match", getJsonQueryFromString(request.query())));
		// For each field in specified collection document you need to define the field and the operations
		// Outsource in helper function
		command.getJsonArray("pipeline")
				.add(new JsonObject()
						.put("$group", getGroupStageFromFields(request.aggregate())))
				.add(new JsonObject()
						.put("$project", new JsonObject()
								.put("_id", 0)
								.put("min", "$min")
								.put("avg", "$avg")
								.put("max", "$max")
								.put("last", "$last")
								.put("time", new JsonObject().put("$toLong", "$time"))));
		command.put("cursor", new JsonObject());
		client.runCommand("aggregate", command, result -> {
			if (result.failed()) {
				logger.error("Aggregation failed: ", result.cause().getMessage());
				handler.handle(Future.failedFuture(result.cause()));
				return;
			}
			handler.handle(Future.succeededFuture(result.result()));
		});
	}

	private JsonObject getGroupStageFromFields(String field) {
		var group = new JsonObject().put("_id", "$datetime");
		// calculate avg/min/max for  field
		group.put("min", new JsonObject().put("$min", "$" + field));
		group.put("avg", new JsonObject().put("$avg", "$" + field));
		group.put("max", new JsonObject().put("$max", "$" + field));
		group.put("last", new JsonObject().put("$last", "$" + field));
		group.put("time", new JsonObject().put("$first", "$datetime"));
		return group;
	}

	/**
	 * Helper function to set the {@link io.vertx.ext.mongo.FindOptions}
	 * for the {@link io.vertx.ext.mongo.MongoClient#findWithOptions(String, JsonObject, FindOptions)}.
	 *
	 * @param limit		Limits the amount of returned entries. -1 equals all entries found.
	 * @param skip		Specifies if and how many entries should be skipped.
	 * @return			{@link io.vertx.ext.mongo.FindOptions} for the MongoClient.
	 */
	private FindOptions setFindOptions(List<String> fields, List<String> sort, int limit, int skip) {
		return setFindOptions(fields, sort).setLimit(limit).setSkip(skip);
	}

	/**
	 * Helper function to set the {@link io.vertx.ext.mongo.FindOptions}
	 * for the {@link io.vertx.ext.mongo.MongoClient#findWithOptions(String, JsonObject, FindOptions)}.
	 *
	 * @param fields	List of key Strings in the collection limiting the fields that should be returned.
	 * @param sort		List of key Strings that the returned data should be sorted by.
	 * @return			{@link io.vertx.ext.mongo.FindOptions} for the MongoClient.
	 */
	private FindOptions setFindOptions(List<String> fields, List<String> sort) {
		FindOptions findOptions = new FindOptions();
		if (!fields.isEmpty()) {
			JsonObject jsonFields = new JsonObject();
			fields.forEach(f -> jsonFields.put(f, true));
			findOptions.setFields(jsonFields);
		}
		if (!sort.isEmpty()) {
			JsonObject jsonSort = new JsonObject();
			sort.forEach(s -> jsonSort.put(s, -1));
			findOptions.setSort(jsonSort);
		}
		return findOptions;
	}

	/**
	 * Helper function to parse a query string to a {@link io.vertx.core.json.JsonObject}.
	 *
	 * @param query	JSON String - "{"key":"value"}, ..."
	 * @return {@link io.vertx.core.json.JsonObject} query for
	 * {@link io.vertx.ext.mongo.MongoClient#findWithOptions(String, JsonObject, FindOptions)}
	 */
	private JsonObject getJsonQueryFromString(String query) {
		if (query.isEmpty()) {
			return new JsonObject("{}");
		} else {
			try {
				return new JsonObject(query);
			} catch (DecodeException e) {
				logger.error("No valid JSON String: ".concat(e.getMessage()).concat("\nReturning empty JsonObject."));
				return new JsonObject();
			}
		}
	}

	/**
	 * Helper function to convert a {@link java.util.Date} to a ISO-8601 Date/Time string.
	 *
	 * @param date	{@link java.util.Date} that should be converted.
	 * @return	ISO-8601 Date/Time string representation
	 */
	private static String getISO8601StringForDate(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.GERMANY);
		dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
		var dateString = dateFormat.format(date);
		return dateFormat.format(date);
	}

	private static record Configuration(@JsonProperty JsonObject dbConfig, @JsonProperty String dbPoolName) {
		private Configuration() {
			this(new JsonObject(), "");
		}
	}
}
