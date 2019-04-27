package io.codeager.infra.raft.core.entity;


import io.codeager.infra.raft.core.State;
import io.codeager.infra.raft.core.StateMachine;
import io.grpc.Server;
import io.grpc.vote.VoteRequest;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class Node extends NodeBase {
    private StateMachine stateMachine;
    private List<RemoteNode> neighbours;
    private Server server;

    public Node(String id, String name, URL url, StateMachine stateMachine, ArrayList neighbours) {
        super(id, name, url);
        this.stateMachine = stateMachine;
        this.neighbours = neighbours;
    }

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }
    public boolean checkLog(int index,int term){
        //Todo
        return true;
    }
    public void appendEntry(int index,int term,io.grpc.vote.Entry entry){
        //Todo

    }

    class StateConvert implements Runnable {
        private StateMachine stateMachine;
        private Node node;
        public StateConvert(StateMachine stateMachine,Node node){
            this.stateMachine = stateMachine;
            this.node = node;
        }

        @Override
        public void run() {
            while (true){
                switch (this.stateMachine.getState()){
                    case FOLLOWER:
                        System.err.println("state 1");
//                      //Todo
//                        while(stateMachine.get)
//                        this.state.time.set(new Random().nextInt(5000));
//                        while(this.state.time.get()>0){
//                            this.state.time.incrementAndGet();
//                        }
//                        this.state.state = 2;
                        break;
                    case CANDIDATE:
                        System.err.println("state 2");
                        int count = 0;
                        VoteRequest voteRequest =  VoteRequest.newBuilder().setTerm(this.stateMachine.getTerm()).build();
                        for(RemoteNode neighbour :node.neighbours){
                            if(neighbour.askForVote(voteRequest)){
                                count++;
                            }
                        }
                        if (count>=this.node.stateMachine.getTerm()/2){
                            this.node.stateMachine.setState(State.LEADER);
                        }
                        break;
                    case LEADER:
                        System.err.println("state 1");
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
