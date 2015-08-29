package net.digitalbebop.http.handlers;

import com.google.inject.AbstractModule;

public class HandlersModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DeleteRequestHandler.class);
        bind(GetDataRequestHandler.class);
        bind(IndexRequestHandler.class);
        bind(SearchRequestHandler.class);
        bind(GetThumbnailRequestHandler.class);
    }
}
