package com.kaltura.playkit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * @hide
 */

public class Utils {
    private static final String TAG = "Utils";

    public static String readAssetToString(Context context, String asset) {
        try {
            InputStream assetStream = context.getAssets().open(asset);
            return fullyReadInputStream(assetStream, 1024*1024).toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed reading asset " + asset, e);
            return null;
        }
    }

    @NonNull
    public static ByteArrayOutputStream fullyReadInputStream(InputStream inputStream, int byteLimit) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte data[] = new byte[1024];
        int count;

        while ((count = inputStream.read(data)) != -1) {
            int maxCount = byteLimit - bos.size();
            if (count > maxCount) {
                bos.write(data, 0, maxCount);
                break;
            } else {
                bos.write(data, 0, count);
            }
        }
        bos.flush();
        bos.close();
        inputStream.close();
        return bos;
    }

    public static boolean isNullOrEmpty( final Collection< ? > c ) {
        return c == null || c.isEmpty();
    }
}
