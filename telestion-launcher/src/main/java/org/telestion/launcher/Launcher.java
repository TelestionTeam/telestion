package org.telestion.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

/**
 * A generic launcher class which deploys {@link Verticle Verticles}.
 * 
 * @version 1.0
 * @author Jan von Pichowski, Cedric Boes
 * @see Verticle
 */
public final class Launcher {

    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    /**
     * Simply calls {@link #start(String...)}.
     * 
     * @param args the class names of the {@link Verticle Verticles} which should be deployed
     */
    public static void main(String[] args) {
        start(args);
    }

    /**
     * Deploys the given {@link Verticle Verticles}.</br>
     * If Vert.x fails to deploy a {@link Verticle}, it will retry after 5 secs.
     *
     * @param verticleNames the class names of the {@link Verticle Verticles} which should be deployed
     */
    public static void start(String... verticleNames){
        logger.info("Deploying {} verticles", verticleNames.length);
        var vertx = Vertx.vertx();
        Arrays.stream(verticleNames).forEach(verticleName -> {
            logger.info("Deploying verticle {}", verticleName);
            vertx.setPeriodic(Duration.ofSeconds(5).toMillis(), timerId -> {
                vertx.deployVerticle(verticleName, res -> {
                    if(res.failed()){
                        logger.error("Failed to deploy verticle {} retrying in 5s", verticleName, res.cause());
                        return;
                    }
                    logger.info("Deployed verticle {} with id {}", verticleName, res.result());
                    vertx.cancelTimer(timerId);
                });
            });
        });
    }
}