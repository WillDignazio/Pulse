package net.digitalbebop.http.handlers.indexModules;

import com.google.inject.AbstractModule;

public class IndexModule extends AbstractModule {

    @Override
    public void configure() {
        bind(NewsIndexer.class);
        bind(DefaultIndexer.class);
        bind(GalleryIndexer.class);
        bind(FilesIndexer.class);
    }
}
