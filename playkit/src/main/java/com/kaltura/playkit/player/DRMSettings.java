package com.kaltura.playkit.player;

import com.kaltura.playkit.PKDrmParams;

public class DRMSettings {
    private PKDrmParams.Scheme drmScheme;
    private boolean isMultiSession = false;
    private boolean isForceDefaultLicenseUri = false;
    private boolean isAllowClearlead = true;
    private boolean isForceWidevineL3Playback = false;

    public DRMSettings(PKDrmParams.Scheme drmScheme) {
        this.drmScheme = drmScheme;
        if (drmScheme == PKDrmParams.Scheme.PlayReadyCENC) {
            this.isForceDefaultLicenseUri = true;
        }
    }

    public PKDrmParams.Scheme getDrmScheme() {
        return drmScheme;
    }

    public DRMSettings setDrmScheme(PKDrmParams.Scheme drmScheme) {
        this.drmScheme = drmScheme;
        if (drmScheme == PKDrmParams.Scheme.PlayReadyCENC) {
            this.isForceDefaultLicenseUri = true;
        }
        return this;
    }

    public boolean getIsMultiSession() {
        return isMultiSession;
    }

    /**
     * Sets whether the DRM configuration is multi session enabled.
     */
    public DRMSettings setIsMultiSession(boolean isMultiSession) {
        this.isMultiSession = isMultiSession;
        return this;
    }

    public boolean getIsForceDefaultLicenseUri() {
        return isForceDefaultLicenseUri;
    }

    /**
     * Sets whether to force use the default DRM license server URI even if the media specifies its
     * own DRM license server URI.
     * <br>
     * Default is set to `true` for Playready DRM streams otherwise `false` for Widevine DRM streams.
     * Means in both the cases, DRM license URL should be passed for the playback.
     * </br>
     * <br>
     * <br>
     * If license URL is not passed for Playready Stream and
     * manifest has InStream license URL then set it `false`,
     * by doing this; Player will take the license URL from the manifest.
     * </br>
     * <br>
     * <br>
     * Passing `false` for Widevine streams is not applicable.
     * </br>
     */
    public DRMSettings setIsForceDefaultLicenseUri(boolean isForceDefaultLicenseUri) {
        this.isForceDefaultLicenseUri = isForceDefaultLicenseUri;
        return this;
    }

    public boolean getAllowClearlead() {
        return isAllowClearlead;
    }

    /**
     * Sets whether clear samples within protected content should be played when keys for the
     * encrypted part of the content have yet to be loaded.
     */
    public DRMSettings setIsAllowClearlead(boolean isAllowClearlead) {
        this.isAllowClearlead = isAllowClearlead;
        return this;
    }

    public boolean getIsForceWidevineL3Playback() {
        return isForceWidevineL3Playback;
    }

    public DRMSettings setIsForceWidevineL3Playback(boolean isForceWidevineL3Playback) {
        this.isForceWidevineL3Playback = isForceWidevineL3Playback;
        return this;
    }
}
