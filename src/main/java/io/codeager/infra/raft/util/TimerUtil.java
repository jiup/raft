package io.codeager.infra.raft.util;

import io.codeager.infra.raft.Experimental;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class TimerUtil {
    @Experimental(Experimental.Statement.TODO)
    static class Scheduler extends ScheduledThreadPoolExecutor {
        public Scheduler(int corePoolSize) {
            super(corePoolSize);
        }

        public Scheduler(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
        }

        public Scheduler(int corePoolSize, RejectedExecutionHandler handler) {
            super(corePoolSize, handler);
        }

        public Scheduler(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, threadFactory, handler);
        }

        public void addDelay(TimeUnit timeUnit, int value) {
        }
    }

    public static Scheduler getScheduler() {
        // TODO
        return null;
    }
}
