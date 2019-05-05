package io.codeager.infra.raft.core;


import com.google.protobuf.StringValue;
import io.codeager.infra.raft.conf.Configuration;
import io.codeager.infra.raft.core.entity.Endpoint;
import io.codeager.infra.raft.core.entity.LogEntry;
import io.codeager.infra.raft.core.rpc.Client;
import io.codeager.infra.raft.core.rpc.Server;
import io.codeager.infra.raft.core.util.NodeTimer;
import io.codeager.infra.raft.storage.RevocableMap;
import io.codeager.infra.raft.storage.RevocableMapAdapter;
import io.grpc.vote.DataEntry;
import io.grpc.vote.UpdateLogRequest;
import io.grpc.vote.VoteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class LocalNode extends NodeBase {
    public static final Logger LOG = LoggerFactory.getLogger(LocalNode.class);

    RevocableMap<String, String> internalMap;
    private final StateMachine stateMachine;
    private Configuration configuration;
    private Random random;
    private Server server;
    private RemoteNode leader;
    private List<RemoteNode> peers;
    private NodeTimer voteTimer;
    private NodeTimer waitTimer;
    private NodeTimer heartbeatTimer;
    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private HashSet<RemoteNode> idSending;

    // todo: move internal kv engine (stateMachine.internalMap) outside.

    public LocalNode(Configuration configuration, StateMachine stateMachine) {
        super(configuration.origin.id, configuration.origin.name, configuration.origin.endpoint);
        this.stateMachine = stateMachine;
        this.random = new Random();
        this.server = new Server(this);
        this.peers = new ArrayList<>();
        for (Endpoint e : configuration.registry) {
            this.peers.add(new RemoteNode("undef", "undef", Endpoint.of(e.getHost(), e.getPort()), new Client(e.getHost(), e.getPort())));
        }
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
        this.internalMap = new RevocableMapAdapter<>(new ConcurrentHashMap<String, byte[]>());
        this.idSending = new HashSet<>();
    }

    public void resetWaitTimer() {
        System.err.println("rest the waitTimer");
        this.waitTimer.reset(12000 + random.nextInt(5000)); // todo: read from configuration
    }

    public void initPeersId() {
        HashSet<RemoteNode> set = new HashSet<>(peers);
        while (!set.isEmpty()) {
            Iterator<RemoteNode> it = set.iterator();
            while (it.hasNext()) {
                RemoteNode peer = it.next();
                try {
                    peer.setId(peer.getRemoteId());
                    it.remove();
                    LOG.info("peer-{} now connected", peer.getId());
                } catch (Exception e) {
                    LOG.debug("get peer-{} failure, retry in 2 seconds...", peer.getEndpoint());
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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


    public boolean containsKey(String key) {
        return this.internalMap.containsKey(key);
    }

    public boolean containsValue(String value) {
        return this.internalMap.containsValue(value);
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
        try {
            for (RemoteNode peer : this.peers) {
                if (peer.askForVote(voteRequest)) {
                    this.stateMachine.getState().votes++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        String id = this.getId();
        for (RemoteNode peer : peers) {
            if (!this.idSending.contains(peer)) {
                this.idSending.add(peer);
                executorService.submit(new SendHeartbeatTask(peer, log, id, this));
            }
        }
    }

    void checkVoteResult() {
        this.voteTimer.stop();
        if (this.stateMachine.getState().votes > (this.peers.size() / 2)) {
            synchronized (this.stateMachine) {
                if (this.stateMachine.onState(StateMachine.Role.CANDIDATE))
                    this.stateMachine.setRole(StateMachine.Role.LEADER);
            }
        } else {
            this.stateMachine.setRole(StateMachine.Role.FOLLOWER);
        }
    }

    public String get(String key) {
        return this.internalMap.get(key);
    }

    public void recover(LogEntry logEntry) {
        int curIndex = logEntry.getIndex() + 1;
        List<LogEntry> selfLogs = this.stateMachine.getState().getLog();
        for (int i = curIndex; i < selfLogs.size(); i++) {
            this.internalMap.revoke(selfLogs.get(i).getKey());
        }
    }

    public boolean handleVoteRequest(int voteTerm) {
        if (voteTerm >= this.stateMachine.getState().term) {
            synchronized (this.stateMachine) {
                this.stateMachine.getState().role = StateMachine.Role.FOLLOWER;
            }
        }
        return voteTerm >= this.stateMachine.getState().term;
    }

    public int size() {
        return this.internalMap.size();
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
        boolean status;
        try {
            if (value == null) {
                this.internalMap.remove(logEntry.getKey());
                status = this.stateMachine.removeEntry(logEntry);
            } else {
                this.internalMap.put(logEntry.getKey(), value);
                status = this.stateMachine.appendEntry(logEntry, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            status = false;
        }
        return status;
    }

    public void start() {
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
        synchronized (this.stateMachine) {
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
        this.internalMap = new RevocableMapAdapter<>(new ConcurrentHashMap<String, byte[]>());
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

    private void moveOutFromSet(RemoteNode remoteNode) {
        this.idSending.remove(remoteNode);
    }

    class SendHeartbeatTask implements Runnable {
        private List<LogEntry> log;
        private RemoteNode peer;
        private String id;
        private LocalNode localNode;

        SendHeartbeatTask(RemoteNode peer, List<LogEntry> log, String id, LocalNode localNode) {
            this.log = log;
            this.peer = peer;
            this.id = id;
            this.localNode = localNode;
        }

        @Override
        public void run() {
            LOG.info("send heart beat to {} begin", this.peer.getId());
            try {
                int index = log.size() - 1;
                peer.setIndex(index);
                UpdateLogRequest.Builder updateLogRequestBuilder = UpdateLogRequest.newBuilder().setId(id);

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
                while (flag && index < log.size() - 1) {
                    index++;
                    peer.setIndex(index);
                    updateLogRequestBuilder.setLogEntry(log.get(index).toRpcEntry());
                    //todo: if i want to recover follower's data, how can i get K V in specific version
                    DataEntry dataEntry = DataEntry.newBuilder().setKey(log.get(index).getKey()).setValue(StringValue.of(log.get(index).getValue())).build();
                    updateLogRequestBuilder.setEntry(dataEntry);
                    peer.appendEntry(updateLogRequestBuilder.build());
                }
                LOG.info("send heart beat to {} finish", this.peer.getId());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.localNode.moveOutFromSet(this.peer);
            }


        }
    }

    public static void main(String[] args) throws IOException {
//        Configuration configuration = ConfigurationHelper.load("config4.json");
//        StateMachine stateMachine = new StateMachine();
//        LocalNode node = new LocalNode(configuration, stateMachine);
//        stateMachine.bind(node);
//        node.start();
//        Client client = new Client("127.0.0.1", 5001);
//        int size = client.size(SizeRequest.newBuilder().build());
//        System.out.println(size);
//
//        client.store(StoreRequest.newBuilder().setEntry(DataEntry.newBuilder().setKey("1").setValue(StringValue.of("3")).build()).build());
//        String res1 = client.get(GetRequest.newBuilder().setKey("1").build());
//        System.out.println(res1);
//
//        client.store(StoreRequest.newBuilder().setEntry(DataEntry.newBuilder().setKey("2").setValue(StringValue.of("4")).build()).build());
//        String res2 = client.get(GetRequest.newBuilder().setKey("2").build());
//        System.out.println(res2);

    }
}
