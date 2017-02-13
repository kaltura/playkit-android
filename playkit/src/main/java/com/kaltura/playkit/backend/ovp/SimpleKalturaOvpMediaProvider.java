package com.kaltura.playkit.backend.ovp;

import android.text.TextUtils;

import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.backend.PrimitiveResult;
import com.kaltura.playkit.backend.SessionProvider;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;

/**
 * A MediaEntryProvider for simple OVP cases.
 * Two use-cases are supported: authenticated and non-authenticated access. 
 * Authenticated access means the application provides the Kaltura Session token (KS).
 * Non-authenticated access uses a "Widget Session". Depending on access-controls on the backend,
 * authentication may be required for any/some entries.
 * Unlike {@link KalturaOvpMediaProvider} with {@link OvpSessionProvider}, this class will not 
 * attempt to create an authenticated session.
 */
public class SimpleKalturaOvpMediaProvider implements MediaEntryProvider {
    private String baseUrl;
    private String ks;
    private String entryId;
    private int partnerId;
    
    private KalturaOvpMediaProvider currentProvider;

    /**
     * Simple single-entry load. Creates a MediaEntryProvider, sets it to load an entry, and discards it.
     * @param baseUrl base Kaltura Server URL, such as "https://cdnapisec.kaltura.com".
     * @param partnerId     Kaltura partner id.
     * @param ks            Kaltura Session token.
     * @param entryId       Kaltura entry id.
     * @param completion    Code to run when load completes/fails.
     */
    public static void loadSingleEntry(String baseUrl, int partnerId, String ks, String entryId, OnMediaLoadCompletion completion) {
        new SimpleKalturaOvpMediaProvider(baseUrl, partnerId, ks).setEntryId(entryId).load(completion);
    }

    /**
     * Simple single-entry load. Creates a MediaEntryProvider, sets it to load an entry, and discards it.
     * @param baseUrl base Kaltura Server URL, such as "https://cdnapisec.kaltura.com".
     * @param partnerId     Kaltura partner id.
     * @param entryId       Kaltura entry id.
     * @param completion    Code to run when load completes/fails.
     */
    public static void loadSingleEntry(String baseUrl, int partnerId, String entryId, OnMediaLoadCompletion completion) {
        new SimpleKalturaOvpMediaProvider(baseUrl, partnerId).setEntryId(entryId).load(completion);
    }

    /**
     * Create a media provider for authenticated access.
     * @param baseUrl base Kaltura Server URL, such as "https://cdnapisec.kaltura.com".
     * @param partnerId Kaltura partner id.
     * @param ks Kaltura Session token.
     */
    public SimpleKalturaOvpMediaProvider(String baseUrl, int partnerId, String ks) {
        this.baseUrl = baseUrl;
        this.partnerId = partnerId;
        this.ks = ks;
    }

    /**
     * Create a media provider for non-authenticated access.
     * @param baseUrl base Kaltura Server URL, such as "https://cdnapisec.kaltura.com".
     * @param partnerId Kaltura partner id.
     */
    public SimpleKalturaOvpMediaProvider(String baseUrl, int partnerId) {
        this(baseUrl, partnerId, null);
    }

    /**
     * Set or update the KS.
     * @param ks    Kaltura Session token
     * @return Self.
     */
    public SimpleKalturaOvpMediaProvider setKs(String ks) {
        this.ks = ks;
        return this;
    }

    /**
     * Set or update the entryId.
     * @param entryId    Kaltura entry id.
     * @return Self.
     */
    public SimpleKalturaOvpMediaProvider setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }
    
    @Override
    public void load(final OnMediaLoadCompletion completion) {

        if (TextUtils.isEmpty(entryId)) {
            throw new IllegalArgumentException("Missing entryId");
        }

        SessionProvider sessionProvider = sessionProvider();

        // Try to reuse the previous media provider
        if (currentProvider == null) {
            currentProvider = new KalturaOvpMediaProvider();
        }
        currentProvider.setSessionProvider(sessionProvider).setEntryId(entryId);
        currentProvider.load(completion);
    }
    
    @Override
    public void cancel() {
        if (currentProvider != null) {
            currentProvider.cancel();
        }
    }

    private SessionProvider sessionProvider() {
        // Ensure baseUrl, partnerId are not empty.
        if (TextUtils.isEmpty(baseUrl)) {
            throw new IllegalArgumentException("Missing baseUrl");
        }
        if (partnerId == 0) {
            throw new IllegalArgumentException("Missing partnerId");
        }

        return new SessionProvider() {
            @Override
            public String baseUrl() {
                return baseUrl;
            }

            @Override
            public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
                if (completion != null) {
                    completion.onComplete(new PrimitiveResult(ks));
                }
            }

            @Override
            public int partnerId() {
                return partnerId;
            }
        };
    }
}
