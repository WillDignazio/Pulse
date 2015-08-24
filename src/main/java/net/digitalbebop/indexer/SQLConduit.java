package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.ClientRequests;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.*;

public class SQLConduit implements IndexConduit {
    private static Logger logger = LogManager.getLogger(SQLConduit.class);

    private final String jdbc;
    private final String username;
    private final String password;
    private final SolrConduit solrConduit;

    private Connection connection;

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

    @Override
    public void index(ClientRequests.IndexRequest indexRequest) {
        try {
            System.out.println("JDBC: " + jdbc);
            System.out.println("user: " + username);
            System.out.println("pass: " + password);

            final Connection connection = DriverManager.getConnection(jdbc, username, password);

            Statement stmt = connection.createStatement();
            ResultSet results = stmt.executeQuery("SELECT * FROM *");

            logger.info("RESULTS: " + results.toString());
        } catch (Exception e) {
            logger.info("ERROR: " + e.getLocalizedMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(ClientRequests.DeleteRequest deleteRequest) {

    }
}
