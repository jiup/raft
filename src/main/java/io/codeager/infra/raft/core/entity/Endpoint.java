package io.codeager.infra.raft.core.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Jiupeng Zhang
 * @since 04/29/2019
 */
public final class Endpoint {
    public static final Endpoint DEFAULT = Endpoint.of("0.0.0.0", 9990);

    private String host;
    private int port;

    @JsonCreator
    private Endpoint(
            @JsonProperty("host") String host,
            @JsonProperty("port") int port) {
        this.host = host;
        this.port = port;
    }

    @JsonCreator
    private Endpoint(String address) {
        try {
            int separatorIndex = address.lastIndexOf(':');
            this.host = separatorIndex == 0 ? "0.0.0.0" : address.substring(0, separatorIndex);
            this.port = Integer.parseInt(address.substring(separatorIndex + 1));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid endpoint address (" + e.getMessage() + ")");
        }
    }

    public static Endpoint of(int port) {
        return of("0.0.0.0", port);
    }

    public static Endpoint of(String host, int port) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("port must range of 1 to 65535");

        return new Endpoint(host, port);
    }

    public static Endpoint of(String address) {
        return new Endpoint(address);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("host", host)
                .append("port", port)
                .toString();
    }
}
