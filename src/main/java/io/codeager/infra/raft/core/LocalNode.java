package io.codeager.infra.raft.core;


import com.google.protobuf.StringValue;
import io.codeager.infra.raft.conf.Configuration;
import io.codeager.infra.raft.core.entity.Endpoint;
import io.codeager.infra.raft.core.entity.LogEntry;
import io.codeager.infra.raft.core.rpc.Client;
import io.codeager.infra.raft.core.rpc.Server;
import io.codeager.infra.raft.core.util.NodeTimer;
import io.grpc.vote.DataEntry;
import io.grpc.vote.UpdateLogRequest;
import io.grpc.vote.VoteRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class LocalNode extends NodeBase {
    private Configuration configuration;
    private StateMachine stateMachine;
    private Random random;
    private Server server;
    private RemoteNode leader;
    private List<RemoteNode> peers;
    private NodeTimer voteTimer;
    private NodeTimer waitTimer;
    private NodeTimer heartbeatTimer;
    // todo: move internal kv engine (stateMachine.internalMap) outside.

    public LocalNode(String id, String name, Endpoint endpoint, int[] peers) {
        super(id, name, endpoint);
        this.random = new Random();
        this.server = new Server(this);
        this.peers = new ArrayList<>();
        for (int port : peers) {
            this.peers.add(new RemoteNode(" ", " ", null, new Client("127.0.0.1", port)));
        }
        this.initTimers();
    }

    public void resetWaitTimer() {
        this.waitTimer.reset(10000 + random.nextInt(5000)); // todo: read from configuration
    }

    public boolean store(String key, String value) {
        StateMachine.State state = this.stateMachine.getState();
        List<LogEntry> logEntities = state.getLog();
        //todo version is not used currently
        LogEntry newLogEntry = LogEntry.of(state.index + 1, state.term, key, value, 0);
        //when log is empty, which means the kv system is new. so it doesn't need to ask for commit
        if (logEntities.size() == 0 || askCommit()) {
            DataEntry dataEntry = DataEntry.newBuilder().setKey(key).setValue(StringValue.of(value)).build();
            UpdateLogRequest appendLogRequest = UpdateLogRequest.newBuilder()
                    .setLogEntry(newLogEntry.toRpcEntry())
                    .setEntry(dataEntry).build();
            for (RemoteNode peer : peers) {
                peer.appendEntry(appendLogRequest);
            }
            this.appendEntry(newLogEntry, value);
            return true;

        }
        return false;
    }

    public boolean remove(String key) {
        StateMachine.State state = this.stateMachine.getState();
        List<LogEntry> logEntities = state.getLog();
        //todo version is not used currently
        LogEntry newLogEntry = LogEntry.of(state.index + 1, state.term, key, null, 0);
        if (logEntities.size() == 0 || askCommit()) {
//            DataEntry dataEntry  = DataEntry.newBuilder().setKey(key).build();
            UpdateLogRequest appendLogRequest = UpdateLogRequest.newBuilder()
                    .setLogEntry(newLogEntry.toRpcEntry()).build();
            for (RemoteNode peer : peers) {
                peer.appendEntry(appendLogRequest);
            }
            return this.appendEntry(newLogEntry, null);
        }
        return false;
    }

    void askForVote() {
        VoteRequest voteRequest = VoteRequest.newBuilder().setTerm(this.stateMachine.getState().term).build();
        for (RemoteNode peer : this.peers) {
            if (peer.askForVote(voteRequest)) {
                this.stateMachine.getState().votes++;
            }
        }
    }

    public boolean askCommit() {
        int count = 0;
        StateMachine.State state = this.stateMachine.getState();
        List<LogEntry> logEntities = state.getLog();
        if (logEntities.size() > 0) {
            LogEntry preLogEntry = logEntities.get(logEntities.size() - 1);
            UpdateLogRequest updateLogRequest = UpdateLogRequest.newBuilder().setLogEntry(preLogEntry.toRpcEntry()).build();
            for (RemoteNode peer : peers) {
                if (peer.updateLog(updateLogRequest)) {
                    count++;
                }
            }
        }
        return count > peers.size() / 2;
    }

    void sendHeartbeat() {
        List<LogEntry> log = this.stateMachine.getState().getLog();
        for (RemoteNode peer : peers) {
            int index = log.size() - 1;
            peer.setIndex(index);
            UpdateLogRequest.Builder updateLogRequestBuilder = UpdateLogRequest.newBuilder().setId(this.getId());

            if (index >= 0) {
                updateLogRequestBuilder.setLogEntry(log.get(index).toRpcEntry());
            }
            boolean flag = false;
            while (!peer.updateLog(updateLogRequestBuilder.build()) && index >= 0) {
                peer.setIndex(index);
                updateLogRequestBuilder.setLogEntry(log.get(index).toRpcEntry());
                index--;
                flag = true;
            }
            while (flag && index < this.stateMachine.getState().getLog().size() - 1) {
                index++;
                peer.setIndex(index);
                updateLogRequestBuilder.setLogEntry(log.get(index).toRpcEntry());
                //todo: if i want to recover follower's data, how can i get K V in specific version
                DataEntry dataEntry = DataEntry.newBuilder().setKey(log.get(index).getKey()).setValue(StringValue.of(log.get(index).getValue())).build();
                updateLogRequestBuilder.setEntry(dataEntry);
                peer.appendEntry(updateLogRequestBuilder.build());
            }
        }
    }

    void checkVoteResult() {
        this.voteTimer.stop();
        if (this.stateMachine.getState().votes > (this.peers.size() / 2)) {
            synchronized (this.stateMachine.lock) {
                if (this.stateMachine.onState(StateMachine.Role.CANDIDATE))
                    this.stateMachine.setRole(StateMachine.Role.LEADER);
            }
        }
    }

    public String get(String key) {
        return this.stateMachine.internalMap.get(key);
    }

    public void recover(LogEntry logEntry) {
        int curIndex = logEntry.getIndex() + 1;
        List<LogEntry> selfLogs = this.stateMachine.getState().getLog();
        for (int i = curIndex; i < selfLogs.size(); i++) {
            this.stateMachine.internalMap.revoke(selfLogs.get(i).getKey());
        }
    }

    public boolean handleVoteRequest(int voteTerm) {
        if (voteTerm >= this.stateMachine.getState().term) {
            synchronized (this.stateMachine.lock) {
                this.stateMachine.getState().role = StateMachine.Role.FOLLOWER;
            }
        }
        return voteTerm >= this.stateMachine.getState().term;
    }

    public int size() {
        return this.stateMachine.getInternalMap().size();
    }

    public boolean checkLog(LogEntry logEntry, String id) {
        if (logEntry.getIndex() < this.stateMachine.getState().getLog().size()) {
            for (RemoteNode peer : peers) {
                if (peer.getId().equals(id)) {
                    this.leader = peer;
                }
            }
            return this.stateMachine.getState().getLog().get(logEntry.getIndex()).getTerm() == logEntry.getTerm();
        }
        return false;
    }

    public boolean appendEntry(LogEntry logEntry, String value) {
        if (value == null) {
            return this.stateMachine.removeEntry(logEntry);
        }
        return this.stateMachine.appendEntry(logEntry, value);
    }

    public void start(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
        ServerContainer serverContainer = new ServerContainer(this.server);
        Thread thread1 = new Thread(serverContainer);
        Thread thread2 = new Thread(this.stateMachine);
        thread2.start();
        thread1.start();
        try {
            thread2.join();
            thread1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        start(new StateMachine(this));
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public RemoteNode getLeader() {
        return leader;
    }

    public NodeTimer getVoteTimer() {
        return voteTimer;
    }

    public NodeTimer getWaitTimer() {
        return waitTimer;
    }

    public NodeTimer getHeartbeatTimer() {
        return heartbeatTimer;
    }

    private void becomeCandidate() {
        this.stateMachine.setRole(StateMachine.Role.CANDIDATE);
        synchronized (this.stateMachine.lock) {
            this.stateMachine.notifyAll();
        }
    }

    private void initTimers() {
        this.waitTimer = new NodeTimer(this, "waitTimer", configuration.origin.waitTimeout) {
            protected void onTrigger() {
                this.node.becomeCandidate();
                this.stop();
            }
        };
        this.voteTimer = new NodeTimer(this, "voteTimer", random.nextInt(configuration.origin.voteTimeout)) {
            @Override
            protected void onTrigger() {
                this.node.checkVoteResult();
            }
        };
        this.heartbeatTimer = new NodeTimer(this, "heartbeatTimer", random.nextInt(configuration.origin.heartbeatTimeout)) {
            protected void onTrigger() {
                this.node.sendHeartbeat();
            }
        };
    }

    class ServerContainer implements Runnable {
        private Server server;

        public ServerContainer(Server server) {
            this.server = server;
        }

        @Override
        public void run() {
            try {
                server.start();
                server.blockUntilShutdown();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } finally {
                server.stop();
            }
        }
    }
}
