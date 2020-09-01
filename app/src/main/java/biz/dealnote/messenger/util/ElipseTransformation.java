package biz.dealnote.messenger.util;

import com.squareup.picasso3.RequestHandler;
import com.squareup.picasso3.Transformation;

import org.jetbrains.annotations.NotNull;

public class ElipseTransformation implements Transformation {

    @NotNull
    @Override
    public String key() {
        return "elipse()";
    }

    @NotNull
    @Override
    public RequestHandler.Result.Bitmap transform(@NotNull RequestHandler.Result.Bitmap source) {
        return new RequestHandler.Result.Bitmap(ImageHelper.getElipsedBitmap(source.getBitmap()), source.loadedFrom, source.exifRotation);
    }
}
