package io.codeager.infra.raft.core;


import io.codeager.infra.raft.conf.Configuration;
import io.codeager.infra.raft.core.rpc.Client;
import io.codeager.infra.raft.core.rpc.Server;
import io.codeager.infra.raft.core.util.NodeTimer;
import io.grpc.vote.UpdateLogRequest;

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
    private Server server;
    public NodeTimer voteTimer;
    public NodeTimer waitTimer;
    public NodeTimer heartbeatTimer;

    public LocalNode(String id, String name, URL url, int[] peers) {
        super(id, name, url);
        this.server = new Server(this);
        this.peers = new ArrayList<>();
        for (int port : peers) {
            this.peers.add(new RemoteNode(" ", " ", null, new Client("127.0.0.1", port)));
        }
        this.waitTimer = new NodeTimer(this, "waitTimer", 20000) {
            @Override
            protected void onTrigger() {
//                this.node.changeState(State.CANDIDATE, "waitTimer"); todo
            }
        };
        this.voteTimer = new NodeTimer(this, "voteTimer", new Random().nextInt(5000)) {
            @Override
            protected void onTrigger() {
                this.node.checkVoteResult();
            }
        };
        this.heartbeatTimer = new NodeTimer(this, "heartbeatTimer", new Random().nextInt(5000)) {
            @Override
            protected void onTrigger() {
                this.node.sendHeartbeat();
            }
        };
    }

    private void sendHeartbeat() {
        UpdateLogRequest updateLogRequest = UpdateLogRequest.newBuilder()
                .setTerm(this.stateMachine.getState().getTerm())
                .setIndex(this.stateMachine.getState().getIndex())
                .build();
        for (RemoteNode peer : peers) {
            peer.updateLog(updateLogRequest);
        }
    }

    synchronized private void checkVoteResult() {
        if (this.stateMachine.getState().getVotes() > (this.peers.size()) / 2) {
            changeState(StateMachine.Role.LEADER, "voteTimer");
        } else {
//            changeState(State.FOLLOWER, "voteTimer"); // todo
        }
    }

    private void changeState(StateMachine.Role role, String timerName) {
        this.stateMachine.setRole(role); // todo
        if (timerName.equals("waitTimer")) {
            this.waitTimer.stop();
        } else if (timerName.equals("voteTimer")) {
            this.voteTimer.stop();
        }

//        synchronized (this.stateConvert) { // todo
//            this.stateConvert.notifyAll();
//        }
        System.err.println("notifyAll");

    }

    public boolean checkLog(int index, int term) {
        //Todo
        return true;
    }

    public void appendEntry(int index, int term, io.grpc.vote.Entry entry) {
        //Todo
    }

    public void start() {
        this.stateMachine = new StateMachine(this);
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

    public static void main(String... args) throws MalformedURLException {
        int[] ports = {5001, 5002, 5003};
        LocalNode node1 = new LocalNode(" ", "no1", new URL("FTP", "127.0.0.1", 5001, " "), new int[]{5004, 5002, 5003});
        node1.start();
//        LocalNode node2 = new LocalNode(" ","no1",new URL("FTP","127.0.0.1",5002," "),new StateMachine(),new int[]{5001,5004,5003});
//        node2.start();
//        LocalNode node3 = new LocalNode(" ","no1",new URL("FTP","127.0.0.1",5003," "),new StateMachine(),new int[]{5001,5002,5004});
//        node3.start();
//        LocalNode node4 = new LocalNode(" ","no1",new URL("FTP","127.0.0.1",5004," "),new StateMachine(),new int[]{5001,5002,5003});
//        node4.start();
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
