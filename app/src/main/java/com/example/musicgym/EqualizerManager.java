package com.example.musicgym;

import android.media.audiofx.Equalizer;

/** 均衡器管理器 — 5种运动预设 + 自定义频段调节 */
public class EqualizerManager {

    private Equalizer equalizer;
    private boolean enabled;
    private int audioSessionId;

    // 预设
    public static final int PRESET_RUN = 0;      // 跑步: 低音增强
    public static final int PRESET_LIFT = 1;     // 举铁: 重低音
    public static final int PRESET_STRETCH = 2;  // 拉伸: 温暖中频
    public static final int PRESET_VOCAL = 3;    // 人声: 中频提升
    public static final int PRESET_CUSTOM = 4;   // 自定义

    private int currentPreset = PRESET_RUN;

    public EqualizerManager(int audioSessionId) {
        this.audioSessionId = audioSessionId;
        try {
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            enabled = true;
        } catch (Exception e) {
            enabled = false;
            equalizer = null;
        }
    }

    public boolean isAvailable() { return enabled && equalizer != null; }
    public int getCurrentPreset() { return currentPreset; }
    public short getNumberOfBands() { return equalizer != null ? equalizer.getNumberOfBands() : (short) 0; }
    public short[] getBandLevelRange() { return equalizer != null ? equalizer.getBandLevelRange() : new short[]{0, 0}; }
    public int getCenterFreq(short band) { return equalizer != null ? equalizer.getCenterFreq(band) : 0; }
    public int getBandLevel(short band) { return equalizer != null ? equalizer.getBandLevel(band) : 0; }

    /** 应用预设 */
    public void applyPreset(int preset) {
        if (!isAvailable()) return;
        currentPreset = preset;
        resetBands();

        short bands = equalizer.getNumberOfBands();
        int max = equalizer.getBandLevelRange()[1];

        for (short i = 0; i < bands; i++) {
            int freq = equalizer.getCenterFreq(i) / 1000; // 转为 kHz
            int level = 0;

            switch (preset) {
                case PRESET_RUN:     // 跑步: 低音+8, 中音+3, 高音+2
                    level = freq < 0.25 ? max * 60 / 100 :
                            freq < 2    ? max * 20 / 100 :
                            freq < 6    ? max * 10 / 100 : 0;
                    break;
                case PRESET_LIFT:    // 举铁: 重低音+10, 高音+5
                    level = freq < 0.2 ? max * 80 / 100 :
                            freq < 1  ? max * 30 / 100 :
                            freq > 8  ? max * 35 / 100 : 0;
                    break;
                case PRESET_STRETCH: // 拉伸: 中频+4, 低频-2
                    level = freq < 0.3 ? max * -15 / 100 :
                            freq >= 1 && freq <= 3 ? max * 30 / 100 :
                            freq > 6 ? max * 10 / 100 : 0;
                    break;
                case PRESET_VOCAL:   // 人声: 中频+6
                    level = freq >= 1 && freq <= 4 ? max * 45 / 100 :
                            freq > 8  ? max * 15 / 100 : 0;
                    break;
                // PRESET_CUSTOM: 不覆盖用户手动设置
                default: return;
            }
            equalizer.setBandLevel(i, (short) level);
        }
    }

    /** 设置单个频段 */
    public void setBandLevel(short band, short level) {
        if (isAvailable()) {
            currentPreset = PRESET_CUSTOM;
            equalizer.setBandLevel(band, level);
        }
    }

    /** 全部归零 */
    public void resetBands() {
        if (!isAvailable()) return;
        short bands = equalizer.getNumberOfBands();
        for (short i = 0; i < bands; i++) equalizer.setBandLevel(i, (short) 0);
    }

    public void release() {
        if (equalizer != null) {
            equalizer.setEnabled(false);
            equalizer.release();
            equalizer = null;
        }
    }
}
