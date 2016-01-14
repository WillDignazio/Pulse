package net.digitalbebop.http.extensions;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.io.FiberSocketChannel;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.PulseException;
import net.digitalbebop.http.HttpStatus;
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
    public FiberSocketChannel handleConnection(FiberSocketChannel input) throws SuspendExecution {
        try {
            final  SSLContext sslContext = SSLContext.getInstance("TLS"); ;

            sslContext.init(keyManagerFactory.getKeyManagers(),
                            trustManagerFactory.getTrustManagers(), null);

            final SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(true);

            final SSLSession localSession = sslEngine.getSession();
            final int appBufferMax = localSession.getApplicationBufferSize();

            while(!sslEngine.isInboundDone() &&
                  !sslEngine.isOutboundDone()) {
                /*
                 * Excerpt from sample:
                 * We'll make the input buffers a bit bigger than the max needed
                 * size, so that unwrap()s following a successful data transfer
                 * won't generate BUFFER_OVERFLOWS.
                 */
                final ByteBuffer serverIn = ByteBuffer.allocate(appBufferMax + BUFFER_PAD_BYTES);
                final ByteBuffer clientIn = ByteBuffer.allocate(appBufferMax + BUFFER_PAD_BYTES);

                final ByteBuffer initial = ByteBuffer.wrap("TEST".getBytes());
                final SSLEngineResult serverResult = sslEngine.wrap(initial, serverIn);
                runDelegatedTasks(serverResult, sslEngine);

                logger.debug("Handshake status: " + serverResult.getHandshakeStatus().toString());
                int nr = input.read(clientIn);
                logger.debug("Read " + nr + " bytes from client");

                final ByteBuffer unwrappedClientIn = ByteBuffer.allocate(appBufferMax + BUFFER_PAD_BYTES);
                SSLEngineResult clientResult = sslEngine.unwrap(clientIn, unwrappedClientIn);
                logger.debug("Read in client data: " + new String(unwrappedClientIn.array()));
                runDelegatedTasks(clientResult, sslEngine);

                break;
            }

        } catch (KeyManagementException e) {
            logger.error("Unable to read stores: " + e.getLocalizedMessage(), e);
            throw new PulseException(HttpStatus.INTERNAL_ERROR, "Internal SSL Failure");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Invalid encryption algorithem: " + e.getLocalizedMessage(), e);
            throw new PulseException(HttpStatus.INTERNAL_ERROR, "Internal SSL Failure");
        } catch (IOException e) {
            logger.warn("Client crapped out while decoding ssl handshake: " + e.getLocalizedMessage());
            throw new PulseException(HttpStatus.BAD_REQUEST, "Invalid SSL Stream");
        }
        return input;
    }

    /*
     * If the result indicates that we have outstanding tasks to do,
     * go ahead and run them in this thread.
     */
    private static void runDelegatedTasks(SSLEngineResult result,
                                          SSLEngine engine) {
        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            logger.info("NEED_TASK");

            Runnable runnable;

            // TODO: Run async with Fibers
            while ((runnable = engine.getDelegatedTask()) != null) {
                logger.debug("running delegated task...");
                runnable.run();
            }

            SSLEngineResult.HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw new PulseException(HttpStatus.INTERNAL_ERROR, "handshake shouldn't need additional tasks");
            }

            logger.debug("\tnew HandshakeStatus: " + hsStatus);
        }

        logger.info("Finishing delegated tasks.");
    }
}
