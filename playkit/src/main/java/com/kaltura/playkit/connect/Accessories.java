package com.kaltura.playkit.connect;

/**
 * Created by tehilarozin on 10/11/2016.
 */

public class Accessories {

    public static <D> ResultElement<D> buildResult(final D data,  final ErrorElement error) {
        return new ResultElement<D>() {
            @Override
            public D getResponse() {
                return data;
            }

            @Override
            public boolean isSuccess() {
                return null == error;
            }

            @Override
            public ErrorElement getError() {
                return error;
            }
        };
    }

    public static ResponseElement buildResponse(final String data,  final ErrorElement error) {

        //return (ResponseElement) buildResult(data, error);

        return new ResponseElement() {
            @Override
            public String getCode() {
                return error == null ? ResponseElement.Ok : error.getCode();
            }

            @Override
            public String getRequestId() {
                return null;
            }

            @Override
            public String getResponse() {
                return data;
            }

            @Override
            public boolean isSuccess() {
                return error==null;
            }

            @Override
            public ErrorElement getError() {
                return error;
            }
        };
    }
}
