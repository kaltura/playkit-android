/*
 * ============================================================================
 * Copyright (C) 2020 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit.player;

// Sets the maximum allowed video width and height.

import androidx.annotation.NonNull;

public class PKMaxVideoSize {

    private int maxVideoWidth = Integer.MAX_VALUE;
    private int maxVideoHeight = Integer.MAX_VALUE;

    public int getMaxVideoWidth() {
        return maxVideoWidth;
    }

    public PKMaxVideoSize setMaxVideoWidth(int maxVideoWidth) {
        if (maxVideoWidth == 0) {
            this.maxVideoWidth = Integer.MAX_VALUE;
            return this;
        }

        this.maxVideoWidth = maxVideoWidth;
        return this;
    }

    public int getMaxVideoHeight() {
        return maxVideoHeight;
    }

    public PKMaxVideoSize setMaxVideoHeight(int maxVideoHeight) {
        if (maxVideoHeight == 0) {
            this.maxVideoHeight = Integer.MAX_VALUE;
            return this;
        }

        this.maxVideoHeight = maxVideoHeight;
        return this;
    }
}
