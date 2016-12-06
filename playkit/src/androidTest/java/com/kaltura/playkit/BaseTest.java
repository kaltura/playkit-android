package com.kaltura.playkit;

import org.junit.After;

import java.util.concurrent.CountDownLatch;


/**
 * Created by tehilarozin on 13/11/2016.
 */

public class BaseTest {

    protected CountDownLatch testWaitCount;
    protected String TAG = "BaseTest";

    public BaseTest(){
    }

    public BaseTest(String tag){
        this.TAG = tag;
    }

    protected void resume(){
        if(testWaitCount != null) {
            testWaitCount.countDown();
            PKLog.i(TAG, "count down reduced to "+testWaitCount.getCount());
        }
    }

    protected void resume(int delay){
        try {
            Thread.sleep(delay);
            resume();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected synchronized void wait(int count){
        testWaitCount = new CountDownLatch(count);
        try {
            testWaitCount.await();
            PKLog.i(TAG, "count down set for "+count);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown(){
        /*resume();*/
    }
}
