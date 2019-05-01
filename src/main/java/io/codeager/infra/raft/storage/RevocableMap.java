package io.codeager.infra.raft.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Jiupeng Zhang
 * @since 04/30/2019
 */
public interface RevocableMap<K, V> extends ConcurrentMap<K, V> {
    V revoke(K key);

    Map<K, RevocableMapAdapter.RevisionNode<V>> intern();
}
