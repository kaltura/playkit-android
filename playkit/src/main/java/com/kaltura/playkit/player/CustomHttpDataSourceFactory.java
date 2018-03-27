package com.kaltura.playkit.player;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.kaltura.playkit.PKRequestParams;
import java.util.Map;

/** A {@link HttpDataSource.Factory} that produces {@link DefaultHttpDataSource} instances. */
public final class CustomHttpDataSourceFactory extends HttpDataSource.BaseFactory {

    private final String userAgent;
    private final PKRequestParams pkRequestParams;
    private final TransferListener<? super DataSource> listener;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final boolean allowCrossProtocolRedirects;

    /**
     * @param userAgent The User-Agent string that should be used.
     * @param listener An optional listener.
     * @param connectTimeoutMillis The connection timeout that should be used when requesting remote
     *     data, in milliseconds. A timeout of zero is interpreted as an infinite timeout.
     * @param readTimeoutMillis The read timeout that should be used when requesting remote data, in
     *     milliseconds. A timeout of zero is interpreted as an infinite timeout.
     * @param allowCrossProtocolRedirects Whether cross-protocol redirects (i.e. redirects from HTTP
     *     to HTTPS and vice versa) are enabled.
     */
    public CustomHttpDataSourceFactory(String userAgent, PKRequestParams pkRequestParams,
                                       TransferListener<? super DataSource> listener, int connectTimeoutMillis,
                                       int readTimeoutMillis, boolean allowCrossProtocolRedirects) {
        this.userAgent = userAgent;
        this.pkRequestParams = pkRequestParams;
        this.listener = listener;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
    }


    @Override
    protected DefaultHttpDataSource createDataSourceInternal(HttpDataSource.RequestProperties defaultRequestProperties) {
        if (pkRequestParams != null && pkRequestParams.headers != null) {
            for(Map.Entry<String,String> entry : pkRequestParams.headers.entrySet()) {
                defaultRequestProperties.set(entry.getKey(), entry.getValue());
            }
        }
        return new DefaultHttpDataSource(userAgent, null, listener, connectTimeoutMillis,
                readTimeoutMillis, allowCrossProtocolRedirects, defaultRequestProperties);
    }
}
