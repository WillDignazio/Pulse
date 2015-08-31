package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class IndexProvider implements Provider<IndexConduit> {

    @Inject SolrConduit solrConduit;

    @Override
    public IndexConduit get() {
        return new ThreadLocal<IndexConduit>() {
            @Override
            public IndexConduit initialValue() {
                return new SolrConduit(hBaseWrapper, solrConduit);
            }
        }.get();
    }
}