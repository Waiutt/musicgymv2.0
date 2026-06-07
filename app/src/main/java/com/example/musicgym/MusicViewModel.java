package com.example.musicgym;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 音乐页 ViewModel — 管理播放状态、歌单、扫描、收藏、均衡器、BPM */
public class MusicViewModel extends AndroidViewModel {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SharedPreferences favPrefs;
    private final Set<String> favSet;

    // ── LiveData ──
    private final MutableLiveData<List<TrackInfo>> tracks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(-1);
    private final MutableLiveData<String> trackCount = new MutableLiveData<>("0 首");
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<String> title = new MutableLiveData<>("未在播放");
    private final MutableLiveData<String> artist = new MutableLiveData<>("");
    private final MutableLiveData<Integer> playMode = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isFavorited = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> eqPreset = new MutableLiveData<>(0); // 0=跑步
    private final MutableLiveData<Integer> cadence = new MutableLiveData<>(0);
    private final MutableLiveData<String> scanStatus = new MutableLiveData<>("扫描歌曲中...");
    private final MutableLiveData<Boolean> autoPlay = new MutableLiveData<>(false);

    public MusicViewModel(@NonNull Application app) {
        super(app);
        favPrefs = app.getSharedPreferences("music_fav", Context.MODE_PRIVATE);
        favSet = new LinkedHashSet<>(favPrefs.getStringSet("favs", new LinkedHashSet<>()));
    }

    // ── Getter ──
    public LiveData<List<TrackInfo>> getTracks() { return tracks; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<String> getTrackCount() { return trackCount; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<String> getTitle() { return title; }
    public LiveData<String> getArtist() { return artist; }
    public LiveData<Integer> getPlayMode() { return playMode; }
    public LiveData<Boolean> getIsFavorited() { return isFavorited; }
    public LiveData<Integer> getEqPreset() { return eqPreset; }
    public LiveData<Integer> getCadence() { return cadence; }
    public LiveData<String> getScanStatus() { return scanStatus; }

    // ── 播放 ──
    public void setPlaying(boolean p) { isPlaying.postValue(p); }
    public void setTrack(String t, String a) { title.postValue(t); artist.postValue(a); }

    // ── 播放模式 ──
    public void cycleMode() {
        int v = playMode.getValue() != null ? playMode.getValue() : 0;
        playMode.postValue((v + 1) % 3);
    }
    public int getMode() { return playMode.getValue() != null ? playMode.getValue() : 0; }

    // ── 歌单 ──
    public List<TrackInfo> getTrackList() {
        List<TrackInfo> t = tracks.getValue();
        return t != null ? t : new ArrayList<>();
    }
    public TrackInfo getCurrentTrack() {
        List<TrackInfo> t = tracks.getValue();
        Integer idx = currentIndex.getValue();
        if (t != null && idx != null && idx >= 0 && idx < t.size()) return t.get(idx);
        return null;
    }
    public int getNextIndex() {
        List<TrackInfo> t = tracks.getValue();
        Integer idx = currentIndex.getValue();
        if (t == null || t.isEmpty() || idx == null) return -1;
        int mode = getMode();
        if (mode == 2) return (int) (Math.random() * t.size());
        if (mode == 1) return idx;
        return (idx + 1) % t.size();
    }
    public int getPrevIndex() {
        List<TrackInfo> t = tracks.getValue();
        Integer idx = currentIndex.getValue();
        if (t == null || t.isEmpty() || idx == null) return -1;
        return (idx - 1 + t.size()) % t.size();
    }
    public void setCurrentIndex(int i) { currentIndex.postValue(i); updateFavorite(); }
    public void setTracks(List<TrackInfo> list) {
        tracks.postValue(new ArrayList<>(list));
        trackCount.postValue(list.size() + " 首");
    }

    // ── 扫描 ──
    public void scanMusic(Context ctx) {
        executor.execute(() -> {
            Set<String> seen = new LinkedHashSet<>();
            List<TrackInfo> found = new ArrayList<>();
            String[] proj = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DATA};
            Cursor c = ctx.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    proj, MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String p = c.getString(2);
                    if (p != null && seen.add(p))
                        found.add(new TrackInfo(nn(c.getString(0)), nn(c.getString(1)), p));
                }
                c.close();
            }
            for (File dir : new File[]{Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    new File("/sdcard/Download"), new File("/sdcard/Music")}) {
                if (dir != null && dir.isDirectory()) {
                    File[] fs = dir.listFiles();
                    if (fs != null) for (File f : fs)
                        if (f.getName().toLowerCase().endsWith(".mp3")
                                && seen.add(f.getAbsolutePath()))
                            found.add(new TrackInfo(
                                    f.getName().replace(".mp3", "").replace(".MP3", ""),
                                    "本地", f.getAbsolutePath()));
                }
            }
            setTracks(found);
            if (!found.isEmpty() && (currentIndex.getValue() == null
                    || currentIndex.getValue() < 0)) {
                selectNoPlay(0);
            }
            scanStatus.postValue(found.isEmpty() ? "0 首" : "");
        });
    }

    // ── 收藏 ──
    public void toggleFavorite() {
        TrackInfo t = getCurrentTrack();
        if (t == null) return;
        synchronized (favSet) {
            if (favSet.contains(t.path)) favSet.remove(t.path);
            else favSet.add(t.path);
        }
        favPrefs.edit().putStringSet("favs", new LinkedHashSet<>(favSet)).apply();
        synchronized (favSet) {
            isFavorited.postValue(favSet.contains(t.path));
        }
    }
    public boolean isFavorite(String path) {
        synchronized (favSet) { return favSet.contains(path); }
    }
    private void updateFavorite() {
        TrackInfo t = getCurrentTrack();
        synchronized (favSet) {
            isFavorited.postValue(t != null && favSet.contains(t.path));
        }
    }

    // ── 均衡器 ──
    public void setEqPreset(int p) { eqPreset.postValue(p); }

    // ── 步频 ──
    public void setCadence(int spm) { cadence.postValue(spm); }

    // ── 自动播放标志 ──
    public LiveData<Boolean> getAutoPlay() { return autoPlay; }
    public void selectAndPlay(int idx) {
        autoPlay.postValue(true);
        currentIndex.postValue(idx);
    }
    public void selectNoPlay(int idx) {
        autoPlay.postValue(false);
        currentIndex.postValue(idx);
    }
    public void clearAutoPlay() { autoPlay.postValue(false); }

    // ── 数据类 ──
    public static class TrackInfo {
        public final String title, artist, path;
        public TrackInfo(String t, String a, String p) { title = t; artist = a; path = p; }
    }

    private static String nn(String s) { return s != null ? s : "未知"; }

    @Override protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
