package io.codeager.infra.raft.core.rpc;

import io.codeager.infra.raft.core.LocalNode;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.vote.*;

import java.io.IOException;


public class Server extends GreeterGrpc.GreeterImplBase {
    private io.grpc.Server server;
    private LocalNode node;

    public Server(LocalNode node) {
        this.node = node;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(this.node.getUrl().getPort()).addService(this).build().start();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null)
            server.awaitTermination();
    }

    public void stop() {
        this.server.shutdown();
    }

    @Override
    public void askForVote(VoteRequest request, StreamObserver<VoteReply> responseObserver) {
        node.waitTimer.reset(5000);
        VoteReply voteReply;
        // todo: move the code below inside the node itself
//        if (request.getTerm() >= this.node.getStateMachine().getState().getTerm() && this.node.getStateMachine().getState().getLastVoteTerm() < request.getTerm()) {
//            voteReply = VoteReply.newBuilder().setStatus(true).build();
//            this.node.getStateMachine().setRole(StateMachine.Role.FOLLOWER);
//        } else {
//            voteReply = VoteReply.newBuilder().setStatus(false).build();
//        }
//        responseObserver.onNext(voteReply);
        responseObserver.onCompleted();
    }

    @Override
    public void updateLog(UpdateLogRequest request, StreamObserver<UpdateLogReply> responseObserver) {
        node.waitTimer.reset(5000);
        boolean checkState = this.node.checkLog(request.getIndex(), request.getTerm());
        UpdateLogReply updateLogReply = null;
        if (checkState) {
            this.node.appendEntry(request.getIndex(), request.getTerm(), request.getEntry());
            updateLogReply = UpdateLogReply.newBuilder().setStatus(true).build();
        } else {
            updateLogReply = UpdateLogReply.newBuilder().setStatus(false).build();
        }
        responseObserver.onNext(updateLogReply);
        responseObserver.onCompleted();
    }


    public static void main(String... args) {
//        final Server server = new Server(new Role(0,5000,2,1,1000));
//        try {
//            server.start();
//            server.blockUntilShutdown();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }
}
