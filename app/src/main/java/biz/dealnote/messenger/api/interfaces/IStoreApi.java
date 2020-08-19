package biz.dealnote.messenger.api.interfaces;

import biz.dealnote.messenger.api.model.VkApiStickersKeywords;
import io.reactivex.Single;


public interface IStoreApi {
    Single<VkApiStickersKeywords> getStickers();
}