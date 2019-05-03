package io.codeager.infra.raft;

import java.util.concurrent.ConcurrentMap;

/**
 * @author Jiupeng Zhang
 * @since 05/03/2019
 */
public interface DistributedMap<K, V> extends ConcurrentMap<K, V> {
}
