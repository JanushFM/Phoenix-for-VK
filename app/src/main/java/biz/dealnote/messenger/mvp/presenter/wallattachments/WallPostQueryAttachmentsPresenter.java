package biz.dealnote.messenger.mvp.presenter.wallattachments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.model.VKApiPost;
import biz.dealnote.messenger.domain.IWallsRepository;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.model.Article;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.Link;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.PhotoAlbum;
import biz.dealnote.messenger.model.Poll;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Video;
import biz.dealnote.messenger.model.criteria.WallCriteria;
import biz.dealnote.messenger.mvp.presenter.base.PlaceSupportPresenter;
import biz.dealnote.messenger.mvp.view.wallattachments.IWallPostQueryAttachmentsView;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.mvp.reflect.OnGuiCreated;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.util.RxUtils.dummy;
import static biz.dealnote.messenger.util.RxUtils.ignore;
import static biz.dealnote.messenger.util.Utils.getCauseIfRuntime;
import static biz.dealnote.messenger.util.Utils.safeCountOf;

public class WallPostQueryAttachmentsPresenter extends PlaceSupportPresenter<IWallPostQueryAttachmentsView> {

    private final ArrayList<Post> mPost;
    private final IWallsRepository fInteractor;
    private final int owner_id;
    private final CompositeDisposable actualDataDisposable = new CompositeDisposable();
    private int loaded;
    private boolean actualDataReceived;
    private boolean endOfContent;
    private boolean actualDataLoading;
    private String Query;

    public WallPostQueryAttachmentsPresenter(int accountId, int ownerId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.owner_id = ownerId;
        this.mPost = new ArrayList<>();
        this.fInteractor = Repository.INSTANCE.getWalls();
    }

    @Override
    public void onGuiCreated(@NonNull IWallPostQueryAttachmentsView view) {
        super.onGuiCreated(view);
        view.displayData(this.mPost);
    }

    private void loadActualData(int offset) {
        this.actualDataLoading = true;

        resolveRefreshingView();

        final int accountId = super.getAccountId();
        actualDataDisposable.add(fInteractor.getWallNoCache(accountId, owner_id, offset, 100, WallCriteria.MODE_ALL)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(data -> onActualDataReceived(offset, data), this::onActualDataGetError));

    }

    public void fireSearchRequestChanged(String q, boolean only_insert) {
        Query = q == null ? null : q.trim();
        if (only_insert) {
            return;
        }
        this.actualDataDisposable.clear();
        this.actualDataLoading = false;
        resolveRefreshingView();
        getView().onSetLoadingStatus(0);
        fireRefresh();
    }

    private void onActualDataGetError(Throwable t) {
        this.actualDataLoading = false;
        showError(getView(), getCauseIfRuntime(t));

        resolveRefreshingView();
    }

    private boolean check(final String data, final List<String> str) {
        for (String i : str) {
            if (data.toLowerCase().contains(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean doCompare(final String data, final List<String> str) {
        return Utils.safeCheck(data, () -> check(data, str));
    }

    private boolean checkDocs(final ArrayList<Document> docs, final List<String> str, final List<Integer> ids) {
        if (Utils.isEmpty(docs)) {
            return false;
        }
        for (Document i : docs) {
            if (doCompare(i.getTitle(), str) || doCompare(i.getExt(), str) || ids.contains(i.getOwnerId())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPhotos(final ArrayList<Photo> docs, final List<String> str, final List<Integer> ids) {
        if (Utils.isEmpty(docs)) {
            return false;
        }
        for (Photo i : docs) {
            if (doCompare(i.getText(), str) || ids.contains(i.getOwnerId())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkVideos(final ArrayList<Video> docs, final List<String> str, final List<Integer> ids) {
        if (Utils.isEmpty(docs)) {
            return false;
        }
        for (Video i : docs) {
            if (doCompare(i.getTitle(), str) || doCompare(i.getDescription(), str) || ids.contains(i.getOwnerId())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkAlbums(final ArrayList<PhotoAlbum> docs, final List<String> str, final List<Integer> ids) {
        if (Utils.isEmpty(docs)) {
            return false;
        }
        for (PhotoAlbum i : docs) {
            if (doCompare(i.getTitle(), str) || doCompare(i.getDescription(), str) || ids.contains(i.getOwnerId())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkLinks(final ArrayList<Link> docs, final List<String> str) {
        if (Utils.isEmpty(docs)) {
            return false;
        }
        for (Link i : docs) {
            if (doCompare(i.getTitle(), str) || doCompare(i.getDescription(), str) || doCompare(i.getCaption(), str)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkArticles(final ArrayList<Article> docs, final List<String> str, final List<Integer> ids) {
        if (Utils.isEmpty(docs)) {
            return false;
        }
        for (Article i : docs) {
            if (doCompare(i.getTitle(), str) || doCompare(i.getSubTitle(), str) || ids.contains(i.getOwnerId())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPoll(final ArrayList<Poll> docs, final List<String> str, final List<Integer> ids) {
        if (Utils.isEmpty(docs)) {
            return false;
        }
        for (Poll i : docs) {
            if (doCompare(i.getQuestion(), str) || ids.contains(i.getOwnerId())) {
                return true;
            }
        }
        return false;
    }

    private void update(List<Post> data, final List<String> str, final List<Integer> ids) {

        for (Post i : data) {
            if ((i.hasText() && doCompare(i.getText(), str)) || ids.contains(i.getOwnerId()) || ids.contains(i.getSignerId()) || ids.contains(i.getAuthorId())) {
                mPost.add(i);
            } else if (i.getAuthor() != null && doCompare(i.getAuthor().getFullName(), str)) {
                mPost.add(i);
            } else if (i.hasAttachments() && (checkDocs(i.getAttachments().getDocs(), str, ids)
                    || checkAlbums(i.getAttachments().getPhotoAlbums(), str, ids) || checkArticles(i.getAttachments().getArticles(), str, ids)
                    || checkLinks(i.getAttachments().getLinks(), str) || checkPhotos(i.getAttachments().getPhotos(), str, ids)
                    || checkVideos(i.getAttachments().getVideos(), str, ids) || checkPoll(i.getAttachments().getPolls(), str, ids))) {
                mPost.add(i);
            }
            if (i.hasCopyHierarchy())
                update(i.getCopyHierarchy(), str, ids);
        }
    }

    private void onActualDataReceived(int offset, List<Post> data) {

        this.actualDataLoading = false;
        this.endOfContent = data.isEmpty();
        this.actualDataReceived = true;
        if (this.endOfContent && isGuiResumed())
            getView().onSetLoadingStatus(2);

        String[] str = Query.split("\\|");
        for (int i = 0; i < str.length; i++) {
            str[i] = str[i].trim().toLowerCase();
        }

        List<Integer> ids = new ArrayList<>();
        for (String cc : str) {
            if (cc.contains("*id")) {
                try {
                    ids.add(Integer.parseInt(cc.replace("*id", "")));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (offset == 0) {
            this.loaded = data.size();
            this.mPost.clear();
            update(data, Arrays.asList(str), ids);
            resolveToolbar();
            callView(IWallPostQueryAttachmentsView::notifyDataSetChanged);
        } else {
            int startSize = mPost.size();
            this.loaded += data.size();
            update(data, Arrays.asList(str), ids);
            resolveToolbar();
            callView(view -> view.notifyDataAdded(startSize, mPost.size() - startSize));
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
            if (!endOfContent)
                getView().onSetLoadingStatus(actualDataLoading ? 1 : 0);
        }
    }

    @OnGuiCreated
    private void resolveToolbar() {
        if (isGuiReady()) {
            getView().setToolbarTitle(getString(R.string.attachments_in_wall));
            getView().setToolbarSubtitle(getString(R.string.query, safeCountOf(mPost)) + " " + getString(R.string.posts_analized, loaded));
        }
    }

    @Override
    public void onDestroyed() {
        actualDataDisposable.dispose();
        super.onDestroyed();
    }

    public boolean fireScrollToEnd() {
        if (Utils.isEmpty(Query)) {
            return true;
        }
        if (!endOfContent && actualDataReceived && !actualDataLoading) {
            loadActualData(loaded);
            return false;
        }
        return true;
    }

    public void fireRefresh() {
        if (Utils.isEmpty(Query)) {
            return;
        }
        this.actualDataDisposable.clear();
        this.actualDataLoading = false;

        loadActualData(0);
    }

    public void firePostBodyClick(Post post) {
        if (Utils.intValueIn(post.getPostType(), VKApiPost.Type.SUGGEST, VKApiPost.Type.POSTPONE)) {
            getView().openPostEditor(getAccountId(), post);
            return;
        }

        super.firePostClick(post);
    }

    public void firePostRestoreClick(Post post) {
        appendDisposable(fInteractor.restore(getAccountId(), post.getOwnerId(), post.getVkid())
                .compose(RxUtils.applyCompletableIOToMainSchedulers())
                .subscribe(dummy(), t -> showError(getView(), t)));
    }

    public void fireLikeLongClick(Post post) {
        getView().goToLikes(getAccountId(), "post", post.getOwnerId(), post.getVkid());
    }

    public void fireShareLongClick(Post post) {
        getView().goToReposts(getAccountId(), "post", post.getOwnerId(), post.getVkid());
    }

    public void fireLikeClick(Post post) {
        final int accountId = super.getAccountId();

        appendDisposable(fInteractor.like(accountId, post.getOwnerId(), post.getVkid(), !post.isUserLikes())
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(ignore(), t -> showError(getView(), t)));
    }
}
