package biz.dealnote.messenger.util;

import com.squareup.picasso3.RequestHandler;
import com.squareup.picasso3.Transformation;

import org.jetbrains.annotations.NotNull;

public class PolyTransformation implements Transformation {

    @NotNull
    @Override
    public String key() {
        return "poly()";
    }

    @NotNull
    @Override
    public RequestHandler.Result.Bitmap transform(@NotNull RequestHandler.Result.Bitmap source) {
        return new RequestHandler.Result.Bitmap(ImageHelper.getPolyBitmap(source.getBitmap()), source.loadedFrom, source.exifRotation);
    }
}
