package net.digitalbebop.indexer;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.digitalbebop.PulseModule;
import net.digitalbebop.PulseProperties;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hbase.async.GetRequest;
import org.hbase.async.HBaseClient;
import org.hbase.async.PutRequest;

/**
 * Wraps around all the HBase actions. This keeps its own thread pool to allow for asynchronous
 * put requests. All properties are set through Pulse's configuration.
 */
public class HBaseWrapper {
    private static final Logger logger = LogManager.getLogger(HBaseWrapper.class);
    private static final String DEFAULT_ZK_DIR = "/hbase";
    private static final byte[] INDEX_COLUMN_FAMILY = "index".getBytes();
    private static final byte[] DATA_COLUMN_FAMILY = "data".getBytes();
    private static final byte[] CURRENT_QUALIFIER = "current".getBytes();

    private HBaseClient hBaseClient;
    private byte[] tableName;

    protected static PulseProperties defaultProperties;

    static {
        Injector injector = Guice.createInjector(new PulseModule());
        defaultProperties = injector.getInstance(PulseProperties.class);
    }

    public HBaseWrapper() {
        this.tableName = defaultProperties.HBaseTable.getBytes();
        hBaseClient = new HBaseClient(defaultProperties.ZookeeperQuorum,
                DEFAULT_ZK_DIR, Executors.newCachedThreadPool());

        tableExists(INDEX_COLUMN_FAMILY);
        tableExists(DATA_COLUMN_FAMILY);
    }

    /**
     * Inserts an index record into HBase asynchronously.
     * @param index compresses this Avro record to store into HBase
     * @param current determines if this record is the most up to date version of the element.
     *                Stores the most current version in index:current, else it is stored in
     *                index:<timestamp>
     * @throws Exception
     */
    public void putIndex(PulseAvroIndex index, boolean current) throws Exception {
        logger.debug("Putting index for " + index.getModuleName() + ", " + index.getModuleId());
        byte[] rowKey = generateRowKey(index.getModuleName(), index.getModuleId());
        byte[] qualifier;

        if (current) {
            qualifier = CURRENT_QUALIFIER;
        } else {
            qualifier = index.getTimestamp().toString().getBytes();
        }

        PutRequest indexRequest = new PutRequest(tableName, rowKey, INDEX_COLUMN_FAMILY,
                qualifier, compressAvro(index));
        hBaseClient.put(indexRequest).addCallbacks(
                req -> {
                    logger.debug("Successfully inserted index: " + index.getModuleName() + ", " +
                            index.getModuleId() + "," + new String(qualifier));
                    return null;
                },
                exception -> {
                    logger.error("Error inserting index" + index.getModuleName() + ", " +
                            index.getModuleId() + "," + new String(qualifier), exception);
                    return null;
                });
    }

    /**
     * Inserts the raw payload into the data section of HBase asynchronously.
     * @throws Exception
     */
    public void putData(String moduleName, String moduleId, Long timestamp, byte[] payload)
            throws Exception {
        logger.debug("Putting data for " + moduleName + ", " + moduleId + ", " + timestamp);
        byte[] rowKey = generateRowKey(moduleName, moduleId);
        PutRequest dataRequest = new PutRequest(tableName, rowKey, DATA_COLUMN_FAMILY,
                timestamp.toString().getBytes(), payload);

        hBaseClient.put(dataRequest).addCallbacks(
                req -> {
                    logger.debug("Successfully inserted data: " + moduleName + ", " +
                            moduleId + "," + timestamp.toString());
                    return null;
                },
                exception -> {
                    logger.error("Error inserting data: " + moduleName + ", " +
                            moduleId + "," + timestamp.toString(), exception);
                    return null;
                });
    }

    /**
     * Gets the raw data for the given ID. The caller needs to know how to interpret the binary
     * array.
     */
    public byte[] getData(String moduleName, String moduleId, Long timestamp) throws Exception {
        logger.debug("getting data for " + moduleName + ", " + moduleId + ", " + timestamp);
        byte[] rowKey = generateRowKey(moduleName, moduleId);
        byte[] qualifier = timestamp.toString().getBytes();
        GetRequest request = new GetRequest(tableName, rowKey, DATA_COLUMN_FAMILY, qualifier);
        return hBaseClient.get(request).joinUninterruptibly(3000).get(0).value();
    }

    private byte[] compressAvro(PulseAvroIndex index) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        SpecificDatumWriter<PulseAvroIndex> writer = new SpecificDatumWriter<>(index.getSchema());
        writer.write(index, encoder);
        encoder.flush();
        out.close();
        byte[] arr = out.toByteArray();
        logger.debug("compressing avro to " + arr.length + " bytes");
        return arr;
    }

    private byte[] generateRowKey(String moduleName, String moduleId) {
        return (moduleName + "::" + moduleId).getBytes();
    }

    /**
     * Checks to see if the table and the given column family exists, throws RuntimeException
     * otherwise
     */
    private void tableExists(byte[] column) throws RuntimeException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean fail = new AtomicBoolean(true);
        hBaseClient.ensureTableFamilyExists(tableName, column).addCallbacks(
                arg -> {
                    latch.countDown();
                    fail.set(false);
                    return null;
                },
                arg -> {
                    latch.countDown();
                    return null;
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Interrupt exception looking for " + new String(tableName) + ":" +
                    new String(column), e);
        }
        if (fail.get()) {
            logger.error("Table or column family " + new String(tableName) + ":" +
                    new String(column) + "does not exist");
            throw new RuntimeException("Table or column family " + new String(tableName) + ":" +
                    new String(column) + "does not exist");
        }
    }
}