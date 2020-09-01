package biz.dealnote.messenger.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.squareup.picasso3.RequestHandler;
import com.squareup.picasso3.Transformation;

import org.jetbrains.annotations.NotNull;

public class MaskTransformation implements Transformation {

    private static final Paint mMaskingPaint = new Paint();

    static {
        mMaskingPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    private final Context mContext;
    private final int mMaskId;

    /**
     * @param maskId If you change the mask file, please also rename the mask file, or Glide will get
     *               the cache with the old mask. Because getId() return the same values if using the
     *               same make file name. If you have a good idea please tell us, thanks.
     */
    public MaskTransformation(Context context, int maskId) {
        mContext = context;
        mMaskId = maskId;
    }

    public static Drawable getMaskDrawable(Context context, int maskId) {
        Drawable drawable = ContextCompat.getDrawable(context, maskId);

        if (drawable == null) {
            throw new IllegalArgumentException("maskId is invalid");
        }

        return drawable;
    }

    @NotNull
    @Override
    public String key() {
        return "MaskTransformation(maskId=" + mContext.getResources().getResourceEntryName(mMaskId)
                + ")";
    }

    @NotNull
    @Override
    public RequestHandler.Result.Bitmap transform(@NotNull RequestHandler.Result.Bitmap source_request) {
        Bitmap source = source_request.getBitmap();
        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Drawable mask = getMaskDrawable(mContext, mMaskId);

        Canvas canvas = new Canvas(result);
        mask.setBounds(0, 0, width, height);
        mask.draw(canvas);
        canvas.drawBitmap(source, 0, 0, mMaskingPaint);

        source.recycle();

        return new RequestHandler.Result.Bitmap(result, source_request.loadedFrom, source_request.exifRotation);
    }
}