package com.example.musicgym;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class Playlist {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public long createdAt;

    public Playlist() {}
    @Ignore
    public Playlist(String name) { this.name = name; this.createdAt = System.currentTimeMillis(); }
}
