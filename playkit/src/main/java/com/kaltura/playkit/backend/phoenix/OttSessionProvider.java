package com.kaltura.playkit.backend.phoenix;

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
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tehilarozin on 27/11/2016.
 */

public class OttSessionProvider extends BaseSessionProvider {

    private static final String TAG = "BaseResult";

    private static final long TimeDelta = 2 * 60 * 60;//2 hours in seconds - default refresh delta for 24h session
    private static final int DeltaPercent = 12;
    public static final long NEED_REFRESH = 0;
    private static final String DummyUserId = "1";
    private static final int EXECUTOR_KEEP_ALIVE = 10;
    private static final int MilliInSecond = 1000;

    public static final int KsParam = 0;
    public static final int RefreshTokenParam = 1;
    public static final int UdidParam = 2;

    private String sessionUdid;
    private String refreshToken;
    private long refreshDelta = TimeDelta;
    private int partnerId = 0;

    private Future<?> refreshTaskFurure;
    private ThreadPoolExecutor refreshExecutor;
    private AtomicBoolean refreshInProgress = new AtomicBoolean(false);

    private OnCompletion<PrimitiveResult> sessionRecoveryCallback;
    private OnCompletion<String> sessionRefreshListener;

    //region refresh callable
    private Callable<Boolean> refreshCallable = new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
            if(refreshToken == null){
                Log.d(TAG, "refreshToken is not available, can't activate refresh");
                onSessionRefreshTaskResults(new PrimitiveResult(ErrorElement.SessionError.addMessage(" FAILED TO RECOVER SESSION!!")));
                return false;
            }

            if(refreshInProgress.get()){
                Log.d(TAG, "refresh already in progress");
                return false;
            }

            PKLog.d(TAG, "start running refresh token");


            if(refreshTaskFurure != null && refreshTaskFurure.isCancelled()){
                refreshTaskFurure = null;
                PKLog.d(TAG, "refresh operation got canceled");
                return false;
            }

            refreshInProgress.set(true);

            refreshSessionCall();
            return true;
        }
    };

    private void onSessionRefreshTaskResults(PrimitiveResult result) {
        if(sessionRecoveryCallback != null){
            sessionRecoveryCallback.onComplete(result);
            sessionRecoveryCallback = null;
        }

        if(sessionRefreshListener != null ){
            sessionRefreshListener.onComplete(result.error == null? result.getResult() : null);
        }
    }
    //endregion


    public OttSessionProvider(String baseUrl, int partnerId) {
        super(baseUrl, "");
        this.partnerId = partnerId;

        initRefreshExecutor();
    }

    private void initRefreshExecutor() {
        refreshExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        refreshExecutor.setKeepAliveTime(EXECUTOR_KEEP_ALIVE, TimeUnit.SECONDS);
    }

    /**
     * sets a listener to listen to auto session refreshes
     * @param listener
     * @return
     */
    public OttSessionProvider setRefreshListener(OnCompletion<String> listener){
        this.sessionRefreshListener = listener;
        return this;
    }

    /**
     * starts anonymous session
     *
     * @param udid
     */
    public void startAnonymousSession(@Nullable String udid, final OnCompletion<PrimitiveResult> completion) {

        this.sessionUdid = udid;

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
     * login and get the session expiration for refresh purposes.
     *
     * @param username
     * @param password
     * @param udid
     * @param completion
     */
    public void startSession(@NonNull String username, @NonNull String password, @Nullable String udid, final OnCompletion<PrimitiveResult> completion) {
        //1. login user
        //2. get session data: expiration time

        this.sessionUdid = udid;

        MultiRequestBuilder multiRequest = PhoenixService.getMultirequest(apiBaseUrl, null);
        multiRequest.add(OttUserService.userLogin(apiBaseUrl, partnerId, username, password, udid),
                PhoenixSessionService.get(apiBaseUrl, "{1:result:loginSession:ks}")).
                completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        handleStartSession(response, completion);
                    }
                });
        APIOkRequestsExecutor.getSingleton().queue(multiRequest.build());
    }


    /**
     * starts social network related session
     * login and get the session expiration for refresh purposes.
     *
     * @param socialToken - social network valid token
     * @param udid
     * @param completion
     */
    public void startSocialSession(@NonNull OttUserService.KalturaSocialNetwork socialNetwork, @NonNull String socialToken, @Nullable String udid, final OnCompletion<PrimitiveResult> completion) {

        this.sessionUdid = udid;

        MultiRequestBuilder multiRequest = PhoenixService.getMultirequest(apiBaseUrl, null);
        multiRequest.add(OttUserService.socialLogin(apiBaseUrl, partnerId, socialToken, socialNetwork.value, udid),
                PhoenixSessionService.get(apiBaseUrl, "{1:result:loginSession:ks}")).
                completion(new OnRequestCompletion() {
                    @Override
                    public void onComplete(ResponseElement response) {
                        handleStartSession(response, completion);
                    }
                });
        APIOkRequestsExecutor.getSingleton().queue(multiRequest.build());
    }

    /**
     * switch to another user in household
     * @param userId
     * @param completion
     */
    public void switchUser(@NonNull final String userId, final OnCompletion<PrimitiveResult> completion) {

        getSessionToken(new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(response.error == null) { // in case the session checked for expiry and ready to use:
                    MultiRequestBuilder multiRequest = PhoenixService.getMultirequest(apiBaseUrl, null);
                    multiRequest.add(PhoenixSessionService.switchUser(apiBaseUrl, response.getResult(), userId),
                            PhoenixSessionService.get(apiBaseUrl, "{1:result:ks}")).
                            completion(new OnRequestCompletion() {
                                @Override
                                public void onComplete(ResponseElement response) {
                                    handleStartSession(response, completion);
                                }
                            });
                    APIOkRequestsExecutor.getSingleton().queue(multiRequest.build());

                } else { // in case ks retrieval failed:
                    completion.onComplete(response);
                }
            }
        });
    }

    /**
     * handles start session response.
     * if session was established update members and pass "ks" on the callback
     * if failed pass {@link ErrorElement#SessionError}
     * @param response
     * @param completion
     */
    private void handleStartSession(ResponseElement response, OnCompletion<PrimitiveResult> completion) {

        ErrorElement error = null;

        if (response != null && response.isSuccess()) {
            PKLog.d(TAG, "handleStartSession: response success, checking inner responses");
            List<BaseResult> responses = PhoenixParser.parse(response.getResponse()); // parses KalturaLoginResponse, KalturaSession

            if (responses.get(0).error != null) { //!- failed to login
                PKLog.d(TAG, "handleStartSession: first response failure: "+responses.get(0).error);

                //?? clear session?
                error = ErrorElement.SessionError;

            } else {
                refreshToken = responses.get(0) instanceof KalturaLoginResponse ? ((KalturaLoginResponse) responses.get(0)).getLoginSession().getRefreshToken() :
                        ((KalturaLoginSession) responses.get(0)).getRefreshToken();
                // session data is taken from second response since its common for both user/anonymous login
                // and we need this response for the expiry.
                if (responses.get(1).error == null) { // get session data success
                    PKLog.d(TAG, "handleStartSession: second response success");

                    KalturaSession session = (KalturaSession) responses.get(1);
                    setSession(session.getKs(), session.getExpiry(), session.getUserId()); // save new session

                    if (completion != null) {
                        completion.onComplete(new PrimitiveResult(session.getKs()));
                    }
                } else {
                    PKLog.d(TAG, "handleStartSession: second response failure: "+responses.get(1).error);

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
        //this.sessionParams = new OttSessionParams().setUdid(udid);
        this.sessionUdid = udid;

        this.refreshToken = refreshToken;
        this.sessionRecoveryCallback = sessionRecoveryCallback;
        setSession(ks, Unset, userId);
    }

    public boolean isAnonymousSession() {
        return getUserSessionType().equals(UserSessionType.Anonymous);
    }

    /**
     * Ends current active session. if it's a {@link com.kaltura.playkit.backend.base.BaseSessionProvider.UserSessionType#User} session
     * logout, if {@link com.kaltura.playkit.backend.base.BaseSessionProvider.UserSessionType#Anonymous} will return, since
     * logout on anonymous session doesn't make the session invalid.
     *
     * If logout was activated, session params are cleared.
     */
    public void endSession(final OnCompletion<BaseResult> completion) {

        if (hasActiveSession()) {

            if (isAnonymousSession()) { //no need to logout anonymous session
                if (completion != null) {
                    completion.onComplete(new BaseResult());
                }
                return;
            }

            // make sure the ks is valid for the request (refreshes it if needed)
            getSessionToken(new OnCompletion<PrimitiveResult>() {
                @Override
                public void onComplete(PrimitiveResult response) {
                    if(response.error == null) { // in case the session checked for expiry and ready to use:

                        APIOkRequestsExecutor.getSingleton().queue(OttUserService.logout(apiBaseUrl, response.getResult(), sessionUdid)
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
                                        sessionUdid = null;
                                        if (completion != null) {
                                            completion.onComplete(new BaseResult(error));
                                        }
                                    }
                                }).build());

                    } else { // in case ks retrieval failed:
                        completion.onComplete(response);
                    }
                }
            });

        } else {
            Log.w(TAG, "endSession: but no active session available");
            sessionUdid = null;
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
            // to get the refreshed ks on the completion callback
            sessionRecoveryCallback = completion;
            submitRefreshSessionTask();
        }
    }

    protected String validateSession() {
        long currentDate = System.currentTimeMillis() / 1000;
        long timeLeft = expiryDate == Unset ? NEED_REFRESH : expiryDate - currentDate;

        String token = null;
        // returns current session token if current time didn't exceed the ks expiry time minus delta
        // otherwise return null to submit refresh
        if (refreshDelta > 0 && timeLeft > refreshDelta) {
            token = getSessionToken();
        }

        return token;
    }


    private void refreshSessionCall() {
        // multi request needed to fetch the new expiration date.
        MultiRequestBuilder multiRequest = PhoenixService.getMultirequest(apiBaseUrl, null);
        multiRequest.add(OttUserService.refreshSession(apiBaseUrl, getSessionToken(), refreshToken, sessionUdid),
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

                        final PrimitiveResult refreshedKsResult = refreshResult != null ?
                                    refreshResult :
                                    new PrimitiveResult(ErrorElement.SessionError.addMessage(" FAILED TO RECOVER SESSION!!"));

                        onSessionRefreshTaskResults(refreshedKsResult);
                    }
                });
        APIOkRequestsExecutor.getSingleton().queue(multiRequest.build());
    }


    /**
     * submit task to refresh current session
     */
    private synchronized void submitRefreshSessionTask() {
        PKLog.i(TAG, "submit refresh session task");
        refreshTaskFurure = refreshExecutor.submit(refreshCallable);
    }

    private void cancelCurrentRefreshTask() {
        Log.d(TAG, "cancelCurrentRefreshTask: Thread - "+Thread.currentThread().getId());

        if(refreshTaskFurure != null && !refreshTaskFurure.isDone()){
            refreshTaskFurure.cancel(true);
        }
        refreshTaskFurure = null;
    }

    @Override
    public void clearSession() {
        super.clearSession();
        refreshToken = null;
        refreshDelta = TimeDelta;

        cancelCurrentRefreshTask();
    }

    @Override
    protected void setSession(String sessionToken, long expiry, String userId) {
        super.setSession(sessionToken, expiry, userId);
        if(expiry != Unset) {
            updateRefreshDelta(expiry);

        } else {
            submitRefreshSessionTask();
        }
    }

    private void updateRefreshDelta(long expiry) {
        long currentDate = System.currentTimeMillis() / MilliInSecond;
        refreshDelta = (expiry - currentDate) * DeltaPercent / 100; // 20% of total validation time
    }

    /**
     * encrypt session info for storage purposes
     * @return
     */
    public String encryptSession(){

        String sessionToken = getSessionToken();
        if(sessionToken == null){
            return null;
        }
        
        StringBuilder data = new StringBuilder(sessionToken).append(" ~~ ")
                .append(refreshToken).append(" ~~ ").append(sessionUdid);

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
        maintainSession(data[KsParam], data[RefreshTokenParam], DummyUserId, data.length >= 3 && !data[UdidParam].equals("null") ? data[UdidParam] : null, sessionRecoveryCallback);
        return true;
    }
}
