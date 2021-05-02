package org.telestion.protocol.mavlink.message;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.telestion.api.message.JsonMessage;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, property="className")
public interface PacketInformation extends JsonMessage {
}
