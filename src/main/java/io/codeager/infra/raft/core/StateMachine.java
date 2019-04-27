package io.codeager.infra.raft.core;

import com.google.common.util.concurrent.AbstractScheduledService;
import io.codeager.infra.raft.Experimental;
import io.codeager.infra.raft.util.TimerUtil;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
@Experimental(Experimental.Statement.NOT_FULLY_DESIGNED)
public class StateMachine {
    private State state;
    private int term;
    private int delay;
    private TimerUtil.Scheduler scheduler;

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

    public TimerUtil.Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(TimerUtil.Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public StateMachine() {
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
