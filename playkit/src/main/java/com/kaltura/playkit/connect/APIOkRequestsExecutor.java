package com.kaltura.playkit.connect;


import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * Created by tehilarozin on 21/07/2016.
 */
public class APIOkRequestsExecutor implements RequestQueue {

    public interface IdFactory{
        String factorId(String factor);
    }

    public static final String TAG = "APIOkRequestsExecutor";
    static final MediaType JSON_MediaType = MediaType.parse("application/json");

    public static RequestConfiguration DefaultConfig = new RequestConfiguration() {
        @Override
        public long getReadTimeout() {
            return 20000;
        }

        @Override
        public long getWriteTimeout() {
            return 20000;
        }

        @Override
        public long getConnectTimeout() {
            return 10000;
        }

        @Override
        public int getRetry() {
            return 2;
        }
    };

    //private String mEndPoint = "http://52.210.223.65/8080/v4_0/api_v3";

    private OkHttpClient mOkClient;
    private boolean addSig;
    private IdFactory idFactory = new RequestIdFactory(); // default

    private static APIOkRequestsExecutor self;

    public static APIOkRequestsExecutor getSingleton(){
        if(self == null){
            self = new APIOkRequestsExecutor();
        }
        return self;
    }

    //private IdInterceptor idInterceptor = new IdInterceptor();
   // private GzipInterceptor gzipInterceptor = new GzipInterceptor();

    public APIOkRequestsExecutor(){
        getOkClient();
    }

    public APIOkRequestsExecutor setRequestIdFactory(IdFactory factory){
        this.idFactory = factory;
        return this;
    }

    //change default request config
    private OkHttpClient getOkClient(RequestConfiguration configuration){
        if(configuration != null) {
            OkHttpClient.Builder builder = configClient(getOkClient().newBuilder(), configuration);
            mOkClient = builder.build();
        }
        return mOkClient;
    }

    //create okClient with default config
    private OkHttpClient getOkClient(){
        if(mOkClient == null){
            mOkClient = configClient(new OkHttpClient.Builder()
                    .connectionPool(new ConnectionPool()), DefaultConfig).build(); // default connection pool - holds 5 connections up to 5 minutes idle time
        }
        return mOkClient;
    }

    private OkHttpClient.Builder configClient(OkHttpClient.Builder builder, RequestConfiguration config){
        builder.followRedirects(true).connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getWriteTimeout(), TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(config.getRetry() > 0);

        /*if (!builder.interceptors().contains(idInterceptor)) {
            builder.addInterceptor(idInterceptor);
        }*/
        return builder;
    }


    /*@Override
    public String queue(final ParamsRequestElement action) {
        final Request request = buildRestRequest(action, new BodyBuilder() {
            @Override
            public RequestBody build(RequestElement requestElement) {

                ParamsRequestElement paramsRequestElement = (ParamsRequestElement) requestElement;
                if(paramsRequestElement.isMultipart()){
                    return buildMultipartBody(paramsRequestElement.getParams());
                } else {
                    return buildFormBody(paramsRequestElement.getParams());
                }

            }
        });
        return queue(request, action);
    }*/

    private RequestBody buildMultipartBody(HashMap<String, String> params) {
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
        for(String key : params.keySet()){
            bodyBuilder.addFormDataPart(key, params.get(key));
        }
        return bodyBuilder.build();
    }

    private RequestBody buildFormBody(HashMap<String, String> params) {
        FormBody.Builder bodyBuilder = new FormBody.Builder();
        for(String key : params.keySet()){
            bodyBuilder.add(key, params.get(key));
        }
        return bodyBuilder.build();
    }

    @Override
    public String queue(final RequestElement requestElement) {
        final Request request = buildRestRequest(requestElement, BodyBuilder.Default);
        return queue(request, requestElement);
    }

    private String queue(final Request request, final RequestElement action) {

        try {
            Call call = getOkClient(action.config()).newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) { //!! in case of request error on client side

                    if(call.isCanceled()){
                        //logger.warn("onFailure: call "+call.request().tag()+" was canceled. not passing results");
                        return;
                    }
                    // handle failures: create response from exception
                    action.onComplete(new ExecutedRequest().error(e).success(false));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if(call.isCanceled()){
//                        logger.warn("call "+call.request().tag()+" was canceled. not passing results");
                        return;
                    }

                    // pass parsed response to action completion block
                    ResponseElement responseElement = onGotResponse(response, action);
                    action.onComplete(responseElement);
                }
            });
            return  (String) call.request().tag();

        } catch (Exception e) {
            e.printStackTrace();
            ExecutedRequest responseElement = new ExecutedRequest().response(getErrorResponse(e)).success(false);
            action.onComplete(responseElement);

        }
        return null; // no call id to return.
    }

    private String getErrorResponse(Exception e) {
        return e.getClass().getName()+": "+ e.getMessage();
    }

    @Override
    public ResponseElement execute(RequestElement request) {
        try {
            Response response = getOkClient(request.config()).newCall(buildRestRequest(request, BodyBuilder.Default)).execute();
            return onGotResponse(response, request);

        } catch (IOException e){
            // failure on request execution - create error response
             return new ExecutedRequest().response(getErrorResponse(e)).success(false);
        }
    }

    //TODO: cancel check on executor + null check on provider

    //@Override
    public boolean hasRequest(String reqId) {
        Dispatcher dispatcher = getOkClient().dispatcher();

        Call call = findCall(reqId, dispatcher.queuedCalls());
        if(call != null){
            return true;
        }
        call = findCall(reqId, dispatcher.runningCalls());
        return call != null;
    }

    @Override
    public void cancelRequest(String reqId) {
        Dispatcher dispatcher = getOkClient().dispatcher();

        Call call = findCall(reqId, dispatcher.queuedCalls());
        if(call != null){
            call.cancel();
            Log.d(TAG, "call canceled:"+call.request().tag());
        }
        call = findCall(reqId, dispatcher.runningCalls());
        if(call != null){
            call.cancel();
            Log.d(TAG, "call canceled:"+call.request().tag());
        }
    }

    private Call findCall(String reqId, List<Call> calls) {
        for(Call call : calls) {
            if(call.request().tag().equals(reqId)) {
                return call;
            }
        }
        return null;
    }

    @Override
    public void clearRequests() {
        if(mOkClient != null) {
            mOkClient.dispatcher().cancelAll();
        }
    }

    @Override
    public boolean isEmpty() {
        return mOkClient == null || mOkClient.dispatcher().queuedCallsCount() == 0;
    }

    private ResponseElement onGotResponse(Response response, RequestElement action) {
        String requestId = getRequestId(response);

        if (!response.isSuccessful()) { // in case response has failure status
            return new ExecutedRequest().requestId(requestId).error(ErrorElement.fromCode(response.code(), response.message())).success(false);

        } else {

            String responseString = null;
            try {
                responseString = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
                //logger.error("failed to retrieve the response body!");
            }

            //logger.debug("response body:\n" + responseString);

            return new ExecutedRequest().requestId(requestId).response(responseString).code(response.code()).success(responseString != null);
        }
    }

    private String getRequestId(Response response) {
        try {
            return response.request().tag().toString();
        }catch (NullPointerException e){
            return "";
        }
    }

    private String getContentType(String header) {
        return header.contains("application/xml") ? "xml" : "json";
    }

    private interface BodyBuilder{
        RequestBody build(RequestElement requestElement);

        BodyBuilder Default = new BodyBuilder() {
            @Override
            public RequestBody build(RequestElement requestElement) {
                return requestElement.getBody() != null ? RequestBody.create(JSON_MediaType, requestElement.getBody().getBytes()) : null;
            }
        };
    }

    private Request buildRestRequest(RequestElement request, BodyBuilder bodyBuilder) {

        String url = request.getUrl();
        System.out.println("request url: "+url +"\nrequest body:\n"+request.getBody()+"\n");

        RequestBody body = bodyBuilder.build(request);// RequestBody.create(JSON_MediaType, action.getBody().getBytes());

        return new Request.Builder()
                .headers(Headers.of(request.getHeaders()))
                .method(request.getMethod(), body)
                .url(url)
                .tag(idFactory.factorId(request.getTag()))
                .build();
    }

    public static String getRequestBody(Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }

}
