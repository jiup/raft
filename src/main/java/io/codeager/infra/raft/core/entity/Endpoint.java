package io.codeager.infra.raft.core.entity;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author Jiupeng Zhang
 * @since 04/29/2019
 */
public final class Endpoint {
    private static final Endpoint DEFAULT = Endpoint.from("0.0.0.0", 9990);

    private String host;
    private int port;

    private Endpoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static Endpoint from(String address) {
        try {
            int separatorIndex = address.lastIndexOf(':');
            return from(
                    separatorIndex == 0 ? "0.0.0.0" : address.substring(0, separatorIndex),
                    Integer.parseInt(address.substring(separatorIndex + 1))
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid endpoint address (" + e.getMessage() + ")");
        }
    }

    public static Endpoint from(int port) {
        return from("0.0.0.0", port);
    }

    public static Endpoint from(String host, int port) {
        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("port must range from 1 to 65535");

        return new Endpoint(host, port);
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
