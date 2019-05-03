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
        try (Rafty rafty = Rafty.connect(Endpoint.of(":9990"))) {
            RaftyMap<String, String> map = rafty.subscribe("test");
            Assert.assertNotNull(map);
        }
    }
}
