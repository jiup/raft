package io.codeager.infra.raft.cli;

import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.cli.rpc.Client;
import io.codeager.infra.raft.core.entity.Endpoint;

/**
 * @author Jiupeng Zhang
 * @since 05/01/2019
 */
@Experimental(Experimental.Statement.TODO)
public class Rafty {
    private Client client;

    private Rafty() {
    }

    public void disconnect() {
        // client.close();
    }

    public static Rafty subscribe(Endpoint endpoint) {
        return new Rafty(); // todo: build and assign raftyClient
    }

    public static void main(String[] args) {
        Rafty rafty = Rafty.subscribe(Endpoint.of(":9990"));
        rafty.disconnect();
    }
}
