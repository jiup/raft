package io.codeager.infra.raft.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author Jiupeng Zhang
 * @since 04/25/2019
 */
@Experimental(value = Experimental.Statement.NOT_FULLY_CODED, ref = Configuration.class)
public class ConfigurationHelper {
    public static final Logger LOG = LoggerFactory.getLogger(ConfigurationHelper.class);
    private static final String BASE_PATH = System.getProperty("user.dir").replaceAll("\\\\", "/");
    public static final String DEFAULT_PATH = "config.json";

    public static Configuration load() throws IOException {
        return load(DEFAULT_PATH);
    }

    public static Configuration load(String path) throws IOException {
        InputStream inputStream = ConfigurationHelper.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            try {
                inputStream = new FileInputStream(new File(BASE_PATH, path));
                LOG.debug("found '{}' in user.dir", path);
            } catch (FileNotFoundException e) {
                LOG.error("file not found: {}", e.getMessage());
                throw new FileNotFoundException();
            }
        } else {
            LOG.debug("found '{}' in classpath", path);
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return mapper.reader(Configuration.class).readValue(inputStream);
    }

    public static void save(Configuration configuration) throws IOException {
        save(configuration, DEFAULT_PATH);
    }

    public static void save(Configuration configuration, String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(DEFAULT_PATH), configuration);
    }

    public static boolean validate(Configuration configuration) {
        LOG.error("validation not passed");
        return false;
    }
}
