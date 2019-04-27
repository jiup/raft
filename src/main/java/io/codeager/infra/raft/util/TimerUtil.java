package io.codeager.infra.raft.util;

import io.codeager.infra.raft.Experimental;

import java.util.concurrent.*;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class TimerUtil {
    @Experimental(Experimental.Statement.TODO)
    public static class Scheduler extends ScheduledThreadPoolExecutor {

        private int delayBuf;
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
            delayBuf+=value;
        }


        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            do{
                delay += delayBuf;
                delayBuf = 0;
                super.schedule(command,delay,unit);
            }while (delayBuf>0);
            return null;
        }
    }

    public static Scheduler getScheduler() {
        // TODO
        return null;
    }
}
