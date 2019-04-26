package io.codeager.infra.raft.core;

import io.codeager.infra.raft.Experimental;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
@Experimental(Experimental.Statement.NOT_FULLY_DESIGNED)
public class StateMachine {
    private State state;
    private int term;

    public StateMachine() {
    }
}
