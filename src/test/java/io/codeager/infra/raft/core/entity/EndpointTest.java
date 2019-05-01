package io.codeager.infra.raft.core.entity;

import org.junit.Test;

/**
 * @author Jiupeng Zhang
 * @since 04/29/2019
 */
public class EndpointTest {
    @Test
    public void test() {
        Endpoint.of("127.0.0.1:8080");
        Endpoint.of("127.0.0.1", 8088);
        Endpoint.of(":8080");
        Endpoint.of(8080);
    }
}
