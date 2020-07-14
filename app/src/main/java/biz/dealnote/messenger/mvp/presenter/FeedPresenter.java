package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.db.model.PostUpdate;
import biz.dealnote.messenger.domain.IFeedInteractor;
import biz.dealnote.messenger.domain.IWallsRepository;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.FeedList;
import biz.dealnote.messenger.model.FeedSource;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.News;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.IFeedView;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.DisposableHolder;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.mvp.reflect.OnGuiCreated;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class FeedPresenter extends PlaceSupportPresenter<IFeedView> {

    private final IFeedInteractor feedInteractor;
    private final IWallsRepository walls;
    private final List<News> mFeed;
    private final List<FeedSource> mFeedSources;
    private final DisposableHolder<Void> loadingHolder = new DisposableHolder<>();
    private final DisposableHolder<Void> cacheLoadingHolder = new DisposableHolder<>();
    private String mNextFrom;
    private String mSourceIds;
    private boolean loadingNow;
    private String loadingNowNextFrom;
    private boolean cacheLoadingNow;
    private String mTmpFeedScrollOnGuiReady;

    public FeedPresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);

        walls = Repository.INSTANCE.getWalls();

        appendDisposable(walls.observeMinorChanges()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(this::onPostUpdateEvent));

        feedInteractor = InteractorFactory.createFeedInteractor();
        mFeed = new ArrayList<>();
        mFeedSources = new ArrayList<>(createDefaultFeedSources());

        refreshFeedSourcesSelection();

        restoreNextFromAndFeedSources();

        refreshFeedSources();

        String scrollState = Settings.get()
                .other()
                .restoreFeedScrollState(accountId);

        loadCachedFeed(scrollState);
    }

    private static List<FeedSource> createDefaultFeedSources() {
        List<FeedSource> data = new ArrayList<>(6);
        data.add(new FeedSource(null, R.string.news_feed));
        data.add(new FeedSource("friends", R.string.friends));
        data.add(new FeedSource("groups", R.string.groups));
        data.add(new FeedSource("pages", R.string.pages));
        data.add(new FeedSource("following", R.string.subscriptions));
        data.add(new FeedSource("recommendation", R.string.recommendation));
        return data;
    }

    private void refreshFeedSources() {
        final int accountId = super.getAccountId();

        appendDisposable(feedInteractor.getCachedFeedLists(accountId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(lists -> {
                    onFeedListsUpdated(lists);
                    requestActualFeedLists();
                }, ignored -> requestActualFeedLists()));
    }

    private void requestActualFeedLists() {
        final int accountId = super.getAccountId();
        appendDisposable(feedInteractor.getActualFeedLists(accountId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onFeedListsUpdated, ignore()));
    }

    private void onPostUpdateEvent(PostUpdate update) {
        if (nonNull(update.getLikeUpdate())) {
            PostUpdate.LikeUpdate like = update.getLikeUpdate();

            int index = indexOf(update.getOwnerId(), update.getPostId());
            if (index != -1) {
                this.mFeed.get(index).setLikeCount(like.getCount());
                this.mFeed.get(index).setUserLike(like.isLiked());
                callView(view -> view.notifyItemChanged(index));
            }
        }
    }

    private void requestFeedAtLast(final String startFrom) {
        this.loadingHolder.dispose();

        final int accountId = super.getAccountId();
        final String sourcesIds = this.mSourceIds;

        this.loadingNowNextFrom = startFrom;
        this.loadingNow = true;

        resolveLoadMoreFooterView();
        resolveRefreshingView();

        loadingHolder.append(feedInteractor.getActualFeed(accountId, 25, startFrom, "post", null, sourcesIds)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(pair -> onActualFeedReceived(startFrom, pair.getFirst(), pair.getSecond()), this::onActualFeedGetError));
    }

    private void onActualFeedGetError(Throwable t) {
        t.printStackTrace();

        this.loadingNow = false;
        this.loadingNowNextFrom = null;

        resolveLoadMoreFooterView();
        resolveRefreshingView();

        showError(getView(), t);
    }

    private void onActualFeedReceived(String startFrom, List<News> feed, String nextFrom) {
        this.loadingNow = false;
        this.loadingNowNextFrom = null;

        this.mNextFrom = nextFrom;

        if (isEmpty(startFrom)) {
            this.mFeed.clear();
            this.mFeed.addAll(feed);
            callView(IFeedView::notifyFeedDataChanged);
        } else {
            int startSize = this.mFeed.size();
            this.mFeed.addAll(feed);
            callView(view -> view.notifyDataAdded(startSize, feed.size()));
        }

        resolveRefreshingView();
        resolveLoadMoreFooterView();
    }

    @Override
    public void onGuiCreated(@NonNull IFeedView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayFeedSources(mFeedSources);

        int sourceIndex = getActiveFeedSourceIndex();
        if (sourceIndex != -1) {
            viewHost.scrollFeedSourcesToPosition(sourceIndex);
        }

        viewHost.displayFeed(mFeed, mTmpFeedScrollOnGuiReady);

        mTmpFeedScrollOnGuiReady = null;
    }

    private void setCacheLoadingNow(boolean cacheLoadingNow) {
        this.cacheLoadingNow = cacheLoadingNow;

        resolveRefreshingView();
        resolveLoadMoreFooterView();
    }

    private void loadCachedFeed(@Nullable String thenScrollToState) {
        final int accountId = super.getAccountId();

        setCacheLoadingNow(true);

        cacheLoadingHolder.append(feedInteractor
                .getCachedFeed(accountId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(feed -> onCachedFeedReceived(feed, thenScrollToState), ignore()));
    }

    @Override
    public void onDestroyed() {
        loadingHolder.dispose();
        cacheLoadingHolder.dispose();
        super.onDestroyed();
    }

    private void onCachedFeedReceived(List<News> data, @Nullable String thenScrollToState) {
        setCacheLoadingNow(false);

        this.mFeed.clear();
        this.mFeed.addAll(data);

        if (nonNull(thenScrollToState)) {
            if (isGuiReady()) {
                getView().displayFeed(mFeed, thenScrollToState);
            } else {
                mTmpFeedScrollOnGuiReady = thenScrollToState;
            }
        } else {
            if (isGuiReady()) {
                getView().notifyFeedDataChanged();
            }
        }

        if (mFeed.isEmpty()) {
            requestFeedAtLast(null);
        } else {
            if (Utils.needReloadNews()) {
                getView().askToReload();
            }
        }
    }

    private boolean canLoadNextNow() {
        return nonEmpty(mNextFrom) && !cacheLoadingNow && !loadingNow;
    }

    private void onFeedListsUpdated(List<FeedList> lists) {
        List<FeedSource> sources = new ArrayList<>(lists.size());

        for (FeedList list : lists) {
            sources.add(new FeedSource("list" + list.getId(), list.getTitle()));
        }

        mFeedSources.clear();
        mFeedSources.addAll(createDefaultFeedSources());
        mFeedSources.addAll(sources);

        int selected = refreshFeedSourcesSelection();

        if (isGuiReady()) {
            getView().notifyFeedSourcesChanged();

            if (selected != -1) {
                getView().scrollFeedSourcesToPosition(selected);
            }
        }
    }

    private int refreshFeedSourcesSelection() {
        int result = -1;
        for (int i = 0; i < mFeedSources.size(); i++) {
            FeedSource source = mFeedSources.get(i);

            if (isEmpty(mSourceIds) && isEmpty(source.getValue())) {
                source.setActive(true);
                result = i;
                continue;
            }

            if (nonEmpty(mSourceIds) && nonEmpty(source.getValue()) && mSourceIds.equals(source.getValue())) {
                source.setActive(true);
                result = i;
                continue;
            }

            source.setActive(false);
        }

        return result;
    }

    private void restoreNextFromAndFeedSources() {
        mSourceIds = Settings.get()
                .other()
                .getFeedSourceIds(getAccountId());

        mNextFrom = Settings.get()
                .other()
                .restoreFeedNextFrom(getAccountId());
    }

    private boolean isRefreshing() {
        return cacheLoadingNow || (loadingNow && isEmpty(loadingNowNextFrom));
    }

    private boolean isMoreLoading() {
        return loadingNow && nonEmpty(loadingNowNextFrom);
    }

    @OnGuiCreated
    private void resolveRefreshingView() {
        if (isGuiReady()) {
            getView().showRefreshing(isRefreshing());
        }
    }

    private int getActiveFeedSourceIndex() {
        for (int i = 0; i < mFeedSources.size(); i++) {
            if (mFeedSources.get(i).isActive()) {
                return i;
            }
        }

        return -1;
    }

    @OnGuiCreated
    private void resolveLoadMoreFooterView() {
        if (isGuiReady()) {
            if (mFeed.isEmpty() || isEmpty(mNextFrom)) {
                getView().setupLoadMoreFooter(LoadMoreState.END_OF_LIST);
            } else if (isMoreLoading()) {
                getView().setupLoadMoreFooter(LoadMoreState.LOADING);
            } else if (canLoadNextNow()) {
                getView().setupLoadMoreFooter(LoadMoreState.CAN_LOAD_MORE);
            } else {
                getView().setupLoadMoreFooter(LoadMoreState.END_OF_LIST);
            }
        }
    }

    public void fireScrollStateOnPause(String json) {
        Settings.get()
                .other()
                .storeFeedScrollState(getAccountId(), json);
    }

    public void fireRefresh() {
        this.cacheLoadingHolder.dispose();
        this.loadingHolder.dispose();
        this.loadingNow = false;
        this.cacheLoadingNow = false;

        requestFeedAtLast(null);
    }

    public void fireScrollToBottom() {
        if (canLoadNextNow()) {
            requestFeedAtLast(this.mNextFrom);
        }
    }

    public void fireLoadMoreClick() {
        if (canLoadNextNow()) {
            requestFeedAtLast(this.mNextFrom);
        }
    }

    public void fireFeedSourceClick(FeedSource entry) {
        this.mSourceIds = entry.getValue();
        this.mNextFrom = null;

        this.cacheLoadingHolder.dispose();
        this.loadingHolder.dispose();
        this.loadingNow = false;
        this.cacheLoadingNow = false;

        refreshFeedSourcesSelection();
        getView().notifyFeedSourcesChanged();

        requestFeedAtLast(null);
    }

    public void fireNewsShareLongClick(News news) {
        getView().goToReposts(getAccountId(), news.getType(), news.getSourceId(), news.getPostId());
    }

    public void fireNewsLikeLongClick(News news) {
        getView().goToLikes(getAccountId(), news.getType(), news.getSourceId(), news.getPostId());
    }

    public void fireNewsCommentClick(News news) {
        if ("post".equalsIgnoreCase(news.getType())) {
            getView().goToPostComments(getAccountId(), news.getPostId(), news.getSourceId());
        }
    }

    public void fireNewsBodyClick(News news) {
        if ("post".equals(news.getType())) {
            Post post = news.toPost();
            getView().openPost(getAccountId(), post);
        }
    }

    public void fireNewsRepostClick(News news) {
        if ("post".equals(news.getType())) {
            getView().repostPost(getAccountId(), news.toPost());
        }
    }

    public void fireLikeClick(News news) {
        if ("post".equalsIgnoreCase(news.getType())) {
            final boolean add = !news.isUserLike();
            int accountId = super.getAccountId();

            appendDisposable(walls.like(accountId, news.getSourceId(), news.getPostId(), add)
                    .compose(RxUtils.applySingleIOToMainSchedulers())
                    .subscribe(ignore(), ignore()));
        }
    }

    private int indexOf(int sourceId, int postId) {
        for (int i = 0; i < mFeed.size(); i++) {
            if (mFeed.get(i).getSourceId() == sourceId && mFeed.get(i).getPostId() == postId) {
                return i;
            }
        }

        return -1;
    }
}