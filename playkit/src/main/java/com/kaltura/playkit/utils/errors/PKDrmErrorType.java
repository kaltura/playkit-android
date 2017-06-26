package com.kaltura.playkit.utils.errors;

/**
 * Created by anton.afanasiev on 20/06/2017.
 */

public enum PKDrmErrorType {

    DRM_RIGHTS_NOT_INSTALLED(2400),
    DRM_RIGHTS_RENEWAL_NOT_ALLOWED(2401),
    DRM_NOT_SUPPORTED(2402),
    DRM_OUT_OF_MEMORY(2403),
    DRM_NO_INTERNET_CONNECTION(2404),
    PROCESS_DRM_INFO_FAILED(2405),
    DRM_REMOVE_ALL_RIGHTS_FAILED(2406),
    ACQUIRE_DRM_INFO_FAILED(2407),

    NO_WIDEVINE_PSSH(2450),
    DRM_FAILED_TO_OPEN_SESSION(2451);

    public final int errorCode;

    PKDrmErrorType(int errorCode) {
        this.errorCode = errorCode;
    }

}
