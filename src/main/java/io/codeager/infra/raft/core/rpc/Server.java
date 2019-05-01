package io.codeager.infra.raft.core.rpc;

import io.codeager.infra.raft.core.LocalNode;
import io.codeager.infra.raft.core.StateMachine;
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
        server = ServerBuilder.forPort(this.node.getEndpoint().getPort()).addService(this).build().start();
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
        node.resetWaitTimer();
        boolean status = this.node.handleVoteRequest(request.getTerm());
        VoteReply voteReply = VoteReply.newBuilder().setStatus(status).build();
        responseObserver.onNext(voteReply);
        responseObserver.onCompleted();
    }

    @Override
    public void updateLog(UpdateLogRequest request, StreamObserver<UpdateLogReply> responseObserver) {
        node.resetWaitTimer();
        boolean checkState = this.node.checkLog(request.getIndex(), request.getTerm(), request.getIp());
        UpdateLogReply updateLogReply;
        if (checkState) {
            this.node.appendEntry(request.getIndex(), request.getTerm(), request.getEntry());
            updateLogReply = UpdateLogReply.newBuilder().setStatus(true).build();
        } else {
            updateLogReply = UpdateLogReply.newBuilder().setStatus(false).build();
        }
        responseObserver.onNext(updateLogReply);
        responseObserver.onCompleted();
    }

    @Override
    public void appendLog(UpdateLogRequest request, StreamObserver<UpdateLogReply> responseObserver) {
        node.resetWaitTimer();
        this.node.appendEntry(request.getIndex(), request.getTerm(), request.getEntry());
        UpdateLogReply updateLogReply;
        updateLogReply = UpdateLogReply.newBuilder().setStatus(true).build();
        responseObserver.onNext(updateLogReply);
        responseObserver.onCompleted();
    }

    @Override
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
        StoreResponse storeResponse;
        boolean status;
        if (this.node.getStateMachine().getState().role == StateMachine.Role.LEADER) {
            status = this.node.store(request.getEntry());
            storeResponse = StoreResponse.newBuilder().setStatus(status).build();
        } else {
            status = this.node.leader.store(request);
            storeResponse = StoreResponse.newBuilder().setStatus(status).build();
        }
        responseObserver.onNext(storeResponse);
        responseObserver.onCompleted();
    }

    public static void main(String... args) {

    }
}
