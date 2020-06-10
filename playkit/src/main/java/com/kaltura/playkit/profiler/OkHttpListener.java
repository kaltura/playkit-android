package com.kaltura.playkit.profiler;

import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.Utils.GsonObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.Call;
import okhttp3.CipherSuite;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.TlsVersion;

class OkHttpListener extends EventListener {

    private static final PKLog log = PKLog.get("OkHttpListener");

    private static AtomicLong nextId = new AtomicLong(1);

    private final long id = nextId.getAndIncrement();

    private final PlayKitProfiler profiler;

    private long callStartTime;

    // DNS, TCP, TLS - static because they are shared
    private Map<String, Long> dnsStartTimes;
    private Map<InetSocketAddress, Long> connectStartTimes;
    private long secureConnectStartTime;

    private long dnsTotalTime;
    private long connectTotalTime;
    private long secureConnectTotalTime;
    private boolean redirecting;
    private String lastHostName;
    private int redirectCount;


    OkHttpListener(PlayKitProfiler playKitProfiler) {
        profiler = playKitProfiler;
    }

    private GsonObject logStart(String event) {
        return profiler.logStart("net_" + event)
                .add("id", id);
    }

    private long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    @Override
    public void callStart(Call call) {
        callStartTime = elapsedRealtime();
        updateLastDomain(call.request().url().toString());
    }

    @Override
    public void callEnd(Call call) {
        callEndOrFail("callEnd", call);
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        callEndOrFail("callFailed", call);
    }

    private void callEndOrFail(String eventName, Call call) {
        if (callStartTime <= 0) {
            return;
        }

        if (redirecting) {
            // OkHttp bug https://github.com/square/okhttp/issues/4386
            // Ignore this "callEnd"
            return;
        }

        long callTotalTime = elapsedRealtime() - callStartTime;
        callStartTime = 0;

        final GsonObject event = logStart(eventName)
                .add("url", call.request().url().toString())
                .addTime("totalTime", callTotalTime);

        // If ANY connection-related time was logged, show all of them.
        if (dnsTotalTime > 0 || connectTotalTime > 0 || secureConnectTotalTime > 0) {
            event
                    .addTime("dnsTime", dnsTotalTime)
                    .addTime("connectTime", connectTotalTime)
                    .addTime("secureConnectTime", secureConnectTotalTime)
                    .addTime("totalConnectionOverhead", dnsTotalTime + connectTotalTime + secureConnectTotalTime);
        }

        if (redirectCount > 0) {
            event.add("redirects", redirectCount);
        }

        event.end();
    }

    @Override
    public void responseHeadersEnd(Call call, Response response) {
        redirecting = response.isRedirect();
        if (redirecting) {
            updateLastDomain(response.header("location"));
            redirectCount++;
        }
    }

    private void updateLastDomain(String location) {
        if (!TextUtils.isEmpty(location)) {
            final Uri uri = Uri.parse(location);
            if (uri.isAbsolute()) {
                final String host = uri.getHost();
                if (!TextUtils.isEmpty(host)) {
                    lastHostName = host;
                }
            }
        }
    }

    @Override
    public void dnsStart(Call call, String domainName) {
        if (dnsStartTimes == null) {
            dnsStartTimes = new HashMap<>();
        }
        dnsStartTimes.put(domainName, elapsedRealtime());
    }

    @Override
    public void dnsEnd(Call call, String domainName, List<InetAddress> addressList) {
        if (dnsStartTimes == null) {
            return; // shouldn't happen
        }

        final Long domainStartTime = dnsStartTimes.get(domainName);
        if (domainStartTime != null) {
            final long dnsTime = elapsedRealtime() - domainStartTime;
            dnsTotalTime += dnsTime;
            final InetAddress address = addressList.get(0);
            if (address != null) {
                logStart("domainResolution")
                        .add("dnsTime", dnsTime)
                        .add("domainName", domainName)
                        .add("hostIp", address.getHostAddress())
                        .add("canonicalHostName", address.getCanonicalHostName())
                        .end();
            }
            dnsStartTimes.remove(domainName);
        }
    }

    @Override
    public void connectStart(Call call, InetSocketAddress inetSocketAddress, Proxy proxy) {
        if (connectStartTimes == null) {
            connectStartTimes = new HashMap<>();
        }

        connectStartTimes.put(inetSocketAddress, elapsedRealtime());
    }

    @Override
    public void connectEnd(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol) {
        logConnectEndOrFail(inetSocketAddress);
    }

    private void logConnectEndOrFail(InetSocketAddress inetSocketAddress) {
        if (connectStartTimes == null) {
            return; // shouldn't happen
        }

        final Long connectStartTime = connectStartTimes.get(inetSocketAddress);
        if (connectStartTime != null) {
            connectTotalTime += (elapsedRealtime() - connectStartTime);
            connectStartTimes.remove(inetSocketAddress);
        }
    }

    @Override
    public void connectFailed(Call call, InetSocketAddress inetSocketAddress, Proxy proxy, Protocol protocol, IOException ioe) {
        logConnectEndOrFail(inetSocketAddress);
    }

    @Override
    public void secureConnectStart(Call call) {
        secureConnectStartTime = elapsedRealtime();
    }

    @Override
    public void secureConnectEnd(Call call, Handshake handshake) {
        if (secureConnectStartTime > 0) {
            secureConnectTotalTime += (elapsedRealtime() - secureConnectStartTime);
            secureConnectStartTime = 0;
        }

        final CipherSuite cipherSuite = handshake.cipherSuite();
        final TlsVersion tlsVersion = handshake.tlsVersion();

        logStart("securityInfo")
                .add("hostName", lastHostName)
                .add("cipherSuite", cipherSuite.javaName())
                .add("tlsVersion", tlsVersion.javaName())
                .end();
    }
}
