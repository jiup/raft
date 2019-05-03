package io.codeager.infra.raft.core;


import io.codeager.infra.raft.conf.Configuration;
import io.codeager.infra.raft.core.entity.Endpoint;
import io.codeager.infra.raft.core.entity.LogEntity;
import io.codeager.infra.raft.core.rpc.Client;
import io.codeager.infra.raft.core.rpc.Server;
import io.codeager.infra.raft.core.util.NodeTimer;
import io.grpc.vote.*;

import java.net.MalformedURLException;
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
    private List<RemoteNode> peers;
    private Server server; // todo
    public NodeTimer voteTimer;
    public NodeTimer waitTimer;
    public NodeTimer heartbeatTimer;
    public RemoteNode leader;

    public LocalNode(String id, String name, Endpoint endpoint, int[] peers) {
        super(id, name, endpoint);
        this.server = new Server(this);
        this.peers = new ArrayList<>();
        for (int port : peers) {
            this.peers.add(new RemoteNode(" ", " ", null, new Client("127.0.0.1", port)));
        }
        this.waitTimer = new NodeTimer(this, "waitTimer", 40000) {
            @Override
            protected void onTrigger() {
                this.node.changeState(StateMachine.Role.CANDIDATE);
                this.stop();
            }
        };
        this.voteTimer = new NodeTimer(this, "voteTimer", new Random().nextInt(10000)) {
            @Override
            protected void onTrigger() {
                this.node.checkVoteResult();
            }
        };
        this.heartbeatTimer = new NodeTimer(this, "heartbeatTimer", new Random().nextInt(10000)) {
            @Override
            protected void onTrigger() {
                this.node.sendHeartbeat();
            }
        };
    }


    public RemoteNode getLeader() {
        return leader;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    //    public LocalNode(String id, String name, URL url, int[] peers) {
//        this(id, name, url, peers, new StateMachine(this));
////        StateMachine stateMachine = new StateMachine(this);
//
//    }


    public void resetWaitTimer() {
        this.waitTimer.reset(10000 + new Random().nextInt(5000));
    }

    public boolean askCommit() {
        int count = 0;
        StateMachine.State state = this.stateMachine.getState();
        List<LogEntity> logEntities = state.getLog();
        if (logEntities.size() > 0) {
            LogEntity preLogEntity = logEntities.get(logEntities.size() - 1);
            UpdateLogRequest updateLogRequest = UpdateLogRequest.newBuilder().setLogEntry(preLogEntity.toRPCEntry()).build();
            for (RemoteNode peer : peers) {
                if (peer.updateLog(updateLogRequest)) {
                    count++;
                }
            }
        }
        return count > peers.size() / 2;
    }

    public boolean store(String key, String value) {
        StateMachine.State state = this.stateMachine.getState();
        List<LogEntity> logEntities = state.getLog();
        //todo version is not used currently
        LogEntity newLogEntity = LogEntity.of(state.index + 1, state.term, key, value, 0);
        //when log is empty, which means the kv system is new. so it doesn't need to ask for commit
        if (logEntities.size() == 0 || askCommit()) {
            DataEntry dataEntry = DataEntry.newBuilder().setKey(key).setValue(value).build();
            UpdateLogRequest appendLogRequest = UpdateLogRequest.newBuilder()
                    .setLogEntry(newLogEntity.toRPCEntry())
                    .setEntry(dataEntry).build();
            for (RemoteNode peer : peers) {
                peer.appendEntry(appendLogRequest);
            }
            this.appendEntry(newLogEntity, value);
            return true;

        }
        return false;
    }

    public boolean remove(String key) {
        StateMachine.State state = this.stateMachine.getState();
        List<LogEntity> logEntities = state.getLog();
        //todo version is not used currently
        LogEntity newLogEntity = LogEntity.of(state.index + 1, state.term, key, null, 0);
        if (logEntities.size() == 0 || askCommit()) {
//            DataEntry dataEntry  = DataEntry.newBuilder().setKey(key).build();
            UpdateLogRequest appendLogRequest = UpdateLogRequest.newBuilder()
                    .setLogEntry(newLogEntity.toRPCEntry()).build();
            for (RemoteNode peer : peers) {
                peer.appendEntry(appendLogRequest);
            }
            this.appendEntry(newLogEntity, null);
            return true;

        }
        return false;
    }

    void askForVote() {
        VoteRequest voteRequest = VoteRequest.newBuilder().setTerm(this.stateMachine.getState().term).build();
        for (RemoteNode peer : this.getPeers()) {
            if (peer.askForVote(voteRequest)) {
                this.stateMachine.getState().votes++;
            }
        }

    }


    void sendHeartbeat() {
        List<LogEntity> log = this.stateMachine.getState().getLog();
        for (RemoteNode peer : peers) {
            int index = log.size() - 1;
            peer.setIndex(index);
            UpdateLogRequest.Builder updateLogRequestBuilder = UpdateLogRequest.newBuilder().setId(this.getId());

            if (index >= 0) {
                updateLogRequestBuilder.setLogEntry(log.get(index).toRPCEntry());
            }
            boolean flag = false;
            while (!peer.updateLog(updateLogRequestBuilder.build()) && index >= 0) {
                peer.setIndex(index);
                updateLogRequestBuilder.setLogEntry(log.get(index).toRPCEntry());
                index--;
                flag = true;
            }
            while (flag && index < this.stateMachine.getState().getLog().size() - 1) {
                index++;
                peer.setIndex(index);
                updateLogRequestBuilder.setLogEntry(log.get(index).toRPCEntry());
                //todo: if i want to recover follower's data, how can i get K V in specific version
                DataEntry dataEntry = DataEntry.newBuilder().setKey(log.get(index).getKey()).setValue(log.get(index).getValue()).build();
                updateLogRequestBuilder.setEntry(dataEntry);
                peer.appendEntry(updateLogRequestBuilder.build());
            }

        }
    }

    synchronized void checkVoteResult() {
        this.voteTimer.stop();
        if (this.stateMachine.getState().votes > (this.peers.size() / 2)) {
            synchronized (this) {
                if (this.stateMachine.getState().role == StateMachine.Role.CANDIDATE)
                    this.stateMachine.setRole(StateMachine.Role.LEADER);
            }

        }
    }

    public String get(String key) {
        return this.stateMachine.kvdb.get(key);
    }

    public void recover(LogEntity logEntity) {
        int curIndex = logEntity.getIndex() + 1;
        List<LogEntity> selfLogs = this.stateMachine.getState().getLog();
        for (int i = curIndex; i < selfLogs.size(); i++) {
            this.stateMachine.kvdb.revoke(selfLogs.get(i).getKey());
        }
    }

    public boolean handleVoteRequest(int voteTerm) {
        if (voteTerm >= this.stateMachine.getState().term) {
            synchronized (this) {
                this.stateMachine.getState().role = StateMachine.Role.FOLLOWER;
            }

        }
        return voteTerm >= this.stateMachine.getState().term;
    }

    private void changeState(StateMachine.Role role) {
        this.stateMachine.setRole(role);
        synchronized (this.stateMachine) {
            this.stateMachine.notifyAll();
        }

    }

    public int size() {
        return this.stateMachine.getKvdb().size();
    }


    public boolean checkLog(LogEntity logEntity, String id) {
        if (logEntity.getIndex() < this.stateMachine.getState().getLog().size()) {
            for (RemoteNode peer : peers) {
                if (peer.getId().equals(id)) {
                    this.leader = peer;
                }
            }
            return this.stateMachine.getState().getLog().get(logEntity.getIndex()).getTerm() == logEntity.getTerm();
        }
        return false;
    }

    public boolean appendEntry(LogEntity logEntity, String value) {

        if (value == null) {
            return this.stateMachine.removeEntry(logEntity);
        }
        return this.stateMachine.appendEntry(logEntity, value);
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

    public static void main(String... args) throws MalformedURLException {
//        LocalNode node1 = new LocalNode(" ", "no1", Endpoint.of("127.0.0.1",5001), new int[]{5002, 5003});
//        StateMachine stateMachine1 = new StateMachine(node1);
//        node1.setStateMachine(stateMachine1);
//        node1.start(stateMachine1);

//        LocalNode node2 = new LocalNode(" ", "no2", Endpoint.of("127.0.0.1",5002), new int[]{5001, 5003});
//        StateMachine stateMachine2 = new StateMachine(node2);
//        node2.setStateMachine(stateMachine2);
//        node2.start(stateMachine2);
//
//        LocalNode node3 = new LocalNode(" ", "no3", Endpoint.of("127.0.0.1", 5003), new int[]{5002, 5001});
//        StateMachine stateMachine3 = new StateMachine(node3);
//        node3.setStateMachine(stateMachine3);
//        node3.start(stateMachine3);
//
        Client client = new Client("127.0.0.1", 5001);
//        boolean status = client.store(StoreRequest.newBuilder().setEntry(DataEntry.newBuilder().setKey("1").setValue("3").build()).build());
//        System.out.println(status);
        boolean status1 = client.store(StoreRequest.newBuilder().setEntry(DataEntry.newBuilder().setKey("2").setValue("4").build()).build());
        System.out.println(status1);
//        boolean status2 = client.store(StoreRequest.newBuilder().setEntry(DataEntry.newBuilder().setKey("1").build()).build());
//        System.out.println(status2);
        boolean status3 = client.remove(RemoveRequest.newBuilder().setKey("2").build());
        System.out.println(status3);

//        String value2 = client.get(GetRequest.newBuilder().setKey("2").build());
//        System.out.println(value2);

//        String value3 = client.get(GetRequest.newBuilder().setKey("1").build());
//        System.out.println(value2);
    }

    public List<RemoteNode> getPeers() {
        return peers;
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
