package biz.dealnote.messenger.task;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.webkit.MimeTypeMap;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.HttpLogger;
import biz.dealnote.messenger.api.ProxyUtil;
import biz.dealnote.messenger.longpoll.AppNotificationChannels;
import biz.dealnote.messenger.longpoll.NotificationHelper;
import biz.dealnote.messenger.util.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InternalDownloadTask extends AsyncTask<String, Integer, String> {

    private static final DateFormat DOWNLOAD_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    private final String photourl;
    private final String ID;
    private final NotificationManagerCompat mNotifyManager;
    private final NotificationCompat.Builder mBuilder;
    private final boolean UseMediaScanner;
    protected String file;
    private String filename;

    public InternalDownloadTask(Context context, String url, String file, String ID, boolean UseMediaScanner) {
        this.mContext = context.getApplicationContext();
        this.file = file;
        this.photourl = url;
        this.ID = ID;
        this.UseMediaScanner = UseMediaScanner;
        this.mNotifyManager = NotificationManagerCompat.from(this.mContext);
        if (Utils.hasOreo()) {
            this.mNotifyManager.createNotificationChannel(AppNotificationChannels.getDownloadChannel(this.mContext));
        }
        this.mBuilder = new NotificationCompat.Builder(this.mContext, AppNotificationChannels.DOWNLOAD_CHANNEL_ID);
        if (new File(file).exists()) {
            int lastExt = this.file.lastIndexOf('.');
            if (lastExt != -1) {
                String ext = this.file.substring(lastExt);

                String file_temp = this.file.substring(0, lastExt);
                this.file = file_temp + ("." + DOWNLOAD_DATE_FORMAT.format(new Date())) + ext;
            } else
                this.file += ("." + DOWNLOAD_DATE_FORMAT.format(new Date()));
        }

        this.filename = this.file;
        int lastPath = this.filename.lastIndexOf(File.separator);
        if (lastPath != -1) {
            this.filename = this.filename.substring(lastPath + 1);
        }

        this.mBuilder.setContentTitle(this.mContext.getString(R.string.downloading))
                .setContentText(this.mContext.getString(R.string.downloading) + " " + this.filename)
                .setSmallIcon(R.drawable.save)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true);
    }

    private static String getFileExtension(File file) {
        String extension = "";

        try {
            if (file != null && file.exists()) {
                String name = file.getName();
                extension = name.substring(name.lastIndexOf('.') + 1);
            }
        } catch (Exception e) {
            extension = "";
        }

        return extension;

    }

    @Override
    protected String doInBackground(String... params) {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wl.acquire(10 * 60 * 1000L /*10 minutes*/);

        try (OutputStream output = new FileOutputStream(file)) {
            if (photourl == null || photourl.isEmpty())
                throw new Exception(mContext.getString(R.string.null_image_link));

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(HttpLogger.DEFAULT_LOGGING_INTERCEPTOR).addInterceptor(chain -> {
                        Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(null)).build();
                        return chain.proceed(request);
                    });
            ProxyUtil.applyProxyConfig(builder, Injection.provideProxySettings().getActiveProxy());
            final Request request = new Request.Builder()
                    .url(photourl)
                    .build();

            Response response = builder.build().newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new Exception("Server return " + response.code() +
                        " " + response.message());
            }
            InputStream is = Objects.requireNonNull(response.body()).byteStream();
            BufferedInputStream input = new BufferedInputStream(is);
            byte[] data = new byte[8 * 1024];
            int bufferLength;
            double downloadedSize = 0.0;

            String cntlength = response.header("Content-Length");
            int totalSize = 1;
            if (!Utils.isEmpty(cntlength))
                totalSize = Integer.parseInt(cntlength);
            while ((bufferLength = input.read(data)) != -1) {
                output.write(data, 0, bufferLength);
                downloadedSize += bufferLength;
                publishProgress((int) ((downloadedSize / totalSize) * 100));
                mBuilder.setProgress(100, (int) ((downloadedSize / totalSize) * 100), false);
                mNotifyManager.notify(ID, NotificationHelper.NOTIFICATION_DOWNLOADING, mBuilder.build());
            }

            output.flush();
            input.close();

            if (UseMediaScanner) {
                mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(file))));

                Intent intent_open = new Intent(Intent.ACTION_VIEW);
                intent_open.setDataAndType(FileProvider.getUriForFile(mContext, Constants.FILE_PROVIDER_AUTHORITY, new File(file)), MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(getFileExtension(new File(file)))).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


                PendingIntent ReadPendingIntent = PendingIntent.getActivity(mContext, ID.hashCode(), intent_open, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(ReadPendingIntent);
            }

            mBuilder.setContentText(mContext.getString(R.string.success) + " " + this.filename)
                    .setProgress(0, 0, false)
                    .setAutoCancel(true)
                    .setOngoing(false);
            mNotifyManager.cancel(ID, NotificationHelper.NOTIFICATION_DOWNLOADING);
            mNotifyManager.notify(ID, NotificationHelper.NOTIFICATION_DOWNLOAD, mBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
            mBuilder.setContentText(mContext.getString(R.string.error) + " " + e.getLocalizedMessage() + ". " + this.filename)
                    .setSmallIcon(R.drawable.ic_error_toast_vector)
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setProgress(0, 0, false);
            mNotifyManager.cancel(ID, NotificationHelper.NOTIFICATION_DOWNLOADING);
            mNotifyManager.notify(ID, NotificationHelper.NOTIFICATION_DOWNLOAD, mBuilder.build());
            return e.getLocalizedMessage();
        } finally {
            wl.release();
        }

        return null;
    }

    public void doDownload() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
