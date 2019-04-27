package io.codeager.infra.raft.util.timer;

import org.junit.Test;

/**
 * @author Jiupeng Zhang
 * @since 04/26/2019
 */
public class RepeatedTimerTest {
    @Test
    public void test() {
        RepeatedTimer timer = new RepeatedTimer("hello world", 1000) {
            @Override
            protected void onTrigger() {
                System.out.println("hello");
            }
        };
        timer.start();
    }

    public static void main(String[] args) throws InterruptedException {
        RepeatedTimer timer = new RepeatedTimer("hello world", 5000) {
            @Override
            protected void onTrigger() {
                System.out.println("hello");
            }

            @Override
            protected void onDestroy() {
                System.out.println("done!");
            }

            @Override
            protected void onException(Throwable throwable) {
                super.onException(throwable);
            }

            @Override
            protected long updateTimeout(long initTimeout, long prevTimeout) {
                return super.updateTimeout(initTimeout, prevTimeout);
            }
        };
        timer.start();
        System.out.println("test");
//        Thread.sleep(3000);
//        timer.stop();
//        Thread.sleep(3000);
//        timer.start();
//        Thread.sleep(3000);
//        timer.destroy();
        System.out.println(timer.getTriggerCount());
    }
}
