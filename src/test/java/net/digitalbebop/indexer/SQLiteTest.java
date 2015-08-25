package net.digitalbebop.indexer;

import com.google.inject.Key;
import com.google.inject.name.Names;
import net.digitalbebop.ClientRequests;
import net.digitalbebop.UnitTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SQLiteTest extends UnitTestBase {

    private SQLConduit conduit;
    private String jdbc;

    public SQLiteTest() {
        super(false, false);
    }

    private final ClientRequests.IndexRequest index = ClientRequests.IndexRequest.newBuilder()
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
        System.out.println("Using JDBC: " + jdbc);

        File dbFile = new File(jdbc.split(":")[2]);
        System.out.println("FILE: " + dbFile.toString());

        if (dbFile.exists()) {
            System.out.println("Deleting previous test database");
            Assert.assertTrue(dbFile.delete());
        }

        /* Create Template Table */
        final Connection connection = DriverManager.getConnection(jdbc);
        final Statement stmt = connection.createStatement();

        stmt.execute(
                "CREATE TABLE pulse (" +
                        "id TEXT PRIMARY KEY, " +
                        "timestamp INTEGER, " +
                        "current BOOLEAN, " +
                        "format TEXT, " +
                        "tags TEXT, " +
                        "username TEXT, " +
                        "deleted BOOLEAN, " +
                        "moduleName TEXT, " +
                        "moduleId TEXT, " +
                        "metaData TEXT, " +
                        "data TEXT, " +
                        "location TEXT" +
                        ");"
        );

        conduit = injector().getInstance(SQLConduit.class);
    }

    @Test
    public void indexTest() throws ClassNotFoundException {
        conduit.index(index);
    }
}