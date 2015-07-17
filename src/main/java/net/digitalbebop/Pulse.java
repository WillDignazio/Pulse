package net.digitalbebop;

import org.apache.log4j.Logger;

import java.util.concurrent.Executors;

public class Pulse extends Application {
    private static Logger logger = Logger.getLogger(Pulse.class);

    public Pulse() {
        super("pulse");
    }

    public static void main(String[] args) {
        logger.info("Starting Pulse application....");
        Executors.newCachedThreadPool().submit(new Pulse());
    }
}
