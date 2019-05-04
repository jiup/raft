package io.codeager.infra.raft.cli.rpc;

import com.google.protobuf.StringValue;
import io.codeager.infra.raft.cli.RaftyException;
import io.grpc.vote.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * @author Jiupeng Zhang
 * @since 05/01/2019
 */
public class Client implements Closeable {
    public static final Logger LOG = LoggerFactory.getLogger(Client.class);

    private io.codeager.infra.raft.core.rpc.Client coreClient;

    public Client(io.codeager.infra.raft.core.rpc.Client rpcClient) {
        this.coreClient = rpcClient;
    }

    io.codeager.infra.raft.core.rpc.Client core() {
        return coreClient;
    }

    public int size() {
        try {
            return coreClient.size(SizeRequest.newBuilder().build());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    public boolean store(String key, String value) {
        try {
            return coreClient.store(StoreRequest.newBuilder()
                    .setEntry(DataEntry.newBuilder()
                            .setKey(key)
                            .setValue(StringValue.of(value))
                            .build())
                    .build());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    public boolean remove(String key) {
        try {
            return coreClient.remove(RemoveRequest.newBuilder().setKey(key).build());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    public String get(String key) {
        try {
            return coreClient.get(GetRequest.newBuilder().setKey(key).build());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    @Override
    public void close() {
        try {
            coreClient.shutdown();
        } catch (InterruptedException e) {
            LOG.error("api-client shut down failure: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
