package com.connect.backend.phoenix.data;

import android.text.TextUtils;

import com.connect.backend.BaseResult;

/**
 * Created by tehilarozin on 20/11/2016.
 */

public class KalturaLicensedUrl extends BaseResult {
    String mainUrl = "";
    String altUrl = "";

    public String getMainUrl() {
        return mainUrl;
    }

    public String getLicensedUrl() {
        return TextUtils.isEmpty(mainUrl) ? altUrl : mainUrl;
    }

    public String getAltUrl() {
        return altUrl;
    }
}
