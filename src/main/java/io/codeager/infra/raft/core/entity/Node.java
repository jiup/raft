package io.codeager.infra.raft.core.entity;


import io.codeager.infra.raft.core.Client;
import io.codeager.infra.raft.core.State;
import io.codeager.infra.raft.core.StateMachine;
import io.codeager.infra.raft.util.timer.RepeatedTimer;
import io.grpc.vote.Entry;
import io.grpc.vote.UpdateLogRequest;
import io.grpc.vote.VoteRequest;
import io.codeager.infra.raft.core.Server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class Node extends NodeBase {
    private StateMachine stateMachine;
    private List<RemoteNode> neighbours;
    private Server server;
    public RepeatedTimer voteTimer;
    public RepeatedTimer waitTimer;
    public RepeatedTimer heartTimer;
    private ServerContainer serverContainer;
    private StateConvert stateConvert;

    private

    abstract class Timer extends RepeatedTimer {
        Node currentNode;

        Timer(Node currentNode, String name, long timeoutInMillis) {
            super(name, timeoutInMillis);
            this.currentNode = currentNode;
        }
    }

    public Node(String id, String name, URL url, StateMachine stateMachine, int[] neighbours) {
        super(id, name, url);
        this.server = new Server(this);
        this.stateMachine = stateMachine;
        this.neighbours = new ArrayList<>();
        for (int port:neighbours){
            this.neighbours.add(new RemoteNode(" "," ",null,new Client("127.0.0.1",port)));
        }
        this.waitTimer = new Timer(this, "waitTimer", 5000000) {

            @Override
            protected void onTrigger() {
                this.currentNode.changeState(State.CANDIDATE,"waitTimer");

            }
        };
        this.voteTimer = new Timer(this, "voteTimer", new Random().nextInt(5000)) {

            @Override
            protected void onTrigger() {
                this.currentNode.checkVoteResult();
            }
        };
        this.heartTimer = new Timer(this,"heartTimer",new Random().nextInt(5000)) {
            @Override
            protected void onTrigger() {
                this.currentNode.sendHeart();
            }
        };
    }
    private  void sendHeart(){
        UpdateLogRequest updateLogRequest = UpdateLogRequest.newBuilder().setTerm(this.getStateMachine().getTerm()).setIndex(this.getStateMachine().getIndex()).build();
        for(RemoteNode neighbour:neighbours){
            neighbour.
        }
    }

    private void checkVoteResult() {
        if (this.getStateMachine().getVotes() > (this.neighbours.size()) / 2){
            changeState(State.LEADER,"voteTimer");
        }else{

            this.stateConvert.notifyAll();
        }

    }

    private void changeState(State state,String timerName) {
        this.getStateMachine().setState(state);
        if(timerName.equals("waitTimer")){
            this.waitTimer.stop();
        }else if(timerName.equals("voteTimer")) {
            this.voteTimer.stop();
        }
        System.err.println("notifyAll");
        this.stateConvert.notifyAll();

    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    public boolean checkLog(int index, int term) {
        //Todo
        return true;
    }

    public void appendEntry(int index, int term, io.grpc.vote.Entry entry) {
        //Todo

    }

    public void start(){
        this.stateConvert = new StateConvert(this.getStateMachine(),this);
        this.serverContainer = new ServerContainer(this.server);
        Thread thread1 = new Thread(this.serverContainer);
        Thread thread2 = new Thread(this.stateConvert);
        thread2.start();
        thread1.start();
        try {
            thread2.join();
            thread1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String...args) throws MalformedURLException {
//        int[] ports = {5001,5002,5003};
//        Node node1 = new Node(" ","no1",new URL("FTP","127.0.0.1",5001," "),new StateMachine(),new int[]{5004,5002,5003});
//        node1.start();
//        Node node2 = new Node(" ","no1",new URL("FTP","127.0.0.1",5002," "),new StateMachine(),new int[]{5001,5004,5003});
//        node2.start();
//        Node node3 = new Node(" ","no1",new URL("FTP","127.0.0.1",5003," "),new StateMachine(),new int[]{5001,5002,5004});
//        node3.start();
        Node node4 = new Node(" ","no1",new URL("FTP","127.0.0.1",5004," "),new StateMachine(),new int[]{5001,5002,5003});
        node4.start();


    }
    class ServerContainer implements Runnable{
        private Server server;
        public ServerContainer(Server server){
            this.server = server;
        }
        @Override
        public void run() {
            try {
                server.start();
                server.blockUntilShutdown();
            }catch (Exception e){
                System.out.println(e.getMessage());
            }finally {
                server.stop();
            }

        }
    }


    class StateConvert implements Runnable {
//        private StateMachine stateMachine;
        private Node node;

        public StateConvert(StateMachine stateMachine, Node node) {
//            this.stateMachine = stateMachine;
            this.node = node;
        }

        @Override
        public void run() {
            while (true) {
                switch (this.node.getStateMachine().getState()) {
                    case FOLLOWER:
                        System.err.println("state FOLLOWER");
//                      //Todo
                        node.waitTimer.start();
                        synchronized (this){
                            try {
                                this.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        break;
                    case CANDIDATE:
                        System.err.println("state CANDIDATE");
                        voteTimer.start();
                        VoteRequest voteRequest = VoteRequest.newBuilder().setTerm(this.node.stateMachine.getTerm()).build();
                        for (RemoteNode neighbour : node.neighbours) {
                            if (neighbour.askForVote(voteRequest)) {
                                this.node.getStateMachine().addVotes();
                            }
                        }
                        if (this.node.getStateMachine().getVotes() > (this.node.neighbours.size() / 2)) {
                            this.node.stateMachine.setState(State.LEADER);
                        }
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        this.node.getStateMachine().addTerm();
                        break;
                    case LEADER:
                        System.err.println("state Leader");

//                        this.state.time= new AtomicInteger();
//                        this.state.time.set(new Random().nextInt(5000));
//                        while(this.state.time.get()>0){
//                            this.state.time.incrementAndGet();
//                        }
//                        this.state.state = 2;
                        break;
                }
            }
        }
    }
}
