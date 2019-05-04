package io.codeager.infra.raft.cli;

import io.codeager.infra.raft.core.entity.Endpoint;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jiupeng Zhang
 * @since 05/03/2019
 */
public class RaftyTest {
    @Test
    public void test() {
        try (Rafty rafty = Rafty.create(Endpoint.of(":9990"))) {
            RaftyMap<String, String> map = rafty.subscribe("test");
//            map.put("k1", "v1");
//            System.out.println(map.get("k1"));
            Assert.assertNotNull(map);
        }
    }
}
