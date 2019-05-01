package io.codeager.infra.raft.core;

import io.codeager.infra.raft.core.entity.Endpoint;
import io.codeager.infra.raft.core.rpc.Client;
import io.grpc.vote.StoreRequest;
import io.grpc.vote.UpdateLogRequest;
import io.grpc.vote.VoteRequest;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class RemoteNode extends NodeBase {
    private Client client;
    private int index;

    public RemoteNode(String id, String name, Endpoint endpoint, Client client) {
        super(id, name, endpoint);
        this.client = client;
        this.index = 0;


    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean askForVote(VoteRequest voteRequest) {
        return this.client.askForVote(voteRequest);
    }

    public boolean updateLog(UpdateLogRequest request) {
        return this.client.updateLog(request);
    }

    public void appendEntry(UpdateLogRequest request) {
        this.client.appendEntry(request);
    }

    public boolean store(StoreRequest storeRequest) {
        return this.client.store(storeRequest);
    }
}
