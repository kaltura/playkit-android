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

public class ExternalTextTrackLoadErrorPolicy extends DefaultLoadErrorHandlingPolicy {

    private static final PKLog log = PKLog.get("CustomTextLoadError");

    private ExternalTextTrackLoadErrorPolicy.OnTextTrackLoadErrorListener textTrackLoadErrorListener;

    public interface OnTextTrackLoadErrorListener {
        void onTextTrackLoadError(PKError currentError);
    }

    public void setOnTextTrackErrorListener(ExternalTextTrackLoadErrorPolicy.OnTextTrackLoadErrorListener onTextTrackErrorListener) {
        this.textTrackLoadErrorListener = onTextTrackErrorListener;
    }

    @Override
    public long getRetryDelayMsFor(
            int dataType,
            long loadDurationMs,
            IOException exception,
            int errorCount) {

        if (exception instanceof HttpDataSource.InvalidResponseCodeException || exception instanceof HttpDataSource.HttpDataSourceException) {
            Uri pathSegment = getPathSegmentUri(exception);
            if (pathSegment != null) {
                String lastPathSegment = pathSegment.getLastPathSegment();
                if (lastPathSegment.endsWith(PKSubtitleFormat.vtt.pathExt) || lastPathSegment.endsWith(PKSubtitleFormat.srt.pathExt)) {
                    PKError currentError = new PKError(PKPlayerErrorType.SOURCE_ERROR, PKError.Severity.Recoverable, "TextTrack is invalid url=" + pathSegment, exception);
                    if (textTrackLoadErrorListener != null) {
                        log.e("Error-Event sent, type = " + PKPlayerErrorType.SOURCE_ERROR);
                        textTrackLoadErrorListener.onTextTrackLoadError(currentError);
                    }
                    return Consts.TIME_UNSET;
                }
            }
        }
        return super.getRetryDelayMsFor(
                dataType, loadDurationMs, exception, errorCount);
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
