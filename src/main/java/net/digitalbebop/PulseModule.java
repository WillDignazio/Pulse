package net.digitalbebop;

import com.google.inject.Binder;
import com.google.inject.Module;
import org.apache.commons.configuration.BaseConfiguration;

public class PulseModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(BaseConfiguration.class).to(PulseProperties.class);
    }
}
