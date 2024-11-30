package com.example.condec.Database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "default_blocked_urls")
public class DefaultBlockedUrl {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String url;

    public DefaultBlockedUrl(String url) {
        this.url = url;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
