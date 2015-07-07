package net.digitalbebop;

import com.google.inject.Inject;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class PulseProperties extends BaseConfiguration {
    private final Logger logger = LogManager.getLogger(PulseProperties.class);
    private Configuration baseConfig;
    private Configuration pulseConfig;
    private Map<String, String> envVars;

    @Inject
    private PulseProperties(Configuration baseConfig) {
        this.baseConfig = baseConfig;

        envVars = System.getenv();
        try {
            String pulseConfig = envVars.get(PulseEnvironment.PULSE_CONFIG);

            logger.info("Configuring Pulse from: " + pulseConfig + "/pulse.properties");
            this.pulseConfig = new PropertiesConfiguration(pulseConfig);
        } catch(Exception e) {
            throw new RuntimeException("Failed to parse properties file: " + e.toString());
        }
    }
}
