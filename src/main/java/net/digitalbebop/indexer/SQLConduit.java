package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.ClientRequests;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConduit implements IndexConduit {
    private static Logger logger = LogManager.getLogger(SQLConduit.class);

    private final Connection connection;
    private final SolrConduit solrConduit;

    @Inject
    public SQLConduit(@Named("sqlJDBC") String jdbc,
                      @Named("sqlUser") String username,
                      @Named("sqlPassword") String password,
                      SolrConduit solrConduit) throws SQLException {
        logger.info("Initializing sql conduit for user " + username + " on " + jdbc);

        this.connection = DriverManager.getConnection(jdbc, username, password);
        this.solrConduit = solrConduit;
    }

    @Override
    public void index(ClientRequests.IndexRequest indexRequest) {

    }

    @Override
    public void delete(ClientRequests.DeleteRequest deleteRequest) {

    }
}
