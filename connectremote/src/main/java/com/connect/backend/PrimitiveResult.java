package com.connect.backend;


import com.connect.utils.ErrorElement;

/**
 * Created by tehilarozin on 07/12/2016.
 */

public class PrimitiveResult extends BaseResult {
    private String result;

    public PrimitiveResult(String result) {
        super();
        this.result = result;
    }

    public PrimitiveResult() {
        super();
    }

    public PrimitiveResult(ErrorElement error) {
        super(error);
    }

    public String getResult() {
        return result;
    }

    public PrimitiveResult result(String result){
        this.result = result;
        return this;
    }

    public PrimitiveResult error(ErrorElement error) {
        this.error = error;
        return this;
    }
}
