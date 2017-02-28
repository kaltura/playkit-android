package com.kaltura.playkit.backend.phoenix;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.backend.BaseResult;
import com.kaltura.playkit.backend.PrimitiveResult;
import com.kaltura.playkit.backend.base.BaseSessionProvider;
import com.kaltura.playkit.backend.phoenix.data.KalturaLoginResponse;
import com.kaltura.playkit.backend.phoenix.data.KalturaLoginSession;
import com.kaltura.playkit.backend.phoenix.data.KalturaSession;
import com.kaltura.playkit.backend.phoenix.data.PhoenixParser;
import com.kaltura.playkit.backend.phoenix.services.OttUserService;
import com.kaltura.playkit.backend.phoenix.services.PhoenixService;
import com.kaltura.playkit.backend.phoenix.services.PhoenixSessionService;
import com.kaltura.playkit.connect.APIOkRequestsExecutor;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.MultiRequestBuilder;
import com.kaltura.playkit.connect.OnRequestCompletion;
import com.kaltura.playkit.connect.ResponseElement;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tehilarozin on 27/11/2016.
 */

public class OttSessionProvider extends BaseSessionProvider {

    public static final long TimeDelta = 2 * 60 * 60;//hour in seconds
    private static final String TAG = "BaseResult";
    public static final int DeltaPercent = 12;
    public static final long IMMEDIATE_REFRESH = 1;
    public static final String DummyUserId = "1";

    private OttSessionParams sessionParams;
    private String refreshToken;
    private long refreshDelta = 9 * 60 * 60 * 24;//TimeDelta;
    private int partnerId = 0;

    private ScheduledFuture<?> scheduledRefreshTask;
    private ScheduledThreadPoolExecutor refreshScheduleExecutor;
    private AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    private OnCompletion<PrimitiveResult> sessionRecoveryCallback;


    //region refresh callable
    private Callable<Boolean> refreshCallable = new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
            if(refreshToken == null){
                Log.d(TAG, "refreshToken is not available, can't activate refresh");
                return false;
            }

            if(refreshInProgress.get()){
                Log.d(TAG, "refresh already in progress");
                return false;
            }

            PKLog.d(TAG, "start running refresh token");


            if(scheduledRefreshTask != null && scheduledRefreshTask.isCancelled()){
                scheduledRefreshTask = null;
                PKLog.d(TAG, "refresh operation got canceled");
                return false;
            }

            refreshInProgress.set(true);

            refreshSessionCall();
            return true;
        }
    };
    //endregion


    public OttSessionProvider(String baseUrl, int partnerId) {
        super(baseUrl, "");
        this.partnerId = partnerId;

        initRefreshExecutor();
    }

    private void initRefreshExecutor() {
        refreshScheduleExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            refreshScheduleExecutor.setRemoveOnCancelPolicy(true);
        }
        refreshScheduleExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        refreshScheduleExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        refreshScheduleExecutor.setKeepAliveTime(10, TimeUnit.SECONDS);
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
     * try to preserve given session token. refresh of token is done immediate by that check if session
     * is still valid and get a new refreshToken and expiry time.
     * @param ks
     * @param refreshToken
     * @param userId
     * @param udid
     */
    public void maintainSession(@NonNull String ks, String refreshToken, String userId, String udid, OnCompletion<PrimitiveResult> sessionRecoveryCallback){
        this.sessionParams = new OttSessionParams().setUdid(udid);
        this.refreshToken = refreshToken;
        this.sessionRecoveryCallback = sessionRecoveryCallback;
        setSession(ks, Unset, userId);
    }


    /**
     * try to re-login with current credentials, if available
     */
    private void renewSession(OnCompletion<PrimitiveResult> completion) {
        if (sessionParams != null) {
            if (sessionParams.username != null) {
                startSession(sessionParams.username, sessionParams.password, sessionParams.udid, completion);

            } else if(getUserSessionType().equals(UserSessionType.Anonymous) /*sessionParams.isAnonymous*/) {
                startAnonymousSession(sessionParams.udid, completion);
            } // ?? in case session with no user credential expires, should we login as anonymous or return empty ks?

        } else {
            Log.e(TAG, "sessionParams are not available can't create/renew session.");
            //Log.e(TAG, "Session was ended or failed to start when this was called.\nCan't recover session if not started before");
            //completion.onComplete(new PrimitiveResult().error(ErrorElement.SessionError.message("Session expired")));
            completion.onComplete(new PrimitiveResult(""));
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
        if (timeLeft > 0) { // validate refreshToken expiration time
            token = getSessionToken();

            if (timeLeft < refreshDelta) {
                // call refreshToken
                scheduleRefreshSessionTask(IMMEDIATE_REFRESH);
            }
        }
        return token;
    }


    private void refreshSessionCall() {
        // multi request needed to fetch the new expiration date.
        MultiRequestBuilder multiRequest = PhoenixService.getMultirequest(apiBaseUrl, null);
        multiRequest.add(OttUserService.refreshSession(apiBaseUrl, getSessionToken(), refreshToken, sessionParams.udid),
                PhoenixSessionService.get(apiBaseUrl, "{1:result:ks}"))
                .completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {

                        refreshInProgress.set(false);

                        PrimitiveResult refreshResult = null;

                        if (response != null && response.isSuccess()) {
                            List<BaseResult> responses = PhoenixParser.parse(response.getResponse());
                            if (responses.get(0).error != null) {
                                PKLog.e(TAG, "failed to refresh session. token may be invalid and cause access issues. ");
                                // session may have still time before it expires so actually if fails, do nothing.

                            } else {// refresh success
                                KalturaLoginSession loginSession = (KalturaLoginSession) responses.get(0);
                                refreshToken = loginSession.getRefreshToken();

                                if (responses.get(1).error == null) {
                                    KalturaSession session = (KalturaSession) responses.get(1);
                                    setSession(session.getKs(), session.getExpiry(), session.getUserId()); // save new session
                                }

                                refreshResult = new PrimitiveResult(getSessionToken());
                            }
                        }
                        if(sessionRecoveryCallback != null){
                            sessionRecoveryCallback.onComplete(refreshResult != null ?
                                    refreshResult :
                                    new PrimitiveResult(ErrorElement.SessionError.message("failed to recover session")));

                            sessionRecoveryCallback = null;
                        }
                    }
                });
        APIOkRequestsExecutor.getSingleton().queue(multiRequest.build());
    }


    /**
     * set scheduler to refresh tokens according to calculated delay. (expiration time and safety padding)
     * @param delay - the time to schedule the refresh to. if the delay is 0, force refresh.
     */
    private synchronized void scheduleRefreshSessionTask(long delay) {
        PKLog.i(TAG, "scheduling refresh in about " + delay + " sec from now");
        scheduledRefreshTask = refreshScheduleExecutor.schedule(refreshCallable, delay, TimeUnit.SECONDS);
    }

    private void clearScheduled() {
        Log.d(TAG, "clearScheduled: Thread - "+Thread.currentThread().getId());

        if(scheduledRefreshTask != null && !scheduledRefreshTask.isDone()){
            scheduledRefreshTask.cancel(true);
        }
        scheduledRefreshTask = null;
    }

    @Override
    public void clearSession() {
        super.clearSession();
        refreshToken = null;
        refreshDelta = TimeDelta;

        clearScheduled();
    }

    private boolean isScheduled() {
        return scheduledRefreshTask != null && !scheduledRefreshTask.isDone() && !scheduledRefreshTask.isCancelled();
    }

    @Override
    protected void setSession(String sessionToken, long expiry, String userId) {
        super.setSession(sessionToken, expiry, userId);
        long delay = 1;
        if(expiry != Unset) {
            updateRefreshDelta(expiry);
            delay = expiry - refreshDelta;
        }
        scheduleRefreshSessionTask(delay);
    }

    private void updateRefreshDelta(long expiry) {
        long currentDate = System.currentTimeMillis() / 1000;
        refreshDelta = (expiry - currentDate) * DeltaPercent / 100; // 20% of total validation time
    }

    /**
     * encrypt session info for storage purposes
     * @return
     */
    public String encryptSession(){
        StringBuilder data = new StringBuilder(getSessionToken()).append(" ~~ ")
                .append(refreshToken).append(" ~~ ").append(sessionParams.udid());

        return Base64.encodeToString(data.toString().getBytes(), Base64.NO_WRAP);
    }

    /**
     * maintain session recovered from encrypt session info.
     * @param encryptSession
     */
    public boolean recoverSession(String encryptSession, OnCompletion<PrimitiveResult> sessionRecoveryCallback){
        String decrypt = new String(Base64.decode(encryptSession, Base64.NO_WRAP));
        String[] data = decrypt.split(" ~~ ");
        if(data.length < 2){
            return false;
        }
        maintainSession(data[0], data[1], DummyUserId, data.length >= 3 && !data[2].equals("null") ? data[2] : null, sessionRecoveryCallback);
        return true;
    }



    private class OttSessionParams {
        private String udid = "";
        private String username;
        private String password;
        //private boolean isAnonymous = false;

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

        /*public OttSessionParams setAnonymous() {
            this.isAnonymous = true;
            return this;
        }*/
    }

}
