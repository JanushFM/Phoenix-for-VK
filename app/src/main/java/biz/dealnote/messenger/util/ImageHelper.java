package biz.dealnote.messenger.util;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;

public class ImageHelper {

    public static Bitmap getRoundedBitmap(Bitmap bitmap, int percentage_x, int percentage_y) {
        if (bitmap == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        float cfx = (percentage_x / 100f);
        float cfy = (percentage_y / 100f);

        RectF rect = new RectF(0f, 0f, bitmap.getWidth(), bitmap.getHeight());
        Path path = new Path();
        path.addRoundRect(rect, (bitmap.getWidth() / 2f) * cfx, (bitmap.getHeight() / 2f) * cfy, Path.Direction.CW);
        canvas.drawPath(path, paint);

        if (bitmap != output) {
            bitmap.recycle();
        }

        return output;
    }
}