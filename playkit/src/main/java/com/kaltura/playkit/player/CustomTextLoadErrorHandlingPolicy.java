package com.kaltura.playkit.player;

import com.kaltura.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.kaltura.android.exoplayer2.upstream.HttpDataSource;
import com.kaltura.playkit.utils.Consts;

import java.io.IOException;

public class CustomTextLoadErrorHandlingPolicy extends DefaultLoadErrorHandlingPolicy {

    @Override
    public long getRetryDelayMsFor(
            int dataType,
            long loadDurationMs,
            IOException exception,
            int errorCount) {
        if (exception instanceof HttpDataSource.InvalidResponseCodeException) {
            HttpDataSource.InvalidResponseCodeException invalidResponseCodeException = (HttpDataSource.InvalidResponseCodeException) exception;
            if (invalidResponseCodeException != null &&
                    invalidResponseCodeException.dataSpec != null &&
                    invalidResponseCodeException.dataSpec.uri != null &&
                    invalidResponseCodeException.dataSpec.uri.getLastPathSegment() != null) {

                String lastPathSegment = invalidResponseCodeException.dataSpec.uri.getLastPathSegment();
                if (lastPathSegment.endsWith(".vtt") || lastPathSegment.endsWith(".srt")) {
                    return Long.MAX_VALUE;
                }
            }
        }
        return Consts.TIME_UNSET;
    }

    @Override
    public int getMinimumLoadableRetryCount(int dataType) {
        return Integer.MAX_VALUE;
    }

//    @Override
//    public long getBlacklistDurationMsFor(
//            int dataType,
//            long loadDurationMs,
//            IOException exception,
//            int errorCount) {
//        if (exception instanceof HttpDataSource.InvalidResponseCodeException) {
//            HttpDataSource.InvalidResponseCodeException invalidResponseCodeException = (HttpDataSource.InvalidResponseCodeException) exception;
//            if (invalidResponseCodeException != null &&
//                    invalidResponseCodeException.dataSpec != null &&
//                    invalidResponseCodeException.dataSpec.uri != null &&
//                    invalidResponseCodeException.dataSpec.uri.getLastPathSegment() != null) {
//
//                String lastPathSegment = invalidResponseCodeException.dataSpec.uri.getLastPathSegment();
//                if (lastPathSegment.endsWith(".vtt") || lastPathSegment.endsWith(".srt")) {
//                    return Consts.TIME_UNSET;                }
//            }
//        }
//        return super.getBlacklistDurationMsFor(
//                dataType, loadDurationMs, exception, errorCount);
//    }
}
