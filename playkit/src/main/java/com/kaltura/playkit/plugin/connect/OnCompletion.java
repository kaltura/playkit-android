package com.kaltura.playkit.plugin.connect;


import com.kaltura.playkit.R;

/**
 * Created by tehilarozin on 21/07/2016.
 */
public interface OnCompletion<R> {
    void onComplete(R response);
}
public interface OnCompletion extends OnCompletion<ResultElement>{
    void onComplete(ResultElement response);
}
