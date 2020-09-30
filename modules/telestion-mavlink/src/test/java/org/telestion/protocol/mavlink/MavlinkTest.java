package org.telestion.protocol.mavlink;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.vertx.core.eventbus.Message;
import io.vertx.junit5.Checkpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.telestion.api.message.JsonMessage;
import org.telestion.core.connection.TcpConn;
import org.telestion.core.monitoring.MessageLogger;
import org.telestion.protocol.mavlink.exception.PacketException;
import org.telestion.protocol.mavlink.message.MessageIndex;
import org.telestion.protocol.mavlink.message.RawPayload;
import org.telestion.protocol.mavlink.messages.official.minimal.Heartbeat;
import org.telestion.protocol.mavlink.security.HeaderContext;
import org.telestion.protocol.mavlink.security.MavV2Signator;
import org.telestion.protocol.mavlink.security.SecretKeySafe;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class MavlinkTest {
	
	private static Logger logger = org.slf4j.LoggerFactory.getLogger(MavlinkTest.class);
	
    @Test
    void testReceiver(Vertx vertx, VertxTestContext testContext) throws Throwable {
        var tcpToReceiver = "tcpToReceiver";
        var receiverToParser = "receiverToParser";
        var parserOut = "parserOut";
        var v1ToRaw = "v1ToRaw";
        var v2ToRaw = "v2ToRaw";
        var parserToTransmitter = "parserToTransmitter";

        if (!MessageIndex.isRegistered(HEARTBEAT_ID)) {
        	MessageIndex.put(HEARTBEAT_ID, Heartbeat.class);
        }

        vertx.deployVerticle(new TcpConn(null, 42124, tcpToReceiver, null, null));
        vertx.deployVerticle(new Receiver(tcpToReceiver, receiverToParser));
        vertx.deployVerticle(new MavlinkParser(new MavlinkParser.Configuration(
                        receiverToParser, parserOut,
                        v1ToRaw, v2ToRaw, parserToTransmitter)));

        vertx.eventBus().consumer(parserOut, msg -> {
            JsonMessage.on(Heartbeat.class, msg, heartbeat -> {
                testContext.completeNow();
            });
        });

        Thread.sleep(Duration.ofSeconds(1).toMillis());

        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start(Promise<Void> startPromise) throws Exception {
                var client = vertx.createNetClient();
                client.connect(42124, "localhost", netSocketResult -> {
                    assertThat(netSocketResult.succeeded(), is(true));
                    if(netSocketResult.failed()) {
                        startPromise.fail(netSocketResult.cause());
                        return;
                    }
                    var socket = netSocketResult.result();
                    socket.write(Buffer.buffer(HEARTBEAT_MESSAGE_V2));
                    startPromise.complete();
                });
            }
        });

        assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS), is(true));
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }
    
    @Test
    void testTransmitter(Vertx vertx, VertxTestContext testContext) throws Throwable {
        var checkpoint = testContext.checkpoint(3);

        var receiverToParser = "receiverToParser";
        var parserOut = "parserOut";
        var v1ToRaw = "v1ToRaw";
        var v2ToRaw = "v2ToRaw";
        var parserToTransmitter = "parserToTransmitter";
        var transmitterConsumer = "transmitterConsumer";
    	
        if (!MessageIndex.isRegistered(HEARTBEAT_ID)) {
        	MessageIndex.put(HEARTBEAT_ID, Heartbeat.class);
        }

        MavlinkParser parser = new MavlinkParser(
        		new HeaderContext((short) 0x0, (short) 0x0, (short) 0x1, (short) 0x1, (short) 0x2),
        		new SecretKeySafe(secretKey),
        		new MavlinkParser.Configuration(receiverToParser, parserOut, v1ToRaw, v2ToRaw, parserToTransmitter));
        
        vertx.deployVerticle(new Transmitter(parserToTransmitter, transmitterConsumer));
        vertx.deployVerticle(parser);
        vertx.deployVerticle(new MessageLogger());

        vertx.eventBus().consumer(transmitterConsumer, msg -> verifyResult(msg, testContext, checkpoint));
        
        logger.info("Testing MAVLinkV1");
        vertx.eventBus().publish(v1ToRaw, new Heartbeat(1L, 2, 3, 4, 5, 6).json());
        Thread.sleep(Duration.ofSeconds(1).toMillis());

        logger.info("Testing MAVLinkV2 (without signing)");
        vertx.eventBus().publish(v2ToRaw, new Heartbeat(1L, 2, 3, 4, 5, 6).json());
        Thread.sleep(Duration.ofSeconds(1).toMillis());

        parser.changeHeaderContext(new HeaderContext((short) 0x01, (short) 0x0, (short) 0x0, (short) 0x0, (short) 0x2));
        logger.info("Testing MAVLinkV2 (with signing)");
        vertx.eventBus().publish(v2ToRaw, new Heartbeat(1L, 2, 3, 4, 5, 6).json());
        
        assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS), is(true));
        if (testContext.failed()) {
        	throw testContext.causeOfFailure();
        }
    }

    private static void verifyResult(Message<?> msg, VertxTestContext testContext, Checkpoint checkpoint){
        // Test RawMavlinkV1
        JsonMessage.on(RawPayload.class, msg, handler -> {

            try {
                byte[] payload = handler.payload();

                var message = new byte[0];
                if (payload[0] == (byte) 0xFE) {
                    message = HEARTBEAT_MESSAGE_V1;
                    message[2] = payload[2];		// Message-Index is unimportant
                } else if (payload[0] == (byte) 0xFD) {
                    message = HEARTBEAT_MESSAGE_V2;
                    message[4] = payload[4];		// Message-Index is unimportant

                    if (payload[2] == 0x1) {
                        System.out.println(Arrays.toString(payload));
                        System.out.println(Arrays.toString(message));

                        message[2] = (byte) 0x1;

                        System.out.println(Arrays.toString(message));

                        var timestamp = Arrays.copyOfRange(payload, payload[1] + 11, payload[1] + 17);
                        var signature = MavV2Signator.rawSignature(secretKey, Arrays.copyOfRange(message, 1, 10),
                                Arrays.copyOfRange(message, 10, 10 + message[1]), 0x32, (short) 0x2,
                                timestamp);
                        var buildMsg = new byte[message.length + 13];
                        var index = 0;
                        for (byte b : message) {
                            buildMsg[index++] = b;
                        }

                        buildMsg[index++] = (byte) 0x2;

                        for (byte b : timestamp) {
                            buildMsg[index++] = b;
                        }

                        for (byte b : signature) {
                            buildMsg[index++] = b;
                        }

                        message = buildMsg;
                    }
                } else {
                    testContext.failNow(new PacketException("First byte of payload has to be [0xFE, 0xFD]"));
                }

                assertThat(handler.payload(), is(message));
                checkpoint.flag();
            } catch (AssertionError | NoSuchAlgorithmException e) {
                testContext.failNow(e);
            }
        });
    }
    
    private static final byte[] HEARTBEAT_MESSAGE_V1 = {
    		(byte) 0xFE,	// Mavlink V1
    		(byte) 0x09,	// Length
    		(byte) 0x00,	// Seq
    		(byte) 0x01,	// Comp-Id
    		(byte) 0x00,	// msg-id
    		(byte) 0x00,	// p
    		(byte) 0x00,	// a
    		(byte) 0x00,	// y
    		(byte) 0x01,	// l
    		(byte) 0x02,	// o
    		(byte) 0x03,	// a
    		(byte) 0x04,	// d
    		(byte) 0x05,	// V
    		(byte) 0x06,	// 1
    		(byte) 0x01,	// checksum #1
    		(byte) 0xC1,	// checksum #2
    };
    
    private static final byte[] HEARTBEAT_MESSAGE_V2 = {
            (byte)0xFD,		// Mavlink V2
            (byte)0x09,		// Length
            (byte)0x00,		// Incompat-Flags
            (byte)0x00,		// Compat-Flags
            (byte)0x00,		// Seq
            (byte)0x01,		// Sys-Id
            (byte)0x01,		// comp-id
            (byte)0x00,		// message-id #1
            (byte)0x00,		// message-id #2
            (byte)0x00,		// message-id #3
            (byte)0x09,		// p
            (byte)0x00,		// a
            (byte)0x02,		// y
            (byte)0x00,		// l
            (byte)0x00,		// o
            (byte)0x00,		// a
            (byte)0x00,		// d
            (byte)0x04,		// V
            (byte)0x14,		// 2
            (byte)0x54,		// checksum #1
            (byte)0x28		// checksum #2
    };
    
    private static final int HEARTBEAT_ID = 0;

    private static final byte[] secretKey = new byte[] {(byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0xA0};
}
