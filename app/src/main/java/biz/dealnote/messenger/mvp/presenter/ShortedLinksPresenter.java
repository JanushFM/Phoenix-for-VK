package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IUtilsInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.ShortLink;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IShortedLinksView;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Unixtime;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class ShortedLinksPresenter extends AccountDependencyPresenter<IShortedLinksView> {

    private final List<ShortLink> links;

    private final IUtilsInteractor fInteractor;

    private boolean actualDataReceived;

    private boolean endOfContent;
    private boolean actualDataLoading;
    private String mInput;
    private final CompositeDisposable actualDataDisposable = new CompositeDisposable();

    public ShortedLinksPresenter(int accountId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.links = new ArrayList<>();
        this.fInteractor = InteractorFactory.createUtilsInteractor();

        loadActualData(0);
    }

    @Override
    public void onGuiCreated(@NonNull IShortedLinksView view) {
        super.onGuiCreated(view);
        view.displayData(this.links);
    }

    private void loadActualData(int offset) {
        this.actualDataLoading = true;

        resolveRefreshingView();

        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.getLastShortenedLinks(accountId, 10, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onActualDataReceived(offset, data), this::onActualDataGetError));

    }

    private void onActualDataGetError(Throwable t) {
        this.actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private void onActualDataReceived(int offset, List<ShortLink> data) {

        this.actualDataLoading = false;
        this.endOfContent = data.isEmpty();
        this.actualDataReceived = true;

        if (offset == 0) {
            this.links.clear();
            this.links.addAll(data);
            callView(IShortedLinksView::notifyDataSetChanged);
        } else {
            int startSize = this.links.size();
            this.links.addAll(data);
            callView(view -> view.notifyDataAdded(startSize, data.size()));
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
        if (!endOfContent && nonEmpty(links) && actualDataReceived && !actualDataLoading) {
            loadActualData(this.links.size());
            return false;
        }
        return true;
    }

    public void fireDelete(int index, ShortLink link) {
        actualDataDisposable.add(fInteractor.deleteFromLastShortened(getAccountId(), link.getKey())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> {
                    links.remove(index);
                    callView(IShortedLinksView::notifyDataSetChanged);
                }, this::onActualDataGetError));
    }

    public void fireRefresh() {

        this.actualDataDisposable.clear();
        this.actualDataLoading = false;

        loadActualData(0);
    }

    public void fireInputEdit(CharSequence s) {
        mInput = s.toString();
    }

    public void fireShort() {
        actualDataDisposable.add(fInteractor.getShortLink(getAccountId(), mInput, 1)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> {
                    data.setTimestamp(Unixtime.now());
                    data.setViews(0);
                    links.add(0, data);
                    callView(IShortedLinksView::notifyDataSetChanged);
                    callView(view -> view.updateLink(data.getShort_url()));
                }, this::onActualDataGetError));
    }

    public void fireValidate() {
        actualDataDisposable.add(fInteractor.checkLink(getAccountId(), mInput)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> {
                    callView(view -> view.updateLink(data.link));
                    callView(view -> view.showLinkStatus(data.status));
                }, this::onActualDataGetError));
    }
}
