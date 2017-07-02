/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 * 
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 * 
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.api.ovp;

import android.text.TextUtils;

import com.kaltura.netkit.connect.response.PrimitiveResult;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.netkit.utils.SessionProvider;

/**
 * A SessionProvider that just reflects its input parameters -- baseUrl, partnerId, ks.
 * This class does not attempt to manage a session. The application is expected to provide a valid KS, which it
 * can update as required by calling {@link #setKs(String)}.
 * For some use cases, the KS can be null (anonymous media playback, if allowed by access-control).
 * <p>
 * Basic usage with a {@link com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider}:
 * <pre>
 * {@code
 *      new KalturaOvpMediaProvider()
 *          .setSessionProvider(new SimpleOvpSessionProvider("https://cdnapisec.kaltura.com", 1851571, null))
 *          .setEntryId("0_pl5lbfo0")
 *          .load(completion);
 * }
 * </pre>
 * </p>
 */
public class SimpleOvpSessionProvider implements SessionProvider {

    private String baseUrl;
    private int partnerId;
    private String ks;

    /**
     * Build an OVP {@link SessionProvider} with the specified parameters.
     * 
     * @param baseUrl       Kaltura Server URL, such as "https://cdnapisec.kaltura.com".
     * @param partnerId     Kaltura partner id.
     * @param ks            Kaltura Session token.
     */
    public SimpleOvpSessionProvider(String baseUrl, int partnerId, String ks) {
        // Ensure baseUrl, partnerId are not empty.
        if (TextUtils.isEmpty(baseUrl)) {
            throw new IllegalArgumentException("Missing baseUrl");
        }
        if (partnerId == 0) {
            throw new IllegalArgumentException("Missing partnerId");
        }

        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.ks = ks;
    }

    /**
     * Update the session token.
     * @param ks            Valid Kaltura Session token.
     */
    public void setKs(String ks) {
        this.ks = ks;
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }

    @Override
    public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
        completion.onComplete(new PrimitiveResult(ks));
    }

    @Override
    public int partnerId() {
        return partnerId;
    }
}
