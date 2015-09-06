package net.digitalbebop;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import net.digitalbebop.auth.AuthModule;
import net.digitalbebop.http.HttpModule;
import net.digitalbebop.indexer.IndexerModule;
import net.digitalbebop.storage.StorageModule;

public class PulseModule extends AbstractModule {
    /**
     * This is the shared global instance of PulseProperties.
     */
    final PulseProperties properties = new PulseProperties();

    @Provides
    public PulseProperties provideProperties() {
        return properties;
    }

    @Override
    protected void configure() {
        Names.bindProperties(binder(), properties);

        bind(App.class).to(PulseApp.class);
        bind(AppBootstrapper.class);

        install(new HttpModule());
        install(new IndexerModule());
        install(new StorageModule());
        install(new AuthModule());
    }
}
