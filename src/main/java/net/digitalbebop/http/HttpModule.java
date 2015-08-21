package net.digitalbebop.http;

import com.google.inject.AbstractModule;
import net.digitalbebop.http.handlers.HandlersModule;

public class HttpModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HttpRouter.class).to(EndpointRouter.class);
        bind(HttpServer.class).to(BasicHttpServerImpl.class);

        install(new HandlersModule());
    }
}
