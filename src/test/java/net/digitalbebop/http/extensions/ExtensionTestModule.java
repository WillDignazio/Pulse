package net.digitalbebop.http.extensions;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.net.URL;

/**
 * This module manually binds the values of both "keystorePath" and "truststorePath"
 * for the test environment. This is so that we may dynamically load the store from
 * the resources directory before the tests are run with Guice.
 */
public class ExtensionTestModule extends AbstractModule {
    @Override
    protected void configure() {
        final URL keystoreURL = ExtensionTestModule.class
                .getClassLoader()
                .getResource("keystore.jks");
        final URL truststoreURL = ExtensionTestModule.class
                .getClassLoader()
                .getResource("truststore.jks");

        assert keystoreURL != null;
        assert truststoreURL != null;

        bindConstant()
                .annotatedWith(Names.named("keystorePath"))
                .to(keystoreURL.getPath());
        bindConstant()
                .annotatedWith(Names.named("truststorePath"))
                .to(truststoreURL.getPath());
        bindConstant()
                .annotatedWith(Names.named("keystorePass"))
                .to("tits123");
    }
}
