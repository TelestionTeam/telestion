package de.wuespace.telestion.services.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import de.wuespace.telestion.api.message.JsonMessage;

public record DataRequest(
		@JsonProperty String collection,
		@JsonProperty String query,
		@JsonProperty String operation,
		@JsonProperty JsonObject operationParams) implements JsonMessage {
			private DataRequest() {
				this("", "", "", new JsonObject());
			}
}
