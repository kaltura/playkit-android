package com.kaltura.playkit.profiler;

import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;

import com.kaltura.playkit.PKLog;

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

import static com.kaltura.playkit.profiler.ProfilerUtil.field;
import static com.kaltura.playkit.profiler.ProfilerUtil.timeField;

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
        if (request != null) {
            HttpUrl httpUrl = request.url();
            if (httpUrl != null) {
                hostName = httpUrl.host();
                url = httpUrl.toString();
            } else {
                hostName = null;
                url = null;
            }
        } else {
            url = null;
            hostName = null;
        }
    }

    private void log(String event, String... strings) {
        profiler.log("net_" + event, field("id", id), timeField("callTime", relTime()), TextUtils.join("\t", strings));
    }

    private long relTime() {
        return SystemClock.elapsedRealtime() - startTime;
    }

    @Override
    public void callStart(Call call) {
        log.d("callStart " + id);
        log("callStart",
                field("url", url), field("hostName", hostName),
                field("method", call.request().method()));
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        log("dnsStart", field("hostName", domainName));
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> inetAddressList) {
        if (!inetAddressList.isEmpty()) {
            final InetAddress address = inetAddressList.get(0);
            log("dnsEnd",
                    field("hostName", domainName),
                    field("hostIp", address.getHostAddress()),
                    field("canonicalHostName", address.getCanonicalHostName()));
        } else {
            log("dnsEnd", field("hostName", domainName));
        }
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        log("connectStart",
                field("hostName", host(inetSocketAddress)),
                field("hostIp", inetSocketAddress.getAddress().getHostAddress()),
                field("port", inetSocketAddress.getPort()),
                field("proxy", String.valueOf(proxy)));
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
        log("secureConnectStart");
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        final CipherSuite cipherSuite = handshake.cipherSuite();
        final TlsVersion tlsVersion = handshake.tlsVersion();

        log("secureConnectEnd",
                field("cipherSuite", "" + cipherSuite),
                field("tlsVersion", tlsVersion == null ? null : tlsVersion.javaName()));
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        log("connectEnd", field("protocol", "" + protocol));
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        log("connectFailed", field("error", ioe.toString()));
    }

    @Override
    public void connectionAcquired(Call call, Connection connection) {
        log("connectionAcquired");
    }

    @Override
    public void connectionReleased(Call call, Connection connection) {
        log("connectionReleased");
    }

    @Override
    public void requestHeadersStart(Call call) {
        log("requestHeadersStart");
    }

    @Override
    public void requestHeadersEnd(Call call, Request request) {
        log("requestHeadersEnd");
    }

    @Override
    public void requestBodyStart(Call call) {
        log("requestBodyStart");
    }

    @Override
    public void requestBodyEnd(Call call, long byteCount) {
        log("requestBodyEnd");
    }

    @Override
    public void responseHeadersStart(Call call) {
        log("responseHeadersStart");
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        log("responseHeadersEnd");
    }

    @Override
    public void responseBodyStart(Call call) {
        log("responseBodyStart");
    }

    @Override
    public void responseBodyEnd(Call call, long byteCount) {
        log("responseBodyEnd", field("byteCount", byteCount));
    }

    @Override
    public void callEnd(Call call) {
        log("callEnd");
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        log("callFailed", field("error", ioe.toString()));
    }
}
