package com.kaltura.playkit.prefetch;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.RoomDatabase;
import android.support.annotation.NonNull;

import java.util.Date;
import java.util.List;

@Database(entities = {WatchedEntry.class}, version = 1)
abstract class PrefetchDb extends RoomDatabase {
    abstract HistoryDao dao();
}

@Entity
class WatchedEntry {

    @PrimaryKey
    @NonNull
    final String entryId;
    @ColumnInfo(name = "timestamp")
    final long timestamp;

    WatchedEntry(@NonNull String entryId, long timestamp) {
        this.entryId = entryId;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
                "entryId='" + entryId + '\'' +
                ", timestamp=" + new Date(timestamp) +
                '}';
    }
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM WatchedEntry")
    List<WatchedEntry> getAllWatched();

    @Insert(onConflict = OnConflictStrategy.REPLACE)  // or OnConflictStrategy.IGNORE
    void insert(WatchedEntry entry);

    @Delete
    void delete(WatchedEntry entry);

    @Query("DELETE FROM WatchedEntry")
    void clearHistory();
}
