package io.codeager.infra.raft.core;


import com.google.protobuf.StringValue;
import io.codeager.infra.raft.conf.Configuration;
import io.codeager.infra.raft.core.entity.Endpoint;
import io.codeager.infra.raft.core.entity.LogEntry;
import io.codeager.infra.raft.core.rpc.Client;
import io.codeager.infra.raft.core.rpc.Server;
import io.codeager.infra.raft.core.util.NodeTimer;
import io.codeager.infra.raft.storage.KvEngine;
import io.codeager.infra.raft.storage.RevocableMap;
import io.codeager.infra.raft.storage.RevocableMapAdapter;
import io.codeager.infra.raft.util.ConfigurationHelper;
import io.grpc.vote.DataEntry;
import io.grpc.vote.UpdateLogRequest;
import io.grpc.vote.VoteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.codeager.infra.raft.conf.Configuration.Mode.PROTECTED;


/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class LocalNode extends NodeBase {
    public static final Logger LOG = LoggerFactory.getLogger(LocalNode.class);

    RevocableMap<String, Object> settingsMap;
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
        this.settingsMap = new RevocableMapAdapter<>(new KvEngine(new File("logs")).map("default"));

        if (configuration.mode == PROTECTED) {
            LOG.info("Start in protected mode");
            //todo load database
            File file = new File("DBfile");
            if (file.exists()) {
                this.internalMap = new RevocableMapAdapter<>(new KvEngine(file).map("default"));
                loadLog();
            } else {
                this.internalMap = new RevocableMapAdapter<>(new KvEngine().map("default"));
            }

        } else {
            LOG.info("Start in debug mode");
            this.internalMap = new RevocableMapAdapter<>(new KvEngine().map("default"));
        }

        this.idSending = new HashSet<>();
        this.configuration = configuration;

    }

    public void resetWaitTimer() {
        LOG.debug("Get package from Leader, reset the WaitTimer");
        this.waitTimer.reset(this.configuration.origin.heartbeatTimeout + 1000 + random.nextInt(5000)); // todo: read from configuration
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
            this.peers.parallelStream().forEach(
                    peer -> {
                        try {
                            peer.appendEntry(appendLogRequest);
                        } catch (Exception e) {
                            LOG.error("storing, append entry to node:{} failure: {}", peer.getId(), e.getMessage());
                        }

                    }
            );
            this.appendEntry(newLogEntry, value);
            return true;

        }
        return false;
    }

    public void loadLog() {
        List<LogEntry> log = (List<LogEntry>) settingsMap.get("logs");
        if (!log.isEmpty()) {
            this.stateMachine.getState().setLog(log);
            this.stateMachine.getState().index = log.get(log.size() - 1).getIndex();
            this.stateMachine.getState().term = log.get(log.size() - 1).getTerm();
            LOG.info("Load log size : {}, index: {}, index: {}", log.size(), this.stateMachine.getState().index, this.stateMachine.getState().term);
        }
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
            UpdateLogRequest appendLogRequest = UpdateLogRequest.newBuilder()
                    .setLogEntry(newLogEntry.toRpcEntry()).build();
            this.peers.parallelStream().forEach(peer -> {
                try {
                    peer.appendEntry(appendLogRequest);
                } catch (Exception e) {
                    LOG.error("removing, append entry to node:{} failure: {}", peer.getId(), e.getMessage());
                }

            });
            return this.appendEntry(newLogEntry, null);
        }
        return false;
    }

    void askForVote() {
        LOG.info("ask for vote");
        VoteRequest voteRequest = VoteRequest.newBuilder().setTerm(this.stateMachine.getState().term).build();
        final StateMachine stateMachine = this.stateMachine;
        this.peers.parallelStream().forEach(peer -> {
            try {
                if (peer.askForVote(voteRequest)) {
                    stateMachine.getState().votes++;
                }
            } catch (Exception e) {
                LOG.error("ask vote failure: {}", e.getMessage());
            }
        });
    }

    public boolean askCommit() {
        final int[] count = new int[1];
        StateMachine.State state = this.stateMachine.getState();
        List<LogEntry> logEntities = state.getLog();
        if (logEntities.size() > 0) {
            LogEntry preLogEntry = logEntities.get(logEntities.size() - 1);
            UpdateLogRequest updateLogRequest = UpdateLogRequest.newBuilder().setLogEntry(preLogEntry.toRpcEntry()).build();
            this.peers.parallelStream().forEach(peer -> {
                try {
                    if (peer.updateLog(updateLogRequest)) {
                        count[0]++;
                    }
                } catch (Exception e) {
                    LOG.error("ask for commit failure: {}", e.getMessage());
                }

            });
        }
        return count[0] > peers.size() / 2;
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
        LOG.debug("get {} votes", this.stateMachine.getState().votes);
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
        LOG.info("get key = {}", key);
        return this.internalMap.get(key);
    }

    public void recover(LogEntry logEntry) {
        LOG.info("Begin to recover log");
        int curIndex = logEntry.getIndex() + 1;
        List<LogEntry> selfLogs = this.stateMachine.getState().getLog();
        for (int i = curIndex; i < selfLogs.size(); i++) {
            this.internalMap.revoke(selfLogs.get(i).getKey());
        }
    }

    public boolean handleVoteRequest(int voteTerm) {
        LOG.info("vote term is {}, my term is {}", voteTerm, this.stateMachine.getState().term);
        if (voteTerm >= this.stateMachine.getState().term) {
            synchronized (this.stateMachine) {
                this.stateMachine.getState().role = StateMachine.Role.FOLLOWER;
            }
        }
        return voteTerm >= this.stateMachine.getState().term;
    }

    public int size() {
        LOG.info("get size");
        return this.internalMap.size();
    }

    public Collection<String> getValues() {
        LOG.info("get values");
        return this.internalMap.values();
    }

    public Collection<String> getKeys() {
        LOG.info("get keys");
        return this.internalMap.keySet();
    }

    public Collection<Map.Entry<String, String>> getEntries() {
        LOG.info("get entries");
        return this.internalMap.entrySet();
    }

    public boolean checkLog(LogEntry logEntry, String id) {
        LOG.info("checking if the log is update to leader...");
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
        LOG.info("Updating log with leader...");
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
            LOG.warn("Updating failure: {}", e.getMessage());
            status = false;
        }
        return status;
    }

    public void start() {
        ServerContainer serverContainer = new ServerContainer(this.server);
        Thread thread1 = new Thread(serverContainer);
        Thread thread2 = new Thread(this.stateMachine);
        thread2.start();
        LOG.info("server is  started");
        thread1.start();
        LOG.info("stateMachine is  started");
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

    public void stop() {
        this.server.stop();
    }

    public void pause() {

    }

    public void resume() {

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
            LOG.debug("before send heartbeat to {}", this.peer.getId());
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
                    DataEntry dataEntry = DataEntry.newBuilder().setKey(log.get(index).getKey()).setValue(StringValue.of(log.get(index).getValue())).build();
                    updateLogRequestBuilder.setEntry(dataEntry);
                    peer.appendEntry(updateLogRequestBuilder.build());
                }
                LOG.debug("sent heartbeat to {}", this.peer.getId());
            } catch (Exception e) {
                LOG.warn("cannot send heartbeat to node@{}", this.peer.getEndpoint());
            } finally {
                this.localNode.moveOutFromSet(this.peer);
            }


        }
    }

    public static void main(String[] args) throws IOException {
        Configuration configuration = ConfigurationHelper.load("config.json");
        StateMachine stateMachine = new StateMachine();
        LocalNode node = new LocalNode(configuration, stateMachine);
        stateMachine.bind(node);
        node.start();
    }
}
