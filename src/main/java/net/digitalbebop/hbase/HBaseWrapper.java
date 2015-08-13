package net.digitalbebop.hbase;

import com.stumbleupon.async.Deferred;
import net.digitalbebop.avro.PulseAvroIndex;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hbase.async.GetRequest;
import org.hbase.async.HBaseClient;
import org.hbase.async.KeyValue;
import org.hbase.async.PutRequest;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.validation.constraints.NotNull;

public class HBaseWrapper {
    private static final Logger logger = LogManager.getLogger(HBaseWrapper.class);
    public static final String DEFAULT_ZK_DIR = "/hbase";
    public static final String INDEX_COLUMN_FAMILY = "index";
    public static final String DATA_COLUMN_FAMILY = "data";
    public static final String CURRENT_QUALIFIER = "current";

    private HBaseClient hBaseClient;
    private String tableName;

    public HBaseWrapper(@NotNull String zkQuorum, @NotNull String tableName, Executor executor) {
        this.tableName = tableName;
        hBaseClient = new HBaseClient(zkQuorum, DEFAULT_ZK_DIR, executor);

        tableExists(INDEX_COLUMN_FAMILY);
        tableExists(DATA_COLUMN_FAMILY);
    }

    /**
     * Inserts the data with its epoch timestamp, inserts the index marked as current, and
     * inserts an index with its epoch timestamp marked as old. Inserting 2 indexed make it easier
     * in the future to do updates since the current index insert will always overwrite an existing
     * one.
     */
    public void putIndex(PulseAvroIndex index, byte[] rawData) throws Exception {
        logger.debug("putting new index for " + index.getModuleName() + ", " + index.getModuleId());
        String currentRowKey = index.getModuleName() + "-" + index.getModuleId() + "-current";
        String oldRowKey = index.getModuleName() + "-" + index.getModuleId() + "-" + index.getTimestamp();

        PutRequest currentIndex = new PutRequest(tableName.getBytes(), currentRowKey.getBytes(),
                INDEX_COLUMN_FAMILY.getBytes(), CURRENT_QUALIFIER.getBytes(), compressAvro(index));
        index.put("current", false);
        PutRequest oldIndex = new PutRequest(tableName.getBytes(), oldRowKey.getBytes(),
                INDEX_COLUMN_FAMILY.getBytes(), "index".getBytes(),
                compressAvro(index));
        PutRequest dataRequest = new PutRequest(tableName.getBytes(), oldRowKey.getBytes(),
                DATA_COLUMN_FAMILY.getBytes(), "data".getBytes(), rawData);

        Deferred dataPut = hBaseClient.put(dataRequest);
        Deferred currentPut = hBaseClient.put(currentIndex);
        Deferred oldPut = hBaseClient.put(oldIndex);

        dataPut.joinUninterruptibly();
        currentPut.joinUninterruptibly();
        oldPut.joinUninterruptibly();
    }

    /**
     * Gets the raw data for the given ID. The caller needs to know how to interpret the binary
     * array.
     */
    public byte[] getData(String moduleName, String moduleId, Long timestamp) throws Exception {
        logger.debug("getting data for " + moduleName + ", " + moduleId + ", " + timestamp);
        String rowKey = moduleName + "-" + moduleId + "-" + timestamp;
        GetRequest request = new GetRequest(tableName, rowKey, DATA_COLUMN_FAMILY, timestamp.toString());
        return hBaseClient.get(request).joinUninterruptibly(3000).get(0).value();
    }

    private byte[] compressAvro(PulseAvroIndex index) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        SpecificDatumWriter<PulseAvroIndex> writer = new SpecificDatumWriter<>(index.getSchema());
        writer.write(index, encoder);
        encoder.flush();
        out.close();
        return out.toByteArray();
    }

    /**
     * Checks to see if the table and the given column family exists, throws RuntimeException
     * otherwise
     */
    private void tableExists(String column) throws RuntimeException {
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
            logger.error("Interrupt exception looking for " + tableName + ":" + column, e);
        }
        if (fail.get()) {
            logger.error("Table or column family " + tableName + ":" + column + "does not exist");
            throw new RuntimeException("Table or column family " + tableName + ":" + column +
                    "does not exist");
        }
    }
}
