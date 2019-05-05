package io.codeager.infra.raft.core;

import io.codeager.infra.raft.core.entity.Endpoint;
import io.codeager.infra.raft.core.rpc.Client;
import io.grpc.vote.*;

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

    public String get(GetRequest getRequest) {
        return this.client.get(getRequest);
    }

    public int size(SizeRequest sizeRequest) {
        return this.client.size(sizeRequest);
    }

    public boolean remove(RemoveRequest removeRequest) {
        return this.client.remove(removeRequest);
    }

    public boolean containsKey(String key) {
        ContainsRequest containsRequest = ContainsRequest.newBuilder().setContent(key).build();
        return this.client.containsKey(containsRequest);
    }

    public boolean containsValue(String value) {
        ContainsRequest containsRequest = ContainsRequest.newBuilder().setContent(value).build();
        return this.client.containsValue(containsRequest);
    }

    public String getRemoteId() {
        GetIdRequest getIdRequest = GetIdRequest.newBuilder().build();
        return this.client.getRemoteId(getIdRequest);
    }
}
