package com.kaltura.playkit.prefetch;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.RoomDatabase;

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
