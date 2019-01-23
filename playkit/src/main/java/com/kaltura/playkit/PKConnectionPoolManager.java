package com.kaltura.playkit;

import android.os.AsyncTask;
import android.os.Looper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.ConnectionPool;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PKConnectionPoolManager {
    private static final PKLog log = PKLog.get("PKConnectionPoolManager");

    private static final ConnectionPool okPool = new ConnectionPool(10, 10, TimeUnit.MINUTES);

    private static final OkHttpClient okClient = new OkHttpClient.Builder()
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .connectionPool(okPool)
            .eventListener(new EventListener() {
                @Override
                public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
                    log.d("PKCon connectStart " + inetSocketAddress.getHostName() + " " + inetSocketAddress.getAddress().getHostAddress());
                }

                @Override
                public void connectionAcquired(Call call, Connection connection) {
                    log.d("PKCon connectionAcquired " + connection.socket().toString());
                }

                @Override
                public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
                    log.d("dnsEnd: " + domainName);
                }

                @Override
                public void secureConnectEnd(Call call, Handshake handshake) {
                    log.d("secureConnectEnd: " + call.request().url());
                }
            })
            .build();

    public static void warmUp(String... urls) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            AsyncTask.execute(() -> warmUp(urls));
            return;
        }

        for (String url : urls) {

            final Call call = okClient.newCall(new Request.Builder().url(url).build());
            try {
                final Response response = call.execute();
                final ResponseBody body = response.body();
                if (body != null) {
                    if (body.contentLength() < 10_000_000) {
                        body.bytes();
                    }
                    body.close();
                    showPool("After close");
                }
            } catch (IOException e) {
                showPool("After exception");
            }
        }
    }

    private static void showPool(String s) {

        log.d(s + ": Pool count=" + okPool.idleConnectionCount() + "; idleCount=" + okPool.idleConnectionCount());
    }

    public static ConnectionPool getOkPool() {
        return okPool;
    }
}
