package net.digitalbebop.indexer;

import com.google.inject.AbstractModule;

public class IndexerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SolrConduit.class);
    }
}
