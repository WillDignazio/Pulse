package net.digitalbebop.indexer;

import com.google.inject.Key;
import com.google.inject.name.Names;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.UnitTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class SQLiteTest extends UnitTestBase {

    private SQLConduit conduit;
    private String jdbc;

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

    @Before
    public void configureTests() throws Exception {
        String jdbc = injector().getInstance(Key.get(String.class, Names.named("sqlJDBC")));
        System.out.println("Using JDBC: "  + jdbc);

        File dbFile = new File(jdbc.split(":")[2]);
        System.out.println("FILE: " + dbFile.toString());

        if (dbFile.exists()) {
            System.out.println("Deleting previous test database");
            Assert.assertTrue(dbFile.delete());
        }



        conduit = injector().getInstance(SQLConduit.class);
    }

    @Test
    public void indexTest() throws ClassNotFoundException {

    }
}