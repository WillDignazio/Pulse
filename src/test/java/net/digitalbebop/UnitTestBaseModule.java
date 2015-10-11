package net.digitalbebop;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import net.digitalbebop.http.extensions.ExtensionTestModule;
import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class UnitTestBaseModule extends AbstractModule {
    @Override
    protected void configure() {
        InputStream configStream = UnitTestBase.class
                .getClassLoader()
                .getResourceAsStream("test.properties");

        final Properties properties = new Properties();
        try {
            properties.load(configStream);

            Names.bindProperties(binder(), properties);
            install(new ExtensionTestModule());

            configStream.close();
        } catch (IOException e) {
            Assert.fail("Error loading configuration file: " + e.toString());
        }
    }
}
