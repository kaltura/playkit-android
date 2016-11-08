package com.kaltura.playkit.plugins.connect;

import com.kaltura.playkit.plugins.mediaproviders.base.ErrorElement;

/**
 * Created by tehilarozin on 06/09/2016.
 */
public interface ResultElement<T> {

    T getResponse();

    boolean isSuccess();

    ErrorElement getError();

}
