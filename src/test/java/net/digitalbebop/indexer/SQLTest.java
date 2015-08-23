package net.digitalbebop.indexer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.digitalbebop.ClientRequests;
import org.junit.BeforeClass;
import org.junit.Test;

public class SQLTest {
    private static Injector injector;
    private final ClientRequests.IndexRequest index =  ClientRequests.IndexRequest.newBuilder()
            .setIndexData("FooIndex")
            .setLocation("FooLocation")
            .setMetaTags("{}")
            .setModuleId("FooID")
            .setModuleName("FooModule")
            .addTags("FooTag1")
            .setUsername("slackwill")
            .setTimestamp(0)
            .build();

    @BeforeClass
    public static void configureTests() {
        injector = Guice.createInjector(new SQLTestModule());
    }

    @Test
    public void indexTest() {
        SQLConduit conduit = injector.getInstance(SQLConduit.class);

        conduit.index(index);
    }
}
