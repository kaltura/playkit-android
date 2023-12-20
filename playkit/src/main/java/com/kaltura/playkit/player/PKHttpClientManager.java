package com.kaltura.playkit.player;

import com.kaltura.androidx.media3.datasource.DefaultHttpDataSource;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayKitManager;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
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
    private static final int WARMUP_TIMES = 1;

    private static String httpProviderId;

    private static ConnectionPool okConnectionPool = new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION, TimeUnit.MINUTES);

    // Called by the player
    public static OkHttpClient.Builder newClientBuilder() {
            return new OkHttpClient.Builder()
                    .connectionPool(okConnectionPool)
                    .followRedirects(true)
                    .connectTimeout(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .readTimeout(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1));
    }

    // Called by the player
    static boolean useSystem() {
        return HTTP_PROVIDER_SYSTEM.equalsIgnoreCase(httpProviderId);
    }

    /**
     * Set the http provider. Valid options are "system" (use the build-in java.net.HttpURLConnection)
     * and "okhttp" (use Square's <a href="https://square.github.io/okhttp/">OkHttp</a> library).
     * @param providerId "system" (default) or "okhttp".
     */
    public static void setHttpProvider(String providerId) {
        httpProviderId = providerId;
    }

    public static String getHttpProvider() {
        return httpProviderId;
    }

    /**
     * Warm up the connection to a list of URLs. There should be only one URL per host, and the URLs
     * should resolve to valid pathnames. A good choice might be favicon.ico or crossdomain.xml.
     * @param urls List of URLs.
     */
    public static void warmUp(String... urls) {
        final ExecutorService service = Executors.newCachedThreadPool();

        List<Callable<Void>> calls = new ArrayList<>(urls.length);
        OkHttpClient okHttpClient = newClientBuilder().followRedirects(false).build();
        for (String url : urls) {
            final Callable<Void> callable = useSystem() ? getSystemCallable(url) : getOkCallable(okHttpClient, url);

            for (int i = 0; i < WARMUP_TIMES; i++) {
                calls.add(callable);
            }
        }

        try {
            service.invokeAll(calls, 6, TimeUnit.SECONDS);
            log.d("All urls finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Callable<Void> getSystemCallable(String url) {
        return () -> {
            try {
                final HttpURLConnection urlConnection = ((HttpURLConnection) new URL(url).openConnection());
                urlConnection.setConnectTimeout(3000);
                urlConnection.setReadTimeout(3000);
                urlConnection.addRequestProperty("user-agent", PKHttpClientManager.warmUpUserAgent);
                final InputStream inputStream = urlConnection.getInputStream();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        };
    }

    private static Callable<Void> getOkCallable(OkHttpClient  okHttpClient, String url) {
        return () -> {
            final Call call = okHttpClient.newCall(
                    new Request.Builder()
                            .url(url)
                            .header("user-agent", PKHttpClientManager.warmUpUserAgent)
                            .build()
            );
            final Response response = call.execute();
            final ResponseBody body = response.body();
            if (body != null) {
                body.close();
            }
            return null;
        };
    }
}
