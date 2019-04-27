package io.codeager.infra.raft.core;

import com.google.common.util.concurrent.AbstractScheduledService;
import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.util.timer.RepeatedTimer;

import java.util.Random;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
@Experimental(Experimental.Statement.NOT_FULLY_DESIGNED)
public class StateMachine {
    private State state;
    private int term;
    public int votes;
    public int index;
    public int lastVoteTerm;

    public int getLastVoteTerm() {
        return lastVoteTerm;
    }

    public void setLastVoteTerm(int lastVoteTerm) {
        this.lastVoteTerm = lastVoteTerm;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }



    public StateMachine() {
        this.state = State.FOLLOWER;
        this.term = 1;
        this.votes = 0;
        this.lastVoteTerm = 0;

    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }
    public  void addVotes(){
        this.votes++;
    }
    public  void addTerm(){
        this.term++;
    }
}
