package io.codeager.infra.raft.core.entity;

import io.codeager.infra.raft.core.StateMachine;

import java.net.URL;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class Node extends NodeBase {
    private StateMachine stateMachine;

    public Node(String id, String name, URL url) {
        super(id, name, url);
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }
}
