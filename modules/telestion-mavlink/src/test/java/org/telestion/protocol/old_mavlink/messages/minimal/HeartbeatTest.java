package org.telestion.protocol.old_mavlink.messages.minimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.telestion.protocol.old_mavlink.messages.official.minimal.Heartbeat;

public class HeartbeatTest {

	@Test
	void testExist() {
		assertDoesNotThrow(() -> Class.forName("org.telestion.protocol.old_mavlink.messages.official.minimal.Heartbeat")
				.getConstructor(long.class, int.class, int.class, int.class, int.class, int.class)
				.newInstance(0, 0, 0, 0, 0, 0));
	}

	@Test
	void testCrc() {
		var msg = new Heartbeat(0, 0, 0, 0, 0, 0);
		assertThat(msg.getCrc(), is(50));
	}

}
