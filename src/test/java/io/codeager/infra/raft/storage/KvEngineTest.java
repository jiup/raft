package io.codeager.infra.raft.storage;

import org.junit.Test;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Jiupeng Zhang
 * @since 04/24/2019
 */
public class KvEngineTest {
    @Test
    public void demo() {
        try (KvEngine engine = new KvEngine()) {
            engine.map("map1").put("a", "2".getBytes());
            engine.map("map2").put("a", "3".getBytes());
            engine.map("map1").put("b", "4".getBytes());
            System.out.println(engine.map("map1").keySet());
            System.out.println(engine.map("map2").keySet());
            ConcurrentMap<String, Object> map = new RevocableMapAdapter<>(engine.map("default"));
        }
    }

    @Test
    public void combinedTest() {
        try (KvEngine engine = new KvEngine()) {
            RevocableMap<String, String> map = new RevocableMapAdapter<>(engine.map("default"));
            map.put("k1", "v1");
            map.put("k2", "v1");
            map.put("k1", "v2");
            System.out.println(map);
            map.revoke("k1");
            System.out.println(map);
            map.revoke("k2");
            System.out.println(map);
        }
    }
}
