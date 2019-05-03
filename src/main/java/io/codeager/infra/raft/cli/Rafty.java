package io.codeager.infra.raft.cli;

import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.cli.rpc.Client;
import io.codeager.infra.raft.core.entity.Endpoint;

import java.util.Map;

/**
 * @author Jiupeng Zhang
 * @since 05/01/2019
 */
@Experimental(Experimental.Statement.TODO)
public class Rafty {
    private Client client;
    private Map<String, RaftyMap<?, ?>> channels;

    private Rafty() {
    }

    private <K, V> RaftyMap<K, V> createMap(String name, Client client) {
        // todo: initialize a new distributed map instance
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
        // client.close();
    }

    public static Rafty connect(Endpoint endpoint) {
        return new Rafty(); // todo: build and assign raftyClient
    }

    public static void main(String[] args) {
        Rafty rafty = Rafty.connect(Endpoint.of(":9990"));
        RaftyMap<String, String> map = rafty.subscribe("test");
        rafty.disconnect();
    }
}
