package io.codeager.infra.raft.cli;

import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.cli.rpc.Client;
import io.codeager.infra.raft.core.entity.Endpoint;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jiupeng Zhang
 * @since 05/01/2019
 */
@Experimental(Experimental.Statement.TODO)
public class Rafty implements Closeable {
    private Client client;
    private Map<String, RaftyMap<?, ?>> channels;

    private Rafty(Endpoint endpoint) {
        this.channels = new HashMap<>();
        this.client = new Client(
                new io.codeager.infra.raft.core.rpc.Client(endpoint.getHost(), endpoint.getPort())
        );
    }

    public static Rafty create(Endpoint endpoint) {
        return new Rafty(endpoint);
    }

    private <K, V> RaftyMap<K, V> createMap(String name, Client client) {
        return new RaftyMap<>(name, client);
    }

    public <K, V> RaftyMap<K, V> subscribe(String name) {
        return map(name);
    }

    public <K, V> RaftyMap<K, V> map(String name) {
        if (!channels.containsKey(name)) {
            channels.put(name, createMap(name, this.client));
        }
        return (RaftyMap<K, V>) channels.get(name);
    }

    public boolean containsRepo(String name) {
        return channels.containsKey(name);
    }

    public void disconnect() {
        client.close();
    }

    @Override
    public void close() {
        disconnect();
    }
}
