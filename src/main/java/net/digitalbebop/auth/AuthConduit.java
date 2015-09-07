package net.digitalbebop.auth;

import org.apache.http.HttpRequest;

import java.net.InetSocketAddress;

public interface AuthConduit {

    boolean auth(HttpRequest request, InetSocketAddress address);
}
