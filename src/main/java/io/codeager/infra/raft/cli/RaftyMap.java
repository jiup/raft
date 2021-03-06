package io.codeager.infra.raft.cli;

import io.codeager.infra.raft.DistributedMap;
import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.cli.rpc.Client;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Jiupeng Zhang
 * @since 05/03/2019
 */
@Experimental(Experimental.Statement.TODO_TEST)
public class RaftyMap<K, V> implements DistributedMap<K, V> {
    public static final Logger LOG = LoggerFactory.getLogger(RaftyMap.class);

    private String name;
    private String prefix;
    private Client client;

    RaftyMap(String name, Client client) {
        if (name != null) {
            LOG.info("channelled map is not fully functional");
        }

        this.name = name;
        this.prefix = (name != null && name.length() > 0) ? name.concat(":") : "";
        this.client = client;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return client.getId();
    }

    @Override
    public int size() {
        return client.size();
    }

    @Override
    public boolean isEmpty() {
        return client.size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return client.containsKey(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        return client.containsValue(value.toString());
    }

    @Override
    public V get(Object key) {
        return (V) client.get(key.toString());
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        V result = (V) client.get(key.toString());
        client.store(key.toString(), value.toString());
        return result;
    }

    @Override
    public V remove(Object key) {
        V result = (V) client.get(key.toString());
        client.remove(key.toString());
        return result;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return (Set<K>) client.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return (Collection<V>) client.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return client.entries();
    }

    @Override
    public V putIfAbsent(@NotNull K key, V value) {
        synchronized (this) {
            if (containsKey(key)) {
                return (V) client.get(key.toString());
            } else {
                client.store(key.toString(), value.toString());
                return null;
            }
        }
    }

    @Override
    public boolean remove(@NotNull Object key, Object value) {
        return client.remove(key.toString());
    }

    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
        if (oldValue.equals(client.get(key.toString()))) {
            return client.store(key.toString(), newValue.toString());
        } else {
            return false;
        }
    }

    @Override
    public V replace(@NotNull K key, @NotNull V value) {
        V result = (V) client.get(key.toString());
        client.store(key.toString(), value.toString());
        return result;
    }
}
