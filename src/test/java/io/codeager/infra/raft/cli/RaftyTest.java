package io.codeager.infra.raft.cli;

import io.codeager.infra.raft.conf.Configuration;
import io.codeager.infra.raft.core.LocalNode;
import io.codeager.infra.raft.core.StateMachine;
import io.codeager.infra.raft.core.entity.Endpoint;
import io.codeager.infra.raft.util.ConfigurationHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Jiupeng Zhang
 * @since 05/03/2019
 */
public class RaftyTest {
    @Test
    public void initializeNode1() throws IOException {
        Configuration configuration = ConfigurationHelper.load("config.json");
        StateMachine stateMachine = new StateMachine();
        LocalNode node = new LocalNode(configuration, stateMachine);
        stateMachine.bind(node);
        node.start();
    }

    @Test
    public void initializeNode2() throws IOException {
        Configuration configuration = ConfigurationHelper.load("config1.json");
        StateMachine stateMachine = new StateMachine();
        LocalNode node = new LocalNode(configuration, stateMachine);
        stateMachine.bind(node);
        node.start();
    }

    @Test
    public void initializeNode3() throws IOException {
        Configuration configuration = ConfigurationHelper.load("config2.json");
        StateMachine stateMachine = new StateMachine();
        LocalNode node = new LocalNode(configuration, stateMachine);
        stateMachine.bind(node);
        node.start();
    }

    @Test
    public void initializeNode4() throws IOException {
        Configuration configuration = ConfigurationHelper.load("config3.json");
        StateMachine stateMachine = new StateMachine();
        LocalNode node = new LocalNode(configuration, stateMachine);
        stateMachine.bind(node);
        node.start();
    }

    @Test
    public void initializeNode5() throws IOException {
        Configuration configuration = ConfigurationHelper.load("config4.json");
        StateMachine stateMachine = new StateMachine();
        LocalNode node = new LocalNode(configuration, stateMachine);
        stateMachine.bind(node);
        node.start();
    }
    @Test
    public void test() {
        try (Rafty rafty = Rafty.create(Endpoint.of(":5001"))) {
            RaftyMap<String, String> map = rafty.subscribe("test");
            map.put("name", "zhangyu");
            Assert.assertEquals("zhangyu", map.get("name"));
            map.put("name", "jiupeng");
            Assert.assertEquals("jiupeng", map.get("name"));
            map.put("id", "yzh269");
            Assert.assertEquals(2, map.size());

            System.out.println(map.keySet());
            Assert.assertTrue(map.containsKey("name"));
            Assert.assertTrue(map.containsKey("id"));
//
            System.out.println(map.values());
            Assert.assertTrue(map.containsValue("jiupeng"));
            Assert.assertTrue(map.containsValue("yzh269"));
            map.remove("id");
            Assert.assertNull(map.get("id"));
        }
    }

    @Test
    public void testBadnetwork() {
        try (Rafty rafty = Rafty.create(Endpoint.of(":5002"))) {
            RaftyMap<String, String> map = rafty.subscribe("test");
            map.put("id", "jzh");
//            map.put("name","jiupeng");
            System.out.println(map.get("id"));
            //shut down 5001
//            map.put("name","jiupeng");
//            map.put("id","yzh269");
//            System.out.println(map.get("id"));
//            System.out.println(map.get("name"));
        }
    }

    @Test
    public void multiDataWork() {
        try (Rafty rafty = Rafty.create(Endpoint.of(":5004"))) {
            RaftyMap<String, String> map = rafty.subscribe("test");
            for (int i = 0; i < 100; i++) {
                map.put(String.valueOf(i), String.valueOf(i + 10));
            }
            System.out.println(map.size());

        }
    }
}
