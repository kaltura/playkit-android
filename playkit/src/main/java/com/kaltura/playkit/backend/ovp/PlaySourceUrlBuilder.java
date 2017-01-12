package com.kaltura.playkit.backend.ovp;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.List;
import java.util.UUID;

import static android.text.TextUtils.isEmpty;

/**
 * Created by tehilarozin on 30/11/2016.
 */

public class PlaySourceUrlBuilder {

    public static final String DefFormat = "url";

    private String baseUrl = null;
    private String partnerId = null;
    private String entryId = null;
    private String ks = null;
    private String uiConfId = null;
    private String format = null;
    private String protocol = null;
    private String extension = null;
    private String flavorIds = null;
    private String sessionId = null;

    PlaySourceUrlBuilder() {
        // set defaults:
        protocol = OvpConfigs.DefaultHttpProtocol;
        format = DefFormat;
        sessionId = UUID.randomUUID().toString(); //!! should be created and added to the source by the player (playerConfig)
    }

    PlaySourceUrlBuilder(PlaySourceUrlBuilder builder) {
        baseUrl = builder.baseUrl;
        partnerId = builder.partnerId;
        entryId = builder.entryId;
        ks = builder.ks;
        uiConfId = builder.uiConfId;
        format = builder.format;
        protocol = builder.protocol;
        extension = builder.extension;
        flavorIds = builder.flavorIds;
        sessionId = builder.sessionId;
    }

    public PlaySourceUrlBuilder setBaseUrl(@NonNull String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public PlaySourceUrlBuilder setPartnerId(@NonNull String partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public PlaySourceUrlBuilder setEntryId(@NonNull String entryId) {
        this.entryId = entryId;
        return this;
    }

    /**
     * optional - anonymous user don't need to pass ks.
     *
     * @param ks
     * @return
     */
    public PlaySourceUrlBuilder setKs(@NonNull String ks) {
        this.ks = ks;
        return this;
    }

    /**
     * if flavors are not provided will be added to the url base structure otherwise will be added as
     * url parameter.
     *
     * @param uiConfId
     * @return
     */
    public PlaySourceUrlBuilder setUiConfId(@NonNull String uiConfId) {
        this.uiConfId = uiConfId;
        return this;
    }

    public PlaySourceUrlBuilder setFormat(@NonNull String format) {
        this.format = format;
        return this;
    }


    /**
     * we support http or https. defaults to DefaultHttpProtocol
     *
     * @param protocol
     * @return
     */
    public PlaySourceUrlBuilder setProtocol(@NonNull String protocol) {
        this.protocol = protocol;
        return this;
    }

    public PlaySourceUrlBuilder setExtension(@NonNull String extension) {
        this.extension = extension;
        return this;
    }

    /**
     * optional
     *
     * @param flavorIds
     * @return
     */
    public PlaySourceUrlBuilder setFlavorIds(@NonNull String flavorIds) {
        this.flavorIds = flavorIds;
        return this;
    }

    public PlaySourceUrlBuilder setFlavorIds(@NonNull List<String> flavorIds) {
        setFlavorIds(TextUtils.join(",", flavorIds));
        return this;
    }

    private boolean assertMandatoryValues() {
        return !isEmpty(baseUrl) && !isEmpty(partnerId) && !isEmpty(entryId) && !isEmpty(extension) && !isEmpty(format);
    }

    public String build() {
        if (!assertMandatoryValues()) {
            return null;
        }

        StringBuilder playUrl = new StringBuilder(baseUrl);
        if(!baseUrl.endsWith("/")){
            playUrl.append("/");
        }
        playUrl.append("p/").append(partnerId).append("/sp/")
                .append(partnerId).append("00").append("/playManifest").append("/entryId/").append(entryId).append("/protocol/")
                .append(protocol).append("/format/").append(format);

        boolean hasUiConfId = !isEmpty(uiConfId);
        boolean hasFlavors = !isEmpty(flavorIds);
        if (hasFlavors) {
            playUrl.append("/falvorIds/").append(flavorIds);
        } else if (hasUiConfId) {
            playUrl.append("/uiConfId/").append(uiConfId);
        }

        if (!isEmpty(ks)) {
            playUrl.append("/ks/").append(ks);
        }


        playUrl.append("/a.").append(extension);

        //TODO: add it on player side!: playUrl.append("?playSessionId=").append(sessionId);

        if (hasFlavors && hasUiConfId) {
            playUrl.append("?uiConfId=").append(uiConfId);
        }

        return playUrl.toString();
    }

}
