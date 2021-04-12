package org.telestion.protocol.mavlink.dummy;

import org.telestion.api.message.JsonMessage;

/**
 * This is a temporary record before the network adapter update will be completed.
 *
 * @author Cedric
 * @version 1.0
 */
public record NetPacket(byte[] raw) implements JsonMessage {

}
