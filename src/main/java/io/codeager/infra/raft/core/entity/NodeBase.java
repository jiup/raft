package io.codeager.infra.raft.core.entity;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.net.URL;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public abstract class NodeBase {
    private String id;
    private String name;
    private URL url;

    public NodeBase(String id, String name, URL url) {
        this.id = id;
        this.name = name;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("url", url)
                .toString();
    }
}
