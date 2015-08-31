package net.digitalbebop.storage;

import com.google.inject.AbstractModule;

public class StorageModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StorageConduit.class).toProvider(StorageProvider.class);
    }
}
