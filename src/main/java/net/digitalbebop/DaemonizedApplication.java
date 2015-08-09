package net.digitalbebop;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DaemonizedApplication extends Application implements Runnable {
    private static Logger logger = LogManager.getLogger(DaemonizedApplication.class);
    private static ExecutorService executor = Executors.newCachedThreadPool();

    private final AtomicBoolean shutdown;
    private Thread parentThread;

    private class DaemonShutdownTask implements Runnable {
        @Override
        public void run() {
            logger.info("Shutting down daemon");
            shutdown.set(true);
        }
    }

    public DaemonizedApplication(@NotNull String name, String[] args) {
        super(name);
        parentThread = Thread.currentThread();
        shutdown = new AtomicBoolean(false);
        Runtime.getRuntime().addShutdownHook(new Thread(new DaemonShutdownTask()));
    }

    public DaemonizedApplication(@NotNull String name) {
        this(name, new String[0]);
    }

    public void init() {
        logger.info("Initializing daemonized application spawned from " + parentThread.getName());
        executor.submit(this);
    }

    @Override
    public void run() {
        logger.info("Running daemon background thread.");
        for(;;) {
            try {
                if(shutdown.get()) {
                    logger.info("Daemon is shutting down...");
                    break;
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.error("Daemon interrupted from sleep.");
            }
        }
    }
}
