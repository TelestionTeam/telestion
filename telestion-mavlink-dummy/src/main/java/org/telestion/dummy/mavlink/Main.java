package org.telestion.dummy.mavlink;

import org.telestion.core.HelloWorld;
import org.telestion.launcher.Launcher;

public class Main {

    public static void main(String[] args) {
        Launcher.start(HelloWorld.class.getName());
    }
}
