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

package com.kaltura.playkit.player;

/**
 * Created by anton.afanasiev on 20/06/2017.
 */

public enum PKPlayerErrorType {

    SOURCE_ERROR(7000),
    RENDERER_ERROR(7001),
    UNEXPECTED(7002),
    SOURCE_SELECTION_FAILED(7003),
    FAILED_TO_INITIALIZE_PLAYER(7004),
    DRM_ERROR(7005),
    TRACK_SELECTION_FAILED(7006),
    LOAD_ERROR(7007),
    OUT_OF_MEMORY(7008),
    REMOTE_COMPONENT_ERROR(7009),
    TIMEOUT(7010),
    IO_ERROR(7011),
    MISCELLANEOUS(7012);

    public final int errorCode;

    PKPlayerErrorType(int errorCode) {
        this.errorCode = errorCode;
    }
}
