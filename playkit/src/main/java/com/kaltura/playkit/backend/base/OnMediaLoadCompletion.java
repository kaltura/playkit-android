package com.kaltura.playkit.backend.base;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKPublicAPI;
import com.kaltura.playkit.connect.ResultElement;

/**
 * Created by tehilarozin on 08/11/2016.
 */

@PKPublicAPI
public interface OnMediaLoadCompletion extends OnCompletion<ResultElement<PKMediaEntry>> {
}
