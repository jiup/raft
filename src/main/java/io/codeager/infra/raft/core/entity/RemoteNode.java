package io.codeager.infra.raft.core.entity;

import java.net.URL;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class RemoteNode extends NodeBase {
    public RemoteNode(String id, String name, URL url) {
        super(id, name, url);
    }
}
