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
    public static final byte[] INDEX_COLUMN_FAMILY = "index".getBytes();
    public static final byte[] DATA_COLUMN_FAMILY = "data".getBytes();
    public static final byte[] CURRENT_QUALIFIER = "current".getBytes();
    public static final byte[] DATA_QUALIFER = "data".getBytes();

    private HBaseClient hBaseClient;
    private byte[] tableName;

    public HBaseWrapper(@NotNull String zkQuorum, @NotNull String tableName, Executor executor) {
        this.tableName = tableName.getBytes();
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
        String currentRowKey = index.getModuleName() + "::" + index.getModuleId();
        String oldRowKey = currentRowKey + "::" + index.getTimestamp();

        /** Updates the record set as current record */
        index.setId(index.getModuleName() + "::" + index.getModuleId());
        index.put("current", true);
        byte[] indexBytes = compressAvro(index);
        PutRequest currentIndex = new PutRequest(tableName, currentRowKey.getBytes(),
                INDEX_COLUMN_FAMILY, CURRENT_QUALIFIER, indexBytes);
        PutRequest currentData = new PutRequest(tableName, currentRowKey.getBytes(),
                DATA_COLUMN_FAMILY, "data".getBytes(), rawData);

        /** Inserts the entry marked as old to keep history of changes */
        index.setId(index.getModuleName() + "::" + index.getModuleId() + "::" + index.getTimestamp());
        index.put("current", false);
        PutRequest oldIndex = new PutRequest(tableName, currentRowKey.getBytes(),
                INDEX_COLUMN_FAMILY, CURRENT_QUALIFIER, indexBytes);
        PutRequest oldData = new PutRequest(tableName, oldRowKey.getBytes(),
                DATA_COLUMN_FAMILY, "data".getBytes(), rawData);


        Deferred currentIndexD = hBaseClient.put(currentIndex);
        Deferred oldIndexD = hBaseClient.put(oldIndex);
        currentIndexD.joinUninterruptibly();
        oldIndexD.joinUninterruptibly();

        /*
        Deferred currendDataD = hBaseClient.put(currentData);
        Deferred oldDataD = hBaseClient.put(oldData);
        currendDataD.joinUninterruptibly();
        oldDataD.joinUninterruptibly();
        */
    }

    /**
     * Gets the raw data for the given ID. The caller needs to know how to interpret the binary
     * array.
     */
    public byte[] getData(String moduleName, String moduleId, Long timestamp) throws Exception {
        logger.debug("getting data for " + moduleName + ", " + moduleId + ", " + timestamp);
        String rowKey = moduleName + "::" + moduleId + "::" + timestamp;
        GetRequest request = new GetRequest(tableName, rowKey.getBytes(), DATA_COLUMN_FAMILY, DATA_QUALIFER);
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
        logger.debug("compressing avro to size " + arr.length);
        return arr;
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
            logger.error("Interrupt exception looking for " + tableName + ":" + column, e);
        }
        if (fail.get()) {
            logger.error("Table or column family " + tableName + ":" + column + "does not exist");
            throw new RuntimeException("Table or column family " + tableName + ":" + column +
                    "does not exist");
        }
    }
}
