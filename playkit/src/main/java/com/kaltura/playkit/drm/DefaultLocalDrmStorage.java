package com.kaltura.playkit.drm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.kaltura.playkit.PKLog;

import java.io.FileNotFoundException;

/**
 * Default implementation of the {@link LocalDrmStorage}. Actually doing the basic save/load/remove actions
 * to the {@link SharedPreferences}.
 * Created by anton.afanasiev on 13/12/2016.
 */

public class DefaultLocalDrmStorage implements LocalDrmStorage {

    private static final PKLog log = PKLog.get("DefaultLocalDrmStorage");

    private static final String LOCAL_DRM_SHARED_PREFERENCE_STORAGE = "PlayKitLocalDrmStorage";
    private final SharedPreferences sharedPreferences;

    public DefaultLocalDrmStorage(Context context){
        sharedPreferences = context.getSharedPreferences(LOCAL_DRM_SHARED_PREFERENCE_STORAGE, 0);
    }

    @Override
    public void save(String key, byte[] value) {
        String encodedValue = Base64.encodeToString(value, Base64.NO_WRAP);
        log.i("save to storage with key " + key + " and value " + encodedValue);
        sharedPreferences.edit()
                .putString(key, encodedValue)
                .apply();
    }

    @Override
    public byte[] load(String key) throws FileNotFoundException {

        String value = sharedPreferences.getString(key, null);
        log.i("load from storage with key " + key);
        if (value == null) {
            throw new FileNotFoundException("Key not found in the storage " + key);
        }

        return Base64.decode(value, Base64.NO_WRAP);
    }

    @Override
    public void remove(String key) {
        log.i("remove from storage with key " + key);
        sharedPreferences.edit()
                .remove(key)
                .apply();
    }
}
