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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verticle periodically querying the database for preconfigured data,
 * publishing the results to a preconfigured outgoing address.
 *
 * @author Jan Tischhöfer
 * @version 07-05-2021
 */
public final class PeriodicDataPublisher extends AbstractVerticle {
	private final Configuration forcedConfig;
	private Configuration config;

	private final Logger logger = LoggerFactory.getLogger(PeriodicDataPublisher.class);
	private Handler<Long> reqTimer;
	private Long timer;
	private DbRequest dbRequest;
	private String timeOfLastDataSet = null;

	/**
	 * MongoDB Eventbus Address.
	 */
	private final String db = Address.incoming(MongoDatabaseService.class, "find");
	private final String agg = Address.incoming(MongoDatabaseService.class, "aggregate");

	/**
	 * This constructor supplies default options.
	 *
	 * @param collection	the name of the MongoDB collection (table in SQL databases)
	 * @param rate			the desired rate (1 per rate milliseconds) at which the data should be queried
	 * @param outAddress	the desired outgoing address of the periodic data
	 */
	public PeriodicDataPublisher(String collection, int rate, String outAddress, String aggregate) {
		this.forcedConfig = new Configuration(
				collection,
				"",
				Collections.emptyList(),
				Collections.emptyList(),
				rate,
				outAddress,
				aggregate
		);
	}

	/**
	 * If this constructor is used, settings have to be specified in the config file.
	 */
	public PeriodicDataPublisher() { this.forcedConfig = null; }

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		config = Config.get(forcedConfig, config(), Configuration.class);
		dbRequest = getDbRequestFromConfig();
		timer = getRateInMillis(config.rate());
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
		if (timeOfLastDataSet != null) {
			var dateQuery = new JsonObject()
				.put("datetime", new JsonObject().put("$gt", new JsonObject().put("$date", timeOfLastDataSet)));
			dbRequest = getDbRequestFromConfig(dateQuery.toString());
		}
		vertx.eventBus().request(agg, dbRequest.json(), (Handler<AsyncResult<Message<JsonObject>>>) reply -> {
			if (reply.failed()) {
				logger.error(reply.cause().getMessage());
				return;
			}

			logger.info(reply.result().body().toString());
			var jArr = reply.result().body().getJsonArray("result");
			// Set timeOfLastDataSet to the datetime of the last received data
			// timeOfLastDataSet = jArr.getJsonObject(jArr.size()-1).getJsonObject("datetime").getString("$date");
			// vertx.eventBus().publish(config.outAddress(), jArr);
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
				config.query(),
				config.fields(),
				config.sort(),
				-1,
				0,
				config.aggregate()
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
				config.fields(),
				config.sort(),
				-1,
				0,
				config.aggregate()
		);
	}

	/**
	 * Helper function to turn rate into milliseconds.
	 *
	 * @param rate the desired data rate
	 * @return milliseconds of (1/rate)
	 */
	private static long getRateInMillis(int rate) {
		BigDecimal bd = new BigDecimal((double) (1 / rate));
		bd = bd.setScale(3, RoundingMode.HALF_UP);
		return (long) bd.doubleValue() * 1000L;
	}

	private static record Configuration(
			@JsonProperty String collection,
			@JsonProperty String query,
			@JsonProperty List<String> fields,
			@JsonProperty List<String> sort,
			@JsonProperty int rate,
			@JsonProperty String outAddress,
			@JsonProperty String aggregate
	) {
		private Configuration() {
			this("", "", Collections.emptyList(), Collections.emptyList(), 0, "", "");
		}
	}
}
