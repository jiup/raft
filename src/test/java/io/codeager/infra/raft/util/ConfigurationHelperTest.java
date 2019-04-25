package io.codeager.infra.raft.util;

import io.codeager.infra.raft.conf.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Jiupeng Zhang
 * @since 04/25/2019
 */
public class ConfigurationHelperTest {
    @Test
    public void saveTest() throws IOException {
        ConfigurationHelper.save(new Configuration());
        Assert.assertTrue(new File(ConfigurationHelper.DEFAULT_PATH).exists());
    }

    @Test
    public void loadTest() throws IOException {
        Assert.assertNotNull(ConfigurationHelper.load());
    }
}
