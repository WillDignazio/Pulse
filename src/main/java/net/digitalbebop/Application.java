package net.digitalbebop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class Application implements Runnable {
    private static Injector injector;
    private static final Logger logger = LogManager.getLogger(Application.class);

    protected static PulseProperties defaultProperties;

    protected final String appName;
    protected final File pidFile;

    /*
     * Make sure to initialize any base objects here, critically, we must
     * configure all dependency injections transparently from all deriving classes.
     */
    static {
        injector = Guice.createInjector(new PulseModule());
        defaultProperties = injector.getInstance(PulseProperties.class);
    }

    /**
     * Application construction paramaters should match the properties of the PulseApp
     * annotation. At runtime, the reflection tools will find classes that have both the
     * PulseApp annotation, and extend the Application class.
     * @param appName
     */
    public Application(String appName) {
        this.appName = appName;
        this.pidFile = getPIDFile();
        Runtime.getRuntime().addShutdownHook(new Thread(new ApplicationShutdownTask()));
    }

    /**
     * This task will be run at Application exit, and will do various cleanup duties.
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
        final String pidPath = defaultProperties.PulsePIDPath + "/" + appName + ".pid";
        final File pidFile = new File(pidPath);
        final FileLock pidLock;

        logger.info("PID File for application will reside in " + pidPath);

        /*
         * XXX: We need to make this platform independent/checkable later, presumably this won't
         * work outside of Unix environments.
         */
        String pname = ManagementFactory.getRuntimeMXBean().getName().split("@")[0]; // PID@host
        byte[] pidbuf = pname.getBytes();
        logger.info("Identified as process: " + pname);

        try {
            if(pidFile.exists()) {
                logger.warn("PID file for application \"" + appName + "\" already exists.");
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

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10000); // Sleep 10 Seconds
            } catch (InterruptedException e) {
                logger.warn("Daemon interrupted: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
