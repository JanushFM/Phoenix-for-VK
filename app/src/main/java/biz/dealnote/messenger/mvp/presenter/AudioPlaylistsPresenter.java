package biz.dealnote.messenger.mvp.presenter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.AudioPlaylist;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAudioPlaylistsView;
import biz.dealnote.messenger.util.FindAt;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class AudioPlaylistsPresenter extends AccountDependencyPresenter<IAudioPlaylistsView> {

    private static final int SEARCH_COUNT = 20;
    private static final int WEB_SEARCH_DELAY = 1000;
    private final List<AudioPlaylist> pages;
    private final IAudioInteractor fInteractor;
    private final int owner_id;
    private final CompositeDisposable actualDataDisposable = new CompositeDisposable();
    private boolean actualDataReceived;
    private boolean endOfContent;
    private boolean actualDataLoading;
    private FindAt search_at;

    public AudioPlaylistsPresenter(int accountId, int ownerId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.owner_id = ownerId;
        this.pages = new ArrayList<>();
        this.fInteractor = InteractorFactory.createAudioInteractor();
        this.search_at = new FindAt();
    }

    public void LoadAudiosTool() {
        loadActualData(0);
    }

    public int getOwner_id() {
        return owner_id;
    }

    @Override
    public void onGuiCreated(@NonNull IAudioPlaylistsView view) {
        super.onGuiCreated(view);
        view.displayData(this.pages);
    }

    private void loadActualData(int offset) {
        this.actualDataLoading = true;

        resolveRefreshingView();

        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.getPlaylists(accountId, owner_id, offset)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onActualDataReceived(offset, data), this::onActualDataGetError));

    }

    private void onActualDataGetError(Throwable t) {
        this.actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private void onActualDataReceived(int offset, List<AudioPlaylist> data) {

        this.actualDataLoading = false;
        this.endOfContent = data.isEmpty();
        this.actualDataReceived = true;

        if (offset == 0) {
            this.pages.clear();
            this.pages.addAll(data);
            callView(IAudioPlaylistsView::notifyDataSetChanged);
        } else {
            int startSize = this.pages.size();
            this.pages.addAll(data);
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
        if (!endOfContent && nonEmpty(pages) && actualDataReceived && !actualDataLoading) {
            loadActualData(this.pages.size());
            return false;
        }
        return true;
    }

    private void doSearch(int accountId) {
        this.actualDataLoading = true;
        resolveRefreshingView();
        actualDataDisposable.add(fInteractor.search_owner_playlist(accountId, search_at.getQuery(), owner_id, SEARCH_COUNT, search_at.getOffset(), 0)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(playlist -> onSearched(playlist.getFirst(), playlist.getSecond()), this::onActualDataGetError));
    }

    private void onSearched(FindAt search_at, List<AudioPlaylist> playlist) {
        this.actualDataLoading = false;
        this.actualDataReceived = true;
        this.endOfContent = search_at.isEnded();

        if (this.search_at.getOffset() == 0) {
            pages.clear();
            pages.addAll(playlist);
            callView(IAudioPlaylistsView::notifyDataSetChanged);
        } else {
            if (nonEmpty(playlist)) {
                int startSize = pages.size();
                pages.addAll(playlist);
                callView(view -> view.notifyDataAdded(startSize, playlist.size()));
            }
        }
        this.search_at = search_at;
        resolveRefreshingView();
    }

    private void search(boolean sleep_search) {
        if (this.actualDataLoading) return;
        int accountId = super.getAccountId();

        if (!sleep_search) {
            doSearch(accountId);
            return;
        }

        actualDataDisposable.add(Single.just(new Object())
                .delay(WEB_SEARCH_DELAY, TimeUnit.MILLISECONDS)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(videos -> doSearch(accountId), this::onActualDataGetError));
    }

    public void fireSearchRequestChanged(String q) {
        String query = q == null ? null : q.trim();
        if (!search_at.do_compare(query)) {
            this.actualDataLoading = false;
            if (Utils.isEmpty(query)) {
                this.actualDataDisposable.clear();
                fireRefresh(false);
            } else {
                fireRefresh(true);
            }
        }
    }

    public void onDelete(int index, AudioPlaylist album) {
        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.deletePlaylist(accountId, album.getId(), album.getOwnerId())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> {
                    pages.remove(index);
                    callView(view -> view.notifyDataSetChanged());
                    getView().getPhoenixToast().showToast(R.string.success);
                }, throwable ->
                        getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage())));
    }

    public void onAdd(AudioPlaylist album) {
        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.followPlaylist(accountId, album.getId(), album.getOwnerId(), album.getAccess_key())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> getView().getPhoenixToast().showToast(R.string.success), throwable ->
                        getView().getPhoenixToast().showToastError(throwable.getLocalizedMessage())));
    }

    public void fireRefresh(boolean sleep_search) {

        this.actualDataDisposable.clear();
        this.actualDataLoading = false;

        if (this.search_at.isSearchMode()) {
            this.search_at.reset();
            search(sleep_search);
        } else {
            loadActualData(0);
        }
    }
}
