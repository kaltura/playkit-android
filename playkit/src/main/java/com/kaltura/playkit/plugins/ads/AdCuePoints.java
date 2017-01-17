package com.kaltura.playkit.plugins.ads;

import java.util.List;

/**
 * Created by gilad.nadav on 22/11/2016.
 */

public class AdCuePoints {

    private List<Long> adCuePoints;

    public AdCuePoints(List<Long> adCuePoints) {
        this.adCuePoints = adCuePoints;
    }

    public boolean hasPreRoll() {
        if (adCuePoints != null && !adCuePoints.isEmpty()) {
            if (adCuePoints.get(0) == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMidRoll() {
        if (adCuePoints != null && !adCuePoints.isEmpty()) {
            for (Long cuePoint : adCuePoints) {
                if (cuePoint > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasPostRoll() {
        if (adCuePoints != null && !adCuePoints.isEmpty()) {
            if (adCuePoints.get(adCuePoints.size() - 1) < 0) {
                return true;
            }
        }
        return false;
    }
}
