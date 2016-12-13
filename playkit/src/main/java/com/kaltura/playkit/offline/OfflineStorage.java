package com.kaltura.playkit.offline;

import java.io.FileNotFoundException;

/**
 * Created by anton.afanasiev on 13/12/2016.
 */

public interface OfflineStorage {

    void save(byte[] initData, byte[] keySetId);

    byte[] load(byte[] initData) throws FileNotFoundException;

    void remove(byte[] initData);
}
