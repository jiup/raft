package io.codeager.infra.raft.core.entity;

import io.codeager.infra.raft.core.Client;
import io.grpc.vote.UpdateLogRequest;
import io.grpc.vote.VoteRequest;

import java.net.URL;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class RemoteNode extends NodeBase {
    private Client client;

    public RemoteNode(String id, String name, URL url,Client client)
    {
        super(id, name, url);
        this.client = client;


    }
    public boolean askForVote(VoteRequest voteRequest){
        return this.client.askForVote(voteRequest);
    }
    public boolean updateLog(UpdateLogRequest request){
        return this.client.updateLog(request);
    }
}
