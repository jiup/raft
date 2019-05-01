package io.codeager.infra.raft.storage;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jiupeng Zhang
 * @since 04/30/2019
 */
public class RevocableMapTest {
    @Test
    public void basicRevokeTest() {
        RevocableMap<String, String> map = new RevocableMapAdapter<>(new ConcurrentHashMap<String, byte[]>());
        Assert.assertNull(map.put("k1", "v1"));
        Assert.assertEquals("v1", map.put("k1", "v2"));
        Assert.assertEquals("v2", map.put("k1", "v3"));
        Assert.assertEquals("v3", map.get("k1"));
        Assert.assertEquals("v3", map.revoke("k1"));
        Assert.assertEquals("v2", map.get("k1"));
        Assert.assertEquals("v2", map.revoke("k1"));
        Assert.assertEquals("v1", map.get("k1"));
        Assert.assertEquals("v1", map.revoke("k1"));
        Assert.assertNull(map.get("k1"));
    }

    @Test
    public void removeRevokeTest() {
        RevocableMap<String, String> map = new RevocableMapAdapter<>(new ConcurrentHashMap<String, byte[]>());
        Assert.assertNull(map.put("k1", "v1"));
        Assert.assertEquals("v1", map.put("k1", "v2"));
        Assert.assertEquals("v2", map.remove("k1"));
        Assert.assertNull(map.remove("k1"));
        Assert.assertNull(map.get("k1"));
        Assert.assertEquals(0, map.size());
        Assert.assertNull(map.revoke("k1"));
        Assert.assertNull(map.get("k1"));
        Assert.assertEquals(0, map.size());
        Assert.assertNull(map.revoke("k1"));
        Assert.assertEquals("v2", map.get("k1"));
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("v2", map.revoke("k1"));
        Assert.assertEquals("v1", map.get("k1"));
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("v1", map.revoke("k1"));
        Assert.assertNull(map.get("k1"));
        Assert.assertEquals(0, map.size());
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionTest() {
        RevocableMap<String, String> map = new RevocableMapAdapter<>(new ConcurrentHashMap<String, byte[]>());
        Assert.assertNull(map.remove("k1"));
        map.revoke("k1");
    }

    @Test
    public void toStringTest() {
        RevocableMap<String, String> map = new RevocableMapAdapter<>(new ConcurrentHashMap<String, byte[]>());
        map.put("k1", "v1");
        System.out.println(map.size());
        map.put("k2", "v1");
        System.out.println(map.size());
        map.put("k1", "v2");
        System.out.println(map.size());
        map.remove("k2");
        System.out.println(map + "\nintern: " + map.intern());
        System.out.println(map.size());
        map.revoke("k1");
        System.out.println(map + "\nintern: " + map.intern());
        System.out.println(map.size());
        map.revoke("k1");
        System.out.println(map + "\nintern: " + map.intern());
        System.out.println(map.size());
        map.revoke("k2");
        System.out.println(map + "\nintern: " + map.intern());
        System.out.println(map.size());
        map.revoke("k2");
        System.out.println(map + "\nintern: " + map.intern());
        System.out.println(map.size());
    }
}
