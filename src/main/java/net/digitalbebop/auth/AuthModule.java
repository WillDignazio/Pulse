package net.digitalbebop.auth;

import com.google.inject.AbstractModule;

public class AuthModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AuthConduit.class).to(BasicAuth.class);
    }
}
