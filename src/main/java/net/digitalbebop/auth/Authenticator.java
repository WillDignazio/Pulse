package net.digitalbebop.auth;

import org.apache.http.HttpRequest;

import java.net.InetSocketAddress;

public interface Authenticator {

    boolean isAuthorized(HttpRequest request, InetSocketAddress address);
}
