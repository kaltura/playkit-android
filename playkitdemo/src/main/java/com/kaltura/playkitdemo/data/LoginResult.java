package com.kaltura.playkitdemo.data;


import com.connect.backend.BaseResult;

/**
 * Created by tehilarozin on 16/11/2016.
 */

public class LoginResult extends BaseResult {

    Result result;

    public String getKs(){
        return result != null ? result.getKs() : null;
    }

    class Result{
        LoginSession loginSession;

        public String getKs(){
            return loginSession != null ? loginSession.ks : null;
        }
    }

    class LoginSession{
        String ks;
    }
}
