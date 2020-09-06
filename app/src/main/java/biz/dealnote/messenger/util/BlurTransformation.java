package biz.dealnote.messenger.util;

/**
 * Copyright (C) 2018 Wasabeef
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.squareup.picasso3.RequestHandler;
import com.squareup.picasso3.Transformation;

import org.jetbrains.annotations.NotNull;

public class BlurTransformation implements Transformation {

    private final int mRadius;
    private final int mSampling;

    private final Context mContext;

    public BlurTransformation(int radius, int sampling, Context mContext) {
        mRadius = radius;
        mSampling = sampling;
        this.mContext = mContext;
    }

    @NotNull
    @Override
    public String key() {
        return "BlurTransformation(radius=" + mRadius + ", sampling=" + mSampling + ")";
    }

    public Bitmap blur(Bitmap image) {
        if (null == image) return null;

        Bitmap outputBitmap = Bitmap.createBitmap(image);
        RenderScript renderScript = RenderScript.create(mContext);
        Allocation tmpIn = Allocation.createFromBitmap(renderScript, image);
        Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);

        //Intrinsic Gausian blur filter
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        theIntrinsic.setRadius(mRadius);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }

    @NotNull
    @Override
    public RequestHandler.Result.Bitmap transform(@NotNull RequestHandler.Result.Bitmap source_request) {
        Bitmap source = source_request.getBitmap();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && source.getConfig() == Bitmap.Config.HARDWARE) {
            source = source.copy(Bitmap.Config.ARGB_8888, true);
        }
        /*
        int scaledWidth = source.getWidth() / mSampling;
        int scaledHeight = source.getHeight() / mSampling;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && source.getConfig() == Bitmap.Config.HARDWARE) {
            source = source.copy(Bitmap.Config.ARGB_8888, true);
        }

        Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        canvas.scale(1 / (float) mSampling, 1 / (float) mSampling);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, 0, 0, paint);
        bitmap = FastBlur.blur(bitmap, mRadius, true);
*/
        Bitmap bitmap = blur(source);
        if (source != bitmap) {
            source.recycle();
        }

        assert bitmap != null;
        return new RequestHandler.Result.Bitmap(bitmap, source_request.loadedFrom, source_request.exifRotation);
    }
}
