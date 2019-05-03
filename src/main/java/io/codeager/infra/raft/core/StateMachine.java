package io.codeager.infra.raft.core;

import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.core.entity.LogEntity;
import io.codeager.infra.raft.storage.RevocableMap;
import io.codeager.infra.raft.storage.RevocableMapAdapter;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
@Experimental(Experimental.Statement.NOT_FULLY_DESIGNED)
public class StateMachine implements Runnable {
    public static final Logger LOG = LoggerFactory.getLogger(StateMachine.class);

    public enum Role {FOLLOWER, CANDIDATE, LEADER}

    private State state;
    private LocalNode node;
    RevocableMap<String, String> kvdb;
    private volatile boolean suspend;

    public static class State {
        public Role role = Role.FOLLOWER;
        int term = 0;
        int votes = 0;
        int index = -1;
        int lastVoteTerm = 0;
        private List<LogEntity> log = new ArrayList<>();


        static State newInitialState() {
            return new State();
        }

        public List<LogEntity> getLog() {
            return log;
        }

        public void setLog(List<LogEntity> log) {
            this.log = log;
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

    public boolean appendEntry(LogEntity logEntity, String value) {
        boolean status;
        this.state.term = logEntity.getTerm();
        this.state.index = logEntity.getIndex();
        if (logEntity.getIndex() < this.state.getLog().size()) {
            this.getState().log.set(logEntity.getIndex(), logEntity);
        } else {
            this.state.log.add(logEntity);
        }
        try {
            this.kvdb.put(logEntity.getKey(), value);
            LOG.debug("appendEntry {}", this.state.log);
            for (LogEntity t : this.state.log) {
                System.err.println(t.getKey() + ": " + t.getValue());
            }
            status = true;
        } catch (Exception e) {
            status = false;
        }
        return status;

    }

    public boolean removeEntry(LogEntity logEntity) {
        boolean status;
        this.state.term = logEntity.getTerm();
        this.state.index = logEntity.getIndex();
        if (logEntity.getIndex() < this.state.getLog().size()) {
            this.getState().log.set(logEntity.getIndex(), logEntity);
        } else {
            this.state.log.add(logEntity);
        }
        try {
            this.kvdb.remove(logEntity.getKey());
            LOG.debug("appendEntry {}", this.state.log);
            for (LogEntity t : this.state.log) {
                System.err.println(t.getKey() + ": " + t.getValue());
            }
            status = true;
        } catch (Exception e) {
            status = false;
        }
        return status;
    }



    public StateMachine(LocalNode localNode) {
        this(localNode, State.newInitialState());
    }

    public StateMachine(LocalNode localNode, State initialState) {
        this.node = localNode;
        this.state = initialState;
        this.kvdb = new RevocableMapAdapter<>(new ConcurrentHashMap<String, byte[]>());
    }


    @Override
    public void run() {
        while (!suspend) {
            switch (state.role) {
                case FOLLOWER:
                    LOG.debug("switch > case > FOLLOWER");
                    System.err.println("switch > case > FOLLOWER");
                    this.node.waitTimer.start();
                    System.err.println(this.state.log);
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
                    System.err.println("switch > case > CANDIDATE");
                    System.err.println(this.state.getLog());
                    this.state.votes = 1;
                    this.node.voteTimer.start();
                    this.node.askForVote();
                    this.node.checkVoteResult();
                    this.state.term++;
                    break;
                case LEADER:
                    LOG.debug("switch > case > LEADER");
                    System.err.println("switch > case > LEADER");
                    this.node.sendHeartbeat();
                    this.node.heartbeatTimer.start();
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    }

    public RevocableMap<String, String> getKvdb() {
        return kvdb;
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
