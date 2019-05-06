package io.codeager.infra.raft.core.rpc;

import com.google.protobuf.StringValue;
import io.codeager.infra.raft.core.LocalNode;
import io.codeager.infra.raft.core.StateMachine;
import io.codeager.infra.raft.core.entity.LogEntry;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.vote.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;


public class Server extends GreeterGrpc.GreeterImplBase {
    private io.grpc.Server server;
    private LocalNode node;

    public Server(LocalNode node) {
        this.node = node;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(this.node.getEndpoint().getPort()).addService(this).build().start();
        this.node.initPeersId();
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
        boolean status = this.node.handleVoteRequest(request.getTerm());
//        node.resetWaitTimer();
        VoteReply voteReply = VoteReply.newBuilder().setStatus(status).build();
        responseObserver.onNext(voteReply);
        responseObserver.onCompleted();
    }

    @Override
    public void getId(GetIdRequest request, StreamObserver<GetIdResponse> responseObserver) {
        String id = this.node.getId();
        GetIdResponse getIdResponse = GetIdResponse.newBuilder().setId(id).build();
        responseObserver.onNext(getIdResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void updateLog(UpdateLogRequest request, StreamObserver<UpdateLogReply> responseObserver) {
        node.resetWaitTimer();
        LogEntry logEntry = LogEntry.of(request.getLogEntry());
        boolean checkState = this.node.checkLog(logEntry, request.getId());
        UpdateLogReply updateLogReply;
        if (checkState) {
            this.node.recover(logEntry);
//            this.node.appendEntry(request.getIndex(), request.getTerm(), request.getEntry());
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
        if (request.hasEntry()) {
            this.node.appendEntry(LogEntry.of(request.getLogEntry()), request.getEntry().getValue().getValue());
        } else {
            this.node.appendEntry(LogEntry.of(request.getLogEntry()), null);
        }
        UpdateLogReply updateLogReply;
        updateLogReply = UpdateLogReply.newBuilder().setStatus(true).build();
        responseObserver.onNext(updateLogReply);
        responseObserver.onCompleted();
    }

    @Override
    public void store(StoreRequest request, StreamObserver<StoreResponse> responseObserver) {
        node.resetWaitTimer();
        StoreResponse storeResponse;
        boolean status;
        if (this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            status = this.node.store(request.getEntry().getKey(), request.getEntry().getValue().getValue());
            storeResponse = StoreResponse.newBuilder().setStatus(status).build();
        } else {
            status = this.node.getLeader().store(request);
            storeResponse = StoreResponse.newBuilder().setStatus(status).build();
        }
        responseObserver.onNext(storeResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        node.resetWaitTimer();
        String key = request.getKey();
        GetResponse getResponse;
        // just find the key in self
        if (true || this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            String value = this.node.get(key);
            if (value == null) {
                getResponse = GetResponse.newBuilder().build();
            } else {
                getResponse = GetResponse.newBuilder().setValue(StringValue.of(value)).build();
            }
        } else {
            String value = this.node.getLeader().get(GetRequest.newBuilder().setKey(key).build());
            if (value == null) {
                getResponse = GetResponse.newBuilder().build();
            } else {
                getResponse = GetResponse.newBuilder().setValue(StringValue.of(value)).build();
            }
        }
        responseObserver.onNext(getResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void size(SizeRequest request, StreamObserver<SizeResponse> responseObserver) {
        node.resetWaitTimer();
        SizeResponse sizeResponse;
        if (true || this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            int size = this.node.size();
            sizeResponse = SizeResponse.newBuilder().setSize(size).build();
        } else {
//            System.out.println(this.node.getLeader());
            int size = this.node.getLeader().size(request);
            sizeResponse = SizeResponse.newBuilder().setSize(size).build();
        }
        responseObserver.onNext(sizeResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void remove(RemoveRequest request, StreamObserver<RemoveResponse> responseObserver) {
        node.resetWaitTimer();
        RemoveResponse removeResponse;
        if (this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            boolean status = this.node.remove(request.getKey());
            removeResponse = RemoveResponse.newBuilder().setStatus(status).build();
        } else {
            boolean status = this.node.getLeader().remove(request);
            removeResponse = RemoveResponse.newBuilder().setStatus(status).build();
        }
        responseObserver.onNext(removeResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void containsKey(ContainsRequest request, StreamObserver<ContainsResponse> responseObserver) {
        node.resetWaitTimer();
        boolean status;
        if (true || this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            status = this.node.containsKey(request.getContent());
        } else {
            status = this.node.getLeader().containsKey(request.getContent());
        }
        ContainsResponse containsResponse = ContainsResponse.newBuilder().setStatus(status).build();
        responseObserver.onNext(containsResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void containsValue(ContainsRequest request, StreamObserver<ContainsResponse> responseObserver) {
        node.resetWaitTimer();
        boolean status;
        if (true || this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            status = this.node.containsValue(request.getContent());
        } else {
            status = this.node.getLeader().containsValue(request.getContent());
        }
        ContainsResponse containsResponse = ContainsResponse.newBuilder().setStatus(status).build();
        responseObserver.onNext(containsResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void values(ValuesRequest request, StreamObserver<ValuesResponse> responseObserver) {
        node.resetWaitTimer();
        Collection<String> values;
        ValuesResponse valuesResponse;
        if (true || this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            values = this.node.getValues();
            ValuesResponse.Builder builder = ValuesResponse.newBuilder();
            int i = 0;
            for (String value : values) {
                builder.addValue(value);
            }
            valuesResponse = builder.build();
        } else {
            valuesResponse = this.node.getLeader().getValues();
        }
        responseObserver.onNext(valuesResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void keys(KeysRequest request, StreamObserver<KeysResponse> responseObserver) {
        node.resetWaitTimer();
        Collection<String> keys;
        KeysResponse keysResponse;
        if (true || this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            keys = this.node.getKeys();
            KeysResponse.Builder builder = KeysResponse.newBuilder();
            int i = 0;
            for (String value : keys) {
                builder.addKey(value);
            }
            keysResponse = builder.build();
        } else {
            keysResponse = this.node.getLeader().getKeys();
        }
        responseObserver.onNext(keysResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void entries(EntriesRequest request, StreamObserver<EntriesResponse> responseObserver) {
        node.resetWaitTimer();
        Collection<Map.Entry<String, String>> Entries;
        EntriesResponse entriesResponse;
        if (true || this.node.getStateMachine().onState(StateMachine.Role.LEADER)) {
            Entries = this.node.getEntries();
            EntriesResponse.Builder builder = EntriesResponse.newBuilder();
            int i = 0;
            for (Map.Entry entry : Entries) {
                builder.addEntry(DataEntry.newBuilder().
                        setKey(String.valueOf(entry.getKey())).
                        setValue(StringValue.of((String) entry.getValue())).build());
            }
            entriesResponse = builder.build();
        } else {
            entriesResponse = this.node.getLeader().getEntries();
        }
        responseObserver.onNext(entriesResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void command(CommandRequest request, StreamObserver<CommandResponse> responseObserver) {
        //todo
        if (request.getCommand().equals("STOP")) {
            this.node.stop();
        } else if (request.getCommand().equals("PAUSE")) {
            this.node.pause();
        } else if (request.getCommand().equals("RESUME")) {
            this.node.resume();
        }
        responseObserver.onNext(CommandResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
