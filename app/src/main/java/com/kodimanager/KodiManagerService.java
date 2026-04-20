package com.kodimanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class KodiManagerService extends Service {

    private static final String TAG = "KodiManager";
    private static final String CHANNEL_ID = "kodi_manager_channel";
    private ScreenReceiver screenReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Servicio iniciado");
        startForeground(1, buildNotification());
        registerScreenReceiver();
    }

    private void registerScreenReceiver() {
        screenReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);
        Log.d(TAG, "ScreenReceiver registrado");
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "KodiManager",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Gestiona Kodi automaticamente");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("KodiManager")
            .setContentText("Monitoreando TV...")
            .setSmallIcon(android.R.drawable.ic_media_play);

        return builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
        Intent restart = new Intent(this, KodiManagerService.class);
        startService(restart);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
