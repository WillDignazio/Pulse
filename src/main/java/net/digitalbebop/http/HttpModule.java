package net.digitalbebop.http;

import com.google.inject.AbstractModule;
import net.digitalbebop.http.annotations.Address;
import net.digitalbebop.http.annotations.Port;

public class HttpModule extends AbstractModule {
    @Override
    protected void configure() {
        System.out.println("Installing HttpModule");
        bind(HttpRouter.class).to(EndpointRouter.class);
        bind(HttpServer.class).to(BasicHttpServerImpl.class);

        // Property Configuration
        bind(String.class).annotatedWith(Address.class).toInstance("127.0.0.1");
        bind(Integer.class).annotatedWith(Port.class).toInstance(8080);
    }
}
