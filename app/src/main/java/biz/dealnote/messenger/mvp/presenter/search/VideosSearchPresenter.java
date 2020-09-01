package biz.dealnote.messenger.mvp.presenter.search;

import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.List;

import biz.dealnote.messenger.domain.IVideosInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.fragment.search.criteria.VideoSearchCriteria;
import biz.dealnote.messenger.fragment.search.nextfrom.IntNextFrom;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.mvp.view.search.IVideosSearchView;
import biz.dealnote.messenger.util.Pair;
import io.reactivex.rxjava3.core.Single;

import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class VideosSearchPresenter extends AbsSearchPresenter<IVideosSearchView, VideoSearchCriteria, Video, IntNextFrom> {

    private final IVideosInteractor videosInteractor;

    public VideosSearchPresenter(int accountId, @Nullable VideoSearchCriteria criteria, @Nullable Bundle savedInstanceState) {
        super(accountId, criteria, savedInstanceState);
        videosInteractor = InteractorFactory.createVideosInteractor();
    }

    @Override
    IntNextFrom getInitialNextFrom() {
        return new IntNextFrom(0);
    }

    @Override
    boolean isAtLast(IntNextFrom startFrom) {
        return startFrom.getOffset() == 0;
    }

    @Override
    Single<Pair<List<Video>, IntNextFrom>> doSearch(int accountId, VideoSearchCriteria criteria, IntNextFrom startFrom) {
        int offset = startFrom.getOffset();
        IntNextFrom nextFrom = new IntNextFrom(offset + 50);
        return videosInteractor.search(accountId, criteria, 50, offset)
                .map(videos -> Pair.Companion.create(videos, nextFrom));
    }

    @Override
    VideoSearchCriteria instantiateEmptyCriteria() {
        return new VideoSearchCriteria("", false);
    }

    @Override
    boolean canSearch(VideoSearchCriteria criteria) {
        return nonEmpty(criteria.getQuery());
    }
}