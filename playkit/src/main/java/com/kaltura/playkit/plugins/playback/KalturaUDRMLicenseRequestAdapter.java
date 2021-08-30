/*
 * ============================================================================
 * Copyright (C) 2018 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.plugins.playback;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.kaltura.playkit.PKRequestParams;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.Utils;

import static com.kaltura.playkit.PlayKitManager.CLIENT_TAG;

public class KalturaUDRMLicenseRequestAdapter implements PKRequestParams.Adapter {

    private final String KALTURA_COM_LICENSE_IDENTIFITER = ".kaltura.com";
    private final String applicationName;
    private String playSessionId;

    public static void install(Player player, String applicationName) {
        KalturaUDRMLicenseRequestAdapter decorator = new KalturaUDRMLicenseRequestAdapter(applicationName, player);
        player.getSettings().setLicenseRequestAdapter(decorator);
    }

    private KalturaUDRMLicenseRequestAdapter(String applicationName, Player player) {
        this.applicationName = applicationName;
        updateParams(player);

    }

    @NonNull
    @Override
    public PKRequestParams adapt(PKRequestParams requestParams) {

        boolean isEmptyApplicationName = TextUtils.isEmpty(applicationName);
        if (!isEmptyApplicationName) {
            requestParams.headers.put("Referrer", Utils.toBase64(applicationName.getBytes()));
        }

        Uri licenseUrl = requestParams.url;
        if (licenseUrl != null && licenseUrl.getAuthority().contains(KALTURA_COM_LICENSE_IDENTIFITER)) {
            Uri alt = licenseUrl.buildUpon()
                    .appendQueryParameter("sessionId", playSessionId)
                    .appendQueryParameter("clientTag", CLIENT_TAG).build();

            if (!isEmptyApplicationName) {
                alt = alt.buildUpon().appendQueryParameter("referrer", Utils.toBase64(applicationName.getBytes())).build();
            }

            return new PKRequestParams(alt, requestParams.headers);
        }

        return requestParams;
    }

    @Override
    public void updateParams(Player player) {
        this.playSessionId = player.getSessionId();
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }
}
