package net.digitalbebop.http.extensions;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Extension to provide SSL support to a HTTP server instance. This work is built off
 * of the JSSE mechanisms provided in the Sun standard base. Much of the work provided
 * in this extension is based off of the example sample at:
 * {@link https://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/samples/sslengine}
 */
public class SSLExtension implements HttpServerExtension {
    private static Logger logger = LogManager.getLogger(SSLExtension.class);

    private static int BUFFER_PAD_BYTES = 50;

    private final String keystorePath;
    private final String keystorePass;

    private SSLContext sslContext;
    private SSLEngine sslEngine;

    @Inject
    public SSLExtension(@Named("keystorePath") final String keystorePath,
                        @Named("keystorePass") final String keystorePass) {
        this.keystorePath = keystorePath;
        this.keystorePass = keystorePass;
    }

    /**
     * Creates the necessary contexts for the SSL extension, the logic for reading
     * in the ssl configuration is encoded in this routine.
     */
    @Override
    public void initialize() {
        logger.info("Initializing SSL extension.");

        try {
            File storefn = new File(keystorePath);
            if (!storefn.exists()) {
                logger.error("Keystore does not exist, check keystore path!");
                throw new RuntimeException("Failed to load KeyStore");
            }

            final KeyStore ks = KeyStore.getInstance("JKS");
            final KeyStore ts = KeyStore.getInstance("JKS");

            char[] pass = keystorePass.toCharArray();
            ks.load(new FileInputStream(storefn), pass);
            ts.load(new FileInputStream(storefn), pass);

            final String defaultAlgo = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(defaultAlgo);
            kmf.init(ks, pass);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(defaultAlgo);
            tmf.init(ts);

            SSLContext localContext = SSLContext.getInstance("TLS");
            localContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            SSLEngine localEngine = localContext.createSSLEngine();
            localEngine.setUseClientMode(false);
            localEngine.setNeedClientAuth(false);

            SSLSession localSession = localEngine.getSession();
            int appBufferMax = localSession.getApplicationBufferSize();
            int netBufferMax = localSession.getPacketBufferSize();

            /*
             * Excerpt from sample:
             * We'll make the input buffers a bit bigger than the max needed
             * size, so that unwrap()s following a successful data transfer
             * won't generate BUFFER_OVERFLOWS.
             *
             * We'll use a mix of direct and indirect ByteBuffers for
             * tutorial purposes only.  In reality, only use direct
             * ByteBuffers when they give a clear performance enhancement.
             */
            final ByteBuffer clientIn = ByteBuffer.allocate(appBufferMax + BUFFER_PAD_BYTES);
            final ByteBuffer serverIn = ByteBuffer.allocate(appBufferMax + BUFFER_PAD_BYTES);

            

        } catch (KeyStoreException e) {
            logger.error("Failed to load KeyStore: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            logger.error("Certificate failure: " + e.getLocalizedMessage(), e);
            throw new RuntimeException("Failed to read certificate");
        } catch (NoSuchAlgorithmException e) {
            logger.error("SSL Algorithm not available: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            logger.error("Could not find keystore during use: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error("Failed to use keystore provided: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            logger.error("Unrecoverable key: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            logger.error("Error initializing SSL Context: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
