package net.digitalbebop.http.handlers.indexModules;

import com.google.inject.Inject;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.indexer.IndexConduit;
import net.digitalbebop.storage.StorageConduit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class NewsIndexer implements ServerIndexer {
    private static final Logger logger = LogManager.getLogger(NewsIndexer.class);
    private final IndexConduit indexConduit;
    private final StorageConduit storageConduit;

    @Inject
    public NewsIndexer(IndexConduit indexConduit, StorageConduit storageConduit) {
        this.indexConduit = indexConduit;
        this.storageConduit = storageConduit;
    }

    @Override
    public void index(ClientRequests.IndexRequest indexRequest) throws IOException {
        indexConduit.index(indexRequest);
        byte[] rawPayload = indexRequest.getRawData().toByteArray();
        storageConduit.putRaw(indexRequest.getModuleName(), indexRequest.getModuleId(),
                indexRequest.getTimestamp(), rawPayload);

    }
}
