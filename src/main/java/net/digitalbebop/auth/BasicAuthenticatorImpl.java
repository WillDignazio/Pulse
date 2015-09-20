package net.digitalbebop.auth;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public class BasicAuthenticatorImpl implements Authenticator {
    private static Logger logger = LogManager.getLogger(BasicAuthenticatorImpl.class);
    private final String host;

    @Inject
    public BasicAuthenticatorImpl(@Named("AuthHost") String host) {
        this.host = host;
    }

    @Override
    public boolean isAuthorized(HttpRequest request, InetSocketAddress address) {
        String hostname = address.getHostName();
        boolean result = hostname.startsWith("localhost") || host.equals(hostname);
        logger.debug("checking isAuthorized for hostname: " + hostname + " = " + result);
        return true;
    }
}
