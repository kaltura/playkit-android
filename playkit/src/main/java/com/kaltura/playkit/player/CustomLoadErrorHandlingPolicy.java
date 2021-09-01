package com.kaltura.playkit.player;

import android.net.Uri;

import com.kaltura.android.exoplayer2.upstream.DataSpec;
import com.kaltura.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKSubtitleFormat;
import com.kaltura.playkit.utils.Consts;

import java.io.IOException;

public class CustomLoadErrorHandlingPolicy extends DefaultLoadErrorHandlingPolicy {

    private static final PKLog log = PKLog.get("LoadErrorHandlingPolicy");

    private CustomLoadErrorHandlingPolicy.OnTextTrackLoadErrorListener textTrackLoadErrorListener;
    public static final int LOADABLE_RETRY_COUNT_UNSET = 0;
    private static final int DATA_TYPE_MEDIA_PROGRESSIVE_LIVE = 7; // For progressive live medias, we are keeping the retry count 6(default in ExoPlayer).
    private final int maximumLoadableRetryCount;

    public interface OnTextTrackLoadErrorListener {
        void onTextTrackLoadError(PKError currentError);
    }

    public CustomLoadErrorHandlingPolicy(int maximumLoadableRetryCount) {
        this.maximumLoadableRetryCount = maximumLoadableRetryCount;
    }

    public void setOnTextTrackErrorListener(CustomLoadErrorHandlingPolicy.OnTextTrackLoadErrorListener onTextTrackErrorListener) {
        this.textTrackLoadErrorListener = onTextTrackErrorListener;
    }

    @Override
    public long getRetryDelayMsFor(LoadErrorInfo loadErrorInfo) {
        IOException exception = loadErrorInfo.exception;
        Uri pathSegment = getPathSegmentUri(exception);
        if (pathSegment == null || !(exception instanceof HttpDataSource.HttpDataSourceException)) {
            return getRetryDelay(loadErrorInfo);
        }

        String lastPathSegment = pathSegment.getLastPathSegment();
        if (lastPathSegment != null && (lastPathSegment.endsWith(PKSubtitleFormat.vtt.pathExt) || lastPathSegment.endsWith(PKSubtitleFormat.srt.pathExt))) {
            PKError currentError = new PKError(PKPlayerErrorType.SOURCE_ERROR, PKError.Severity.Recoverable, "TextTrack is invalid url=" + pathSegment, exception);
            if (textTrackLoadErrorListener != null) {
                log.e("Error-Event sent, type = " + PKPlayerErrorType.SOURCE_ERROR);
                textTrackLoadErrorListener.onTextTrackLoadError(currentError);
            }
            return Consts.TIME_UNSET;
        } else {
            return getRetryDelay(loadErrorInfo);
        }
    }

    private long getRetryDelay(LoadErrorInfo loadErrorInfo) {
        if (maximumLoadableRetryCount > LOADABLE_RETRY_COUNT_UNSET && loadErrorInfo.errorCount >= maximumLoadableRetryCount) {
            return Consts.TIME_UNSET;
        } else {
            return super.getRetryDelayMsFor(loadErrorInfo);
        }
    }

    private Uri getPathSegmentUri(IOException ioException) {
        DataSpec dataSpec = null;
        if (ioException instanceof HttpDataSource.InvalidResponseCodeException) {
            dataSpec = ((HttpDataSource.InvalidResponseCodeException) ioException).dataSpec;
        } else if (ioException instanceof HttpDataSource.HttpDataSourceException) {
            dataSpec = ((HttpDataSource.HttpDataSourceException) ioException).dataSpec;
        }

        if (dataSpec != null) {
            return dataSpec.uri;
        }
        return null;
    }
}
