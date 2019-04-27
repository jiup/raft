package io.codeager.infra.raft.util.timer;

import io.codeager.infra.raft.Experimental;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
@Experimental({Experimental.Statement.NOT_FULLY_DESIGNED, Experimental.Statement.TODO_TEST})
public abstract class RepeatedTimer {
    public static final Logger LOG = LoggerFactory.getLogger(RepeatedTimer.class);

    private final Lock lock = new ReentrantLock();
    private final String name;
    private final Timer timer;
    private final long initTimeout;
    private long prevTimeout;
    private volatile boolean running;
    private volatile boolean destroyed;
    private TimerTask timerTask;
    protected long counter;

    protected RepeatedTimer(String name, long timeoutInMillis) {
        this.name = name;
        this.timer = new Timer(name);
        this.initTimeout = timeoutInMillis;
        this.running = false;
        this.destroyed = false;
    }

    protected long updateTimeout(long timeout) {
        return timeout;
    }

    protected long updateTimeout(long initTimeout, long prevTimeout) {
        return updateTimeout(initTimeout);
    }

    protected abstract void onTrigger();

    protected void onDestroy() {
        // do nothing by default
    }

    public void start() {
        if (destroyed)
            throw new IllegalStateException("timer was destroyed");

        lock.lock();
        try {
            if (running) return;
            running = true;
            reschedule();
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        if (destroyed)
            throw new IllegalStateException("timer was destroyed");

        lock.lock();
        try {
            if (!running) return;
            running = false;
            if (timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
        } finally {
            lock.unlock();
        }
    }

    public void reset() {
        reset(initTimeout);
    }

    public void reset(long timeout) {
        if (destroyed)
            throw new IllegalStateException("timer was destroyed");

        lock.lock();
        try {
            if (!running) return;
            prevTimeout = 0;
            reschedule();
        } finally {
            lock.unlock();
        }
    }

    public void destroy() {
        lock.lock();
        try {
            if (destroyed) return;
            if (running) {
                this.stop();
            }
            destroyed = true;
            timer.cancel();
        } finally {
            lock.unlock();
        }
        onDestroy();
    }

    private void reschedule() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        timer.schedule(timerTask = newTimerTask(), prevTimeout = updateTimeout(initTimeout, prevTimeout));
    }

    private TimerTask newTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                lock.lock();
                try {
                    try {
                        onTrigger();
                        counter++;
                    } catch (Throwable throwable) {
                        LOG.error("exception caught: {}", throwable.getMessage());
                        // throwable.printStackTrace();
                    }
                    if (!destroyed && running) {
                        reschedule();
                    }
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    public String getName() {
        return name;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public long getTriggerCount() {
        return counter;
    }

    public long getPrevTimeout() {
        return prevTimeout;
    }

    public long getInitTimeout() {
        return initTimeout;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("timer", timer)
                .append("initTimeout", initTimeout)
                .append("prevTimeout", prevTimeout)
                .append("running", running)
                .append("destroyed", destroyed)
                .toString();
    }
}
