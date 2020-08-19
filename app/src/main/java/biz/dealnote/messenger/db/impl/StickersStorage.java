package biz.dealnote.messenger.db.impl;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.db.column.StickersKeywordsColumns;
import biz.dealnote.messenger.db.column.StikerSetColumns;
import biz.dealnote.messenger.db.interfaces.IStickersStorage;
import biz.dealnote.messenger.db.model.entity.StickerEntity;
import biz.dealnote.messenger.db.model.entity.StickerSetEntity;
import biz.dealnote.messenger.db.model.entity.StickersKeywordsEntity;
import biz.dealnote.messenger.util.Exestime;
import io.reactivex.Completable;
import io.reactivex.Single;

import static biz.dealnote.messenger.db.column.StikerSetColumns.ACTIVE;
import static biz.dealnote.messenger.db.column.StikerSetColumns.PHOTO_140;
import static biz.dealnote.messenger.db.column.StikerSetColumns.PHOTO_35;
import static biz.dealnote.messenger.db.column.StikerSetColumns.PHOTO_70;
import static biz.dealnote.messenger.db.column.StikerSetColumns.PROMOTED;
import static biz.dealnote.messenger.db.column.StikerSetColumns.PURCHASED;
import static biz.dealnote.messenger.db.column.StikerSetColumns.STICKERS;
import static biz.dealnote.messenger.db.column.StikerSetColumns.TITLE;
import static biz.dealnote.messenger.db.column.StikerSetColumns._ID;
import static biz.dealnote.messenger.util.Utils.safeCountOf;


class StickersStorage extends AbsStorage implements IStickersStorage {

    private static final String[] COLUMNS = {
            _ID,
            TITLE,
            PHOTO_35,
            PHOTO_70,
            PHOTO_140,
            PURCHASED,
            PROMOTED,
            ACTIVE,
            STICKERS
    };
    private static final String[] KEYWORDS_STICKER_COLUMNS = {
            _ID,
            StickersKeywordsColumns.KEYWORDS,
            StickersKeywordsColumns.STICKERS
    };
    private static final Type TYPE = new TypeToken<List<StickerEntity>>() {
    }.getType();

    private static final Type WORDS = new TypeToken<List<String>>() {
    }.getType();

    StickersStorage(@NonNull AppStorages base) {
        super(base);
    }

    private static ContentValues createCv(StickerSetEntity entity) {
        ContentValues cv = new ContentValues();
        cv.put(PHOTO_35, entity.getPhoto35());
        cv.put(PHOTO_70, entity.getPhoto70());
        cv.put(PHOTO_140, entity.getPhoto140());

        cv.put(_ID, entity.getId());
        cv.put(TITLE, entity.getTitle());
        cv.put(PURCHASED, entity.isPurchased());
        cv.put(PROMOTED, entity.isPromoted());
        cv.put(ACTIVE, entity.isActive());

        cv.put(STICKERS, GSON.toJson(entity.getStickers()));
        return cv;
    }

    private static ContentValues createCvStickersKeywords(StickersKeywordsEntity entity, int id) {
        ContentValues cv = new ContentValues();
        cv.put(_ID, id);
        cv.put(StickersKeywordsColumns.KEYWORDS, GSON.toJson(entity.getKeywords()));
        cv.put(StickersKeywordsColumns.STICKERS, GSON.toJson(entity.getStickers()));
        return cv;
    }

    private static StickerSetEntity map(Cursor cursor) {
        String stickersJson = cursor.getString(cursor.getColumnIndex(STICKERS));
        return new StickerSetEntity(cursor.getInt(cursor.getColumnIndex(_ID)))
                .setStickers(GSON.fromJson(stickersJson, TYPE))
                .setActive(cursor.getInt(cursor.getColumnIndex(ACTIVE)) == 1)
                .setPurchased(cursor.getInt(cursor.getColumnIndex(PURCHASED)) == 1)
                .setPromoted(cursor.getInt(cursor.getColumnIndex(PROMOTED)) == 1)
                .setPhoto35(cursor.getString(cursor.getColumnIndex(PHOTO_35)))
                .setPhoto70(cursor.getString(cursor.getColumnIndex(PHOTO_70)))
                .setPhoto140(cursor.getString(cursor.getColumnIndex(PHOTO_140)))
                .setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
    }

    private static StickersKeywordsEntity mapStickersKeywords(Cursor cursor) {
        String stickersJson = cursor.getString(cursor.getColumnIndex(StickersKeywordsColumns.STICKERS));
        String keywordsJson = cursor.getString(cursor.getColumnIndex(StickersKeywordsColumns.KEYWORDS));
        return new StickersKeywordsEntity(GSON.fromJson(keywordsJson, WORDS), GSON.fromJson(stickersJson, TYPE));
    }

    @Override
    public Completable store(int accountId, List<StickerSetEntity> sets) {
        return Completable.create(e -> {
            long start = System.currentTimeMillis();

            SQLiteDatabase db = helper(accountId).getWritableDatabase();

            db.beginTransaction();

            try {
                db.delete(StikerSetColumns.TABLENAME, null, null);
                for (StickerSetEntity entity : sets) {
                    db.insert(StikerSetColumns.TABLENAME, null, createCv(entity));
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                e.onComplete();
            } catch (Exception exception) {
                db.endTransaction();
                e.tryOnError(exception);
            }

            Exestime.log("StickersStorage.store", start, "count: " + safeCountOf(sets));
        });
    }

    @Override
    public Completable storeKeyWords(int accountId, List<StickersKeywordsEntity> sets) {
        return Completable.create(e -> {
            long start = System.currentTimeMillis();

            SQLiteDatabase db = helper(accountId).getWritableDatabase();

            db.beginTransaction();

            try {
                db.delete(StickersKeywordsColumns.TABLENAME, null, null);
                int id = 0;
                for (StickersKeywordsEntity entity : sets) {
                    db.insert(StickersKeywordsColumns.TABLENAME, null, createCvStickersKeywords(entity, id++));
                }
                db.setTransactionSuccessful();
                db.endTransaction();
                e.onComplete();
            } catch (Exception exception) {
                db.endTransaction();
                e.tryOnError(exception);
            }

            Exestime.log("StickersStorage.storeKeyWords", start, "count: " + safeCountOf(sets));
        });
    }

    @Override
    public Single<List<StickerSetEntity>> getPurchasedAndActive(int accountId) {
        return Single.create(e -> {
            long start = System.currentTimeMillis();
            String where = PURCHASED + " = ? AND " + ACTIVE + " = ?";
            String[] args = {"1", "1"};
            Cursor cursor = helper(accountId).getReadableDatabase().query(StikerSetColumns.TABLENAME, COLUMNS, where, args, null, null, null);

            List<StickerSetEntity> stickers = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                if (e.isDisposed()) {
                    break;
                }
                stickers.add(map(cursor));
            }

            cursor.close();
            e.onSuccess(stickers);
            Exestime.log("StickersStorage.get", start, "count: " + stickers.size());
        });
    }

    @Override
    public Single<List<StickersKeywordsEntity>> getKeywordsStickers(int accountId) {
        return Single.create(e -> {
            long start = System.currentTimeMillis();
            Cursor cursor = helper(accountId).getReadableDatabase().query(StickersKeywordsColumns.TABLENAME, KEYWORDS_STICKER_COLUMNS, null, null, null, null, null);

            List<StickersKeywordsEntity> stickers = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                if (e.isDisposed()) {
                    break;
                }
                stickers.add(mapStickersKeywords(cursor));
            }

            cursor.close();
            e.onSuccess(stickers);
            Exestime.log("StickersStorage.get", start, "count: " + stickers.size());
        });
    }
}