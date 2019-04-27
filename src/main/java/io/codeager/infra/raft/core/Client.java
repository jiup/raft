package io.codeager.infra.raft.core;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.vote.*;

import java.util.concurrent.TimeUnit;

public class Client {
    private final ManagedChannel channel;
    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    public Client(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true)
                .build());
    }

    Client(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public boolean askForVote(VoteRequest voteRequest) {
        VoteReply voteReply = blockingStub.askForVote(voteRequest);
        return voteReply.getStatus();
    }

    public boolean updateLog(UpdateLogRequest updateLogRequest) {
        UpdateLogReply updateLogReply = blockingStub.updateLog(updateLogRequest);
        return updateLogReply.getStatus();
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void main(String... args) {
        String name = "zhangyu";
        Client client = new Client("127.0.0.1", 5000);
        VoteRequest voteRequest = VoteRequest.newBuilder().setPort(5000).setTerm(2).build();
        boolean b = client.askForVote(voteRequest);
        System.out.println(b);
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
