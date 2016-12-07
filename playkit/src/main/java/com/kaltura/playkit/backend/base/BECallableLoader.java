package com.kaltura.playkit.backend.base;

import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.RequestQueue;

/**
 * Created by tehilarozin on 06/12/2016.
 */

public abstract class BECallableLoader extends CallableLoader {

    protected String loadReq;
    protected RequestQueue requestQueue;
    protected SessionProvider sessionProvider;

    protected BECallableLoader(String tag, RequestQueue requestsExecutor, SessionProvider sessionProvider, OnCompletion completion){
        super(tag, completion);

        this.requestQueue = requestsExecutor;
        this.sessionProvider = sessionProvider;
    }

    protected abstract void requestRemote(String response) throws InterruptedException;

    protected abstract ErrorElement validateKs(String ks);


    @Override
    protected void cancel() {
        if (loadReq != null) {
            PKLog.i(TAG, loadId + ": canceling request execution [" + loadReq + "]");
            requestQueue.cancelRequest(loadReq);
        } else {
            PKLog.i(TAG, loadId+": cancel: request completed ");
        }
        notifyCompletion();
    }

    @Override
    protected void load() throws InterruptedException {

        PKLog.i(TAG, loadId + ": load: start on get ks ");

        sessionProvider.getKs(new OnCompletion<String>() {
            @Override
            public void onComplete(String response) {
                ErrorElement error = validateKs(response);
                if (error == null) {
                    try {
                        requestRemote(response);

                    } catch (InterruptedException e) {
                        interrupted();
                    }

                } else {
                    PKLog.w(TAG, loadId + ": got error on ks fetching");
                    if (completion != null) {
                        completion.onComplete(Accessories.<PKMediaEntry>buildResult(null, error));
                    }
                    notifyCompletion();
                }
            }
        });

        waitCompletion();

        PKLog.i(TAG, loadId+": wait for completion released");
    }

}
