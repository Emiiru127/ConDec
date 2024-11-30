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
public interface UserBlockedUrlDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(UserBlockedUrl url);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(UserBlockedUrl... urls);

    @Query("SELECT * FROM user_blocked_urls")
    LiveData<List<UserBlockedUrl>> getAllUrls();

    @Delete
    void delete(UserBlockedUrl url);

    @Query("DELETE FROM user_blocked_urls WHERE url = :url")
    void deleteByUrl(String url);

    @Query("SELECT url FROM user_blocked_urls")
    List<String> getAllUrlsSync();
}