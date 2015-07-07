package net.digitalbebop;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Application {
    /*
     * Make sure to initialize any base objects here, critically, we must
     * configure all dependency injections transparently from all deriving classes.
     */
    static {
        Injector injector = Guice.createInjector(new PulseModule());
    }

    public Application() { }

    public static void main(String[] args) {

    }
}
