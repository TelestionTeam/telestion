package org.telestion.application;

import org.telestion.core.message.Address;
import org.telestion.core.verticle.MessageLogger;
import org.telestion.core.verticle.RandomPositionPublisher;
import org.telestion.launcher.Launcher;
import org.telestion.widget.WidgetBridge;

import java.util.Collections;

/**
 * Starts the Telestion-Project as a standalone Application.
 * 
 * @version 1.0
 * @author Jan von Pichowski, Cedric Boes
 *
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
				new WidgetBridge("localhost", 8080,
						Collections.emptyList(),
						Collections.singletonList(Address.outgoing(RandomPositionPublisher.class, "MockPos")))
        );
    }

}