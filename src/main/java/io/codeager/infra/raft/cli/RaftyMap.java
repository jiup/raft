package io.codeager.infra.raft.cli;

import io.codeager.infra.raft.DistributedMap;
import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.cli.rpc.Client;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Jiupeng Zhang
 * @since 05/03/2019
 */
@Experimental(Experimental.Statement.TODO)
public class RaftyMap<K, V> implements DistributedMap<K, V> {
    private String name;
    private Client client;

    RaftyMap(String name, Client client) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int size() {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(Object key) {
        // todo
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        // todo
        throw new UnsupportedOperationException();
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
        // todo
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        // todo
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(@NotNull K key, V value) {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(@NotNull Object key, Object value) {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
        // todo
        throw new UnsupportedOperationException();
    }

    @Override
    public V replace(@NotNull K key, @NotNull V value) {
        // todo
        throw new UnsupportedOperationException();
    }
}
