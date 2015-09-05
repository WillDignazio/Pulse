package net.digitalbebop.storage;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class StorageProvider implements Provider<StorageConduit> {

    @Inject
    FileStorageConduit storageConduit;

    @Override
    public StorageConduit get() {
        return new ThreadLocal<StorageConduit>() {
            @Override
            public StorageConduit initialValue() {
                return storageConduit;
            }
        }.get();
    }
}
