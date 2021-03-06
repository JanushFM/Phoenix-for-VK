package biz.dealnote.messenger.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import biz.dealnote.messenger.Constants;


public class FileUtil {

    @SuppressLint("ConstantLocale")
    private static final DateFormat PHOTO_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
    private static final Random Random = new Random();

    public static Uri getExportedUriForFile(Context context, File file) {
        return FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, file);
    }

    public static File createImageFile() throws IOException {
        String timeStamp = PHOTO_DATE_FORMAT.format(new Date());
        String externalStorageDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        File directory = new File(externalStorageDir + "/" + Constants.PHOTOS_PATH);

        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create directory");
        }

        File targetFile = null;

        boolean noExist = false;
        while (!noExist) {
            int randomInt = Random.nextInt(1000000);
            String fileName = "Captured_" + timeStamp + "_" + randomInt + ".jpg";
            File file = new File(directory, fileName);

            if (!file.exists()) {
                targetFile = file;
                noExist = true;
            }
        }

        return targetFile;
    }
}
