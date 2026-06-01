package com.example.musicgym;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
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

    private MediaPlayer mediaPlayer;
    private ImageView ivAlbum;
    private TextView tvTitle, tvArtist, tvCurrentTime, tvTotalTime, tvPlaylistTitle, tvTrackCount;
    private TextView btnShuffle, btnRescan;
    private SeekBar seekBar;
    private ImageButton btnPlay, btnPrev, btnNext;
    private LinearLayout playlistContainer;
    private FrameLayout panel;
    private View dragHandle;
    private ScrollView playlistScroll;

    private final List<Track> playlist = new ArrayList<>();
    private int currentTrackIndex = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ObjectAnimator recordAnimator;
    private ExecutorService executor;
    private boolean isShuffle;

    // 提拉面板状态
    private int panelExpandedHeight, panelCollapsedHeight;
    private boolean panelExpanded;
    private float dragStartY, panelStartHeight;
    private static final int COLLAPSED_DP = 48;

    private static class Track {
        String title, artist, dataPath;
        Track(String t, String a, String p) { title = t; artist = a; dataPath = p; }
    }

    private final ActivityResultLauncher<String> permLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) autoScan(); else {
                    tvTitle.setText("需要存储权限"); tvArtist.setText("无法访问音频");
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        tvPlaylistTitle = view.findViewById(R.id.music_playlist_title);
        tvTrackCount = view.findViewById(R.id.music_track_count);
        playlistContainer = view.findViewById(R.id.music_playlist_container);
        panel = view.findViewById(R.id.music_panel);
        dragHandle = view.findViewById(R.id.music_drag_handle);
        playlistScroll = view.findViewById(R.id.music_playlist_scroll);

        executor = Executors.newSingleThreadExecutor();

        panelCollapsedHeight = dp(COLLAPSED_DP);
        panel.post(() -> {
            panelExpandedHeight = (int) (view.getHeight() * 0.55);
            ViewGroup.LayoutParams lp = panel.getLayoutParams();
            lp.height = panelCollapsedHeight;
            panel.setLayoutParams(lp);
            panelExpanded = false;
        });

        // 提拉手势 + 点击切换
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private boolean isDragging;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX(); downY = event.getY();
                        dragStartY = event.getRawY();
                        panelStartHeight = panel.getHeight();
                        isDragging = false;
                        // 按下时把手变亮
                        dragHandle.setAlpha(0.7f);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getX() - downX);
                        float dy = Math.abs(event.getY() - downY);
                        if (!isDragging && (dx > 10 || dy > 5)) {
                            isDragging = true;
                        }
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
                            // 点击切换
                            animatePanel(panelExpanded ? panelCollapsedHeight : panelExpandedHeight);
                        }
                        isDragging = false;
                        return true;
                }
                return false;
            }
        });

        recordAnimator = ObjectAnimator.ofFloat(ivAlbum, "rotation", 0f, 360f);
        recordAnimator.setDuration(8000);
        recordAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        recordAnimator.setInterpolator(new LinearInterpolator());

        btnPlay.setOnClickListener(v -> togglePlayPause());
        btnPrev.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnShuffle.setOnClickListener(v -> {
            isShuffle = !isShuffle;
            Toast.makeText(getContext(), isShuffle ? "随机播放" : "顺序播放", Toast.LENGTH_SHORT).show();
        });
        btnRescan.setOnClickListener(v -> {
            tvTitle.setText("正在扫描..."); tvArtist.setText("搜索所有音乐");
            autoScan();
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        checkPermissionAndScan();
        return view;
    }

    // ======= 提拉面板 =======
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

    // ======= 扫描 =======
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
                if (!playlist.isEmpty() && currentTrackIndex < 0) { currentTrackIndex = 0; loadTrack(0, false); }
                buildPlaylistUI();
            });
        });
    }

    private String nn(String s) { return s != null ? s : "未知"; }

    // ======= 列表 UI =======
    private void buildPlaylistUI() {
        playlistContainer.removeAllViews();
        for (int i = 0; i < playlist.size(); i++) {
            final int idx = i;
            Track t = playlist.get(i);

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(12, 10, 8, 10);
            boolean isCurrent = idx == currentTrackIndex;
            row.setBackgroundColor(isCurrent ? Color.parseColor("#1aFC4C02") : Color.TRANSPARENT);

            // 序号 / 播放图标
            TextView tvIdx = new TextView(getContext());
            tvIdx.setText(isCurrent && mediaPlayer != null && mediaPlayer.isPlaying() ? "▶" : String.valueOf(idx + 1));
            tvIdx.setTextColor(isCurrent ? Color.parseColor("#FC4C02") : Color.parseColor("#6b7280"));
            tvIdx.setTextSize(11f); tvIdx.setGravity(Gravity.CENTER);
            tvIdx.setLayoutParams(new LinearLayout.LayoutParams(32, WRAP));
            row.addView(tvIdx);

            // 歌曲信息
            LinearLayout info = new LinearLayout(getContext());
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, WRAP, 1));

            TextView tvT = new TextView(getContext());
            tvT.setText(t.title); tvT.setTextColor(Color.WHITE); tvT.setTextSize(14f); tvT.setSingleLine(true);
            info.addView(tvT);

            TextView tvA = new TextView(getContext());
            tvA.setText(t.artist); tvA.setTextColor(Color.parseColor("#9ca3af")); tvA.setTextSize(11f); tvA.setSingleLine(true);
            info.addView(tvA);

            row.addView(info);

            // 删除按钮
            TextView btnDel = new TextView(getContext());
            btnDel.setText("✕"); btnDel.setTextColor(Color.parseColor("#ef4444")); btnDel.setTextSize(14f);
            btnDel.setGravity(Gravity.CENTER); btnDel.setPadding(12, 8, 8, 8);
            btnDel.setLayoutParams(new LinearLayout.LayoutParams(WRAP, WRAP));
            btnDel.setOnClickListener(v -> {
                if (idx == currentTrackIndex) {
                    if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
                    currentTrackIndex = -1;
                    tvTitle.setText("未在播放"); tvArtist.setText("");
                    btnPlay.setImageResource(android.R.drawable.ic_media_play);
                    recordAnimator.pause();
                } else if (idx < currentTrackIndex) {
                    currentTrackIndex--;
                }
                playlist.remove(idx);
                tvTrackCount.setText(playlist.size() + " 首");
                if (playlist.isEmpty()) { tvTitle.setText("列表已清空"); tvArtist.setText("点击 🔄 重新扫描"); }
                buildPlaylistUI();
            });
            row.addView(btnDel);

            row.setOnClickListener(v -> {
                currentTrackIndex = idx; loadTrack(idx, true);
            });

            playlistContainer.addView(row);
        }
    }

    // ======= 播放引擎 =======
    private void loadTrack(int index, boolean autoPlay) {
        if (playlist.isEmpty()) return;
        if (mediaPlayer != null) mediaPlayer.release();

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
                if (isShuffle) currentTrackIndex = (int) (Math.random() * playlist.size());
                else currentTrackIndex = (currentTrackIndex + 1) % playlist.size();
                loadTrack(currentTrackIndex, true);
            });
            if (autoPlay) { mediaPlayer.start(); btnPlay.setImageResource(android.R.drawable.ic_media_pause); recordAnimator.start(); updateSeekBar(); }
            else btnPlay.setImageResource(android.R.drawable.ic_media_play);
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法播放", Toast.LENGTH_SHORT).show();
        }
        buildPlaylistUI();
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) { mediaPlayer.pause(); btnPlay.setImageResource(android.R.drawable.ic_media_play); recordAnimator.pause(); }
        else { mediaPlayer.start(); btnPlay.setImageResource(android.R.drawable.ic_media_pause);
            if (recordAnimator.isPaused()) recordAnimator.resume(); else recordAnimator.start(); updateSeekBar(); }
        buildPlaylistUI();
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        currentTrackIndex = isShuffle ? (int) (Math.random() * playlist.size()) : (currentTrackIndex + 1) % playlist.size();
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

    private int dp(int d) { return (int) (d * getResources().getDisplayMetrics().density); }
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        handler.removeCallbacksAndMessages(null);
        if (recordAnimator != null) recordAnimator.cancel();
        executor.shutdown();
    }
}
