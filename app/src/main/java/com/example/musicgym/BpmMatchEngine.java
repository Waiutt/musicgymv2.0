package com.example.musicgym;

/**
 * BPM 匹配引擎 — 连接步频检测器和歌曲BPM数据库.
 * 当步频变化 > 5% 时，自动从数据库找最匹配BPM的歌曲.
 */
public class BpmMatchEngine implements StepCadenceDetector.CadenceListener {

    private final SongBpmDatabase bpmDb;
    private final MusicFragment fragment;
    private int lastCadence;
    private boolean enabled;
    private long lastSwitchTime;

    private static final int MIN_SWITCH_INTERVAL_MS = 15000; // 最少15秒才能再次切歌
    private static final float CADENCE_CHANGE_THRESHOLD = 0.08f; // 步频变化8%才触发

    public BpmMatchEngine(SongBpmDatabase bpmDb, MusicFragment frag) {
        this.bpmDb = bpmDb;
        this.fragment = frag;
    }

    public void setEnabled(boolean en) { this.enabled = en; }
    public boolean isEnabled() { return enabled; }

    /** 手动设置当前歌曲的 BPM */
    public void setCurrentTrackBpm(String trackPath, int bpm) {
        bpmDb.setBpm(trackPath, bpm);
    }

    /** 获取当前歌曲的 BPM */
    public int getTrackBpm(String trackPath) {
        return bpmDb.getBpm(trackPath);
    }

    // ── CadenceListener ──

    @Override
    public void onCadenceChanged(int cadence) {
        if (!enabled || cadence <= 0) return;

        // 步频变化需要超过阈值
        if (lastCadence > 0 && Math.abs(cadence - lastCadence)
                < lastCadence * CADENCE_CHANGE_THRESHOLD) {
            return;
        }

        // 最少间隔
        long now = System.currentTimeMillis();
        if (now - lastSwitchTime < MIN_SWITCH_INTERVAL_MS) return;

        lastCadence = cadence;

        // 在后台线程找最匹配的歌
        String bestTrack = bpmDb.findBestMatch(cadence);
        if (bestTrack != null) {
            lastSwitchTime = now;
            fragment.onBpmMatchFound(bestTrack, cadence);
        }
    }
}
