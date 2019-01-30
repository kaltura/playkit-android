package com.kaltura.playkit.player;

import android.util.Base64;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PKHttpClientManager {

    private static final PKLog log = PKLog.get("PKHttpClientManager");

    static final String HTTP_PROVIDER_OK = "okhttp";
    static final String HTTP_PROVIDER_SYSTEM = "system";

    private static final String warmUpUserAgent = PlayKitManager.CLIENT_TAG + " connectionWarmUp";

    private static final int MAX_IDLE_CONNECTIONS = 10;
    private static final int KEEP_ALIVE_DURATION = 5;
    private static final int WARMUP_TIMES = 2;

    private static String httpProviderId;


    private static final OkHttpClient okClient = new OkHttpClient.Builder()
            .followRedirects(false)     // Only warm up explicitly specified URLs
            .connectionPool(new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION, TimeUnit.MINUTES))
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))    // Avoid http/2 due to https://github.com/google/ExoPlayer/issues/4078
            .build();

    // Called by the player
    static OkHttpClient.Builder newClientBuilder() {
        return okClient.newBuilder().followRedirects(true);
    }

    // Called by the player
    static boolean useOkHttp() {
        return HTTP_PROVIDER_OK.equalsIgnoreCase(httpProviderId);
    }

    /**
     * Set the http provider. Valid options are "system" (use the build-in {@linkplain java.net.HttpURLConnection})
     * and "okhttp" (use Square's <a href="https://square.github.io/okhttp/">OkHttp</a> library).
     * @param providerId "system" (default) or "okhttp".
     */
    public static void setHttpProvider(String providerId) {
        httpProviderId = providerId;
    }

    /**
     * Warm up the connection to a list of URLs. There should be only one URL per host, and the URLs
     * should resolve to valid pathnames. A good choice might be favicon.ico or crossdomain.xml.
     * @param urls List of URLs.
     */
    public static void warmUp(String... urls) {
        if (useOkHttp()) {
            warmUpOk(okClient, warmUpUserAgent, WARMUP_TIMES, urls);
        } else {
            warmUpSystem(warmUpUserAgent, urls);
        }
    }

    public static void warmUpSystem(String userAgent, String[] urls) {

        final ExecutorService service = Executors.newCachedThreadPool();

        List<Callable<Void>> calls = new ArrayList<>(urls.length);
        for (String url : urls) {
            calls.add(() -> {
                try {
                    final HttpURLConnection urlConnection = ((HttpURLConnection) new URL(url).openConnection());
                    urlConnection.addRequestProperty("user-agent", userAgent);
                    final InputStream inputStream = urlConnection.getInputStream();
                    byte[] buffer = new byte[1024];
                    int size = inputStream.read(buffer);
                    log.d("Read from " + url + ": " + Base64.encodeToString(buffer, 0, size, Base64.NO_WRAP));
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
        }
        try {
            service.invokeAll(calls, 6, TimeUnit.SECONDS);
            log.d("All urls finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void warmUpOk(OkHttpClient okClient, String userAgent, int warmUpTimes, String[] urls) {

        CountDownLatch latch = new CountDownLatch(urls.length * warmUpTimes);

        for (String url : urls) {
            for (int i = 0; i < warmUpTimes; i++) {
                warmUpOkUrl(okClient, userAgent, latch, url);
            }
        }

        try {
            latch.await(6, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void warmUpOkUrl(OkHttpClient okClient, String userAgent, CountDownLatch latch, String url) {
        final Call call = okClient.newCall(
                new Request.Builder()
                        .url(url)
                        .header("user-agent", userAgent)
                        .build()
        );

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) {
                final ResponseBody body = response.body();
                if (body != null) {
                    body.close();
                }
                latch.countDown();
            }
        });
    }
}
