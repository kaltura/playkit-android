package com.kaltura.playkit.profiler;

import android.os.Build;
import android.os.SystemClock;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.player.Profiler.Event;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.Call;
import okhttp3.CipherSuite;
import okhttp3.Connection;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.HttpUrl;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;

class OkHttpListener extends EventListener {

    private static final PKLog log = PKLog.get("OkHttpListener");

    private static AtomicLong nextId = new AtomicLong(1);

    private final PlayKitProfiler profiler;
    private final String url;
    private final String hostName;
    private final long startTime = SystemClock.elapsedRealtime();
    private final long id = nextId.getAndIncrement();

    OkHttpListener(PlayKitProfiler playKitProfiler, Call call) {
        profiler = playKitProfiler;
        final Request request = call.request();
        HttpUrl httpUrl = request.url();
        hostName = httpUrl.host();
        url = httpUrl.toString();
    }

//    private void log(String event, String... strings) {
//        profiler.log(event).add("id", id).addTime("callTime", relTime()), TextUtils.join("\t", strings));
//    }

    private Event logStart(String event) {
        return (Event) profiler.logStart("net_" + event)
                .add("id", id)
                .addTime("callTime", relTime());
    }

    private long relTime() {
        return SystemClock.elapsedRealtime() - startTime;
    }

    @Override
    public void callStart(Call call) {
        logStart("callStart")
                .add("url", url)
                .add("hostName", hostName)
                .add("method", call.request().method()).end();
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        logStart("dnsStart")
                .add("hostName", domainName).end();
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        if (!inetAddressList.isEmpty()) {
            final InetAddress address = inetAddressList.get(0);
            logStart("dnsEnd")
                    .add("hostName", domainName)
                    .add("hostIp", address.getHostAddress())
                    .add("canonicalHostName", address.getCanonicalHostName()).end();
        } else {
            logStart("dnsEnd")
                    .add("hostName", domainName).end();
        }
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        logStart("connectStart")
                .add("hostName", host(inetSocketAddress))
                .add("hostIp", inetSocketAddress.getAddress().getHostAddress())
                .add("port", inetSocketAddress.getPort())
                .add("proxy", String.valueOf(proxy)).end();
    }

    private static String host(InetSocketAddress inetSocketAddress) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return inetSocketAddress.getHostString();
        } else {
            return inetSocketAddress.getHostName();
        }
    }

    private static String host(Call call) {
        try {
            return call.request().url().host();
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public void secureConnectStart(Call call) {
        logStart("secureConnectStart").end();
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        final CipherSuite cipherSuite = handshake.cipherSuite();
        final TlsVersion tlsVersion = handshake.tlsVersion();

        logStart("secureConnectEnd")
                .add("cipherSuite", "" + cipherSuite)
                .add("tlsVersion", tlsVersion.javaName())
                .end();
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        logStart("connectEnd")
                .add("protocol", "" + protocol)
                .end();
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        logStart("connectFailed")
                .add("error", ioe.toString())
                .end();
    }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        logStart("connectionAcquired").end();
    }

    @Override
    public void connectionReleased(Call call, Connection connection) {
        logStart("connectionReleased").end();
    }

    @Override
    public void requestHeadersStart(Call call) {
        logStart("requestHeadersStart").end();
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        logStart("requestHeadersEnd").end();
    }

    @Override
    public void requestBodyStart(Call call) {
        logStart("requestBodyStart").end();
    }

    @Override
    public void requestBodyEnd(Call call, long byteCount) {
        logStart("requestBodyEnd").end();
    }

    @Override
    public void responseHeadersStart(Call call) {
        logStart("responseHeadersStart").end();
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        logStart("responseHeadersEnd").end();
    }

    @Override
    public void responseBodyStart(Call call) {
        logStart("responseBodyStart").end();
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        logStart("responseBodyEnd")
                .add("byteCount", byteCount)
                .end();
    }

    @Override
    public void callEnd(Call call) {
        logStart("callEnd").end();
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        logStart("callFailed")
                .add("error", ioe)
                .end();
    }
}
