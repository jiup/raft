package io.codeager.infra.raft.core.entity;

import org.junit.Test;

/**
 * @author Jiupeng Zhang
 * @since 04/29/2019
 */
public class EndpointTest {
    @Test
    public void test() {
        Endpoint.from("127.0.0.1:8080");
        Endpoint.from("127.0.0.1", 8088);
        Endpoint.from(":8080");
        Endpoint.from(8080);
    }
}
