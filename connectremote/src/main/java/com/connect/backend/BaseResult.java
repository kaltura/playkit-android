package com.connect.backend;

import com.connect.utils.ErrorElement;

/**
 * Created by tehilarozin on 15/11/2016.
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
