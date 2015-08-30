package net.digitalbebop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UnitTestBase {
    private static Injector injector;
    private static ExecutorService executor = Executors.newCachedThreadPool();

    private final boolean zkEnabled;
    private final boolean solrEnabled;

    Thread zookeeperThread;
    ZooKeeperServerMain zookeeper;

    static {
        Configurator.initialize("Pulse Test", Thread.currentThread().getContextClassLoader()
                .getResource("log4j-test.xml").getPath());
    }

    public UnitTestBase() {
        this(true, true);
    }

    public UnitTestBase(boolean zkEnabled,
                        boolean solrEnabled) {

        this.zkEnabled = zkEnabled;
        this.solrEnabled = solrEnabled;

        injector = Guice.createInjector(new UnitTestBaseModule());
    }

    public Injector injector() {
        Assert.assertTrue(injector != null);
        return injector;
    }

    private void prepareZookeeper() {
        final QuorumPeerConfig peerConfig;
        final Properties zkProperties;

        try {
            zkProperties = new Properties();
            zkProperties.load(Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("zoo.cfg"));

            String dataDirPath = zkProperties.getProperty("dataDir");
            if (dataDirPath == null) {
                Assert.fail("dataDir wasn't bound, and couldn't guarantee that we cleaned it up.");
            }

            File zkDataDir = new File(dataDirPath);
            FileUtils.deleteDirectory(zkDataDir);

        } catch (FileNotFoundException e) {
            Assert.fail("Unable to find zoo.cfg test resource");
            throw new RuntimeException(e);
        } catch (IOException e) {
            Assert.fail("Failed to load zoo.cfg: " + e.getLocalizedMessage());
            throw new RuntimeException(e);
        }

        peerConfig = new QuorumPeerConfig();
        try {
            peerConfig.parseProperties(zkProperties);
        } catch (IOException e) {
            Assert.fail("Failed to configure zk: " + e.getLocalizedMessage());
        } catch (QuorumPeerConfig.ConfigException e) {
            Assert.fail("Configuration is wrong for zk: " + e.getLocalizedMessage());
        }

        zookeeper = new ZooKeeperServerMain();

        final ServerConfig config = new ServerConfig();
        config.readFrom(peerConfig);

        zookeeperThread = new Thread() {
            @Override
            public void run() {
                try {
                    zookeeper.runFromConfig(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (Exception ignored) {

                }
            }
        };

        executor.submit(zookeeperThread);
    }

    @Before
    public void initEnv() throws IOException, QuorumPeerConfig.ConfigException {
        if (zkEnabled) {
            System.out.println("Initializing zookeeper...");
            prepareZookeeper();
        }
    }

    @After
    public void destroyEnv() throws InterruptedException {
        executor.shutdownNow();
        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }

        executor = Executors.newSingleThreadExecutor();
    }
}
