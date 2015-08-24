package net.digitalbebop;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public class UnitTestBase {

    private static QuorumPeerConfig peerConfig;
    private static Injector injector;

    static {
    }

    public UnitTestBase() {
        PropertyConfigurator.configure(UnitTestBase.class.getResource("log4j-test.xml"));
        injector = Guice.createInjector(new UnitTestBaseModule());
    }

    public Injector injector() {
        Assert.assertTrue(injector != null);
        return injector;
    }

    @BeforeClass
    public static void initEnv() throws Exception {
        peerConfig = new QuorumPeerConfig();
    }

    @AfterClass
    public static void destroyEnv() {

    }
}
