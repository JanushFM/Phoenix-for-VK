package biz.dealnote.messenger.domain;

import java.util.List;

import biz.dealnote.messenger.model.StickerSet;
import biz.dealnote.messenger.model.StickersKeywords;
import io.reactivex.Completable;
import io.reactivex.Single;


public interface IStickersInteractor {
    Completable getAndStore(int accountId);

    Single<List<StickerSet>> getStickers(int accountId);

    Single<List<StickersKeywords>> getKeywordsStickers(int accountId);
}