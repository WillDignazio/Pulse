package net.digitalbebop.storage;

import com.google.inject.AbstractModule;

import java.io.File;

public class StorageModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DataConduit.class).to(FileStorageConduit.class);
    }
}
