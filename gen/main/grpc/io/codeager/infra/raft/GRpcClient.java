package main.grpc.io.codeager.infra.raft;

import io.codeager.infra.raft.PhoneServiceGrpc;
import io.codeager.infra.raft.Phonebook;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jiupeng Zhang
 * @since 04/24/2019
 */
public class GRpcClient {
    private static final Logger logger = Logger.getLogger(GRpcClient.class.getName());

    private final ManagedChannel channel;

    private final PhoneServiceGrpc.PhoneServiceBlockingStub blockingStub;

    /**
     * Construct client connecting to gRPC server at {@code host:port}.
     */
    public GRpcClient(String host, int port) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true);
        channel = channelBuilder.build();
        blockingStub = PhoneServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * add phone to user.
     */
    public void addPhoneToUser(int uid, Phonebook.PhoneType phoneType, String phoneNubmer) {
        logger.info("Will try to add phone to user " + uid);
        Phonebook.AddPhoneToUserRequest request = Phonebook.AddPhoneToUserRequest.newBuilder().setUid(uid).setPhoneType(phoneType)
                .setPhoneNumber(phoneNubmer).build();
        Phonebook.AddPhoneToUserResponse response;
        try {
            response = blockingStub.addPhoneToUser(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Result: " + response.getResult());
    }

    public static void main(String[] args) throws Exception {
        GRpcClient client = new GRpcClient("localhost", 50051);
        try {
            client.addPhoneToUser(1, Phonebook.PhoneType.WORK, "13888888888");
        } finally {
            client.shutdown();
        }
    }
}