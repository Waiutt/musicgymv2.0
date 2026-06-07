package com.example.musicgym;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

/** 音乐后台播放 Foreground Service + 通知栏播放控制 + MediaSession（蓝牙/线控） */
public class MusicService extends Service {

    private static final String CHANNEL_ID = "music_playback";
    private static final int NOTIFICATION_ID = 1001;

    // 动作
    public static final String ACTION_UPDATE = "com.example.musicgym.UPDATE_NOTIFICATION";
    public static final String ACTION_PLAY_PAUSE = "com.example.musicgym.PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.musicgym.NEXT";
    public static final String ACTION_PREV = "com.example.musicgym.PREV";

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_IS_PLAYING = "is_playing";
    public static final String EXTRA_DURATION = "duration";

    private String currentTitle = "";
    private String currentArtist = "";
    private long currentDuration = 0;
    private boolean isPlaying = false;
    private MusicCallback callback;

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    public interface MusicCallback {
        void onPlayPause();
        void onNext();
        void onPrev();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setupMediaSession();
        registerCallbackReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // 处理蓝牙/线控的媒体按钮
            MediaButtonReceiver.handleIntent(mediaSession, intent);

            if (ACTION_UPDATE.equals(intent.getAction())) {
                currentTitle = intent.getStringExtra(EXTRA_TITLE);
                currentArtist = intent.getStringExtra(EXTRA_ARTIST);
                isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false);
                currentDuration = intent.getLongExtra(EXTRA_DURATION, 0);
                updateNotification();
            }
        }
        return START_STICKY;
    }

    // ── MediaSession ──

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "MusicGym");

        // 播放状态构建器
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                | PlaybackStateCompat.ACTION_STOP);

        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setActive(true);

        // 回调：蓝牙耳机/线控按键 → 通知栏同款动作
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                if (callback != null) callback.onPlayPause();
            }

            @Override
            public void onPause() {
                if (callback != null) callback.onPlayPause();
            }

            @Override
            public void onSkipToNext() {
                if (callback != null) callback.onNext();
            }

            @Override
            public void onSkipToPrevious() {
                if (callback != null) callback.onPrev();
            }
        });
    }

    // ── 公开方法（Fragment 调用） ──

    public void setCallback(MusicCallback cb) { this.callback = cb; }

    public class LocalBinder extends android.os.Binder {
        public MusicService getService() { return MusicService.this; }
    }
    private final IBinder binder = new LocalBinder();

    @Nullable @Override
    public IBinder onBind(Intent intent) { return binder; }

    // ── 通知栏频道 ──

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "音乐播放", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("后台音乐播放控制");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    // ── 通知栏构建（含播放控制按钮 + MediaSession 令牌） ──

    private void updateNotification() {
        // 更新 MediaSession 状态
        updateMediaSessionState();

        // 点击通知 → 回 App
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentPI = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 播放/暂停按钮
        PendingIntent playPausePI = PendingIntent.getBroadcast(this, 1,
                new Intent(ACTION_PLAY_PAUSE).setPackage(getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 下一首
        PendingIntent nextPI = PendingIntent.getBroadcast(this, 2,
                new Intent(ACTION_NEXT).setPackage(getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 上一首
        PendingIntent prevPI = PendingIntent.getBroadcast(this, 3,
                new Intent(ACTION_PREV).setPackage(getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentTitle.isEmpty() ? "MusicGym" : currentTitle)
                .setContentText(currentArtist.isEmpty() ? "未在播放" : currentArtist)
                .setSmallIcon(isPlaying ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                .setContentIntent(contentPI)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_media_previous, "上一首", prevPI)
                .addAction(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        "播放/暂停", playPausePI)
                .addAction(android.R.drawable.ic_media_next, "下一首", nextPI)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    // ── 更新 MediaSession 播放状态 ──

    private void updateMediaSessionState() {
        int state = isPlaying
                ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;
        stateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f);
        mediaSession.setPlaybackState(stateBuilder.build());

        // 更新元数据（锁屏/蓝牙设备显示歌名）
        MediaMetadataCompat.Builder meta = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                        currentTitle.isEmpty() ? "MusicGym" : currentTitle)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        currentArtist.isEmpty() ? "未知艺术家" : currentArtist);
        if (currentDuration > 0) {
            meta.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDuration);
        }
        mediaSession.setMetadata(meta.build());
    }

    // ── 接收通知栏按钮点击 ──

    private void registerCallbackReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (callback == null) return;
                String action = intent.getAction();
                if (ACTION_PLAY_PAUSE.equals(action)) callback.onPlayPause();
                else if (ACTION_NEXT.equals(action)) callback.onNext();
                else if (ACTION_PREV.equals(action)) callback.onPrev();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREV);
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        callback = null;
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
    }
}
