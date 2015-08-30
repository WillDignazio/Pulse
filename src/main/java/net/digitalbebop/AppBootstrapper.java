package net.digitalbebop;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

class AppBootstrapper {
    private static final Logger logger = LogManager.getLogger(AppBootstrapper.class);

    private static ExecutorService executor = Executors.newCachedThreadPool();

    private App app;
    private File pidFile;
    private AtomicBoolean shutdown = new AtomicBoolean(false);

    private static Options options = new Options();
    private final String pidPath;

    @Inject
    public AppBootstrapper(@Named("pidPath") String pidPath) {
        this.pidPath = pidPath;
    }

    @Inject
    public void getApp(App app) {
        this.app = app;
    }

    /**
     * This task will be run at AppBootstrapper exit, and will do various cleanup duties.
     * Any work that needs to be done when the application closes under normal conditions
     * should be encapsulated under this task.
     */
    private class ApplicationShutdownTask implements Runnable {
        @Override
        public void run() {
            logger.info("Running shutdown routine...");

            if(pidFile != null) {
                if(pidFile.exists() && !pidFile.delete()) {
                    logger.error("PID file existed, but failed to delete on shutdown.");
                }
                logger.info("Removed PID file.");
            }
        }
    }

    /**
     * Retrieves a pid file for the application, if there is a already a PID file
     * for this application, we check that it is locked. If the PID file is not locked,
     * then the file will be reused, and the PID contained within the file changed.
     * @return PID File descriptor
     */
    private File getPIDFile() {
        final File pidFile = new File(pidPath + "/pulse.pid");

        /*
         * XXX: We need to make this platform independent/checkable later, presumably this won't
         * work outside of Unix environments.
         */
        String pname = ManagementFactory.getRuntimeMXBean().getName().split("@")[0]; // PID@host
        byte[] pidbuf = pname.getBytes();
        logger.info("Identified as process: " + pname);

        try {
            if(pidFile.exists()) {
                logger.warn("PID file for application already exists.");
            } else {
                logger.info("PID file did not exist, creating one at " + pidFile.getAbsolutePath());
                if(!pidFile.createNewFile()) {
                    logger.error("Failed to create PID file.");
                    return null;
                }

                FileOutputStream fout = new FileOutputStream(pidFile);
                fout.write(pidbuf);
                fout.close();
            }

            FileChannel channel = new FileOutputStream(pidFile).getChannel();
            channel.lock();

            channel.truncate(0);
            channel.write(ByteBuffer.wrap(pidbuf));
            channel.close();

        } catch(IOException e) {
            logger.error("Failed to get lock on PID File: " + e.getMessage(), e);
            return null;
        }

        return pidFile;
    }

    private void init() {
        pidFile = getPIDFile();
        Runtime.getRuntime().addShutdownHook(new Thread(new ApplicationShutdownTask()));

        logger.info("Initializing main application.");
        executor.execute(() -> {
            app.init();

            for (;;) {
                if (shutdown.get()) {
                    logger.info("Application recieved shutdown.");
                    break;
                }

                try {
                    Thread.sleep(5000);
                } catch (Exception ignored) {}
            }
        });
    }

    public static void main(String[] args) throws Exception {
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd;
        final Injector injector;
        final Stage stage;

        options.addOption("mode", true, "Mode to run the application in.");
        cmd = parser.parse(options, args);

        if (cmd.hasOption("mode")) {
            String mode = cmd.getOptionValue("mode");
            if (mode.toLowerCase().equals("dev")) {
                stage = Stage.DEVELOPMENT;
            } else {
                System.err.println("Unknown mode: " + mode);
                stage = null;
                System.exit(1);
            }
        } else {
            stage = Stage.PRODUCTION;
        }

        injector = Guice.createInjector(stage, new PulseModule());
        AppBootstrapper bootstrapper = injector.getInstance(AppBootstrapper.class);

        logger.info("Bootstrapping application....");
        bootstrapper.init();
    }
}
