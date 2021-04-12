package org.telestion.protocol.old_mavlink.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class HeaderContextTest {

	@Test
	void testMessageContext(Vertx vertx, VertxTestContext testContext) throws Throwable {
		Random r = new Random();

		var incompFlags = (short) r.nextInt();
		var compFlags = (short) r.nextInt();
		var sysId = (short) r.nextInt();
		var compId = (short) r.nextInt();
		var linkId = (short) r.nextInt();

		var context = new HeaderContext(incompFlags, compFlags, sysId, compId, linkId);

		assertThat(context.incompFlags(), is(incompFlags));
		assertThat(context.compFlags(), is(compFlags));
		assertThat(context.sysId(), is(sysId));
		assertThat(context.compId(), is(compId));
		assertThat(context.linkId(), is(linkId));

		for (int i = 0; i < Short.MAX_VALUE + 1; i++) {
			assertThat(context.getNewPacketSeq(), is((byte) i));
		}

		testContext.completeNow();
	}
}
