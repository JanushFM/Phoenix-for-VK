package biz.dealnote.messenger.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;

public class ImageHelper {

    private static final PorterDuffXfermode PORTER_DUFF_XFERMODE = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

    public static Bitmap getRoundedBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xff424242);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(PORTER_DUFF_XFERMODE);
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    public static Bitmap getElipsedBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xff424242);
        canvas.drawRoundRect(rectF, (float) bitmap.getWidth() / 2.7f, (float) bitmap.getHeight() / 2.5f, paint);

        paint.setXfermode(PORTER_DUFF_XFERMODE);
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

    public static Bitmap getPolyBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xff424242);
        canvas.drawRoundRect(rectF, (float) bitmap.getWidth() / 6.3f, (float) bitmap.getHeight() / 6.3f, paint);

        paint.setXfermode(PORTER_DUFF_XFERMODE);
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }
}