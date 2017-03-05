package com.kaltura.playkit.backend.ovp;

import android.text.TextUtils;

import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.backend.PrimitiveResult;
import com.kaltura.playkit.backend.SessionProvider;

/**
 * A SessionProvider that just reflects its input parameters -- baseUrl, partnerId, ks.
 * Unlike the full {@link OvpSessionProvider}, this class does not attempt to manage (create,
 * renew, validate, clear) a session. The application is expected to provide a valid KS, which it 
 * can update as required by calling {@link #setKs(String)}.
 * For some use cases, the KS can be null (anonymous media playback, if allowed by access-control).
 *
 * Basic usage with a {@link KalturaOvpMediaProvider}:
 *
 * Example:
 *
 *      new KalturaOvpMediaProvider()
 *          .setSessionProvider(new SimpleOvpSessionProvider("https://cdnapisec.kaltura.com", 1851571, null))
 *          .setEntryId("0_pl5lbfo0")
 *          .load(completion);
 *
 *
 * 
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
