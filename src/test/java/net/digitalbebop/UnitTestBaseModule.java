package net.digitalbebop;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class UnitTestBaseModule extends AbstractModule {
    private static Properties properties;

    @Override
    protected void configure() {
        InputStream configStream = UnitTestBase.class
                .getClassLoader()
                .getResourceAsStream("test.properties");

        properties = new Properties();
        try {
            properties.load(configStream);

            Names.bindProperties(binder(), properties);

            configStream.close();
        } catch (IOException e) {
            Assert.fail("Error loading configuration file: " + e.toString());
        }
    }
}
