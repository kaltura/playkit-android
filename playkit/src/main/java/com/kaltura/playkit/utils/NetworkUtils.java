package com.kaltura.playkit.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.kaltura.playkit.Utils.toBase64;

public class NetworkUtils {

    private static final PKLog log = PKLog.get("NetworkUtils");
    private static final OkHttpClient client = new OkHttpClient();
    private static final String DEFAULT_BASE_URL = "https://analytics.kaltura.com/api_v3/index.php";
    public static final String DEFAULT_KAVA_ENTRY_ID = "1_3bwzbc9o";
    public static final int DEFAULT_KAVA_PARTNER_ID = 2504201;
    public static final String KAVA_EVENT_IMPRESSION = "1";
    public static final String KAVA_EVENT_PLAY_REQUEST = "2";

    public static void requestOvpConfigByPartnerId(Context context, String baseUrl, int partnerId, String apiPrefix, NetworkUtilsCallback callback) {

        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "partner");
        params.put("action", "getPublicInfo");
        params.put("id", String.valueOf(partnerId));
        params.put("format", "1");

        String configByPartnerIdUrl = buildConfigByPartnerIdUrl(context, baseUrl + apiPrefix, params);
        //log.d("ovp configByPartnerIdUrl = " + configByPartnerIdUrl);
        executeGETRequest(context, "requestOvpConfigByPartnerId", configByPartnerIdUrl, callback);
    }

    public static void requestOttConfigByPartnerId(Context context, String baseUrl, int partnerId, String playerName, String udid, NetworkUtilsCallback callback) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("service", "Configurations");
        params.put("action", "serveByDevice");
        params.put("partnerId", String.valueOf(partnerId));
        params.put("applicationName", playerName + "." + partnerId);
        params.put("clientVersion", "4");
        params.put("platform", "Android");
        params.put("tag",  "tag");
        params.put("udid", udid);

        String configByPartnerIdUrl = buildConfigByPartnerIdUrl(context, baseUrl, params);
        //log.d("ott configByPartnerIdUrl = " + configByPartnerIdUrl);
        executeGETRequest(context, "requestOttConfigByPartnerId", configByPartnerIdUrl, callback);
    }

    private static String buildConfigByPartnerIdUrl(Context context, String baseUrl, Map<String, String> params) {

        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
        Set<String> keys = params.keySet();
        if (keys != null) {
            for (String key : keys) {
                builder.appendQueryParameter(key, params.get(key));
            }
        }
        return builder.build().toString();
    }

    public static void sendKavaAnalytics(Context context, int partnerId, String entryId, String eventType, String sessionId) {
        String kavaImpressionUrl = buildKavaImpressionUrl(context, partnerId, entryId, eventType, sessionId);
        log.d("KavaAnalytics URL = " + kavaImpressionUrl);
        executeGETRequest(context, "sendKavaImpression", kavaImpressionUrl, null);
    }

    private static String buildKavaImpressionUrl(Context context, int partnerId, String entryId, String eventType, String sessionId) {
        Uri.Builder builtUri = Uri.parse(DEFAULT_BASE_URL).buildUpon();
        builtUri.appendQueryParameter("service", "analytics")
                .appendQueryParameter("action", "trackEvent")
                .appendQueryParameter("eventType", eventType)
                .appendQueryParameter("partnerId", String.valueOf(partnerId))
                .appendQueryParameter("entryId", entryId)
                .appendQueryParameter("sessionId", !TextUtils.isEmpty(sessionId) ? sessionId : generateSessionId())
                .appendQueryParameter("eventIndex", "1")
                .appendQueryParameter("referrer", toBase64(context.getPackageName().getBytes()))
                .appendQueryParameter("deliveryType", "dash")
                .appendQueryParameter("playbackType", "vod")
                .appendQueryParameter("clientVer", PlayKitManager.CLIENT_TAG)
                .appendQueryParameter("position", "0")
                .appendQueryParameter("application", context.getPackageName());
        return builtUri.build().toString();
    }

    private static String generateSessionId() {
        String mediaSessionId = UUID.randomUUID().toString();
        String newSessionId   = UUID.randomUUID().toString();
        newSessionId += ":";
        newSessionId += mediaSessionId;
        return newSessionId;
    }

    private static void executeGETRequest(Context context, String apiName, String configByPartnerIdUrl, NetworkUtilsCallback callback) {
        try {
            Request request = new Request.Builder()
                    .url(configByPartnerIdUrl)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                final Handler mainHandler = new Handler(context.getMainLooper());
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> {
                        sendError(callback, apiName + " called failed url = " + configByPartnerIdUrl + ", error = " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {

                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> {
                            sendError(callback, apiName + " call failed url = " + configByPartnerIdUrl);
                        });
                    } else {
                        try {
                            ResponseBody responseBody = response.body();
                            if (responseBody != null) {
                                String body = responseBody.string();
                                if (!body.contains("KalturaAPIException")) {
                                    mainHandler.post(() -> {
                                        if (callback != null) {
                                            callback.finished(body, null);
                                        }
                                    });
                                    return;
                                }
                            }
                        } catch(IOException e){
                            mainHandler.post(() -> {
                                sendError(callback, apiName + " call failed url = " + configByPartnerIdUrl + ", error = " + e.getMessage());
                            });
                            return;
                        }

                        mainHandler.post(() -> {
                            sendError(callback, apiName + " called failed url = " + configByPartnerIdUrl);
                        });
                    }
                }
            });
        } catch (Exception e) {
            sendError(callback, apiName + " call failed url = " + configByPartnerIdUrl + ", error = " + e.getMessage());
        }
    }

    private static void sendError(NetworkUtilsCallback callback, String errorMessage) {
        log.e(errorMessage);
        if (callback != null) {
            callback.finished(null, errorMessage);
        }
    }
}
