package org.telestion.application;

import org.telestion.core.connection.EventbusTcpBridge;
import org.telestion.core.message.Address;
import org.telestion.core.monitoring.MessageLogger;
import org.telestion.core.web.WebServer;
import org.telestion.example.RandomPositionPublisher;
import org.telestion.launcher.Launcher;

import java.util.Collections;

/**
 * Starts the Telestion-Project as a standalone Application.
 *
 * @author Jan von Pichowski, Cedric Boes
 * @version 1.0
 */
public class Application {

    /**
     * Calls the Launcher for a specific Testcase (at the moment).</br>
     * Real functionality will be added later.
     *
     * @param args <i>unused at the moment</i>
     */
    public static void main(String[] args) {
        Launcher.start(
                new MessageLogger(),
                new RandomPositionPublisher(),
                new EventbusTcpBridge("localhost", 9870,
                        Collections.emptyList(),
                        Collections.singletonList(Address.outgoing(RandomPositionPublisher.class, "MockPos"))),
                new WebServer(8080)
        );
    }

}
