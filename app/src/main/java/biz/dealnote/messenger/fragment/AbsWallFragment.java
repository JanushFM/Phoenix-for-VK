package biz.dealnote.messenger.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.adapter.WallAdapter;
import biz.dealnote.messenger.adapter.horizontal.HorizontalStoryAdapter;
import biz.dealnote.messenger.fragment.base.PlaceSupportMvpFragment;
import biz.dealnote.messenger.fragment.search.SearchContentType;
import biz.dealnote.messenger.fragment.search.criteria.WallSearchCriteria;
import biz.dealnote.messenger.link.LinkHelper;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment;
import biz.dealnote.messenger.modalbottomsheetdialogfragment.OptionRequest;
import biz.dealnote.messenger.model.EditingPostType;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.Owner;
import biz.dealnote.messenger.model.ParcelableOwnerWrapper;
import biz.dealnote.messenger.model.Photo;
import biz.dealnote.messenger.model.Post;
import biz.dealnote.messenger.model.Story;
import biz.dealnote.messenger.mvp.presenter.AbsWallPresenter;
import biz.dealnote.messenger.mvp.view.IVideosListView;
import biz.dealnote.messenger.mvp.view.IVkPhotosView;
import biz.dealnote.messenger.mvp.view.IWallView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.place.PlaceUtil;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.AppTextUtils;
import biz.dealnote.messenger.util.FindAttachmentType;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.LoadMoreFooterHelper;

import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.isEmpty;
import static biz.dealnote.messenger.util.Utils.isLandscape;

public abstract class AbsWallFragment<V extends IWallView, P extends AbsWallPresenter<V>>
        extends PlaceSupportMvpFragment<P, V> implements IWallView, WallAdapter.ClickListener, WallAdapter.NonPublishedPostActionListener {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WallAdapter mWallAdapter;
    private LoadMoreFooterHelper mLoadMoreFooterHelper;
    private HorizontalStoryAdapter mStoryAdapter;
    private FloatingActionButton fabCreate;
    private boolean isCreatePost = true;
    private final RecyclerView.OnScrollListener mFabScrollListener = new RecyclerView.OnScrollListener() {
        int scrollMinOffset;

        @Override
        public void onScrolled(@NotNull RecyclerView view, int dx, int dy) {
            if (scrollMinOffset == 0) {
                // one-time-init
                scrollMinOffset = (int) Utils.dpToPx(2, view.getContext());
            }

            if (dy > scrollMinOffset && fabCreate.isShown()) {
                fabCreate.hide();
            }

            if (dy < -scrollMinOffset && !fabCreate.isShown()) {
                fabCreate.show();
                if (view.getLayoutManager() instanceof LinearLayoutManager) {
                    LinearLayoutManager myLayoutManager = (LinearLayoutManager) view.getLayoutManager();
                    ToggleFab(myLayoutManager.findFirstVisibleItemPosition() > 7);
                }
            }
        }
    };

    public static Bundle buildArgs(int accoutnId, int ownerId, @Nullable Owner owner) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accoutnId);
        args.putInt(Extra.OWNER_ID, ownerId);
        args.putParcelable(Extra.OWNER, new ParcelableOwnerWrapper(owner));
        return args;
    }

    public static Fragment newInstance(Bundle args) {
        Fragment fragment;
        if (args.getInt(Extra.OWNER_ID) > 0) {
            fragment = new UserWallFragment();
        } else {
            fragment = new GroupWallFragment();
        }

        fragment.setArguments(args);
        return fragment;
    }

    protected static void setupCounter(TextView view, int count) {
        view.setText((count > 0 ? (AppTextUtils.getCounterWithK(count)) : "-"));
        view.setEnabled(count > 0);
    }

    private void ToggleFab(boolean isUp) {
        if (isUp && isCreatePost) {
            isCreatePost = false;
            fabCreate.setImageResource(R.drawable.ic_outline_keyboard_arrow_up);
        } else if (!isUp && !isCreatePost) {
            isCreatePost = true;
            fabCreate.setImageResource(R.drawable.pencil);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_wall, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());

        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        RecyclerView.LayoutManager manager;
        if (Utils.is600dp(requireActivity())) {
            boolean land = isLandscape(requireActivity());
            manager = new StaggeredGridLayoutManager(land ? 2 : 1, StaggeredGridLayoutManager.VERTICAL);
        } else {
            manager = new LinearLayoutManager(requireActivity());
        }

        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(manager);
        recyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
        recyclerView.addOnScrollListener(mFabScrollListener);
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToEnd();
            }
        });

        View headerView = inflater.inflate(headerLayout(), recyclerView, false);
        onHeaderInflated(headerView);

        View footerView = inflater.inflate(R.layout.footer_load_more, recyclerView, false);
        mLoadMoreFooterHelper = LoadMoreFooterHelper.createFrom(footerView, () -> getPresenter().fireLoadMoreClick());
        mLoadMoreFooterHelper.setEndOfListText("• • • • • • • •");

        fabCreate = root.findViewById(R.id.fragment_user_profile_fab);
        fabCreate.setOnClickListener(v -> {
            if (isCreatePost) {
                getPresenter().fireCreateClick();
            } else {
                recyclerView.scrollToPosition(0);
                ToggleFab(false);
            }
        });


        View headerStory = inflater.inflate(R.layout.header_story, recyclerView, false);
        RecyclerView headerStoryRecyclerView = headerStory.findViewById(R.id.header_story);
        headerStoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false));
        mStoryAdapter = new HorizontalStoryAdapter(Collections.emptyList());
        mStoryAdapter.setListener((item, pos) -> openHistoryVideo(Settings.get().accounts().getCurrent(), new ArrayList<>(getPresenter().getStories()), pos));
        headerStoryRecyclerView.setAdapter(mStoryAdapter);

        mWallAdapter = new WallAdapter(requireActivity(), Collections.emptyList(), this, this);
        mWallAdapter.addHeader(headerView);
        mWallAdapter.addHeader(headerStoryRecyclerView);
        mWallAdapter.addFooter(footerView);
        mWallAdapter.setNonPublishedPostActionListener(this);

        recyclerView.setAdapter(mWallAdapter);
        return root;
    }

    @Override
    public void onAvatarClick(int ownerId) {
        super.onOwnerClick(ownerId);
    }

    @Override
    public void showSnackbar(int res, boolean isLong) {
        if (nonNull(getView())) {
            Snackbar.make(getView(), res, isLong ? BaseTransientBottomBar.LENGTH_LONG : BaseTransientBottomBar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void openPhotoAlbum(int accountId, int ownerId, int albumId, ArrayList<Photo> photos, int position) {
        PlaceFactory.getPhotoAlbumGalleryPlace(accountId, albumId, ownerId, photos, position)
                .tryOpenWith(requireActivity());
    }

    @Override
    public void goToWallSearch(int accountId, int ownerId) {
        WallSearchCriteria criteria = new WallSearchCriteria("", ownerId);
        PlaceFactory.getSingleTabSearchPlace(accountId, SearchContentType.WALL, criteria).tryOpenWith(requireActivity());
    }

    @Override
    public void goToConversationAttachments(int accountId, int ownerId) {
        String[] types = {FindAttachmentType.TYPE_PHOTO, FindAttachmentType.TYPE_VIDEO, FindAttachmentType.TYPE_DOC, FindAttachmentType.TYPE_AUDIO,
                FindAttachmentType.TYPE_LINK, FindAttachmentType.TYPE_ALBUM, FindAttachmentType.TYPE_POST_WITH_COMMENT, FindAttachmentType.TYPE_POST_WITH_QUERY};

        ModalBottomSheetDialogFragment.Builder menus = new ModalBottomSheetDialogFragment.Builder();
        menus.add(new OptionRequest(0, getString(R.string.photos), R.drawable.photo_album));
        menus.add(new OptionRequest(1, getString(R.string.videos), R.drawable.video));
        menus.add(new OptionRequest(2, getString(R.string.documents), R.drawable.book));
        menus.add(new OptionRequest(3, getString(R.string.music), R.drawable.song));
        menus.add(new OptionRequest(4, getString(R.string.links), R.drawable.web));
        menus.add(new OptionRequest(5, getString(R.string.photo_album), R.drawable.album_photo));
        menus.add(new OptionRequest(6, getString(R.string.posts_with_comment), R.drawable.comment));
        menus.add(new OptionRequest(7, getString(R.string.posts_with_query), R.drawable.magnify));

        menus.show(getChildFragmentManager(), "attachments_select", option -> PlaceFactory.getWallAttachmentsPlace(accountId, ownerId, types[option.getId()]).tryOpenWith(requireActivity()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                getPresenter().fireRefresh();
                return true;
            case R.id.action_edit:
                getPresenter().fireEdit(requireActivity());
                return true;
            case R.id.action_copy_url:
                getPresenter().fireCopyUrlClick();
                return true;
            case R.id.action_copy_id:
                getPresenter().fireCopyIdClick();
                return true;
            case R.id.action_add_to_news:
                getPresenter().fireAddToNewsClick();
                return true;
            case R.id.action_search:
                getPresenter().fireSearchClick();
                return true;
            case R.id.wall_attachments:
                getPresenter().openConversationAttachments();
                return true;
            case R.id.search_stories:
                ModalBottomSheetDialogFragment.Builder menus = new ModalBottomSheetDialogFragment.Builder();
                menus.add(new OptionRequest(R.id.button_ok, getString(R.string.by_name), R.drawable.pencil));
                menus.add(new OptionRequest(R.id.button_cancel, getString(R.string.by_owner), R.drawable.person));
                menus.show(requireActivity().getSupportFragmentManager(), "search_story_options", option -> {
                    switch (option.getId()) {
                        case R.id.button_ok:
                            getPresenter().searchStory(true);
                            break;
                        case R.id.button_cancel:
                            getPresenter().searchStory(false);
                            break;
                    }
                });
                return true;
            case R.id.action_open_url:
                ClipboardManager clipBoard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipBoard != null && clipBoard.getPrimaryClip() != null && clipBoard.getPrimaryClip().getItemCount() > 0) {
                    String temp = clipBoard.getPrimaryClip().getItemAt(0).getText().toString();
                    LinkHelper.openUrl(getActivity(), getPresenter().getAccountId(), temp);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_wall, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NotNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        OptionView view = new OptionView();
        getPresenter().fireOptionViewCreated(view);

        menu.findItem(R.id.action_open_url).setVisible(view.isDebug);
        menu.findItem(R.id.action_copy_id).setVisible(view.isDebug);
        menu.findItem(R.id.search_stories).setVisible(view.isDebug);
        menu.findItem(R.id.action_add_to_news).setVisible(view.isDebug);
        menu.findItem(R.id.action_edit).setVisible(view.isMy);
    }

    @Override
    public void copyToClipboard(String label, String body) {
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, body);
        clipboard.setPrimaryClip(clip);

        getPhoenixToast().showToast(R.string.copied);
    }

    @Override
    public void goToPostCreation(int accountId, int ownerId, @EditingPostType int postType) {
        PlaceUtil.goToPostCreation(requireActivity(), accountId, ownerId, postType, null);
    }

    @Override
    public void showRefreshing(boolean refreshing) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    @LayoutRes
    protected abstract int headerLayout();

    protected abstract void onHeaderInflated(View headerRootView);

    @Override
    public void displayWallData(List<Post> data) {
        if (nonNull(mWallAdapter)) {
            mWallAdapter.setItems(data);
        }
    }

    @Override
    public void notifyWallDataSetChanged() {
        if (nonNull(mWallAdapter)) {
            mWallAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void updateStory(List<Story> stories) {
        if (nonNull(mStoryAdapter) && !isEmpty(stories)) {
            mStoryAdapter.setItems(stories);
            mStoryAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyWallItemChanged(int position) {
        if (nonNull(mWallAdapter)) {
            mWallAdapter.notifyItemChanged(position + mWallAdapter.getHeadersCount());
        }
    }

    @Override
    public void notifyWallDataAdded(int position, int count) {
        if (nonNull(mWallAdapter)) {
            mWallAdapter.notifyItemRangeInserted(position + mWallAdapter.getHeadersCount(), count);
        }
    }

    @Override
    public void notifyWallItemRemoved(int index) {
        if (nonNull(mWallAdapter)) {
            mWallAdapter.notifyItemRemoved(index + mWallAdapter.getHeadersCount());
        }
    }

    @Override
    public void onOwnerClick(int ownerId) {
        onOpenOwner(ownerId);
    }

    @Override
    public void onShareClick(Post post) {
        getPresenter().fireShareClick(post);
    }

    @Override
    public void onPostClick(Post post) {
        getPresenter().firePostBodyClick(post);
    }

    @Override
    public void onRestoreClick(Post post) {
        getPresenter().firePostRestoreClick(post);
    }

    @Override
    public void onCommentsClick(Post post) {
        getPresenter().fireCommentsClick(post);
    }

    @Override
    public void onLikeLongClick(Post post) {
        getPresenter().fireLikeLongClick(post);
    }

    @Override
    public void onShareLongClick(Post post) {
        getPresenter().fireShareLongClick(post);
    }

    @Override
    public void onLikeClick(Post post) {
        getPresenter().fireLikeClick(post);
    }

    @Override
    public void openPostEditor(int accountId, Post post) {
        PlaceUtil.goToPostEditor(requireActivity(), accountId, post);
    }

    @Override
    public void setupLoadMoreFooter(@LoadMoreState int state) {
        if (nonNull(mLoadMoreFooterHelper)) {
            mLoadMoreFooterHelper.switchToState(state);
        }
    }

    @Override
    public void openPhotoAlbums(int accountId, int ownerId, @Nullable Owner owner) {
        PlaceFactory.getVKPhotoAlbumsPlace(accountId, ownerId, IVkPhotosView.ACTION_SHOW_PHOTOS, ParcelableOwnerWrapper.wrap(owner))
                .tryOpenWith(requireActivity());
    }

    @Override
    public void openVideosLibrary(int accountId, int ownerId, @Nullable Owner owner) {
        PlaceFactory.getVideosPlace(accountId, ownerId, IVideosListView.ACTION_SHOW)
                .withParcelableExtra(Extra.OWNER, owner)
                .tryOpenWith(requireActivity());
    }

    @Override
    public void openAudios(int accountId, int ownerId, @Nullable Owner owner) {
        PlaceFactory.getAudiosPlace(accountId, ownerId)
                .withParcelableExtra(Extra.OWNER, owner)
                .tryOpenWith(requireActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onClearSelection();
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public void onButtonRemoveClick(Post post) {
        getPresenter().fireButtonRemoveClick(post);
    }

    protected static final class OptionView implements IOptionView {

        boolean isMy;

        boolean isDebug;

        @Override
        public void setIsMy(boolean my) {
            isMy = my;
        }

        @Override
        public void setIsDebug(boolean debug) {
            isDebug = debug;
        }
    }
}