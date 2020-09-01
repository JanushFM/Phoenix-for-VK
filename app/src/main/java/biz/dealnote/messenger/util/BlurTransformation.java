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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;

import com.squareup.picasso3.RequestHandler;
import com.squareup.picasso3.Transformation;

import org.jetbrains.annotations.NotNull;

public class BlurTransformation implements Transformation {

    private static final int MAX_RADIUS = 25;
    private static final int DEFAULT_DOWN_SAMPLING = 1;

    private final int mRadius;
    private final int mSampling;

    public BlurTransformation() {
        this(MAX_RADIUS, DEFAULT_DOWN_SAMPLING);
    }

    public BlurTransformation(int radius) {
        this(radius, DEFAULT_DOWN_SAMPLING);
    }

    public BlurTransformation(int radius, int sampling) {
        mRadius = radius;
        mSampling = sampling;
    }

    @NotNull
    @Override
    public String key() {
        return "BlurTransformation(radius=" + mRadius + ", sampling=" + mSampling + ")";
    }

    @NotNull
    @Override
    public RequestHandler.Result.Bitmap transform(@NotNull RequestHandler.Result.Bitmap source_request) {
        Bitmap source = source_request.getBitmap();
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

        source.recycle();

        assert bitmap != null;
        return new RequestHandler.Result.Bitmap(bitmap, source_request.loadedFrom, source_request.exifRotation);
    }
}
