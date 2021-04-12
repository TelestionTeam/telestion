package org.telestion.protocol.old_mavlink;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Linking an address and a port of a net-address.
 *
 * @param address Actual address of a net-address.
 * @param port    Port of a net-address.
 *
 * @author Cedric Boes
 * @version 1.0
 */
@SuppressWarnings("preview")
public final record AddressPort(@JsonProperty String address, @JsonProperty int port) {

	/**
	 * There shall be no default constructor for normal people.<br>
	 * This will only be used by the JSON-parser.
	 */
	@SuppressWarnings("unused")
	private AddressPort() {
		this(null, 0);
	}
}
