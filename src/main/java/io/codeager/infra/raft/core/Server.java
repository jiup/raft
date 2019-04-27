package io.codeager.infra.raft.core;

import io.codeager.infra.raft.core.entity.Node;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.vote.*;


import java.io.IOException;


public class Server extends GreeterGrpc.GreeterImplBase{
    private io.grpc.Server server;
    private Node node;
    Server(Node node){
        this.node = node;
    }
    public void start() throws IOException {
        server = ServerBuilder.forPort(this.node.getUrl().getPort()).addService(this).build().start();
    }
    public void blockUntilShutdown() throws InterruptedException {
        if (server!=null)
            server.awaitTermination();
    }

    public void stop(){
        this.server.shutdown();
    }
    @Override
    public void askForVote(VoteRequest request, StreamObserver<VoteReply> responseObserver) {
        VoteReply voteReply = null;
        if (request.getTerm()>=this.node.getStateMachine().getTerm()){
            voteReply = VoteReply.newBuilder().setStatus(true).build();
            this.node.getStateMachine().setState(State.FOLLOWER);
        }else{
            voteReply = VoteReply.newBuilder().setStatus(true).build();
        }
        responseObserver.onNext(voteReply);
        responseObserver.onCompleted();
    }

    @Override
    public void updateLog(UpdateLogRequest request, StreamObserver<UpdateLogReply> responseObserver) {
        boolean checkState = this.node.checkLog(request.getIndex(),request.getTerm());
        UpdateLogReply updateLogReply = null;
        if (checkState){
            this.node.appendEntry(request.getIndex(),request.getTerm(),request.getEntry());
            updateLogReply = UpdateLogReply.newBuilder().setStatus(true).build();
        }else{
            updateLogReply = UpdateLogReply.newBuilder().setStatus(false).build();
        }
        responseObserver.onNext(updateLogReply);
        responseObserver.onCompleted();
    }


    public static void main(String...args){
//        final Server server = new Server(new State(0,5000,2,1,1000));
//        try {
//            server.start();
//            server.blockUntilShutdown();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }
}
