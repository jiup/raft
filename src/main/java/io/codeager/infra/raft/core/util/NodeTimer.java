package io.codeager.infra.raft.core.util;

import io.codeager.infra.raft.core.LocalNode;
import io.codeager.infra.raft.util.timer.RepeatedTimer;

/**
 * @author Jiupeng Zhang
 * @since 04/27/2019
 */
public abstract class NodeTimer extends RepeatedTimer {
    protected LocalNode node;

    protected NodeTimer(LocalNode node, String name, long timeoutInMillis) {
        super(name, timeoutInMillis);
        this.node = node;
    }

    public LocalNode getNode() {
        return node;
    }
}
