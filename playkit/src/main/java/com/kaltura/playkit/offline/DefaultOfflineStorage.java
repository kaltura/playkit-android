package com.kaltura.playkit.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.io.FileNotFoundException;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

public class DefaultOfflineStorage implements OfflineStorage {

    private static final String OFFLINE_SHARED_PREFERENCE_STORAGE = "OfflineDrmStorage";
    private final SharedPreferences sharedPreferences;

    public DefaultOfflineStorage(Context context){
        sharedPreferences = context.getSharedPreferences(OFFLINE_SHARED_PREFERENCE_STORAGE, 0);
    }

    @Override
    public void save(byte[] initData, byte[] keySetId) {
        String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
        String encodedKeySetId = Base64.encodeToString(keySetId, Base64.NO_WRAP);
        sharedPreferences.edit()
                .putString(encodedInitData, encodedKeySetId)
                .apply();
    }

    @Override
    public byte[] load(byte[] initData) throws FileNotFoundException {
        String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
        String encodedKeySetId = sharedPreferences.getString(encodedInitData, null);

        if (encodedKeySetId == null) {
            throw new FileNotFoundException("Can't load keySetId");
        }

        return Base64.decode(encodedKeySetId, 0);
    }

    @Override
    public void remove(byte[] initData) {
        String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
        sharedPreferences.edit()
                .remove(encodedInitData)
                .apply();
    }
}
