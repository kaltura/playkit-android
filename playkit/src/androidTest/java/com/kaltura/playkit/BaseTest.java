package com.kaltura.playkit;

import org.junit.After;

import java.util.concurrent.CountDownLatch;


/**
 * @hide
 */

public class BaseTest {

    protected CountDownLatch testWaitCount;
    protected String TAG = "BaseTest";
    Object syncObject = new Object();

    public BaseTest() {
    }

    public BaseTest(String tag) {
        this.TAG = tag;
    }

    protected void resume() {
        if (testWaitCount != null) {
            synchronized (syncObject) {
                testWaitCount.countDown();
                PKLog.d(TAG, "count down reduced to " + testWaitCount.getCount());
            }
        }
    }

    protected synchronized void resume(int delay) {
        try {
            Thread.sleep(delay);
            resume();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void wait(int count) {
        synchronized (syncObject) {
            testWaitCount = new CountDownLatch(count);
        }
        try {
            testWaitCount.await(/*10000, TimeUnit.MILLISECONDS*/);
            PKLog.d(TAG, "count down set for " + count);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @After
    public void tearDown() {
        /*resume();*/
    }


    public interface TestBlock<T>{
        void execute(T data) throws AssertionError;
    }
}
