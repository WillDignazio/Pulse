package net.digitalbebop.storage;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class StorageModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(StorageConduit.class).to(FileStorageConduit.class).in(Singleton.class);
    }
}
