package io.codeager.infra.raft.storage;

import org.apache.commons.lang.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Jiupeng Zhang
 * @since 04/30/2019
 */
public class RevocableMapAdapter<K, V> implements RevocableMap<K, V> {
    // concurrent performance downgraded for versioning
    private ConcurrentMap<K, byte[]> internalMap;
    private volatile int size;

    public static class RevisionNode<T> implements Serializable {
        T data;
        RevisionNode<T> next = null;

        private static <T> RevisionNode<T> from(byte[] bytes) {
            Object obj = SerializationUtils.deserialize(bytes);
            if (obj instanceof RevisionNode) {
                return (RevisionNode<T>) obj;
            }
            throw new IllegalArgumentException("bytes were not serialized from expected type");
        }

        public RevisionNode(T data) {
            this.data = data;
        }

        public byte[] getBytes() {
            return SerializationUtils.serialize(this);
        }

        @Override
        public String toString() {
            return "(" + data + ")" + (next == null ? "" : "<-" + next.toString());
        }
    }

    private class Entry<EK, EV> implements Map.Entry<K, V> {
        private K key;
        private V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("entry is immutable");
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    @Override
    public V revoke(K key) {
        if (key == null)
            throw new IllegalArgumentException("revoke key cannot be null");

        synchronized (this) {
            if (internalMap.containsKey(key)) {
                RevisionNode<V> node = RevisionNode.from(internalMap.get(key));
                if (node.next == null) {
                    internalMap.remove(key);
                    if (node.data != null) this.size--;
                    return node.data;
                } else {
                    internalMap.put(key, node.next.getBytes());
                    if (node.next.data == null && node.data != null) {
                        this.size--;
                    }
                    if (node.next.data != null && node.data == null) {
                        this.size++;
                    }
                    return node.data;
                }
            }
        }
        throw new IllegalStateException("nothing to revoke for key: " + key);
    }

    @Override
    public String intern() {
        Map<K, RevisionNode<V>> viewport = new ConcurrentHashMap<>();
        RevisionNode<V> node;
        synchronized (this) {
            for (Map.Entry<K, byte[]> entry : internalMap.entrySet()) {
                node = RevisionNode.from(entry.getValue());
                viewport.put(entry.getKey(), node);
            }
        }
        return viewport.toString();
    }

    public RevocableMapAdapter(ConcurrentMap<K, byte[]> emptyMap) {
        if (emptyMap.size() > 0) {
            throw new IllegalArgumentException("revocable map adapter must take an empty map");
        }

        this.size = 0;
        this.internalMap = emptyMap;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null)
            throw new IllegalArgumentException("key cannot be null");

        return internalMap.containsKey(key) && RevisionNode.from(internalMap.get(key)).data != null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null)
            throw new IllegalArgumentException("value cannot be null");

        synchronized (this) {
            for (Map.Entry<K, byte[]> entry : internalMap.entrySet()) {
                if (value.equals(RevisionNode.from(entry.getValue()).data)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (key == null)
            throw new IllegalArgumentException("key cannot be null");

        return internalMap.containsKey(key) ? (V) RevisionNode.from(internalMap.get(key)).data : null;
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        if (key == null)
            throw new IllegalArgumentException("key cannot be null");
        if (value == null)
            throw new IllegalArgumentException("value cannot be null");

        synchronized (this) {
            if (internalMap.containsKey(key)) {
                RevisionNode<V> node = RevisionNode.from(internalMap.get(key));
                RevisionNode<V> newNode = new RevisionNode<>(value);
                newNode.next = node;
                internalMap.put(key, newNode.getBytes());
                if (node.data == null) this.size++;
                return node.data;
            } else {
                internalMap.put(key, new RevisionNode<>(value).getBytes());
                this.size++;
                return null;
            }
        }
    }

    @Override
    public V remove(Object key) {
        if (key == null)
            throw new IllegalArgumentException("key cannot be null");

        synchronized (this) {
            if (internalMap.containsKey(key)) {
                RevisionNode<V> node = RevisionNode.from(internalMap.get(key));
                RevisionNode<V> newNode = new RevisionNode<>(null);
                newNode.next = node;
                internalMap.put((K) key, newNode.getBytes());
                if (node.data != null) this.size--;
                return node.data;
            }
        }
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("batch process not supported in revocable map");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("batch process not supported in revocable map");
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        Set<K> keySet = new HashSet<>();
        synchronized (this) {
            for (Map.Entry<K, byte[]> entry : internalMap.entrySet()) {
                if (RevisionNode.from(entry.getValue()).data != null) {
                    keySet.add(entry.getKey());
                }
            }
        }
        return keySet;
    }

    @NotNull
    @Override
    public Collection<V> values() {
        Collection<V> values = new ArrayList<>();
        RevisionNode<V> node;
        synchronized (this) {
            for (byte[] value : internalMap.values()) {
                if ((node = RevisionNode.from(value)).data != null) {
                    values.add(node.data);
                }
            }
        }
        return values;
    }

    @NotNull
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = new HashSet<>();
        RevisionNode<V> node;
        synchronized (this) {
            for (Map.Entry<K, byte[]> entry : internalMap.entrySet()) {
                if ((node = RevisionNode.from(entry.getValue())).data != null) {
                    entrySet.add(new Entry<>(entry.getKey(), node.data));
                }
            }
        }
        return entrySet;
    }

    @Override
    public V putIfAbsent(@NotNull K key, V value) {
        if (value == null)
            throw new IllegalArgumentException("value cannot be null");

        RevisionNode<V> node = null;
        synchronized (this) {
            if (!internalMap.containsKey(key) || (node = RevisionNode.from(internalMap.get(key))).data == null) {
                RevisionNode<V> newNode = new RevisionNode<>(value);
                newNode.next = node;
                internalMap.put(key, newNode.getBytes());
                this.size++;
                return null;
            }
        }
        return node.data;
    }

    @Override
    public boolean remove(@NotNull Object key, Object value) {
        if (value == null)
            throw new IllegalArgumentException("value cannot be null");

        RevisionNode<V> node;
        synchronized (this) {
            if (internalMap.containsKey(key) && (node = RevisionNode.from(internalMap.get(key))).data != null) {
                RevisionNode<V> newNode = new RevisionNode<>(null);
                newNode.next = node;
                internalMap.put((K) key, newNode.getBytes());
                if (node.data != null) this.size--;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean replace(@NotNull K key, @NotNull V oldValue, @NotNull V newValue) {
        RevisionNode<V> node;
        synchronized (this) {
            if (internalMap.containsKey(key) && (node = RevisionNode.from(internalMap.get(key))).data == oldValue) {
                RevisionNode<V> newNode = new RevisionNode<>(newValue);
                newNode.next = node;
                internalMap.put(key, newNode.getBytes());
                return true;
            }
        }
        return false;
    }

    @Override
    public V replace(@NotNull K key, @NotNull V value) {
        RevisionNode<V> node;
        V oldValue;
        synchronized (this) {
            if (internalMap.containsKey(key) && (oldValue = (node = RevisionNode.from(internalMap.get(key))).data) != null) {
                RevisionNode<V> newNode = new RevisionNode<>(value);
                newNode.next = node;
                internalMap.put(key, newNode.getBytes());
                return oldValue;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String entrySetToString = entrySet().toString();
        return "{" + entrySetToString.substring(1, entrySetToString.length() - 1) + "}";
    }
}
