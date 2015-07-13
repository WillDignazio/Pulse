package net.digitalbebop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Application {
    static Injector injector;
    private static final Logger logger = LogManager.getLogger(Application.class);

    protected static PulseProperties defaultProperties;

    /*
     * Make sure to initialize any base objects here, critically, we must
     * configure all dependency injections transparently from all deriving classes.
     */
    static {
        injector = Guice.createInjector(new PulseModule());
        defaultProperties = injector.getInstance(PulseProperties.class);
    }

    public Application() {
    }

    public PulseProperties getDefaultProperties() {
        return defaultProperties;
    }

    public static void main(String[] args) {
        Application app;

        try {
            app = new Application();
            System.out.println("Test: " + defaultProperties.getProperty(PulseConfigKeys.LISTEN_ADDRRES.toString()));
        } catch(Exception e) {
            System.err.println("Initialization error: " + e);
            System.exit(1);
        }
    }
}
