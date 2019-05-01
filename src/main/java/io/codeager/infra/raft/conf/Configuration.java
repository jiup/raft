package io.codeager.infra.raft.conf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.core.entity.Endpoint;
import org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jiupeng Zhang
 * @since 04/25/2019
 */
public class Configuration implements Serializable {
    static class LocalNode {
        public String id = "undefined";
        public String name = "undefined";
        public Endpoint endpoint = Endpoint.DEFAULT;
        public long waitTimeout = 40_000; // 40 seconds
        public long voteTimeout = 10_000; // 10 seconds
        public long heartbeatTimeout = 10_000; // 10 seconds
    }

    static class NodeObserver {
        public int maxClient = 1;
        public Endpoint endpoint = Endpoint.of(36507);
    }

    static class Binding {
        public Class cli = Configuration.class;
    }

    static class Logging {
        public String outPath = "/path/to/raft.out";
        public String errPath = "/path/to/raft.err";
    }

    public int maxClient = 1;
    public LocalNode origin = new LocalNode() {{
        id = RandomStringUtils.randomAlphanumeric(8).toLowerCase();
        name = "node-".concat(id);
    }};
    public Set<Endpoint> registry = new HashSet<>();
    public NodeObserver nodeObserver = new NodeObserver();
    public Logging logging = new Logging();
    public Binding binding = new Binding();

    @Override
    @Experimental(Experimental.Statement.NOT_FULLY_DESIGNED)
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
