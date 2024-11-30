package com.example.condec.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DefaultBlockedUrlDao {
    @Insert
    void insert(DefaultBlockedUrl url);

    @Insert
    void insertAll(DefaultBlockedUrl... urls);

    @Query("SELECT * FROM default_blocked_urls")
    List<DefaultBlockedUrl> getAllUrls();

    @Delete
    void delete(DefaultBlockedUrl url);

}