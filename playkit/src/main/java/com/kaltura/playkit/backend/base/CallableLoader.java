package com.kaltura.playkit.backend.base;

import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Created by tehilarozin on 04/12/2016.
 */

public abstract class CallableLoader implements Callable<Void> {

    protected final String loadId = this.toString() + ":" + System.currentTimeMillis();

    protected OnCompletion completion;
    protected CountDownLatch waitCompletion;

    protected String TAG;
    protected final Object syncObject = new Object();
    protected boolean isCanceled = false;


    protected CallableLoader(String tag, OnCompletion completion) {
        this.completion = completion;
        this.TAG = tag;
    }

    abstract protected void load() throws InterruptedException;

    abstract protected void cancel();

    protected void notifyCompletion() {
        if (waitCompletion != null) {
            synchronized (syncObject) {
                PKLog.v(TAG, loadId + ": notifyCompletion: countDown =  " + waitCompletion.getCount());
                waitCompletion.countDown();
            }
        }
    }

    protected void waitCompletion(int countDownLatch) throws InterruptedException {
        if(waitCompletion != null && waitCompletion.getCount() == countDownLatch){
            return;
        }

        synchronized (syncObject) {
            PKLog.i(TAG, loadId + ": waitCompletion: set new counDown " + (waitCompletion != null ? "already has counter " + waitCompletion.getCount() : ""));
            waitCompletion = new CountDownLatch(countDownLatch);
        }
        waitCompletion.await();
        waitCompletion = null;
    }

    protected void waitCompletion() throws InterruptedException {
        waitCompletion(1);
    }

    @Override
    public Void call() {
        if (isCanceled()) { // needed in case cancel done before callable started
            PKLog.i(TAG, loadId + ": Loader call canceled");
            return null;
        }

        PKLog.i(TAG, loadId + ": Loader call started ");

        try {
            load();
            PKLog.i(TAG, loadId + ": load finished with no interruptions");
        } catch (InterruptedException e) {
            interrupted();
        }
        return null;
    }

    protected boolean isCanceled() {
        return isCanceled;// Thread.currentThread().isInterrupted();
    }

    protected void interrupted() {
        PKLog.i(TAG, loadId + ": loader operation interrupted ");
        cancel();
    }


}
