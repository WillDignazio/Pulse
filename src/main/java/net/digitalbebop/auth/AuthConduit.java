package net.digitalbebop.auth;

import java.net.InetSocketAddress;

public interface AuthConduit {

    boolean auth(InetSocketAddress address);
}
