package com.kaltura.playkit.backend;

import com.kaltura.playkit.connect.ErrorElement;

/**
 */

public class BaseResult {
    public double executionTime;
    public ErrorElement error;

    public BaseResult() {
    }

    public BaseResult(ErrorElement error) {
        this.error = error;
    }
}
