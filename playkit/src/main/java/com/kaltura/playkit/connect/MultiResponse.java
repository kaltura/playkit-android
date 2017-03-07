package com.kaltura.playkit.connect;

import android.util.Log;

import com.kaltura.playkit.backend.BaseResult;

import java.util.ArrayList;

/**
 * @hide
 */

public class MultiResponse extends ArrayList<BaseResult> {
    public MultiResponse(){
        super();
    }

    public MultiResponse(BaseResult response){
        super();
        add(response);
    }

    public boolean failedOn(int i) {
        try {
            return get(i).error != null;
        } catch (IndexOutOfBoundsException e) {
            Log.e("MultiResponse","failedOn: index " + i + " out of range");
            return true;
        }
    }

    public <T extends BaseResult> T getAt(String indexStr) {
        int index = 0;
        try {
            index = Integer.valueOf(indexStr) - 1;

        } catch (NumberFormatException e) {
            e.printStackTrace();
            index = -1;
        }

        return getAt(index);
    }

    public <T extends BaseResult> T getAt(int i){
        return i >= 0 && size() > i ? (T) get(i) : null;
    }

}
