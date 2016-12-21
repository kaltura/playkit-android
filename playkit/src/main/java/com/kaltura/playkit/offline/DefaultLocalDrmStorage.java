package com.kaltura.playkit.offline;

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

    private static final String LOCAL_DRM_SHARED_PREFERENCE_STORAGE = "LocalDrmStorage";
    private final SharedPreferences sharedPreferences;

    DefaultLocalDrmStorage(Context context){
        sharedPreferences = context.getSharedPreferences(LOCAL_DRM_SHARED_PREFERENCE_STORAGE, 0);
    }

    @Override
    public void save(String key, byte[] value) {
        String encodedValue = Base64.encodeToString(value, Base64.DEFAULT);
        log.i("save to storage with key " + key + " and value " + encodedValue);
        sharedPreferences.edit()
                .putString(key, encodedValue)
                .putString("AAAAUHBzc2gAAAAA7e+LqXnWSs6jyCfc1R0h7QAAADAIARIQk6L0G9E5OxYCYsO8xcinFxoHa2FsdHVyYSIKMV90bW9tZGFscyoFU0RfSEQ=", encodedValue)
                .apply();
    }

    @Override
    public byte[] load(String key) throws FileNotFoundException {

        String value = sharedPreferences.getString("AAAAUHBzc2gAAAAA7e+LqXnWSs6jyCfc1R0h7QAAADAIARIQk6L0G9E5OxYCYsO8xcinFxoHa2FsdHVyYSIKMV90bW9tZGFscyoFU0RfSEQ=", null);
        log.i("load from storage with key " + key);
        if (value == null) {
            throw new FileNotFoundException("Key not found in the storage" + key);
        }

        return Base64.decode(value, Base64.DEFAULT);
    }

    @Override
    public void remove(String key) {
        log.i("remove from storage with key " + key);
        sharedPreferences.edit()
                .remove(key)
                .apply();
    }
}
