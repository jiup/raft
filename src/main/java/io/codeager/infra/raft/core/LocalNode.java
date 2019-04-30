package io.codeager.infra.raft.core;


import io.codeager.infra.raft.conf.Configuration;
import io.codeager.infra.raft.core.rpc.Client;
import io.codeager.infra.raft.core.rpc.Server;
import io.codeager.infra.raft.core.util.NodeTimer;
import io.grpc.vote.Entry;
import io.grpc.vote.UpdateLogRequest;
import io.grpc.vote.VoteRequest;

import java.net.MalformedURLException;
import java.net.URL;
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

    public LocalNode(String id, String name, URL url, int[] peers) {
        super(id, name, url);
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

    public boolean store(Entry entry) {
        int count = 0;
        for (RemoteNode peer : peers) {
            //Todo
            count++;
        }
        if (count > peers.size() / 2) {
            StateMachine.State state = this.stateMachine.getState();
            this.appendEntry(state.index + 1, state.term, entry);
            UpdateLogRequest updateLogRequest = UpdateLogRequest.newBuilder().setIndex(state.index).setTerm(state.term).build();
            for (RemoteNode peer : peers) {
                peer.appendEntry(updateLogRequest);
            }
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


    private void sendHeartbeat() {
        List<Integer> log = this.stateMachine.getState().getLog();
        for (RemoteNode peer : peers) {
            int index = log.size() - 1;
            peer.setIndex(index);
            System.err.println(index);

            UpdateLogRequest.Builder updateLogRequestBuilder = UpdateLogRequest.newBuilder();
            if (index >= 0) {
                updateLogRequestBuilder.setIndex(index).setTerm(log.get(index));
            }
            boolean flag = false;
            while (!peer.updateLog(updateLogRequestBuilder.build()) && index >= 0) {
                peer.setIndex(index);
                updateLogRequestBuilder.setIndex(index);
                updateLogRequestBuilder.setTerm(log.get(index));
                index--;
                flag = true;
            }
            while (flag && index < this.stateMachine.getState().getLog().size() - 1) {
                index++;
                peer.setIndex(index);
                updateLogRequestBuilder.setIndex(index);
                updateLogRequestBuilder.setTerm(log.get(index));
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


    public boolean checkLog(int index, int term, int id) {
        if (index < this.stateMachine.getState().getLog().size()) {
            for (RemoteNode peer : peers) {
                if (peer.getId().equals(String.valueOf(id))) {
                    this.leader = peer;
                }
            }
            return this.stateMachine.getState().getLog().get(index) == term;
        }
        return false;
    }

    public void appendEntry(int index, int term, Entry entry) {
        //todo only append log
        this.stateMachine.appendEntry(index, term);

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
//        LocalNode node1 = new LocalNode(" ", "no1", new URL("FTP", "127.0.0.1", 5001, " "), new int[]{5002, 5003});
//        StateMachine stateMachine1 = new StateMachine(node1);
//        node1.setStateMachine(stateMachine1);
//        node1.appendEntry(0,1,null);
//        node1.appendEntry(1,1,null);
//        node1.appendEntry(2,2,null);
//        node1.start(stateMachine1);

//        LocalNode node2 = new LocalNode(" ", "no2", new URL("FTP", "127.0.0.1", 5002, " "), new int[]{5001, 5003});
//        StateMachine stateMachine2 = new StateMachine(node2);
//        node2.setStateMachine(stateMachine2);
//        node2.start(stateMachine2);
//
        LocalNode node3 = new LocalNode(" ", "no3", new URL("FTP", "127.0.0.1", 5003, " "), new int[]{5002, 5001});
        StateMachine stateMachine3 = new StateMachine(node3);
        node3.setStateMachine(stateMachine3);
        node3.start(stateMachine3);
//
//        Client client = new Client("127.0.0.1",5001);
//        client.store(StoreRequest.newBuilder().setEntry(Entry.newBuilder().setKey("1").setValue("1").build()).build());

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
