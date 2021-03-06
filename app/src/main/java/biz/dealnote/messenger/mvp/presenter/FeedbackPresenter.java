package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IFeedbackInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.feedback.Feedback;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.IFeedbackView;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.nonEmpty;


public class FeedbackPresenter extends PlaceSupportPresenter<IFeedbackView> {

    private static final String TAG = FeedbackPresenter.class.getSimpleName();
    private static final int COUNT_PER_REQUEST = 15;

    private final List<Feedback> mData;
    private final IFeedbackInteractor feedbackInteractor;
    private final CompositeDisposable cacheDisposable = new CompositeDisposable();
    private final CompositeDisposable netDisposable = new CompositeDisposable();
    private String mNextFrom;
    private boolean actualDataReceived;
    private boolean mEndOfContent;
    private boolean cacheLoadingNow;
    private boolean netLoadingNow;
    private String netLoadingStartFrom;

    public FeedbackPresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);

        feedbackInteractor = InteractorFactory.createFeedbackInteractor();
        mData = new ArrayList<>();

        loadAllFromDb();
        requestActualData(null);
    }

    @OnGuiCreated
    private void resolveLoadMoreFooter() {
        if (!isGuiReady()) return;

        if (isEmpty(mData)) {
            getView().configLoadMore(LoadMoreState.INVISIBLE);
            return;
        }

        if (nonEmpty(mData) && netLoadingNow && nonEmpty(netLoadingStartFrom)) {
            getView().configLoadMore(LoadMoreState.LOADING);
            return;
        }

        if (canLoadMore()) {
            getView().configLoadMore(LoadMoreState.CAN_LOAD_MORE);
            return;
        }

        getView().configLoadMore(LoadMoreState.END_OF_LIST);
    }

    private void requestActualData(String startFrom) {
        netDisposable.clear();

        netLoadingNow = true;
        netLoadingStartFrom = startFrom;

        int accountId = getAccountId();

        resolveLoadMoreFooter();
        resolveSwiperefreshLoadingView();

        netDisposable.add(feedbackInteractor.getActualFeedbacks(accountId, COUNT_PER_REQUEST, startFrom)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(pair -> onActualDataReceived(startFrom, pair.getFirst(), pair.getSecond()), this::onActualDataGetError));
    }

    private void onActualDataGetError(Throwable t) {
        t.printStackTrace();

        netLoadingNow = false;
        netLoadingStartFrom = null;

        showError(getView(), getCauseIfRuntime(t));

        resolveLoadMoreFooter();
        resolveSwiperefreshLoadingView();
    }

    private void safelyMarkAsViewed() {
        int accountId = getAccountId();
        if (Settings.get().accounts().getType(accountId).equals("hacked"))
            return;

        appendDisposable(feedbackInteractor.maskAaViewed(accountId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> callView(IFeedbackView::notifyUpdateCounter), ignore()));
    }

    private void onActualDataReceived(String startFrom, List<Feedback> feedbacks, String nextFrom) {
        if (isEmpty(startFrom)) {
            safelyMarkAsViewed();
        }

        cacheDisposable.clear();
        cacheLoadingNow = false;
        netLoadingNow = false;
        netLoadingStartFrom = null;
        mNextFrom = nextFrom;
        mEndOfContent = isEmpty(nextFrom);
        actualDataReceived = true;

        if (isEmpty(startFrom)) {
            mData.clear();
            mData.addAll(feedbacks);
            callView(IFeedbackView::notifyDataSetChanged);
        } else {
            int sizeBefore = mData.size();
            mData.addAll(feedbacks);
            callView(view -> view.notifyDataAdding(sizeBefore, feedbacks.size()));
        }

        resolveLoadMoreFooter();
        resolveSwiperefreshLoadingView();
    }

    @OnGuiCreated
    private void resolveSwiperefreshLoadingView() {
        if (isGuiReady()) {
            getView().showLoading(netLoadingNow && isEmpty(netLoadingStartFrom));
        }
    }

    private boolean canLoadMore() {
        return nonEmpty(mNextFrom) && !mEndOfContent && !cacheLoadingNow && !netLoadingNow && actualDataReceived;
    }

    @Override
    public void onGuiCreated(@NonNull IFeedbackView viewHost) {
        super.onGuiCreated(viewHost);
        viewHost.displayData(mData);
    }

    private void loadAllFromDb() {
        cacheLoadingNow = true;
        int accountId = getAccountId();

        cacheDisposable.add(feedbackInteractor.getCachedFeedbacks(accountId)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onCachedDataReceived, Throwable::printStackTrace));
    }

    private void onCachedDataReceived(List<Feedback> feedbacks) {
        cacheLoadingNow = false;
        mData.clear();
        mData.addAll(feedbacks);

        callView(IFeedbackView::notifyDataSetChanged);
    }

    @Override
    public void onDestroyed() {
        cacheDisposable.dispose();
        netDisposable.dispose();
        super.onDestroyed();
    }

    public void fireItemClick(@NonNull Feedback notification) {
        getView().showLinksDialog(getAccountId(), notification);
    }

    public void fireLoadMoreClick() {
        if (canLoadMore()) {
            requestActualData(mNextFrom);
        }
    }

    public void fireRefresh() {
        cacheDisposable.clear();
        cacheLoadingNow = false;

        netDisposable.clear();
        netLoadingNow = false;
        netLoadingStartFrom = null;

        requestActualData(null);
    }

    public void fireScrollToLast() {
        if (canLoadMore()) {
            requestActualData(mNextFrom);
        }
    }
}