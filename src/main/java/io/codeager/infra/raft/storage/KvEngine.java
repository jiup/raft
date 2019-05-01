package io.codeager.infra.raft.storage;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Jiupeng Zhang
 * @since 04/30/2019
 */
public class KvEngine implements AutoCloseable {
    private DB database;

    public KvEngine(File file) {
        this.database = DBMaker.fileDB(file).make();
    }

    public KvEngine() {
        this.database = DBMaker.memoryDB().make();
    }

    public ConcurrentMap<String, byte[]> map(String name) {
        return database.hashMap(name, Serializer.STRING, Serializer.BYTE_ARRAY).createOrOpen();
    }

    public DB internal() {
        return database;
    }

    @Override
    public void close() {
        this.database.close();
    }
}
