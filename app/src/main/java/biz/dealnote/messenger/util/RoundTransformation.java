package biz.dealnote.messenger.util;

import com.squareup.picasso3.RequestHandler;
import com.squareup.picasso3.Transformation;

import org.jetbrains.annotations.NotNull;

public class RoundTransformation implements Transformation {
    @NotNull
    @Override
    public String key() {
        return "round()";
    }

    @NotNull
    @Override
    public RequestHandler.Result.Bitmap transform(@NotNull RequestHandler.Result.Bitmap source) {
        return new RequestHandler.Result.Bitmap(ImageHelper.getRoundedBitmap(source.getBitmap(), 100, 100), source.loadedFrom, source.exifRotation);
    }
}
