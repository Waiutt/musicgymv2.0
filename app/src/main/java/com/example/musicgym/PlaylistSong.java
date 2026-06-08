package com.example.musicgym;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_songs")
public class PlaylistSong {
    @PrimaryKey(autoGenerate = true) public long id;
    public long playlistId;
    public String trackPath;
    public String title;
    public String artist;
    public int position;

    public PlaylistSong() {}
    @androidx.room.Ignore
    public PlaylistSong(long playlistId, String path, String title, String artist, int pos) {
        this.playlistId = playlistId;
        this.trackPath = path;
        this.title = title;
        this.artist = artist;
        this.position = pos;
    }
}
