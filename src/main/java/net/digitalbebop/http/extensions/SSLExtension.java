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
import java.nio.channels.Channel;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Extension to provide SSL support to a HTTP server instance. This work is built off
 * of the JSSE mechanisms provided in the Sun standard base. Much of the work provided
 * in this extension is based off of the example sample at:
 * {@link https://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/samples/sslengine}
 */
public class SSLExtension implements ServerExtension {
    private static Logger logger = LogManager.getLogger(SSLExtension.class);

    private static int BUFFER_PAD_BYTES = 50;

    private final String truststorePath;
    private final String keystorePath;
    private final String keystorePass;

    private KeyManagerFactory keyManagerFactory;
    private TrustManagerFactory trustManagerFactory;
    private KeyStore keystore;
    private KeyStore truststore;
    private SSLContext sslContext;
    private SSLEngine sslEngine;

    @Inject
    public SSLExtension(@Named("truststorePath") final String truststorePath,
                        @Named("keystorePath") final String keystorePath,
                        @Named("keystorePass") final String keystorePass) {
        this.truststorePath = truststorePath;
        this.keystorePath = keystorePath;
        this.keystorePass = keystorePass;
    }

    /**
     * Creates the necessary contexts for the SSL extension, the logic for reading
     * in the ssl configuration is encoded in this routine.
     */
    @Override
    public void initialize() {
        logger.info("Initializing SSL extension: \n" +
                        "\tKeyStore: " + keystorePath + "\n" +
                        "\tTrustStore: " + truststorePath);

        try {
            final File keystoreFile = new File(keystorePath);
            if (!keystoreFile.exists()) {
                logger.error("Keystore does not exist, check keystore path!");
                throw new RuntimeException("Failed to load KeyStore");
            }

            File truststoreFile = new File(truststorePath);
            if (!truststoreFile.exists()) {
                logger.error("Truststore does not exist, check truststore path!");
                throw new RuntimeException("Failed to load TrustStore");
            }

            keystore = KeyStore.getInstance("JKS");
            truststore = KeyStore.getInstance("JKS");

            final char[] pass = keystorePass.toCharArray();
            keystore.load(new FileInputStream(keystoreFile), pass);
            truststore.load(new FileInputStream(truststoreFile), pass);

            final String defaultAlgo = KeyManagerFactory.getDefaultAlgorithm();
            keyManagerFactory = KeyManagerFactory.getInstance(defaultAlgo);
            keyManagerFactory.init(keystore, pass);

            trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgo);
            trustManagerFactory.init(truststore);

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
        }
    }

    @Override
    public Channel handleConnection(Channel input) {
        try {
            final SSLContext localContext = SSLContext.getInstance("TLS");

            localContext.init(keyManagerFactory.getKeyManagers(),
                              trustManagerFactory.getTrustManagers(), null);

            final SSLEngine localEngine = localContext.createSSLEngine();
            localEngine.setUseClientMode(false);
            localEngine.setNeedClientAuth(false);

            final SSLSession localSession = localEngine.getSession();
            final int appBufferMax = localSession.getApplicationBufferSize();



            /*
             * Excerpt from sample:
             * We'll make the input buffers a bit bigger than the max needed
             * size, so that unwrap()s following a successful data transfer
             * won't generate BUFFER_OVERFLOWS.
             */
            final ByteBuffer serverIn = ByteBuffer.allocate(appBufferMax + BUFFER_PAD_BYTES);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Encryption not supported: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            logger.info("Failed to retrieve key from store: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        }

        return input;
    }

    /*
     * If the result indicates that we have outstanding tasks to do,
     * go ahead and run them in this thread.
     */
    private static void runDelegatedTasks(SSLEngineResult result,
                                          SSLEngine engine) throws Exception {
        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable runnable;

            // TODO: Run async with Fibers
            while ((runnable = engine.getDelegatedTask()) != null) {
                logger.debug("running delegated task...");
                runnable.run();
            }

            SSLEngineResult.HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw new Exception("handshake shouldn't need additional tasks");
            }

            logger.debug("\tnew HandshakeStatus: " + hsStatus);
        }
    }
}
