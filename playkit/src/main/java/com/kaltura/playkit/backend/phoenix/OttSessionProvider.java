package com.kaltura.playkit.backend.phoenix;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.connect.backend.BaseResult;
import com.connect.backend.PrimitiveResult;
import com.connect.backend.base.BaseSessionProvider;
import com.connect.backend.phoenix.PhoenixConfigs;
import com.connect.backend.phoenix.data.KalturaLoginResponse;
import com.connect.backend.phoenix.data.KalturaLoginSession;
import com.connect.backend.phoenix.data.KalturaSession;
import com.connect.backend.phoenix.data.PhoenixParser;
import com.connect.backend.phoenix.services.OttUserService;
import com.connect.backend.phoenix.services.PhoenixService;
import com.connect.backend.phoenix.services.PhoenixSessionService;
import com.connect.core.OnCompletion;
import com.connect.utils.APIOkRequestsExecutor;
import com.connect.utils.ErrorElement;
import com.connect.utils.MultiRequestBuilder;
import com.connect.utils.OnRequestCompletion;
import com.connect.utils.ResponseElement;
import com.kaltura.playkit.PKLog;

import java.util.List;

/**
 * Created by tehilarozin on 27/11/2016.
 */

public class OttSessionProvider extends BaseSessionProvider {

    public static final long TimeDelta = 2 * 60 * 60;//hour in seconds
    private static final String TAG = "BaseResult";
    public static final int DeltaPercent = 12;

    private OttSessionParams sessionParams;
    private String refreshToken;
    private long refreshDelta = 9 * 60 * 60 * 24;//TimeDelta;
    private int partnerId = 0;

    public OttSessionProvider(String baseUrl, int partnerId) {
        super(baseUrl, PhoenixConfigs.ApiPrefix);
        this.partnerId = partnerId;
    }

    /**
     * starts anonymous session
     *
     * @param udid
     */
    public void startAnonymousSession(@Nullable String udid, final OnCompletion<PrimitiveResult> completion) {
        this.sessionParams = new OttSessionParams().setUdid(udid);

        MultiRequestBuilder multiRequest = PhoenixService.getMultirequest(apiBaseUrl, null);
        multiRequest.add(OttUserService.anonymousLogin(apiBaseUrl, partnerId, udid),
                PhoenixSessionService.get(apiBaseUrl, "{1:result:ks}")).
                completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        handleStartSession(response, completion);
                    }
                });
        APIOkRequestsExecutor.getSingleton().queue(multiRequest.build());
    }

    /**
     * starts new user session
     * login and gets the session expiration for refresh purposes.
     *
     * @param username
     * @param password
     * @param udid
     * @param completion
     */
    public void startSession(@NonNull String username, @NonNull String password, @Nullable String udid, final OnCompletion<PrimitiveResult> completion) {
        // login user
        //get session data for expiration time
        this.sessionParams = new OttSessionParams().setPassword(password).setUsername(username).setUdid(udid);

        MultiRequestBuilder multiRequest = PhoenixService.getMultirequest(apiBaseUrl, null);
        multiRequest.add(OttUserService.userLogin(apiBaseUrl, partnerId, sessionParams.username, sessionParams.password),
                PhoenixSessionService.get(apiBaseUrl, "{1:result:loginSession:ks}")).
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
            List<BaseResult> responses = PhoenixParser.parse(response.getResponse()); // parses KalturaLoginResponse, KalturaSession

            if (responses.get(0).error != null) { //!- failed to login
                //?? clear session?
                error = ErrorElement.SessionError;

            } else {
                refreshToken = responses.get(0) instanceof KalturaLoginResponse ? ((KalturaLoginResponse) responses.get(0)).getLoginSession().getRefreshToken() :
                        ((KalturaLoginSession) responses.get(0)).getRefreshToken();
                // session data is taken from second response since its common for both user/anonymous login
                // and we need this response for the expiry.
                if (responses.get(1).error == null) { // get session data success
                    KalturaSession session = (KalturaSession) responses.get(1);
                    setSession(session.getKs(), session.getExpiry(), session.getUserId()); // save new session

                    if (completion != null) {
                        completion.onComplete(new PrimitiveResult(session.getKs()));
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
                startSession(sessionParams.username, sessionParams.password, sessionParams.udid, completion);
            } else {
                startAnonymousSession(sessionParams.udid, completion);
            }
        } else {
            Log.e(TAG, "Session was ended or failed to start when this was called.\nCan't recover session if not started before");
            completion.onComplete(new PrimitiveResult().error(ErrorElement.SessionError.message("Session expired")));
        }
    }

    /**
     * Ends current active session. if it's a {@link UserSessionType#User} session
     * logout, if {@link UserSessionType#Anonymous} will return, since
     * logout on anonymous session doesn't make the session invalid.
     * <p>
     * If logout was activated, session params are cleared.
     */
    public void endSession(final OnCompletion<BaseResult> completion) {

        if (hasActiveSession()) {

            if (getUserSessionType().equals(UserSessionType.Anonymous)) { //no need to logout anonymous session
                if (completion != null) {
                    completion.onComplete(new BaseResult());
                }
                return;
            }

            APIOkRequestsExecutor.getSingleton().queue(OttUserService.logout(apiBaseUrl, getSessionToken(), sessionParams.udid)
                    .completion(new OnRequestCompletion() {
                        @Override
                        public void onComplete(ResponseElement response) {
                            ErrorElement error = null;
                            if (response != null && response.isSuccess()) {
                                PKLog.d(TAG, "endSession: logout user session success. clearing session data.");
                            } else {
                                error = response.getError() != null ? response.getError() : ErrorElement.GeneralError.message("failed to end session");
                                PKLog.e(TAG, "endSession: session logout failed. clearing session data. " + error.getMessage());
                            }
                            OttSessionProvider.super.endSession();
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
        return this.partnerId;
    }

    @Override
    public void getSessionToken(final OnCompletion<PrimitiveResult> completion) {
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
        long currentDate = System.currentTimeMillis() / 1000;
        long timeLeft = expiryDate - currentDate;

        String token = null;
        if (timeLeft > 0) {
            token = getSessionToken();

            if (timeLeft < refreshDelta) {
                // call refreshToken
                refreshSessionToken();
            }
        } /*else { // token expired - we need to relogin
            //call re-login (renewSession)
            renewSession(null);
        }
*/
        return token;
    }

    private void refreshSessionToken() {
        if (refreshToken == null) {
            return;
        }
        // multi request needed to fetch the new expiration date.
        MultiRequestBuilder multiRequest = PhoenixService.getMultirequest(apiBaseUrl, null);
        multiRequest.add(OttUserService.refreshSession(apiBaseUrl, getSessionToken(), refreshToken, sessionParams.udid),
                PhoenixSessionService.get(apiBaseUrl, "{1:result:ks}"))
                .completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        if (response != null && response.isSuccess()) {
                            List<BaseResult> responses = PhoenixParser.parse(response.getResponse());
                            if (responses.get(0).error != null) { // refresh success
                                PKLog.e(TAG, "failed to refresh session. token may be invalid and cause ");
                                // session may have still time before it expires so actually if fails, do nothing.

                            } else {
                                KalturaLoginSession loginSession = (KalturaLoginSession) responses.get(0);
                                refreshToken = loginSession.getRefreshToken();

                                if (responses.get(1).error == null) {
                                    KalturaSession session = (KalturaSession) responses.get(1);
                                    setSession(session.getKs(), session.getExpiry(), session.getUserId()); // save new session
                                }

                            }
                        }
                    }
                });
        APIOkRequestsExecutor.getSingleton().queue(multiRequest.build());
    }

    @Override
    protected void clearSession() {
        super.clearSession();
        refreshToken = null;
        refreshDelta = TimeDelta;
    }

    @Override
    protected void setSession(String sessionToken, long expiry, String userId) {
        super.setSession(sessionToken, expiry, userId);
        updateRefreshDelta(expiry);
    }

    private void updateRefreshDelta(long expiry) {
        long currentDate = System.currentTimeMillis() / 1000;
        refreshDelta = (expiry - currentDate) * DeltaPercent / 100; // 20% of total validation time
    }


    class OttSessionParams {
        private String udid;
        private String username;
        private String password;

        public String udid() {
            return udid;
        }

        public OttSessionParams setUdid(String udid) {
            this.udid = udid;
            return this;
        }

        public String username() {
            return username;
        }

        public OttSessionParams setUsername(String username) {
            this.username = username;
            return this;
        }

        public String password() {
            return password;
        }

        public OttSessionParams setPassword(String password) {
            this.password = password;
            return this;
        }
    }

}
