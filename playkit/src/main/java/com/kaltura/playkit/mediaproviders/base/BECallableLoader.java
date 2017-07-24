/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.mediaproviders.base;


import android.util.Log;

import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.response.PrimitiveResult;
import com.kaltura.netkit.utils.Accessories;
import com.kaltura.netkit.utils.CallableLoader;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.netkit.utils.SessionProvider;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;

/**
 * Created by tehilarozin on 06/12/2016.
 */

public abstract class BECallableLoader extends CallableLoader<Void> {

    protected String loadReq;
    protected RequestQueue requestQueue;
    protected SessionProvider sessionProvider;

    private boolean waitForCompletion = false;


    protected BECallableLoader(String tag, RequestQueue requestsExecutor, SessionProvider sessionProvider, OnCompletion completion){
        super(tag, completion);

        this.requestQueue = requestsExecutor;
        this.sessionProvider = sessionProvider;
    }

    protected abstract void requestRemote(String response) throws InterruptedException;

    protected abstract ErrorElement validateKs(String ks);


    @Override
    protected void cancel() {
        super.cancel();
        if (loadReq != null) {
            synchronized (syncObject) {
                Log.i(TAG, loadId + ": canceling request execution [" + loadReq + "]");
                requestQueue.cancelRequest(loadReq);
                loadReq = "CANCELED#"+loadReq;
            }
        } else {
            Log.i(TAG, loadId+": cancel: request completed ");
        }

        Log.i(TAG, loadId+": i am canceled ...notifyCompletion");

        notifyCompletion();
    }

    @Override
    protected Void load() throws InterruptedException {

        Log.v(TAG, loadId + ": load: start on get ks ");
        waitForCompletion = true;

        sessionProvider.getSessionToken(new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(isCanceled()){
                    notifyCompletion();
                    waitForCompletion = false;
                    return;
                }

                ErrorElement error = response.error != null ? response.error : validateKs(response.getResult());
                if (error == null) {
                    try {
                        requestRemote(response.getResult());
                        Log.d(TAG, loadId + " remote load request finished...notifyCompletion");
                        notifyCompletion();
                        waitForCompletion = false;
                    } catch (InterruptedException e) {
                         interrupted();
                    }

                } else {
                    Log.w(TAG, loadId + ": got error on ks fetching");
                    if (completion != null) {
                        completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, error));
                    }

                    Log.d(TAG, loadId + "remote load error finished...notifyCompletion");
                    notifyCompletion();
                    waitForCompletion = false;
                }
            }
        });

        if (waitForCompletion && !isCanceled()) { // prevent lock thread on already completed load
            PKLog.v(TAG, loadId + ": load: setting outer completion wait lock");
            waitCompletion();
        }

        PKLog.d(TAG, loadId+": load: wait for completion released");

        return null;
    }

}
