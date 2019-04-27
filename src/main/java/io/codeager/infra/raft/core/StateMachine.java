package io.codeager.infra.raft.core;

import io.codeager.infra.raft.Experimental;
import io.grpc.vote.VoteRequest;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
@Experimental(Experimental.Statement.NOT_FULLY_DESIGNED)
public class StateMachine implements Runnable {
    public static final Logger LOG = LoggerFactory.getLogger(StateMachine.class);

    public enum Role {FOLLOWER, CANDIDATE, LEADER}

    public static class State {
        Role role = Role.FOLLOWER;
        int term = 1;
        int votes = 0;
        int index = 0;
        int lastVoteTerm = 0;

        static State newInitialState() {
            return new State();
        }

        public int getTerm() {
            return term;
        }

        public int getVotes() {
            return votes;
        }

        public int getIndex() {
            return index;
        }

        public int getLastVoteTerm() {
            return lastVoteTerm;
        }
    }

    private State state;
    private LocalNode node;
    private volatile boolean suspend;

    public StateMachine(LocalNode localNode) {
        this(localNode, State.newInitialState());
    }

    public StateMachine(LocalNode localNode, State initialState) {
        this.node = localNode;
        this.state = initialState;
    }

    @Override
    public void run() {
        while (!suspend) {
            switch (state.role) {
                case FOLLOWER:
                    LOG.debug("switch > case > FOLLOWER");
                    this.node.waitTimer.start();
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case CANDIDATE:
                    LOG.debug("switch > case > CANDIDATE");
                    this.node.voteTimer.start();
                    VoteRequest voteRequest = VoteRequest.newBuilder().setTerm(this.state.term).build();
                    for (RemoteNode peer : this.node.getPeers()) {
                        if (peer.askForVote(voteRequest)) {
                            this.state.votes++;
                        }
                    }
//                        if (state.getVotes() > (this.node.peers.size() / 2)) {
//                            this.node.stateMachine.setRole(Role.LEADER);
//                        }
//                    this.node.checkVoteResult(); // todo
//                        synchronized (this){
//                            try {
//                                this.wait();
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
                    this.state.term++;
                    break;
                case LEADER:
                    LOG.debug("switch > case > LEADER");
                    this.node.heartbeatTimer.start();
                    break;
            }
        }
    }

    public void setRole(Role role) {
        this.state.role = role;
    }

    public State getState() {
        return state;
    }

    public StateMachine setState(State state) {
        this.state = state;
        return this;
    }

    public LocalNode getNode() {
        return node;
    }

    public StateMachine setNode(LocalNode node) {
        this.node = node;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("state", state)
                .append("node", node)
                .toString();
    }
}
