package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import biz.dealnote.messenger.domain.IFeedbackInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.AnswerVKOfficialList;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAnswerVKOfficialView;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.RxUtils;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.nonEmpty;


public class AnswerVKOfficialPresenter extends AccountDependencyPresenter<IAnswerVKOfficialView> {

    private final AnswerVKOfficialList pages;

    private final IFeedbackInteractor fInteractor;
    private final CompositeDisposable actualDataDisposable = new CompositeDisposable();
    private boolean actualDataReceived;
    private boolean endOfContent;
    private boolean actualDataLoading;

    public AnswerVKOfficialPresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        pages = new AnswerVKOfficialList();
        pages.fields = new ArrayList<>();
        pages.items = new ArrayList<>();
        fInteractor = InteractorFactory.createFeedbackInteractor();

        loadActualData(0);
    }

    @Override
    public void onGuiCreated(@NonNull IAnswerVKOfficialView view) {
        super.onGuiCreated(view);
        view.displayData(pages);
    }

    private void loadActualData(int offset) {
        actualDataLoading = true;

        resolveRefreshingView();

        int accountId = getAccountId();
        actualDataDisposable.add(fInteractor.getOfficial(accountId, 100, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onActualDataReceived(offset, data), this::onActualDataGetError));
    }

    private void safelyMarkAsViewed() {
        int accountId = getAccountId();
        if (Settings.get().accounts().getType(accountId).equals("hacked"))
            return;

        appendDisposable(fInteractor.maskAaViewed(accountId)
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(() -> callView(IAnswerVKOfficialView::notifyUpdateCounter), ignore()));
    }

    private void onActualDataGetError(Throwable t) {
        actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private void onActualDataReceived(int offset, AnswerVKOfficialList data) {

        actualDataLoading = false;
        endOfContent = (data.items.size() < 100);
        actualDataReceived = true;

        if (offset == 0) {
            safelyMarkAsViewed();
            pages.items.clear();
            pages.fields.clear();
            pages.items.addAll(data.items);
            pages.fields.addAll(data.fields);
            callView(IAnswerVKOfficialView::notifyDataSetChanged);
        } else {
            int startSize = pages.items.size();

            pages.items.addAll(data.items);
            pages.fields.addAll(data.fields);
            callView(view -> view.notifyDataAdded(startSize, data.items.size()));
        }

        resolveRefreshingView();
    }

    @Override
    public void onGuiResumed() {
        super.onGuiResumed();
        resolveRefreshingView();
    }

    private void resolveRefreshingView() {
        if (isGuiResumed()) {
            getView().showRefreshing(actualDataLoading);
        }
    }

    @Override
    public void onDestroyed() {
        actualDataDisposable.dispose();
        super.onDestroyed();
    }

    public boolean fireScrollToEnd() {
        if (!endOfContent && nonEmpty(pages.items) && actualDataReceived && !actualDataLoading) {
            loadActualData(pages.items.size());
            return false;
        }
        return true;
    }

    public void fireRefresh() {

        actualDataDisposable.clear();
        actualDataLoading = false;

        loadActualData(0);
    }
}
