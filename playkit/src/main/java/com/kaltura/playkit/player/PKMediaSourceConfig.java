package com.kaltura.playkit.player;

import android.net.Uri;

import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.PKRequestInfo;

/**
 * Created by Noam Tamim @ Kaltura on 29/03/2017.
 */
class PKMediaSourceConfig {
    PKMediaSource mediaSource;
    PKRequestInfo.Decorator decorator;

    PKMediaSourceConfig(PKMediaSource mediaSource, PKRequestInfo.Decorator decorator) {
        this.mediaSource = mediaSource;
        this.decorator = decorator;
    }

    Uri getUrl() {
        Uri uri = Uri.parse(mediaSource.getUrl());
        if (decorator == null) {
            return uri;
        } else {
            return decorator.getRequestInfo(new PKRequestInfo(uri, null)).getUrl();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PKMediaSourceConfig that = (PKMediaSourceConfig) o;

        if (mediaSource != null ? !mediaSource.equals(that.mediaSource) : that.mediaSource != null)
            return false;
        return decorator != null ? decorator.equals(that.decorator) : that.decorator == null;
    }

    @Override
    public int hashCode() {
        int result = mediaSource != null ? mediaSource.hashCode() : 0;
        result = 31 * result + (decorator != null ? decorator.hashCode() : 0);
        return result;
    }
}
