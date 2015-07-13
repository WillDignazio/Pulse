package net.digitalbebop;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;

public class PulseModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(PulseProperties.class).in(Singleton.class);
    }
}
