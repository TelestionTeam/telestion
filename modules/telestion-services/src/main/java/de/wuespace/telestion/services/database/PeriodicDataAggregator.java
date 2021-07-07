package de.wuespace.telestion.services.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.wuespace.telestion.api.config.Config;
import de.wuespace.telestion.services.message.Address;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verticle periodically querying the database for preconfigured data,
 * publishing the results to a preconfigured outgoing address.
 *
 * @author Jan Tischhöfer
 * @version 07-05-2021
 */
public final class PeriodicDataAggregator extends AbstractVerticle {
	private final Configuration forcedConfig;
	private Configuration config;

	private final Logger logger = LoggerFactory.getLogger(PeriodicDataAggregator.class);
	private Handler<Long> reqTimer;
	private Long timer;
	private DbRequest dbRequest;
	private String timeOfLastDataSet;

	/**
	 * MongoDB Eventbus Address.
	 */
	private final String agg = Address.incoming(MongoDatabaseService.class, "aggregate");

	/**
	 * This constructor supplies default options.
	 *
	 * @param collection	the name of the MongoDB collection (table in SQL databases)
	 * @param rate			the desired rate (1 per rate milliseconds) at which the data should be queried
	 * @param outAddress	the desired outgoing address of the periodic data
	 */
	public PeriodicDataAggregator(String collection, String field, int rate, String outAddress) {
		this.forcedConfig = new Configuration(
				collection, field, rate, outAddress
		);
	}

	/**
	 * If this constructor is used, settings have to be specified in the config file.
	 */
	public PeriodicDataAggregator() { this.forcedConfig = null; }

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		config = Config.get(forcedConfig, config(), Configuration.class);
		dbRequest = getDbRequestFromConfig();
		timer = getRateInMillis(config.rate());
		logger.info("Timer is: " + String.valueOf(timer));
		timeOfLastDataSet = getISO8601StringForDate(new Date());
		reqTimer = id -> {
			databaseRequest();
			vertx.setTimer(timer, reqTimer);
		};
		vertx.setTimer(timer, reqTimer);
		startPromise.complete();
	}

	/**
	 * Function to request the preconfigured DbRequest.
	 */
	private void databaseRequest() {
		var dateQuery = new JsonObject()
				.put("datetime", new JsonObject().put("$gt", new JsonObject().put("$date", timeOfLastDataSet)));
		dbRequest = getDbRequestFromConfig(dateQuery.toString());
		vertx.eventBus().request(agg, dbRequest.json(), (Handler<AsyncResult<Message<JsonObject>>>) reply -> {
			if (reply.failed()) {
				logger.error(reply.cause().getMessage());
				return;
			}
			logger.info(reply.result().body().toString());
			if (reply.result().body() != null) {
				var jArr = reply.result().body().getJsonObject("cursor").getJsonArray("firstBatch");
				if (!jArr.isEmpty()) {
					// Set timeOfLastDataSet to the datetime of the last received data
					long unixTs = Long.parseLong(jArr.getJsonObject(jArr.size()-1).getString("time"));
					timeOfLastDataSet = getISO8601StringForDate(new Date(unixTs));
					vertx.eventBus().publish(config.outAddress(), jArr);
				}
			}
		});
	}

	/**
	 * Function to create DbRequest from config.
	 *
	 * @return {@link de.wuespace.telestion.services.database.DbRequest}
	 */
	private DbRequest getDbRequestFromConfig() {
		// TODO: Make parameters optional and config easier.
		return new DbRequest(
				config.collection(),
				"",
				Collections.emptyList(),
				Collections.emptyList(),
				-1,
				0,
				config.field()
		);
	}

	/**
	 * Function to create DbRequest from config with a new query containing e.g. the new last date/time.
	 *
	 * @param query	new query in JSON String representation
	 * @return {@link de.wuespace.telestion.services.database.DbRequest}
	 */
	private DbRequest getDbRequestFromConfig(String query) {
		return new DbRequest(
				config.collection(),
				query,
				Collections.emptyList(),
				Collections.emptyList(),
				-1,
				0,
				config.field()
		);
	}

	/**
	 * Helper function to turn rate into milliseconds.
	 *
	 * @param rate the desired data rate
	 * @return milliseconds of (1/rate)
	 */
	private static long getRateInMillis(int rate) {
		return (long) ((1.0 / rate) * 1000.5);
	}

	private static String getISO8601StringForDate(Date date) {
		// TODO: fix time zone issues, maybe use newer date implementation
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.GERMANY);
		dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
		return dateFormat.format(date);
	}

	private static record Configuration(
			@JsonProperty String collection,
			@JsonProperty String field,
			@JsonProperty int rate,
			@JsonProperty String outAddress
	) {
		private Configuration() {
			this("", "", 0, "");
		}
	}
}
