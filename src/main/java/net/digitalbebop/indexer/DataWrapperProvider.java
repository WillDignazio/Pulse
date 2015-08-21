package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DataWrapperProvider implements Provider<DataWrapper> {

    @Inject HBaseWrapper hBaseWrapper;
    @Inject SolrWrapper solrWrapper;

    @Override
    public DataWrapper get() {
        return new ThreadLocal<DataWrapper>() {
            @Override
            public DataWrapper initialValue() {
                return new DataWrapper(hBaseWrapper, solrWrapper);
            }
        }.get();
    }
}
