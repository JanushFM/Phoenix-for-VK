package biz.dealnote.messenger.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.longpoll.ILongpollManager;
import biz.dealnote.messenger.longpoll.LongpollInstance;
import biz.dealnote.messenger.settings.ISettings;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class KeepLongpollService extends Service {

    private static final String ACTION_STOP = "KeepLongpollService.ACTION_STOP";
    private static final String KEEP_LONGPOLL_CHANNEL = "keep_longpoll";
    private static final int FOREGROUND_SERVICE = 120;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ILongpollManager longpollManager;

    public static void start(@NonNull Context context) {
        context.startService(new Intent(context, KeepLongpollService.class));
    }

    public static void stop(@NonNull Context context) {
        context.stopService(new Intent(context, KeepLongpollService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startWithNotification();

        longpollManager = LongpollInstance.get();

        sendKeepAlive();

        compositeDisposable.add(longpollManager.observeKeepAlive()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(ignored -> sendKeepAlive(), RxUtils.ignore()));

        compositeDisposable.add(Settings.get().accounts()
                .observeChanges()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(ignored -> sendKeepAlive(), RxUtils.ignore()));
    }

    private void sendKeepAlive() {
        int accountId = Settings.get().accounts().getCurrent();
        if (accountId != ISettings.IAccountsSettings.INVALID_ID) {
            longpollManager.keepAlive(accountId);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void cancelNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(FOREGROUND_SERVICE);
        }
    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        cancelNotification();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("deprecation")
    private void startWithNotification() {
        Intent notificationIntent = new Intent(this, KeepLongpollService.class);
        notificationIntent.setAction(ACTION_STOP);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(KEEP_LONGPOLL_CHANNEL, getString(R.string.channel_keep_longpoll),
                    NotificationManager.IMPORTANCE_NONE);

            NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nManager != null) {
                nManager.createNotificationChannel(channel);
            }

            builder = new NotificationCompat.Builder(this, channel.getId());
        } else {
            builder = new NotificationCompat.Builder(this).setPriority(Notification.PRIORITY_MIN);
        }

        NotificationCompat.Action action_stop = new NotificationCompat.Action.Builder
                (R.drawable.ic_arrow_down,
                        getString(R.string.stop_action), pendingIntent)
                .build();

        builder.setContentTitle(getString(R.string.keep_longpoll_notification_title))
                .setContentText(getString(R.string.may_down_charge))
                .setSmallIcon(R.drawable.phoenix_round)
                .addAction(action_stop)
                .setColor(Color.parseColor("#dd0000"))
                .setOngoing(true)
                .build();

        NotificationCompat.WearableExtender War = new NotificationCompat.WearableExtender();
        War.addAction(action_stop);
        War.setStartScrollBottom(true);

        builder.extend(War);

        startForeground(FOREGROUND_SERVICE, builder.build());
    }
}