package net.digitalbebop.http.handlers;

import com.google.inject.AbstractModule;
import net.digitalbebop.http.handlers.indexModules.IndexModule;

public class HandlersModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DeleteRequestHandler.class);
        bind(GetDataRequestHandler.class);
        bind(IndexRequestHandler.class);
        bind(SearchRequestHandler.class);
        bind(GetThumbnailRequestHandler.class);

        install(new IndexModule());
    }
}
