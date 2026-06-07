package com.example.musicgym;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/**
 * 歌曲 BPM 数据库 — 使用 SharedPreferences 存储.
 * 每个歌曲 path → BPM 值.
 * 未来可升级到 Room 表或从 Spotify API 批量获取.
 */
public class SongBpmDatabase {

    private static final String PREFS_NAME = "song_bpm_db";
    private final SharedPreferences prefs;

    public SongBpmDatabase(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** 设置歌曲 BPM */
    public void setBpm(String trackPath, int bpm) {
        prefs.edit().putInt(sanitize(trackPath), bpm).apply();
    }

    /** 获取歌曲 BPM，未知返回 -1 */
    public int getBpm(String trackPath) {
        return prefs.getInt(sanitize(trackPath), -1);
    }

    /** 获取所有已知 BPM 的歌曲 */
    public Map<String, Integer> getAllBpms() {
        Map<String, Integer> map = new HashMap<>();
        for (String key : prefs.getAll().keySet()) {
            map.put(key, prefs.getInt(key, -1));
        }
        return map;
    }

    /** 查找 BPM 在 [target*0.95, target*1.05] 范围内的歌曲路径 */
    public String findBestMatch(int targetBpm) {
        int bestDiff = Integer.MAX_VALUE;
        String bestTrack = null;

        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            int bpm = (Integer) e.getValue();
            if (bpm <= 0) continue;
            int diff = Math.abs(bpm - targetBpm);
            // 允许 ±5% 或半步频匹配
            boolean inRange = Math.abs(bpm - targetBpm) <= targetBpm * 0.05;
            boolean halfMatch = Math.abs(bpm * 2 - targetBpm) <= targetBpm * 0.05
                    || Math.abs(bpm - targetBpm * 2) <= targetBpm * 0.05;
            if ((inRange || halfMatch) && diff < bestDiff) {
                bestDiff = diff;
                bestTrack = e.getKey();
            }
        }
        return bestTrack;
    }

    private String sanitize(String path) {
        return path.replace(".", "_").replace("/", "_").replace(" ", "_");
    }
}
