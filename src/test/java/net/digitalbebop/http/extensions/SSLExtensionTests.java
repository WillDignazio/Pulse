package net.digitalbebop.http.extensions;

import com.google.inject.name.Named;
import net.digitalbebop.UnitTestBase;
import org.junit.Test;

/**
 * Tests the SSLExtension for the HTTP Server.
 * This test uses the keystore.jks and truststore.jks files in the test
 * resources to load the encryption data (self-signed, dummy certs).
 *
 * The stores are loaded via the ExtensionTestModule, which provides the
 * {@link Named} references for both "keystorePass" and "keystorePath."
 */
public class SSLExtensionTests extends UnitTestBase {

    public SSLExtensionTests() {
        super(false, false);
    }

    @Test
    public void testInitialization() {
        SSLExtension ext = injector().getInstance(SSLExtension.class);
        ext.initialize();
    }
}
