package net.digitalbebop.indexer;

import com.google.inject.AbstractModule;

public class IndexerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SolrWrapper.class);
        bind(HBaseWrapper.class);
        bind(HBaseConduit.class).toProvider(HBaseConduitProvider.class);

        bind(IndexConduit.class).to(HBaseConduit.class);
    }
}
