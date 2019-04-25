package io.codeager.infra.raft.util;

import io.codeager.infra.raft.conf.Configuration;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Jiupeng Zhang
 * @since 04/25/2019
 */
public class ConfigurationHelperTest {
    @Test
    public void saveTest() throws IOException {
        ConfigurationHelper.save(new Configuration());
    }

    @Test
    public void loadTest() throws IOException {
        System.out.println(ConfigurationHelper.load());
    }
}
