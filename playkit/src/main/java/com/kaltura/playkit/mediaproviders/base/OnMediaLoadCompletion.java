package com.kaltura.playkit.mediaproviders.base;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.OnCompletion;
import com.kaltura.playkit.PKMediaEntry;

/**
 * Created by tehilarozin on 08/11/2016.
 */

public interface OnMediaLoadCompletion extends OnCompletion<ResultElement<PKMediaEntry>> {
}
