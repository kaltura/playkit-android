package com.kaltura.playkit.utils.errors;

/**
 * Created by anton.afanasiev on 20/06/2017.
 */

public enum PKOfflineError {

    NO_NETWORK_CONNECTION(2300),
    NO_SOURCE_FOUND(2301),
    ASSET_REGISTRATION_FAILED(2302),
    LOCAL_ASSET_NOT_FOUND(2303);

    public final int errorCode;

    PKOfflineError(int errorCode) {
        this.errorCode = errorCode;
    }
}
