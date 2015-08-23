package net.digitalbebop.indexer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import net.digitalbebop.ClientRequests;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class SQLConduit implements IndexConduit {
    private static Logger logger = LogManager.getLogger(SQLConduit.class);

    @Inject
    public SQLConduit(@Named("sqlJDBC") String jdbc,
                      @Named("sqlUser") String username,
                      @Named("sqlPassword") String password) {

        logger.info("Initializing sql conduit for user " + username + " on " + jdbc);
    }

    @Override
    public void index(ClientRequests.IndexRequest indexRequest) {

    }

    @Override
    public void delete(ClientRequests.DeleteRequest deleteRequest) {

    }
}
