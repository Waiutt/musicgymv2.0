package com.example.musicgym;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 音乐Fragment — MVVM重构版。状态管理委托给MusicViewModel，Fragment仅负责UI+Audio。 */
public class MusicFragment extends Fragment {

    // ── 播放模式 ──
    private static final int MODE_SEQUENTIAL = 0;
    private static final int MODE_REPEAT_ONE = 1;
    private static final int MODE_SHUFFLE = 2;

    private MusicViewModel vm;
    private MediaPlayer mediaPlayer;
    private ImageView ivAlbum;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvTrackCount;
    private TextView btnShuffle, btnRescan, btnModeLabel, btnFavorite;
    private SeekBar seekBar;
    private ImageButton btnPlay, btnPrev, btnNext;
    private LinearLayout playlistContainer;
    private ViewGroup panel;
    private View dragHandle;
    private ScrollView playlistScroll;
    private EditText etSearch;
    private TextView tvEmptyHint;
    private TextView btnEq;

    private EqualizerManager eqManager;
    private ObjectAnimator recordAnimator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean hasAudioFocus;
    private StepCadenceDetector cadenceDetector;
    private TextView tvCadence;
    private SongBpmDatabase bpmDb;
    private BpmMatchEngine bpmEngine;

    private int panelExpandedHeight, panelCollapsedHeight;
    private boolean panelExpanded;
    private float dragStartY, panelStartHeight;
    private static final int COLLAPSED_DP = 48;

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

    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) vm.scanMusic(requireContext()); else {
                    vm.setTrack("需要存储权限", "无法访问音频");
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music, container, false);
        vm = new ViewModelProvider(this).get(MusicViewModel.class);

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
        btnEq = view.findViewById(R.id.music_btn_eq);
        btnFavorite = view.findViewById(R.id.music_btn_favorite);
        btnFavorite.setOnClickListener(v -> vm.toggleFavorite());
        tvTrackCount = view.findViewById(R.id.music_track_count);
        playlistContainer = view.findViewById(R.id.music_playlist_container);
        panel = view.findViewById(R.id.music_panel);
        dragHandle = view.findViewById(R.id.music_drag_handle);
        playlistScroll = view.findViewById(R.id.music_playlist_scroll);
        etSearch = view.findViewById(R.id.music_search);
        tvEmptyHint = view.findViewById(R.id.music_empty_hint);

        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setOnAudioFocusChangeListener(this::handleAudioFocusChange).build();
        }

        requireContext().bindService(new Intent(requireContext(), MusicService.class),
                serviceConn, Context.BIND_AUTO_CREATE);

        // 步频显示
        tvCadence = new TextView(requireContext());
        tvCadence.setTextColor(ColorTokens.TEXT_MUTED);
        tvCadence.setTextSize(12f); tvCadence.setGravity(Gravity.CENTER);
        tvCadence.setVisibility(View.GONE);
        ViewGroup playerArea = (ViewGroup) view.findViewById(R.id.music_player_area);
        View artistRef = view.findViewById(R.id.music_tv_artist);
        playerArea.addView(tvCadence, playerArea.indexOfChild(artistRef) + 1);

        cadenceDetector = new StepCadenceDetector(requireContext());
        bpmDb = new SongBpmDatabase(requireContext());
        bpmEngine = new BpmMatchEngine(bpmDb, this);
        cadenceDetector.setListener(bpmEngine);

        // ── 提拉面板 ──
        panelCollapsedHeight = dp(COLLAPSED_DP);
        panel.post(() -> {
            panelExpandedHeight = (int) (view.getHeight() * 0.55);
            ViewGroup.LayoutParams lp = panel.getLayoutParams();
            lp.height = panelCollapsedHeight; panel.setLayoutParams(lp);
            panelExpanded = false;
        });
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY; private boolean isDragging;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX(); downY = event.getY();
                        dragStartY = event.getRawY(); panelStartHeight = panel.getHeight();
                        isDragging = false; dragHandle.setAlpha(0.7f); return true;
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
                    case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                        dragHandle.setAlpha(1f);
                        if (isDragging) {
                            int curH = panel.getHeight();
                            int mid = (panelCollapsedHeight + panelExpandedHeight) / 2;
                            animatePanel(curH > mid ? panelExpandedHeight : panelCollapsedHeight);
                        } else animatePanel(panelExpanded ? panelCollapsedHeight : panelExpandedHeight);
                        isDragging = false; return true;
                }
                return false;
            }
        });

        recordAnimator = ObjectAnimator.ofFloat(ivAlbum, "rotation", 0f, 360f);
        recordAnimator.setDuration(8000); recordAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        recordAnimator.setInterpolator(new LinearInterpolator());

        // ── 按钮 ──
        btnPlay.setOnClickListener(v -> togglePlayPause());
        btnPrev.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnShuffle.setOnClickListener(v -> cyclePlayMode());
        btnRescan.setOnClickListener(v -> { vm.scanMusic(requireContext()); });
        btnEq.setOnClickListener(v -> showEqualizerDialog());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                buildPlaylistUI(s.toString().trim().toLowerCase());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ── MVVM 观察 ──
        observeViewModel();

        checkPermissionAndScan();
        return view;
    }

    // ═══════════ MVVM 观察 ═══════════

    private void observeViewModel() {
        vm.getTitle().observe(getViewLifecycleOwner(), t -> tvTitle.setText(t));
        vm.getArtist().observe(getViewLifecycleOwner(), a -> tvArtist.setText(a));
        vm.getTrackCount().observe(getViewLifecycleOwner(), c -> tvTrackCount.setText(c));
        vm.getIsPlaying().observe(getViewLifecycleOwner(), p -> {
            btnPlay.setImageResource(p != null && p ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
        });
        vm.getPlayMode().observe(getViewLifecycleOwner(), m -> {
            String[] labels = {"🔁 顺序", "🔂 单曲", "🔀 随机"};
            btnModeLabel.setText(labels[m != null ? m : 0]);
            btnModeLabel.setTextColor(m != null && m > 0 ? ColorTokens.BRAND_ORANGE
                    : ColorTokens.TEXT_SECONDARY);
        });
        vm.getIsFavorited().observe(getViewLifecycleOwner(), f ->
                btnFavorite.setText(f != null && f ? "❤️" : "🤍"));
        vm.getCadence().observe(getViewLifecycleOwner(), s -> {
            if (s != null && s > 0 && tvCadence != null)
                tvCadence.setText("🏃 " + s + " spm");
        });
        vm.getTracks().observe(getViewLifecycleOwner(), t -> {
            buildPlaylistUI(etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "");
        });
        vm.getCurrentIndex().observe(getViewLifecycleOwner(), idx -> {
            if (idx != null && idx >= 0) {
                boolean auto = Boolean.TRUE.equals(vm.getAutoPlay().getValue());
                loadTrack(idx, auto);
                vm.clearAutoPlay();
            }
        });
    }

    // ═══════════ 音频焦点 ═══════════

    private boolean requestAudioFocus() {
        if (audioManager == null) return true;
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null)
            result = audioManager.requestAudioFocus(focusRequest);
        else result = audioManager.requestAudioFocus(focusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return hasAudioFocus;
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null)
            audioManager.abandonAudioFocusRequest(focusRequest);
        else audioManager.abandonAudioFocus(focusChangeListener);
        hasAudioFocus = false;
    }

    private void handleAudioFocusChange(int change) {
        switch (change) {
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) togglePlayPause();
                abandonAudioFocus(); break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) togglePlayPause(); break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null) mediaPlayer.setVolume(0.3f, 0.3f); break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    if (!mediaPlayer.isPlaying() && hasAudioFocus) startPlayback();
                }
                break;
        }
    }

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener =
            this::handleAudioFocusChange;

    // ═══════════ 播放模式 ═══════════

    private void cyclePlayMode() {
        vm.cycleMode();
        String[] toasts = {"顺序播放", "单曲循环", "随机播放"};
        Toast.makeText(getContext(), toasts[vm.getMode()], Toast.LENGTH_SHORT).show();
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
        if (ContextCompat.checkSelfPermission(requireContext(), perm)
                == PackageManager.PERMISSION_GRANTED) vm.scanMusic(requireContext());
        else permLauncher.launch(perm);
    }

    // ═══════════ 歌单 UI ═══════════

    private void buildPlaylistUI() {
        buildPlaylistUI(etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "");
    }

    private void buildPlaylistUI(String filter) {
        playlistContainer.removeAllViews();
        List<MusicViewModel.TrackInfo> tracks = vm.getTrackList();
        Integer curIdx = vm.getCurrentIndex().getValue();
        boolean playing = vm.getIsPlaying().getValue() != null
                && vm.getIsPlaying().getValue();

        for (int i = 0; i < tracks.size(); i++) {
            final int idx = i;
            MusicViewModel.TrackInfo t = tracks.get(i);
            if (!filter.isEmpty() && !t.title.toLowerCase().contains(filter)
                    && !t.artist.toLowerCase().contains(filter)) continue;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(12, 10, 8, 10);
            boolean isCurrent = curIdx != null && idx == curIdx;
            row.setBackgroundColor(isCurrent ? ColorTokens.CURRENT_TRACK_BG : Color.TRANSPARENT);

            TextView tvIdx = new TextView(getContext());
            boolean fv = vm.isFavorite(t.path);
            tvIdx.setText(fv ? "❤" : (isCurrent && playing ? "▶" : String.valueOf(idx + 1)));
            tvIdx.setTextColor(fv ? ColorTokens.ACCENT_RED : (isCurrent ? ColorTokens.BRAND_ORANGE : ColorTokens.TEXT_HINT));
            tvIdx.setTextSize(fv ? 9f : 11f); tvIdx.setGravity(Gravity.CENTER);
            tvIdx.setLayoutParams(new LinearLayout.LayoutParams(32, WRAP)); row.addView(tvIdx);

            LinearLayout info = new LinearLayout(getContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1));
            TextView tvT = new TextView(getContext());
            tvT.setText(t.title); tvT.setTextColor(Color.WHITE); tvT.setTextSize(14f);
            tvT.setSingleLine(true); tvT.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(tvT);
            TextView tvA = new TextView(getContext());
            tvA.setText(t.artist); tvA.setTextColor(ColorTokens.TEXT_SECONDARY); tvA.setTextSize(11f);
            tvA.setSingleLine(true); tvA.setEllipsize(android.text.TextUtils.TruncateAt.END);
            info.addView(tvA); row.addView(info);

            TextView btnDel = new TextView(getContext());
            btnDel.setText("✕"); btnDel.setTextColor(ColorTokens.ACCENT_RED); btnDel.setTextSize(14f);
            btnDel.setPadding(12, 8, 8, 8);
            btnDel.setLayoutParams(new LinearLayout.LayoutParams(WRAP, WRAP));
            btnDel.setOnClickListener(v -> {
                if (idx == (curIdx != null ? curIdx : -1)) {
                    if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
                    vm.setCurrentIndex(-1); abandonAudioFocus();
                    vm.setTrack("未在播放", ""); vm.setPlaying(false);
                    recordAnimator.pause(); notifyService(false);
                } else if (idx < (curIdx != null ? curIdx : -1)) vm.setCurrentIndex(curIdx - 1);
                List<MusicViewModel.TrackInfo> list = vm.getTrackList();
                list.remove(idx); vm.setTracks(list);
                if (list.isEmpty()) {
                    vm.setTrack("列表已清空", "点击 🔄 重新扫描");
                    tvEmptyHint.setVisibility(View.VISIBLE);
                }
                buildPlaylistUI();
            });
            row.addView(btnDel);
            row.setOnClickListener(v -> { vm.selectAndPlay(idx); });
            playlistContainer.addView(row);
        }
    }

    // ═══════════ 播放引擎 ═══════════

    private void loadTrack(int index, boolean autoPlay) {
        List<MusicViewModel.TrackInfo> tracks = vm.getTrackList();
        if (tracks.isEmpty() || index < 0 || index >= tracks.size()) return;
        if (mediaPlayer != null) { mediaPlayer.release(); abandonAudioFocus(); }

        MusicViewModel.TrackInfo track = tracks.get(index);
        vm.setTrack(track.title, track.artist);

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(track.path);
            byte[] art = mmr.getEmbeddedPicture();
            ivAlbum.setImageBitmap(art != null ? BitmapFactory.decodeByteArray(art, 0, art.length)
                    : BitmapFactory.decodeResource(getResources(), R.drawable.pic1));
        } catch (Exception e) {
            ivAlbum.setImageResource(R.drawable.pic1);
        } finally { try { mmr.release(); } catch (Exception ignored) {} }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(track.path);
            mediaPlayer.prepare();
            seekBar.setMax(mediaPlayer.getDuration());
            tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
            mediaPlayer.setOnCompletionListener(mp -> {
                int next = vm.getNextIndex();
                if (next >= 0) vm.selectAndPlay(next);
            });
            if (eqManager != null) eqManager.release();
            eqManager = new EqualizerManager(mediaPlayer.getAudioSessionId());
            if (autoPlay) startPlayback(); else vm.setPlaying(false);
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法播放", Toast.LENGTH_SHORT).show();
        }
        buildPlaylistUI();
    }

    private void startPlayback() {
        if (mediaPlayer == null) return;
        if (!requestAudioFocus()) { Toast.makeText(getContext(), "无法获取音频焦点", Toast.LENGTH_SHORT).show(); return; }
        mediaPlayer.start(); vm.setPlaying(true);
        recordAnimator.start(); updateSeekBar(); notifyService(true);
        if (cadenceDetector != null && cadenceDetector.isAvailable()) {
            cadenceDetector.start(); if (tvCadence != null) tvCadence.setVisibility(View.VISIBLE);
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause(); vm.setPlaying(false);
            recordAnimator.pause(); notifyService(false);
            cadenceDetector.stop(); if (tvCadence != null) tvCadence.setVisibility(View.GONE);
        } else {
            if (!requestAudioFocus()) return;
            mediaPlayer.start(); vm.setPlaying(true);
            if (recordAnimator.isPaused()) recordAnimator.resume(); else recordAnimator.start();
            updateSeekBar(); notifyService(true);
            if (cadenceDetector != null && cadenceDetector.isAvailable()) {
                cadenceDetector.start(); if (tvCadence != null) tvCadence.setVisibility(View.VISIBLE);
            }
        }
        buildPlaylistUI();
    }

    private void notifyService(boolean playing) {
        if (getContext() == null) return;
        // Android 13+ 前台服务需要通知权限
        if (playing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
                // 权限未授予时不启动前台服务，但仍可本地播放
                return;
            }
        }
        Intent intent = new Intent(getContext(), MusicService.class);
        intent.setAction(MusicService.ACTION_UPDATE);
        MusicViewModel.TrackInfo t = vm.getCurrentTrack();
        intent.putExtra(MusicService.EXTRA_TITLE, t != null ? t.title : "");
        intent.putExtra(MusicService.EXTRA_ARTIST, t != null ? t.artist : "");
        intent.putExtra(MusicService.EXTRA_IS_PLAYING, playing);
        if (playing) getContext().startForegroundService(intent);
        else getContext().startService(intent);
    }

    private void playNext() {
        int next = vm.getNextIndex();
        if (next >= 0) vm.selectAndPlay(next);
    }

    private void playPrevious() {
        int prev = vm.getPrevIndex();
        if (prev >= 0) vm.selectAndPlay(prev);
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

    // ═══════════ 均衡器 ═══════════

    private void showEqualizerDialog() {
        if (eqManager == null || !eqManager.isAvailable()) {
            Toast.makeText(getContext(), "均衡器不可用（需播放中）", Toast.LENGTH_SHORT).show(); return;
        }
        String[] presets = {"🏃 跑步", "🏋️ 举铁", "🧘 拉伸", "🎤 人声", "🎛 自定义"};
        int current = vm.getEqPreset().getValue() != null ? vm.getEqPreset().getValue() : 0;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("均衡器")
                .setSingleChoiceItems(presets, current, (d, which) -> {
                    eqManager.applyPreset(which); vm.setEqPreset(which); d.dismiss();
                    if (which == EqualizerManager.PRESET_CUSTOM) showCustomEqDialog();
                }).show();
    }

    private void showCustomEqDialog() {
        if (eqManager == null || !eqManager.isAvailable()) return;
        short bands = eqManager.getNumberOfBands();
        if (bands == 0) { Toast.makeText(getContext(), "设备不支持均衡器", Toast.LENGTH_SHORT).show(); return; }
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL); container.setPadding(40, 20, 40, 10);
        short[] range = eqManager.getBandLevelRange();
        for (short i = 0; i < bands; i++) {
            final short band = i;
            int freq = eqManager.getCenterFreq(i) / 1000;
            TextView label = new TextView(requireContext());
            label.setText((freq < 1 ? freq * 1000 + "Hz" : freq + "kHz") + "  (" + eqManager.getBandLevel(i) + ")");
            label.setTextColor(Color.WHITE); label.setTextSize(12f); label.setPadding(0, 8, 0, 4);
            container.addView(label);
            SeekBar sb = new SeekBar(requireContext());
            sb.setMax(range[1] - range[0]); sb.setProgress(eqManager.getBandLevel(i) - range[0]);
            sb.setPadding(0, 0, 0, 8);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                    if (fromUser) eqManager.setBandLevel(band, (short) (p + range[0]));
                }
                @Override public void onStartTrackingTouch(SeekBar s) {}
                @Override public void onStopTrackingTouch(SeekBar s) {}
            });
            container.addView(sb);
        }
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("自定义均衡器").setView(container).setPositiveButton("完成", null).show();
    }

    // ═══════════ BPM 匹配 ═══════════

    public void onBpmMatchFound(String trackPath, int cadence) {
        List<MusicViewModel.TrackInfo> tracks = vm.getTrackList();
        Integer curIdx = vm.getCurrentIndex().getValue();
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).path.equals(trackPath) && (curIdx == null || i != curIdx)) {
                vm.selectAndPlay(i);
                if (tvCadence != null) {
                    int bpm = bpmDb.getBpm(trackPath);
                    tvCadence.setText("🏃 " + cadence + "spm → 🎵" + bpm + "BPM");
                }
                break;
            }
        }
    }

    // ═══════════ 工具 ═══════════

    private int dp(int d) { return UiUtils.dp(getContext(), d); }
    static final int WRAP = UiUtils.WRAP;

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        if (eqManager != null) { eqManager.release(); eqManager = null; }
        if (cadenceDetector != null) cadenceDetector.stop();
        abandonAudioFocus();
        handler.removeCallbacksAndMessages(null);
        if (recordAnimator != null) recordAnimator.cancel();
        if (getContext() != null) {
            getContext().unbindService(serviceConn);
            getContext().stopService(new Intent(getContext(), MusicService.class));
        }
    }
}
