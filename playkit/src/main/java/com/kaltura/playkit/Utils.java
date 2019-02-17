/*
 * ============================================================================
 * Copyright (C) 2017 Kaltura Inc.
 *
 * Licensed under the AGPLv3 license, unless a different license for a
 * particular library is specified in the applicable library path.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/agpl-3.0.html
 * ============================================================================
 */

package com.kaltura.playkit;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */

public class Utils {
    private static final PKLog log = PKLog.get("Utils");
    private static final int ASSET_READ_LIMIT_BYTES = 1024 * 1024;
    public static final int READ_BUFFER_SIZE = 1024;

    public static String readAssetToString(Context context, String asset) {
        InputStream assetStream = null;
        try {
            assetStream = context.getAssets().open(asset);
            return fullyReadInputStream(assetStream, ASSET_READ_LIMIT_BYTES).toString();
        } catch (IOException e) {
            log.e("Failed reading asset " + asset, e);
            return null;
        } finally {
            safeClose(assetStream);
        }
    }

    public static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.e("Failed to close closeable", e);
            }
        }
    }

    @NonNull
    public static ByteArrayOutputStream fullyReadInputStream(InputStream inputStream, int byteLimit) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte data[] = new byte[READ_BUFFER_SIZE];
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
        return bos;
    }

    public static boolean isNullOrEmpty(final Collection<?> c) {
        return c == null || c.isEmpty();
    }


    public static <E extends Enum<E>> E byValue(Class<E> EClass, String enumValue) {
        return byValue(EClass, enumValue, null);
    }

    public static <E extends Enum<E>> E byValue(Class<E> EClass, String enumValue, E defaultOption) {

        try {
            return enumValue != null ? Enum.valueOf(EClass, enumValue) : defaultOption;

        } catch (IllegalArgumentException | NullPointerException e) {
            return defaultOption;
        }
    }

    public static Bundle mapToBundle(Map<String, ? extends Serializable> input) {
        Bundle output = new Bundle();
        for (String key : input.keySet()) {
            output.putSerializable(key, input.get(key));
        }
        return output;
    }

    public static <T extends Serializable> Map<String, T> bundleToMap(Bundle input, Class<T> c) {
        Map<String, T> output = new HashMap<>();
        for (String key : input.keySet()) {
            output.put(key, c.cast(input.getParcelable(key)));
        }
        return output;
    }

    public static boolean isJsonObjectValueValid(JsonObject jsonObject, String key) {
        return jsonObject.has(key) && !jsonObject.get(key).isJsonNull();
    }

    public static String toBase64(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public static byte[] executePost(String url, byte[] data, Map<String, String> headers) throws IOException {
        return executeHttpRequest(true, url, data, headers);
    }

    public static byte[] executeGet(String url, Map<String, String> headers) throws IOException {
        return executeHttpRequest(false, url, null, headers);
    }

    private static byte[] executeHttpRequest(boolean post, String url, byte[] data, Map<String, String> headers) throws IOException {

        HttpURLConnection urlConnection = null;

        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod(post ? "POST" : "GET");

            if (data != null) {
                urlConnection.setDoOutput(true);
            }

            urlConnection.setDoInput(true);
            if (headers != null) {
                for (Map.Entry<String, String> requestProperty : headers.entrySet()) {
                    urlConnection.setRequestProperty(requestProperty.getKey(), requestProperty.getValue());
                }
            }

            if (data != null) {
                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                out.write(data);
                out.close();
            }

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            return convertInputStreamToByteArray(in);

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private static byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
        byte[] bytes;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte data[] = new byte[READ_BUFFER_SIZE];
        int count;
        while ((count = inputStream.read(data)) != -1) {
            bos.write(data, 0, count);
        }
        bos.flush();
        bos.close();
        inputStream.close();
        bytes = bos.toByteArray();
        return bytes;
    }
}
