package net.digitalbebop.indexer;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

class SQLTestModule extends AbstractModule {
    private static Logger logger = LogManager.getLogger(SQLTestModule.class);

    @Override
    protected void configure() {
        logger.info("Using SQL Test Module");

        Map<String, String> propertyMap = new HashMap<>();

        propertyMap.put("sqlJDBC", "jdbc:sqlite:/tmp");
        propertyMap.put("sqlUser", "");
        propertyMap.put("sqlPassword", "");

        bind(IndexConduit.class).to(SQLConduit.class);
        Names.bindProperties(binder(), propertyMap);
    }
}
