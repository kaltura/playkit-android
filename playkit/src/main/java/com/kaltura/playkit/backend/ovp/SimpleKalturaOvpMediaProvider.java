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
    
    private MediaEntryProvider currentProvider;

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
        
        // Ensure baseUrl, entryId, partnerId are not empty.
        if (TextUtils.isEmpty(baseUrl)) {
            throw new IllegalArgumentException("Missing baseUrl");
        }
        if (TextUtils.isEmpty(entryId)) {
            throw new IllegalArgumentException("Missing entryId");
        }
        if (partnerId == 0) {
            throw new IllegalArgumentException("Missing partnerId");
        }
        
        cancel();
        
        if (ks == null) {
            // No KS? Create widget session.
            final OvpSessionProvider sessionProvider = new OvpSessionProvider(baseUrl);
            sessionProvider.startAnonymousSession(partnerId, new OnCompletion<PrimitiveResult>() {
                @Override
                public void onComplete(PrimitiveResult response) {
                    load(sessionProvider, completion);
                }
            });
        } else {
            load(sessionProvider(baseUrl, partnerId, ks), completion);
        }
    }
    
    @Override
    public void cancel() {
        if (currentProvider != null) {
            currentProvider.cancel();
        }
    }

    private SessionProvider sessionProvider(final String baseUrl, final int partnerId, final String ks) {
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

    private void load(SessionProvider sessionProvider, OnMediaLoadCompletion completion) {
        currentProvider = new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(entryId);
        currentProvider.load(completion);
    }
}
