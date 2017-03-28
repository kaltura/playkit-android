package com.kaltura.playkit;

import android.net.Uri;

public interface UrlDecorator {
    Uri getDecoratedUrl(Uri url);
}
