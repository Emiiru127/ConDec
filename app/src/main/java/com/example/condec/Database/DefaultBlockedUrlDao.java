package com.example.condec.Database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.Collection;
import java.util.List;

@Dao
public interface DefaultBlockedUrlDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(DefaultBlockedUrl url);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(DefaultBlockedUrl... urls);

    @Query("SELECT * FROM default_blocked_urls")
    LiveData<List<DefaultBlockedUrl>> getAllUrls();

    @Delete
    void delete(DefaultBlockedUrl url);

    @Query("DELETE FROM default_blocked_urls WHERE url = :url")
    void deleteByUrl(String url);

    @Query("SELECT url FROM default_blocked_urls")
    List<String> getAllUrlsSync();
}