package com.kaltura.playkit.backend.ovp;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.base.BaseSessionProvider;
import com.kaltura.playkit.backend.ovp.data.KalturaSessionInfo;
import com.kaltura.playkit.backend.ovp.data.PrimitiveResult;
import com.kaltura.playkit.backend.ovp.services.OvpService;
import com.kaltura.playkit.backend.ovp.services.OvpSessionService;
import com.kaltura.playkit.backend.ovp.services.UserService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.GsonParser;
import com.kaltura.playkit.connect.MultiRequestBuilder;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.RequestBuilder;
import com.kaltura.playkit.connect.ResponseElement;

import java.util.List;

import static java.lang.System.currentTimeMillis;

/**
 * Created by tehilarozin on 27/11/2016.
 */

public class OvpSessionProvider extends BaseSessionProvider {

    private static final String TAG = "OvpSessionProvider";

    private static final int DefaultSessionExpiry = 24 * 60 * 60; //in seconds

    /**
     * defines the time before expiration, to restart session on.
     */
    public static final long ExpirationDelta = 10 * 60;//hour in seconds

    private OvpSessionParams sessionParams;


    public OvpSessionProvider(String baseUrl) {
        super(baseUrl);
    }


    /**
     * starts anonymous session
     */
    public void startAnonymousSession(int partnerId, final OnCompletion<PrimitiveResult> completion) {
        sessionParams = new OvpSessionParams().setPartnerId(partnerId);
        final long expiration = System.currentTimeMillis() / 1000 + DefaultSessionExpiry;
        RequestBuilder requestBuilder = OvpSessionService.anonymousSession(baseUrl, sessionParams.partnerId())
                .completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        handleAnonymousResponse(response, expiration, completion);
                    }
                });
        APIOkRequestsExecutor.getSingleton().queue(requestBuilder.build());
    }

    private void handleAnonymousResponse(ResponseElement response, long expiration, OnCompletion<PrimitiveResult> completion) {
        ErrorElement error = null;

        if (response != null && response.isSuccess()) {

            try {
                JsonElement responseElement = GsonParser.toJson(response.getResponse());
                String ks = responseElement.getAsJsonObject().getAsJsonPrimitive("ks").getAsString();
                setSession(ks, expiration, "0");// sets a "dummy" session expiration, since we can't get the actual expiration from the server
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(ks));
                }

            } catch (JsonSyntaxException e) {
                error = ErrorElement.SessionError.message("got response but failed to parse it");
            }

        } else { // failed to start session
            error = response.getError() != null ? response.getError() : ErrorElement.SessionError;
        }

        if (error != null) {
            clearSession(); //clears current saved data - app can try renewSession with the current credentials. or endSession/startSession
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(error)); // in case we can't login - app should provide a solution.
            }
        }
    }

    /**
     * starts new user session
     *
     * @param username - user's email that identifies the user for login (username)
     * @param password
     */
    public void startSession(@NonNull String username, @NonNull String password, int partnerId, final OnCompletion<PrimitiveResult> completion) {
        // login user
        //get session data for expiration time
        this.sessionParams = new OvpSessionParams().setPassword(password).setUsername(username).setPartnerId(partnerId);

        MultiRequestBuilder multiRequest = OvpService.getMultirequest(baseUrl, null);
        multiRequest.add(UserService.loginByLoginId(baseUrl, sessionParams.username, sessionParams.password, sessionParams.partnerId()),
                OvpSessionService.get(baseUrl, "{1:result}")).
                completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        handleStartSession(response, completion);
                    }
                });

        APIOkRequestsExecutor.getSingleton().queue(multiRequest.build());
    }


    /*
    * !! in case the ks expired we need to relogin
    *
    * in case the login fails - or the second request fails message will be passed to the using app
    * session is not valid.
    *
    * */


    private void handleStartSession(ResponseElement response, OnCompletion<PrimitiveResult> completion) {

        ErrorElement error = null;

        if (response != null && response.isSuccess()) {
            List<BaseResult> responses = KalturaOvpParser.parse(response.getResponse()); // parses KalturaLoginResponse, KalturaSession

            if (responses.get(0).error != null) { //!- failed to login
                //?? clear session?
                error = ErrorElement.SessionError;

            } else {
                // first response is the "ks" itself, second response contains the session data (no ks)
                if (responses.get(1).error == null) { // get session data success
                    KalturaSessionInfo session = (KalturaSessionInfo) responses.get(1);
                    String ks = ((PrimitiveResult) responses.get(0)).getResult();
                    setSession(ks, session.getExpiry(), session.getUserId()); // save new session

                    if (completion != null) {
                        completion.onComplete(new PrimitiveResult(ks));
                    }
                } else {
                    error = ErrorElement.SessionError;
                }

            }
        } else { // failed to start session - ?? what to do in case this was a renew session action.
            error = response.getError() != null ? response.getError() : ErrorElement.SessionError;
        }

        if (error != null) {
            clearSession(); //clears current saved data - app can try renewSession with the current credentials. or endSession/startSession
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(error)); // in case we can't login - app should provide a solution.
            }
        }
    }

    /**
     * try to re-login with current credentials
     */
    private void renewSession(OnCompletion<PrimitiveResult> completion) {
        if (sessionParams != null) {
            if (sessionParams.username != null) {
                startSession(sessionParams.username, sessionParams.password, sessionParams.partnerId, completion);
            } else {
                startAnonymousSession(sessionParams.partnerId, completion);
            }
        } else {
            Log.e(TAG, "Session was ended or failed to start when this was called.\nCan't recover session if not started before");
            if (completion != null) {
                completion.onComplete(new PrimitiveResult().error(ErrorElement.SessionError.message("Session expired")));
            }
        }
    }

    /**
     * Ends current active session. if it's a {@link com.kaltura.playkit.backend.base.BaseSessionProvider.UserSessionType#User} session
     * logout, if {@link com.kaltura.playkit.backend.base.BaseSessionProvider.UserSessionType#Anonymous} will return, since
     * logout on anonymous session doesn't make the session invalid.
     * <p>
     * If logout was activated, session params are cleared.
     */
    public void endSession(final OnCompletion<BaseResult> completion) {

        if (hasActiveSession()) {

            if (getUserSessionType().equals(UserSessionType.Anonymous)) {
                if (completion != null) {
                    completion.onComplete(new BaseResult(null));
                }
                return;
            }

            APIOkRequestsExecutor.getSingleton().queue(
                    OvpSessionService.end(baseUrl, getSessionToken())
                            .addParams(OvpService.getOvpConfigParams())
                            .completion(new OnRequestCompletion() {
                                @Override
                                public void onComplete(ResponseElement response) {
                                    ErrorElement error = null;
                                    if (response != null && response.isSuccess()) {//!! end session with success returns null
                                        PKLog.i(TAG, "endSession: logout user session success. clearing session data.");
                                    } else {
                                        PKLog.e(TAG, "endSession: session logout failed. clearing session data. " + (response.getError() != null ? response.getError().getMessage() : ""));
                                        error = response.getError() != null ? response.getError() : ErrorElement.GeneralError.message("failed to end session");
                                    }
                                    endSession();
                                    sessionParams = null;

                                    if (completion != null) {
                                        completion.onComplete(new BaseResult(error));
                                    }

                                }
                            }).build());

        } else {
            sessionParams = null;
        }
    }

    @Override
    public int partnerId() {
        return sessionParams != null ? sessionParams.partnerId : 0;
    }

    @Override
    public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
        String ks = validateSession();
        if (ks != null) {
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(ks));
            }
        } else {
            renewSession(completion);
        }
    }

    protected String validateSession() {
        long currentDate = currentTimeMillis() / 1000;
        long timeLeft = expiryDate - currentDate;
        String sessionToken = null;
        if (timeLeft > 0) {
            sessionToken = getSessionToken();

            if (timeLeft < ExpirationDelta) {
                renewSession(null); // token about to expired - we need to restart session
            }
        }

        return sessionToken;
    }


    class OvpSessionParams {
        private String username;
        private String password;
        public int partnerId;

        public String username() {
            return username;
        }

        public OvpSessionParams setUsername(String username) {
            this.username = username;
            return this;
        }

        public String password() {
            return password;
        }

        public OvpSessionParams setPassword(String password) {
            this.password = password;
            return this;
        }

        public int partnerId() {
            return partnerId;
        }

        public OvpSessionParams setPartnerId(int partnerId) {
            this.partnerId = partnerId;
            return this;
        }
    }

}
