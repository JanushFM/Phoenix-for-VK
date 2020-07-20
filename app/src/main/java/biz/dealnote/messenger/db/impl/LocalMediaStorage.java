package biz.dealnote.messenger.db.impl;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.db.interfaces.ILocalMediaStorage;
import biz.dealnote.messenger.model.LocalImageAlbum;
import biz.dealnote.messenger.model.LocalPhoto;
import biz.dealnote.messenger.model.LocalVideo;
import biz.dealnote.messenger.util.Objects;
import io.reactivex.Single;

import static biz.dealnote.messenger.util.Utils.safeCountOf;


class LocalMediaStorage extends AbsStorage implements ILocalMediaStorage {

    private static final String[] PROJECTION = {BaseColumns._ID, MediaStore.MediaColumns.DATA};
    private static final String[] VIDEO_PROJECTION = {
            BaseColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.TITLE
    };

    LocalMediaStorage(@NonNull AppStorages mRepositoryContext) {
        super(mRepositoryContext);
    }

    private static LocalVideo mapVideo(Cursor cursor) {
        String data = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        return new LocalVideo(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)), Uri.parse(data))
                .setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)))
                .setSize(cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)))
                .setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));
    }

    @Override
    public Single<List<LocalVideo>> getVideos() {
        return Single.create(e -> {
            Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    VIDEO_PROJECTION, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC");

            ArrayList<LocalVideo> data = new ArrayList<>(safeCountOf(cursor));
            if (Objects.nonNull(cursor)) {
                while (cursor.moveToNext()) {
                    data.add(mapVideo(cursor));
                }
                cursor.close();
            }

            e.onSuccess(data);
        });
    }

    @Override
    public Bitmap getVideoThumbnail(long videoId) {
        return MediaStore.Video.Thumbnails.getThumbnail(getContext().getContentResolver(),
                videoId, MediaStore.Video.Thumbnails.MINI_KIND, null);
    }

    @Override
    public Single<List<LocalPhoto>> getPhotos(long albumId) {
        return Single.create(e -> {
            Cursor cursor = getContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, MediaStore.MediaColumns.BUCKET_ID + " = ?",
                    new String[]{String.valueOf(albumId)},
                    MediaStore.MediaColumns.DATE_MODIFIED + " DESC");

            ArrayList<LocalPhoto> result = new ArrayList<>(safeCountOf(cursor));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed()) break;

                    long imageId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));

                    result.add(new LocalPhoto()
                            .setImageId(imageId)
                            .setFullImageUri(Uri.parse(data)));
                }

                cursor.close();
            }

            e.onSuccess(result);
        });
    }

    @Override
    public Single<List<LocalPhoto>> getPhotos() {
        return Single.create(e -> {
            Cursor cursor = getContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION, null, null,
                    MediaStore.MediaColumns.DATE_MODIFIED + " DESC");

            ArrayList<LocalPhoto> result = new ArrayList<>(safeCountOf(cursor));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed()) break;

                    long imageId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                    String data = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));

                    result.add(new LocalPhoto()
                            .setImageId(imageId)
                            .setFullImageUri(Uri.parse(data)));
                }

                cursor.close();
            }

            e.onSuccess(result);
        });
    }

    private boolean hasAlbumById(int albumId, List<LocalImageAlbum> albums) {
        for (LocalImageAlbum i : albums) {
            if (i.getId() == albumId) {
                i.setPhotoCount(i.getPhotoCount() + 1);
                return true;
            }
        }
        return false;
    }

    @Override
    public Single<List<LocalImageAlbum>> getImageAlbums() {
        return Single.create(e -> {
            final String album = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME;
            final String albumId = MediaStore.MediaColumns.BUCKET_ID;
            final String data = MediaStore.MediaColumns.DATA;
            final String coverId = BaseColumns._ID;
            String[] projection = {album, albumId, data, coverId};

            Cursor cursor = getContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC");

            List<LocalImageAlbum> albums = new ArrayList<>(safeCountOf(cursor));

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (e.isDisposed()) break;

                    if (!hasAlbumById(cursor.getInt(1), albums)) {
                        albums.add(new LocalImageAlbum()
                                .setId(cursor.getInt(1))
                                .setName(cursor.getString(0))
                                .setCoverPath(cursor.getString(2))
                                .setCoverId(cursor.getLong(3))
                                .setPhotoCount(1));
                    }
                }

                cursor.close();
            }

            e.onSuccess(albums);
        });
    }

    @Override
    public Bitmap getImageThumbnail(long imageId) {
        return MediaStore.Images.Thumbnails.getThumbnail(getContext().getContentResolver(),
                imageId, MediaStore.Images.Thumbnails.MINI_KIND, null);
    }
}
