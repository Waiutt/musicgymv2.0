package com.example.musicgym;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicFragment extends Fragment {

    // ── 播放模式 ──
    private static final int MODE_SEQUENTIAL = 0;
    private static final int MODE_REPEAT_ONE = 1;
    private static final int MODE_SHUFFLE = 2;

    private MediaPlayer mediaPlayer;
    private ImageView ivAlbum;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvTrackCount;
    private TextView btnShuffle, btnRescan, btnModeLabel;
    private SeekBar seekBar;
    private ImageButton btnPlay, btnPrev, btnNext;
    private LinearLayout playlistContainer;
    private ViewGroup panel;
    private View dragHandle;
    private ScrollView playlistScroll;
    private EditText etSearch;
    private TextView tvEmptyHint;

    private final List<Track> playlist = new ArrayList<>();
    private int currentTrackIndex = -1;
    private int playMode = MODE_SEQUENTIAL;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ObjectAnimator recordAnimator;
    private ExecutorService executor;

    // ── 提拉面板 ──
    private int panelExpandedHeight, panelCollapsedHeight;
    private boolean panelExpanded;
    private float dragStartY, panelStartHeight;
    private static final int COLLAPSED_DP = 48;

    // ── 音频焦点 ──
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean hasAudioFocus;

    // ── 后台 Service ──
    private MusicService musicService;
    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            musicService = ((MusicService.LocalBinder) binder).getService();
            musicService.setCallback(new MusicService.MusicCallback() {
                @Override public void onPlayPause() { handler.post(() -> togglePlayPause()); }
                @Override public void onNext() { handler.post(() -> playNext()); }
                @Override public void onPrev() { handler.post(() -> playPrevious()); }
            });
        }
        @Override public void onServiceDisconnected(ComponentName name) { musicService = null; }
    };

    private static class Track {
        String title, artist, dataPath;
        Track(String t, String a, String p) { title = t; artist = a; dataPath = p; }
    }

    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) autoScan(); else {
                    tvTitle.setText("需要存储权限");
                    tvArtist.setText("请到设置中授予媒体访问权限");
                    tvEmptyHint.setVisibility(View.VISIBLE);
                    tvEmptyHint.setText("未授权访问音频文件\n请授予权限后点击 🔄 重新扫描");
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);

        ivAlbum = view.findViewById(R.id.music_iv_album);
        tvTitle = view.findViewById(R.id.music_tv_title);
        tvArtist = view.findViewById(R.id.music_tv_artist);
        tvCurrentTime = view.findViewById(R.id.music_tv_current_time);
        tvTotalTime = view.findViewById(R.id.music_tv_total_time);
        seekBar = view.findViewById(R.id.music_seekbar);
        btnPlay = view.findViewById(R.id.music_btn_play);
        btnPrev = view.findViewById(R.id.music_btn_prev);
        btnNext = view.findViewById(R.id.music_btn_next);
        btnShuffle = view.findViewById(R.id.music_btn_shuffle);
        btnRescan = view.findViewById(R.id.music_btn_rescan);
        btnModeLabel = view.findViewById(R.id.music_mode_label);
        tvTrackCount = view.findViewById(R.id.music_track_count);
        playlistContainer = view.findViewById(R.id.music_playlist_container);
        panel = view.findViewById(R.id.music_panel);
        dragHandle = view.findViewById(R.id.music_drag_handle);
        playlistScroll = view.findViewById(R.id.music_playlist_scroll);
        etSearch = view.findViewById(R.id.music_search);
        tvEmptyHint = view.findViewById(R.id.music_empty_hint);

        executor = Executors.newSingleThreadExecutor();

        // ── 音频焦点 ──
        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(this::handleAudioFocusChange)
                    .build();
        }

        // ── 绑定 Service ──
        requireContext().bindService(new Intent(requireContext(), MusicService.class),
                serviceConn, Context.BIND_AUTO_CREATE);

        // ── 提拉面板 ──
        panelCollapsedHeight = dp(COLLAPSED_DP);
        panel.post(() -> {
            panelExpandedHeight = (int) (view.getHeight() * 0.55);
            ViewGroup.LayoutParams lp = panel.getLayoutParams();
            lp.height = panelCollapsedHeight;
            panel.setLayoutParams(lp);
            panelExpanded = false;
        });

        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private boolean isDragging;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX(); downY = event.getY();
                        dragStartY = event.getRawY();
                        panelStartHeight = panel.getHeight();
                        isDragging = false;
                        dragHandle.setAlpha(0.7f);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getX() - downX);
                        float dy = Math.abs(event.getY() - downY);
                        if (!isDragging && (dx > 10 || dy > 5)) isDragging = true;
                        if (isDragging) {
                            float moveDy = dragStartY - event.getRawY();
                            int newH = (int) (panelStartHeight + moveDy);
                            newH = Math.max(panelCollapsedHeight, Math.min(newH, panelExpandedHeight));
                            setPanelHeight(newH);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        dragHandle.setAlpha(1f);
                        if (isDragging) {
                            int curH = panel.getHeight();
                            int mid = (panelCollapsedHeight + panelExpandedHeight) / 2;
                            animatePanel(curH > mid ? panelExpandedHeight : panelCollapsedHeight);
                        } else {
                            animatePanel(panelExpanded ? panelCollapsedHeight : panelExpandedHeight);
                        }
                        isDragging = false;
                        return true;
                }
                return false;
            }
        });

        // ── 唱片旋转动画 ──
        recordAnimator = ObjectAnimator.ofFloat(ivAlbum, "rotation", 0f, 360f);
        recordAnimator.setDuration(8000);
        recordAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        recordAnimator.setInterpolator(new LinearInterpolator());

        // ── 按钮事件 ──
        btnPlay.setOnClickListener(v -> togglePlayPause());
        btnPrev.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnShuffle.setOnClickListener(v -> cyclePlayMode());
        btnRescan.setOnClickListener(v -> { tvTitle.setText("正在扫描..."); tvArtist.setText("搜索所有音乐"); autoScan(); });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // ── 歌单搜索 ──
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                buildPlaylistUI(s.toString().trim().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        checkPermissionAndScan();
        return view;
    }

    // ═══════════ 音频焦点 ═══════════

    private boolean requestAudioFocus() {
        if (audioManager == null) return true;
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(focusChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return hasAudioFocus;
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        hasAudioFocus = false;
    }

    private void handleAudioFocusChange(int change) {
        switch (change) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // 永久失去焦点（如电话来了）
                if (mediaPlayer != null && mediaPlayer.isPlaying()) togglePlayPause();
                abandonAudioFocus();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // 短暂失去焦点（如通知音）
                if (mediaPlayer != null && mediaPlayer.isPlaying()) togglePlayPause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // 需要降低音量
                if (mediaPlayer != null) mediaPlayer.setVolume(0.3f, 0.3f);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                // 恢复焦点
                if (mediaPlayer != null) mediaPlayer.setVolume(1.0f, 1.0f);
                if (!mediaPlayer.isPlaying() && hasAudioFocus) {
                    mediaPlayer.start(); btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                    if (recordAnimator.isPaused()) recordAnimator.resume(); else recordAnimator.start();
                    updateSeekBar(); notifyService(true);
                }
                break;
        }
    }

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener =
            this::handleAudioFocusChange;

    // ═══════════ 播放模式 ═══════════

    private void cyclePlayMode() {
        playMode = (playMode + 1) % 3;
        String[] labels = {"🔁 顺序", "🔂 单曲", "🔀 随机"};
        String[] toasts = {"顺序播放", "单曲循环", "随机播放"};
        btnModeLabel.setText(labels[playMode]);
        btnModeLabel.setTextColor(playMode > 0 ? ColorTokens.BRAND_ORANGE : ColorTokens.TEXT_SECONDARY);
        Toast.makeText(getContext(), toasts[playMode], Toast.LENGTH_SHORT).show();
    }

    private int getNextIndex() {
        if (playlist.isEmpty()) return -1;
        switch (playMode) {
            case MODE_SHUFFLE:
                return (int) (Math.random() * playlist.size());
            case MODE_REPEAT_ONE:
                return currentTrackIndex;
            default: // MODE_SEQUENTIAL
                return (currentTrackIndex + 1) % playlist.size();
        }
    }

    // ═══════════ 提拉面板 ═══════════

    private void setPanelHeight(int h) {
        ViewGroup.LayoutParams lp = panel.getLayoutParams();
        lp.height = h; panel.setLayoutParams(lp);
    }

    private void animatePanel(int targetH) {
        ValueAnimator va = ValueAnimator.ofInt(panel.getHeight(), targetH);
        va.setDuration(300);
        va.setInterpolator(new android.view.animation.DecelerateInterpolator(2.5f));
        va.addUpdateListener(a -> setPanelHeight((int) a.getAnimatedValue()));
        va.start();
        panelExpanded = targetH == panelExpandedHeight;
    }

    // ═══════════ 扫描 ═══════════

    private void checkPermissionAndScan() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), perm) == PackageManager.PERMISSION_GRANTED)
            autoScan(); else permLauncher.launch(perm);
    }

    private void autoScan() {
        executor.execute(() -> {
            Set<String> seen = new LinkedHashSet<>();
            List<Track> found = new ArrayList<>();
            String[] proj = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA};
            Cursor c = requireContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
                    MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String p = c.getString(2);
                    if (p != null && seen.add(p))
                        found.add(new Track(nn(c.getString(0)), nn(c.getString(1)), p));
                }
                c.close();
            }
            for (File dir : new File[]{
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    new File("/sdcard/Download"), new File("/sdcard/Music")}) {
                if (dir != null && dir.isDirectory()) {
                    File[] fs = dir.listFiles();
                    if (fs != null) for (File f : fs)
                        if (f.getName().toLowerCase().endsWith(".mp3") && seen.add(f.getAbsolutePath()))
                            found.add(new Track(f.getName().replace(".mp3","").replace(".MP3",""), "本地", f.getAbsolutePath()));
                }
            }
            requireActivity().runOnUiThread(() -> {
                playlist.clear(); playlist.addAll(found);
                tvTrackCount.setText(playlist.size() + " 首");
                if (playlist.isEmpty()) {
                    tvEmptyHint.setVisibility(View.VISIBLE);
                    tvEmptyHint.setText("没有找到音乐文件\n请将 MP3 放入 Music 或 Download 文件夹\n然后点击 🔄 重新扫描");
                } else {
                    tvEmptyHint.setVisibility(View.GONE);
                }
                if (!playlist.isEmpty() && currentTrackIndex < 0) { currentTrackIndex = 0; loadTrack(0, false); }
                buildPlaylistUI("");
            });
        });
    }

    private String nn(String s) { return s != null ? s : "未知"; }

    // ═══════════ 歌单 UI（含搜索过滤） ═══════════

    private void buildPlaylistUI() {
        buildPlaylistUI(etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "");
    }

    private void buildPlaylistUI(String filter) {
        playlistContainer.removeAllViews();
        for (int i = 0; i < playlist.size(); i++) {
            final int idx = i;
            Track t = playlist.get(i);

            // 搜索过滤
            if (!filter.isEmpty() && !t.title.toLowerCase().contains(filter)
                    && !t.artist.toLowerCase().contains(filter)) continue;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(12, 10, 8, 10);
            boolean isCurrent = idx == currentTrackIndex;
            row.setBackgroundColor(isCurrent ? ColorTokens.CURRENT_TRACK_BG : Color.TRANSPARENT);

            TextView tvIdx = new TextView(getContext());
            tvIdx.setText(isCurrent && mediaPlayer != null && mediaPlayer.isPlaying() ? "▶" : String.valueOf(idx + 1));
            tvIdx.setTextColor(isCurrent ? ColorTokens.BRAND_ORANGE : ColorTokens.TEXT_HINT);
            tvIdx.setTextSize(11f); tvIdx.setGravity(Gravity.CENTER);
            tvIdx.setLayoutParams(new LinearLayout.LayoutParams(32, WRAP));
            row.addView(tvIdx);

            LinearLayout info = new LinearLayout(getContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1));
            TextView tvT = new TextView(getContext());
            tvT.setText(t.title); tvT.setTextColor(Color.WHITE); tvT.setTextSize(14f); tvT.setSingleLine(true);
            info.addView(tvT);
            TextView tvA = new TextView(getContext());
            tvA.setText(t.artist); tvA.setTextColor(ColorTokens.TEXT_SECONDARY); tvA.setTextSize(11f); tvA.setSingleLine(true);
            info.addView(tvA);
            row.addView(info);

            TextView btnDel = new TextView(getContext());
            btnDel.setText("✕"); btnDel.setTextColor(ColorTokens.ACCENT_RED); btnDel.setTextSize(14f);
            btnDel.setGravity(Gravity.CENTER); btnDel.setPadding(12, 8, 8, 8);
            btnDel.setLayoutParams(new LinearLayout.LayoutParams(WRAP, WRAP));
            btnDel.setOnClickListener(v -> {
                if (idx == currentTrackIndex) {
                    if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
                    currentTrackIndex = -1; abandonAudioFocus();
                    tvTitle.setText("未在播放"); tvArtist.setText("");
                    btnPlay.setImageResource(android.R.drawable.ic_media_play);
                    recordAnimator.pause(); notifyService(false);
                } else if (idx < currentTrackIndex) currentTrackIndex--;
                playlist.remove(idx);
                tvTrackCount.setText(playlist.size() + " 首");
                if (playlist.isEmpty()) {
                    tvTitle.setText("列表已清空"); tvArtist.setText("点击 🔄 重新扫描");
                    tvEmptyHint.setVisibility(View.VISIBLE);
                    tvEmptyHint.setText("列表已清空\n点击 🔄 重新扫描本地音乐");
                }
                buildPlaylistUI();
            });
            row.addView(btnDel);
            row.setOnClickListener(v -> { currentTrackIndex = idx; loadTrack(idx, true); });
            playlistContainer.addView(row);
        }
    }

    // ═══════════ 播放引擎 ═══════════

    private void loadTrack(int index, boolean autoPlay) {
        if (playlist.isEmpty()) return;
        if (mediaPlayer != null) { mediaPlayer.release(); abandonAudioFocus(); }

        Track track = playlist.get(index);
        tvTitle.setText(track.title); tvArtist.setText(track.artist);

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(track.dataPath);
            byte[] art = mmr.getEmbeddedPicture();
            ivAlbum.setImageBitmap(art != null ? BitmapFactory.decodeByteArray(art, 0, art.length) :
                    BitmapFactory.decodeResource(getResources(), R.drawable.pic1));
        } catch (Exception e) {
            ivAlbum.setImageResource(R.drawable.pic1);
        } finally { try { mmr.release(); } catch (Exception ignored) {} }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(track.dataPath);
            mediaPlayer.prepare();
            seekBar.setMax(mediaPlayer.getDuration());
            tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
            mediaPlayer.setOnCompletionListener(mp -> {
                if (playlist.isEmpty()) return;
                currentTrackIndex = getNextIndex();
                loadTrack(currentTrackIndex, true);
            });
            if (autoPlay) startPlayback(); else btnPlay.setImageResource(android.R.drawable.ic_media_play);
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法播放", Toast.LENGTH_SHORT).show();
        }
        buildPlaylistUI();
    }

    private void startPlayback() {
        if (mediaPlayer == null) return;
        if (!requestAudioFocus()) {
            Toast.makeText(getContext(), "无法获取音频焦点", Toast.LENGTH_SHORT).show();
            return;
        }
        mediaPlayer.start();
        btnPlay.setImageResource(android.R.drawable.ic_media_pause);
        recordAnimator.start();
        updateSeekBar();
        notifyService(true);
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause(); btnPlay.setImageResource(android.R.drawable.ic_media_play);
            recordAnimator.pause(); notifyService(false);
        } else {
            if (!requestAudioFocus()) return;
            mediaPlayer.start(); btnPlay.setImageResource(android.R.drawable.ic_media_pause);
            if (recordAnimator.isPaused()) recordAnimator.resume(); else recordAnimator.start();
            updateSeekBar(); notifyService(true);
        }
        buildPlaylistUI();
    }

    private void notifyService(boolean playing) {
        if (getContext() == null) return;
        Intent intent = new Intent(getContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_UPDATE);
        Track t = currentTrackIndex >= 0 && currentTrackIndex < playlist.size()
                ? playlist.get(currentTrackIndex) : null;
        intent.putExtra(MusicService.EXTRA_TITLE, t != null ? t.title : "");
        intent.putExtra(MusicService.EXTRA_ARTIST, t != null ? t.artist : "");
        intent.putExtra(MusicService.EXTRA_IS_PLAYING, playing);
        if (playing) getContext().startForegroundService(intent);
        else getContext().startService(intent);
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        currentTrackIndex = getNextIndex();
        loadTrack(currentTrackIndex, true);
    }

    private void playPrevious() {
        if (playlist.isEmpty()) return;
        currentTrackIndex = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
        loadTrack(currentTrackIndex, true);
    }

    private void updateSeekBar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
            handler.postDelayed(this::updateSeekBar, 1000);
        }
    }

    private String formatTime(int ms) { int s = (ms / 1000) % 60, m = (ms / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s); }

    private int dp(int d) { return UiUtils.dp(getContext(), d); }
    static final int WRAP = UiUtils.WRAP;

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        abandonAudioFocus();
        handler.removeCallbacksAndMessages(null);
        if (recordAnimator != null) recordAnimator.cancel();
        executor.shutdown();
        if (getContext() != null) {
            getContext().unbindService(serviceConn);
            getContext().stopService(new Intent(getContext(), MusicService.class));
        }
    }
}
