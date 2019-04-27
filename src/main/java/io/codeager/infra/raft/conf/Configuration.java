package io.codeager.infra.raft.conf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.codeager.infra.raft.Experimental;
import org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jiupeng Zhang
 * @since 04/25/2019
 */
@Experimental(Experimental.Statement.NOT_FULLY_DESIGNED)
public class Configuration implements Serializable {
    static class Node {
        public String id;
        public String name;
        public String url;
    }

    static class Controller {
        private enum AccessMode {
            DEV, PROD
        }

        public AccessMode mode = AccessMode.DEV;
    }

    static class Binding {
        public Class core = Configuration.class;
    }

    static class Logging {
        public String outPath = "/path/to/raft.out";
        public String errPath = "/path/to/raft.err";
    }

    public Map<String, Node> registry = new HashMap<String, Node>() {{
        // field for test only
        put("raft-node-1", new Node());
        put("raft-node-2", new Node());
        put("raft-node-3", new Node());
        put("raft-node-4", new Node());
        put("raft-node-5", new Node());
    }};
    public Node origin = new Node() {{
        // field for test only
        id = RandomStringUtils.randomAlphanumeric(8).toLowerCase();
        name = "raft-node-".concat(id);
    }};
    public Controller controller = new Controller();
    public Binding binding = new Binding();
    public Logging logging = new Logging();

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
