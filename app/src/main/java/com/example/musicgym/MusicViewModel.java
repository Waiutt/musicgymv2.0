package com.example.musicgym;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 音乐页 ViewModel — 管理播放状态、歌单、收藏 */
public class MusicViewModel extends AndroidViewModel {

    // ── 播放状态 ──
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentTitle = new MutableLiveData<>("");
    private final MutableLiveData<String> currentArtist = new MutableLiveData<>("");
    private final MutableLiveData<Integer> currentPosition = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> duration = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> playMode = new MutableLiveData<>(0); // 0顺序/1单曲/2随机
    private final MutableLiveData<Integer> cadence = new MutableLiveData<>(0);

    // ── 歌单 ──
    private final MutableLiveData<List<TrackInfo>> tracks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<String> trackCount = new MutableLiveData<>("0 首");

    // ── 收藏 ──
    private final SharedPreferences favPrefs;
    private final Set<String> favSet;
    private final MutableLiveData<Boolean> isFavorited = new MutableLiveData<>(false);

    // ── 均衡器 ──
    private final MutableLiveData<Integer> eqPreset = new MutableLiveData<>(-1);

    public MusicViewModel(@NonNull Application app) {
        super(app);
        favPrefs = app.getSharedPreferences("music_fav", Context.MODE_PRIVATE);
        favSet = new LinkedHashSet<>(favPrefs.getStringSet("favs", new LinkedHashSet<>()));
    }

    // ── Getter ──
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<String> getCurrentTitle() { return currentTitle; }
    public LiveData<String> getCurrentArtist() { return currentArtist; }
    public LiveData<Integer> getCurrentPosition() { return currentPosition; }
    public LiveData<Integer> getDuration() { return duration; }
    public LiveData<Integer> getPlayMode() { return playMode; }
    public LiveData<Integer> getCadence() { return cadence; }
    public LiveData<List<TrackInfo>> getTracks() { return tracks; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<String> getTrackCount() { return trackCount; }
    public LiveData<Boolean> getIsFavorited() { return isFavorited; }
    public LiveData<Integer> getEqPreset() { return eqPreset; }

    // ── 播放控制 ──
    public void setPlaying(boolean playing) { isPlaying.postValue(playing); }
    public void setTrack(String title, String artist, int dur) {
        currentTitle.postValue(title); currentArtist.postValue(artist); duration.postValue(dur);
        updateFavorite();
    }
    public void updatePosition(int pos) { currentPosition.postValue(pos); }
    public void cyclePlayMode() {
        int next = (playMode.getValue() != null ? playMode.getValue() : 0) + 1;
        if (next > 2) next = 0;
        playMode.postValue(next);
    }
    public void setCadence(int spm) { cadence.postValue(spm); }

    // ── 歌单 ──
    public void setTracks(List<TrackInfo> list, int curIdx) {
        tracks.postValue(new ArrayList<>(list));
        currentIndex.postValue(curIdx);
        trackCount.postValue(list.size() + " 首");
    }
    public void setCurrentIndex(int idx) { currentIndex.postValue(idx); updateFavorite(); }

    // ── 收藏 ──
    public void toggleFavorite(String trackPath) {
        if (trackPath == null) return;
        if (favSet.contains(trackPath)) favSet.remove(trackPath);
        else favSet.add(trackPath);
        favPrefs.edit().putStringSet("favs", new LinkedHashSet<>(favSet)).apply();
        isFavorited.postValue(favSet.contains(trackPath));
    }
    private void updateFavorite() {
        TrackInfo t = getCurrentTrack();
        isFavorited.postValue(t != null && favSet.contains(t.path));
    }
    public boolean isFavorite(String path) { return favSet.contains(path); }
    private TrackInfo getCurrentTrack() {
        List<TrackInfo> list = tracks.getValue();
        Integer idx = currentIndex.getValue();
        if (list != null && idx != null && idx >= 0 && idx < list.size())
            return list.get(idx);
        return null;
    }

    // ── 均衡器 ──
    public void setEqPreset(int preset) { eqPreset.postValue(preset); }

    // ── 数据类 ──
    public static class TrackInfo {
        public final String title, artist, path;
        public TrackInfo(String t, String a, String p) { title = t; artist = a; path = p; }
    }
}
