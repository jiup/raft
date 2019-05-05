package io.codeager.infra.raft.cli.rpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.StringValue;
import io.codeager.infra.raft.cli.RaftyException;
import io.grpc.vote.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    public boolean containsKey(String key) {
        try {
            return coreClient.containsKey(ContainsRequest.newBuilder().setContent(key).build());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    public boolean containsValue(String value) {
        try {
            return coreClient.containsValue(ContainsRequest.newBuilder().setContent(value).build());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    public String getId() {
        try {
            return coreClient.getRemoteId(GetIdRequest.newBuilder().build());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    public List<String> values() {
        try {
            ValuesResponse response = coreClient.getValues(ValuesRequest.newBuilder().build());
            return response.getValueList().asByteStringList().stream().map(ByteString::toStringUtf8).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    public Set<String> keySet() {
        try {
            KeysResponse response = coreClient.getKeys(KeysRequest.newBuilder().build());
            return response.getKeyList().asByteStringList().stream().map(ByteString::toStringUtf8).collect(Collectors.toSet());
        } catch (Exception e) {
            throw new RaftyException(e);
        }
    }

    public <K, V> Set<Map.Entry<K, V>> entries() {
        try {
            EntriesResponse response = coreClient.getEntries(EntriesRequest.newBuilder().build());
            Set<Map.Entry<K, V>> result = new HashSet<>();
            response.getEntryList().forEach(entry -> result.add(new Map.Entry<K, V>() {
                public K getKey() {
                    return (K) entry.getKey();
                }

                public V getValue() {
                    return (V) entry.getValue().getValue();
                }

                public V setValue(V value) {
                    throw new UnsupportedOperationException("this entry is readonly");
                }
            }));
            return result;
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
