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
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/** 音乐后台播放 Foreground Service + 通知栏播放控制 */
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

    private String currentTitle = "";
    private String currentArtist = "";
    private boolean isPlaying = false;
    private MusicCallback callback;

    public interface MusicCallback {
        void onPlayPause();
        void onNext();
        void onPrev();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        registerCallbackReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE.equals(intent.getAction())) {
            currentTitle = intent.getStringExtra(EXTRA_TITLE);
            currentArtist = intent.getStringExtra(EXTRA_ARTIST);
            isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false);
            updateNotification();
        }
        return START_STICKY;
    }

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

    // ── 通知栏构建（含播放控制按钮） ──

    private void updateNotification() {
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
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
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
                        .setShowActionsInCompactView(0, 1, 2));

        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
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

    /** Fragment 设置回调，用于接收通知栏按钮事件 */
    public void setCallback(MusicCallback cb) { this.callback = cb; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        callback = null;
    }
}
