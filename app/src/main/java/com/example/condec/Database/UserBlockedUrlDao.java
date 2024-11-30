package com.example.condec.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserBlockedUrlDao {
    @Insert
    void insert(UserBlockedUrl url);

    @Query("SELECT * FROM user_blocked_urls")
    List<UserBlockedUrl> getAllUrls();

    @Delete
    void delete(UserBlockedUrl url);
}