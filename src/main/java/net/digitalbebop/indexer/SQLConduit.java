package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.ClientRequests;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLConduit implements IndexConduit {
    private static Logger logger = LogManager.getLogger(SQLConduit.class);

    private final String jdbc;
    private final String username;
    private final String password;
    private final SolrConduit solrConduit;

    @Inject
    public SQLConduit(@Named("sqlJDBC") String jdbc,
                      @Named("sqlUser") String username,
                      @Named("sqlPassword") String password,
                      SolrConduit solrConduit) throws SQLException {
        logger.info("Initializing sql conduit for user " + username + " on " + jdbc);

        this.jdbc = jdbc;
        this.username = username;
        this.password = password;
        this.solrConduit = solrConduit;
    }

    /**
     * Serve an index request into a backend SQL layer.
     * XXX: WARNING: Testing only currently
     * @param indexRequest Index request protobuf message
     */
    @Override
    public synchronized void index(ClientRequests.IndexRequest indexRequest) {
        try {
            System.out.println("JDBC: " + jdbc);
            System.out.println("user: " + username);
            System.out.println("pass: " + password);

            final Connection connection = DriverManager.getConnection(jdbc, username, password);
            Statement stmt = connection.createStatement();

            String tags = "[";
            for (String tag : indexRequest.getTagsList()) {
                tags += tag + ", ";
            }
            tags += "sql]";

            /* XXX: Using dummy values */
            String sql = "INSERT INTO pulse VALUES (" +
                    "\"0\", " +
                    System.currentTimeMillis() + ", " +
                    "1, " +
                    "\"test\", " +
                    "\"" + tags + "\", " +
                    "\"" + indexRequest.getUsername() + "\", " +
                    "0, " +
                    "\"" + indexRequest.getModuleName() + "\", " +
                    "\"" + indexRequest.getModuleId() + "\", " +
                    "\"" + indexRequest.getMetaTags() + "\", " +
            "\"" + indexRequest.getIndexData() + "\", " +
                    "\"" + indexRequest.getLocation() + "\"" +
                    ");";

            logger.info("Executing: " + sql);
            stmt.execute(sql);
        } catch (Exception e) {
            logger.info("ERROR: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(ClientRequests.DeleteRequest deleteRequest) {
        throw new NotImplementedException("Deletion in SQLConduit Not Implemented.");
    }
}
