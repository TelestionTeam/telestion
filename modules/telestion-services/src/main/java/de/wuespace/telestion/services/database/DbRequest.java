package de.wuespace.telestion.services.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import de.wuespace.telestion.api.message.JsonMessage;

import java.util.Collections;
import java.util.List;

/**
 * Record to provide the structure of a database request.
 *
 * @param collection	String of the desired MongoDB collection.
 * @param query			MongoDB queries are written in JSON.
 *                      This parameter is a String representation of JSONObject. "{ "key": "value" }"
 *                      with key meaning the name of the field in the MongoDB document.
 *                      IN condition: { key: { $in: ["value1", "value2", ...] }}
 *                      AND condition: { key1: "value1", key2: { $lt: value } } with $lt meaning less than
 *                      OR condition: { $or: [{ key1: "value1" }, { key2: { $gt: value2 }}] }
 * @param fields 		List of key Strings in the collection limiting the fields that should be returned.
 * @param sort 			List of key Strings that the returned data should be sorted by.
 * @param limit			Limits the amount of returned entries. -1 equals all entries found.
 * @param skip			Specifies how many entries should be skipped. 0 is default, meaning no entries are skipped.
 * @param aggregate		Field, that should be aggregated.
 * @see <a href="https://docs.mongodb.com/manual/tutorial/query-documents/">MongoDB manual</a> for more information.
 *
 * @author Jan Tischhöfer
 * @version 07-05-2021
 */
public record DbRequest(
		@JsonProperty String collection,
		@JsonProperty String query,
		@JsonProperty List<String> fields,
		@JsonProperty List<String> sort,
		@JsonProperty int limit,
		@JsonProperty int skip,
		@JsonProperty String aggregate) implements JsonMessage {
	private DbRequest() {
		this("", "", Collections.emptyList(), Collections.emptyList(), -1, 0, "");
	}

	public DbRequest(String collection) {
		this(collection, "", Collections.emptyList(), Collections.emptyList(), -1, 0, "");
	}

	public DbRequest(String collection, String query) {
		this(collection, query, Collections.emptyList(), Collections.emptyList(), -1, 0, "");
	}
}
