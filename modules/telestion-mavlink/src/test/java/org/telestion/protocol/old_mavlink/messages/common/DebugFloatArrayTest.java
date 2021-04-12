package org.telestion.protocol.old_mavlink.messages.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.telestion.protocol.old_mavlink.messages.official.common.DebugFloatArray;

public class DebugFloatArrayTest {

	@Test
	void testExist() {
		assertDoesNotThrow(
				() -> Class.forName("org.telestion.protocol.mavlink.messages.official.common" + ".DebugFloatArray")
						.getConstructor(long.class, int.class, int[].class, int[].class).newInstance(0, 0, null, null));
	}

	@Test
	void testCrc() {
		var msg = new DebugFloatArray(0, 0, null, null);
		assertThat(msg.getCrc(), is(232));
	}

}
