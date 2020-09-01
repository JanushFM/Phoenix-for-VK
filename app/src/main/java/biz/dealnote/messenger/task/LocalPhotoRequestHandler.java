package biz.dealnote.messenger.task;

import android.content.Context;
import android.graphics.Bitmap;

import com.squareup.picasso3.Picasso;
import com.squareup.picasso3.Request;
import com.squareup.picasso3.RequestHandler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import biz.dealnote.messenger.db.Stores;

public class LocalPhotoRequestHandler extends RequestHandler {

    private final Context mContext;

    public LocalPhotoRequestHandler(Context context) {
        mContext = context;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return data.uri != null && data.uri.getScheme() != null && data.uri.getScheme().equals("content");
    }

    @Override
    public void load(@NotNull Picasso picasso, @NotNull Request request, @NotNull Callback callback) throws IOException {
        assert request.uri != null;
        long imageId = Long.parseLong(request.uri.getLastPathSegment());

        boolean isVideo = request.uri.getPath().contains("videos");

        Bitmap bm;
        if (!isVideo) {
            bm = Stores.getInstance()
                    .localPhotos()
                    .getImageThumbnail(imageId);
        } else {
            bm = Stores.getInstance()
                    .localPhotos()
                    .getVideoThumbnail(imageId);
        }

        callback.onSuccess(new RequestHandler.Result.Bitmap(bm, Picasso.LoadedFrom.DISK));
    }
}