package io.codeager.infra.raft.core;

import io.codeager.infra.raft.core.entity.Endpoint;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public abstract class NodeBase {
    private String id;
    private String name;
    private Endpoint endpoint;

    public NodeBase(String id) {
        this(id, Endpoint.DEFAULT);
    }

    public NodeBase(String id, Endpoint endpoint) {
        this(id, "raft-" + id, endpoint);
    }

    public NodeBase(String id, String name, Endpoint endpoint) {
        this.id = id;
        this.name = name;
        this.endpoint = endpoint;
    }

    public String getId() {
        return id;
    }

    public NodeBase setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public NodeBase setName(String name) {
        this.name = name;
        return this;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public NodeBase setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("name", name)
                .append("endpoint", endpoint)
                .toString();
    }
}
