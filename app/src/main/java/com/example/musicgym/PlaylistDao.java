package com.example.musicgym;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PlaylistDao {

    // ── 歌单 ──
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    List<Playlist> getAllPlaylists();

    @Insert
    long insertPlaylist(Playlist playlist);

    @Update
    void updatePlaylist(Playlist playlist);

    @Delete
    void deletePlaylist(Playlist playlist);

    // ── 歌单歌曲 ──
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    List<PlaylistSong> getSongs(long playlistId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSong(PlaylistSong song);

    @Delete
    void deleteSong(PlaylistSong song);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void deleteSongsByPlaylist(long playlistId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    int getSongCount(long playlistId);

    // ── 级联删除 ──
    @Transaction
    default void deletePlaylistCascade(Playlist playlist) {
        deleteSongsByPlaylist(playlist.id);
        deletePlaylist(playlist);
    }
}
