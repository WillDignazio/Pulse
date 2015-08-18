package net.digitalbebop;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import net.digitalbebop.http.HttpModule;

public class PulseModule extends AbstractModule {
    @Override
    public void configure() {
        install(new HttpModule());

        bind(PulseProperties.class).in(Singleton.class);
        bind(App.class).to(PulseApp.class);
        bind(AppBootstrapper.class);
    }
}
