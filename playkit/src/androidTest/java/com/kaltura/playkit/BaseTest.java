package com.kaltura.playkit;

import org.junit.After;

import java.util.concurrent.CountDownLatch;


/**
 * Created by tehilarozin on 13/11/2016.
 */

public class BaseTest {

    private CountDownLatch testWaitCount;

    protected void resume(){
        if(testWaitCount != null) {
            testWaitCount.countDown();
        }
    }

    protected synchronized void wait(int count){
        testWaitCount = new CountDownLatch(count);
        try {
            testWaitCount.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown(){
        resume();
    }
}
