package net.digitalbebop;

import com.google.inject.Singleton;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

@Singleton
public class PulseProperties extends Properties {
    private final Logger logger = LogManager.getLogger(PulseProperties.class);

    public PulseProperties() {
        super();

        /*
         * Because we haven't had a chance to setup our logging facilities yet,
         * we're going to configure a basic logging setup until we bring in our own.
         */
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
             * Add base property definitions
             */
            config.addConfiguration(new PropertiesConfiguration(pulseConfigFilepath));

            /* Add in environment variables */
            for(PulseEnvironmentKeys key : Arrays.asList(PulseEnvironmentKeys.values())) {
                this.
                setProperty(key.name(), envVars.get(key.name()));
                logger.debug("Set: " + key);
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
            ConfigKeys.PID_FILE.toString(), "/tmp");

    public final String SolrAddress = getProperty(
            ConfigKeys.SOLR_ADDRESS.toString(), "127.0.0.1:8983/solr");

    public final String ServerAddress = getProperty(
            ConfigKeys.LISTEN_ADDRRES.toString(), "127.0.0.1"
    );

    public final String ServerPort = getProperty(
            ConfigKeys.LISTEN_PORT.toString(), "8080"
    );

    public final String HBaseTable = getProperty(
            ConfigKeys.HBASE_TABLE.toString(), "pulse"
    );

    public final String ZookeeperQuorum = getProperty(
            ConfigKeys.ZOOKEEPER_QUORUM.toString(), "jd-5.ih.csh.rit.edu"
    );
}
