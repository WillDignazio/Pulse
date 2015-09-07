package net.digitalbebop.auth;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class BasicAuth implements AuthConduit {
    private static Logger logger = LogManager.getLogger(BasicAuth.class);
    private final String host;

    @Inject
    public BasicAuth(@Named("AuthHost") String host) {
        this.host = host;
    }

    public boolean auth(InetSocketAddress address) {
        String hostname = address.getHostName();
        logger.debug("checking auth for " + hostname);
        return hostname.startsWith("localhost") || host.equals(hostname);
    }
}
