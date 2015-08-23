package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class HBaseConduitProvider implements Provider<HBaseConduit> {

    @Inject HBaseWrapper hBaseWrapper;
    @Inject SolrConduit solrConduit;

    @Override
    public HBaseConduit get() {
        return new ThreadLocal<HBaseConduit>() {
            @Override
            public HBaseConduit initialValue() {
                return new HBaseConduit(hBaseWrapper, solrConduit);
            }
        }.get();
    }
}
