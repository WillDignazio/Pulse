package net.digitalbebop;

import com.google.inject.Singleton;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

@Singleton
public class PulseProperties extends Properties {
    private final Logger logger = Logger.getLogger(PulseProperties.class);

    public PulseProperties() {
        super();

        /*
         * Because we haven't had a chance to setup our logging facilities yet,
         * we're going to configure a basic logging setup until we bring in our own.
         */
        BasicConfigurator.configure();
        logger.info("Initializing Pulse property instance.");
        bootstrapConfiguration();
    }

    private void bootstrapConfiguration() {
        final CompositeConfiguration config = new CompositeConfiguration();
        final Map<String, String> envVars = System.getenv();

        try {
            /* Retrieve the set configuration path from the Environment Variables */
            String pulseConfigPath = envVars.getOrDefault(PulseEnvironmentKeys.PULSE_CONFIG.toString(), ".");
            String pulseConfigFilepath = pulseConfigPath + "/pulse.properties";

            /*
             * Configure Log4J instance
             */
            PropertyConfigurator.configure(pulseConfigPath + "/log4j.properties");

            /*
             * Add base property definitions
             */
            config.addConfiguration(new PropertiesConfiguration(pulseConfigFilepath));

            /* Add in environment variables */
            for(PulseEnvironmentKeys key : Arrays.asList(PulseEnvironmentKeys.values())) {
                this.
                setProperty(key.name(), envVars.get(key.name()));
                System.out.println("Set: " + key);
            }

            Iterator<String> iter = config.getKeys();
            while(iter.hasNext()) {
                String key = iter.next();
                Object o = config.getProperty(key);

                if(o instanceof String) {
                    setProperty(key, (String) o);
                } else {
                    try {
                        String valstr = o.toString();
                        logger.warn("Property \"" + key + "\" was converted to a string: " + valstr);
                        setProperty(key, valstr);
                    } catch(Exception e) {
                        logger.error("Failed to parse property \"" + key + "\"", e);
                        throw new IllegalArgumentException(e);
                    }
                }
            }
        } catch (Exception e) {
            //logger.error("Failed to parse configuration file.");
            throw new RuntimeException(e);
        }
    }

    public final String PulsePIDPath = getProperty(
            PulseConfigKeys.PID_FILE.toString(), "/tmp");
}
