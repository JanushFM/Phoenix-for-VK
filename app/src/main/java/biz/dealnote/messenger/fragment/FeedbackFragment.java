package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.activity.MainActivity;
import biz.dealnote.messenger.adapter.feedback.FeedbackAdapter;
import biz.dealnote.messenger.dialog.FeedbackLinkDialog;
import biz.dealnote.messenger.fragment.base.PlaceSupportMvpFragment;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.model.LoadMoreState;
import biz.dealnote.messenger.model.feedback.Feedback;
import biz.dealnote.messenger.mvp.presenter.FeedbackPresenter;
import biz.dealnote.messenger.mvp.view.IFeedbackView;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.messenger.view.LoadMoreFooterHelper;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

public class FeedbackFragment extends PlaceSupportMvpFragment<FeedbackPresenter, IFeedbackView> implements SwipeRefreshLayout.OnRefreshListener,
        IFeedbackView, FeedbackAdapter.ClickListener {

    private static final String TAG = FeedbackFragment.class.getSimpleName();

    private FeedbackAdapter mAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mEmptyText;
    private LoadMoreFooterHelper mLoadMoreHelper;

    public static Bundle buildArgs(int accountId) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        return args;
    }

    public static FeedbackFragment newInstance(int accountId) {
        return newInstance(buildArgs(accountId));
    }

    public static FeedbackFragment newInstance(Bundle args) {
        FeedbackFragment feedsFragment = new FeedbackFragment();
        feedsFragment.setArguments(args);
        return feedsFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_feedback, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        mEmptyText = root.findViewById(R.id.fragment_feedback_empty_text);
        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        RecyclerView.LayoutManager manager = new LinearLayoutManager(requireActivity());
        RecyclerView recyclerView = root.findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(manager);
        recyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToLast();
            }
        });

        View footerView = inflater.inflate(R.layout.footer_load_more, recyclerView, false);
        mLoadMoreHelper = LoadMoreFooterHelper.createFrom(footerView, getPresenter()::fireLoadMoreClick);
        mLoadMoreHelper.switchToState(LoadMoreState.INVISIBLE);
        mLoadMoreHelper.setEndOfListText("• • • • • • • •");

        mAdapter = new FeedbackAdapter(requireActivity(), Collections.emptyList(), this);
        mAdapter.addFooter(footerView);
        mAdapter.setClickListener(this);
        recyclerView.setAdapter(mAdapter);

        resolveEmptyTextVisibility();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.NOTIFICATIONS);

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.drawer_feedback);
            actionBar.setSubtitle(null);
        }

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_FEEDBACK);
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public void displayData(List<Feedback> data) {
        if (nonNull(mAdapter)) {
            mAdapter.setItems(data);
            resolveEmptyTextVisibility();
        }
    }

    @Override
    public void showLoading(boolean loading) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(loading));
        }
    }

    @Override
    public void notifyUpdateCounter() {
        ((MainActivity) requireActivity()).UpdateNotificationCount(Settings.get().accounts().getCurrent());
    }

    private void resolveEmptyTextVisibility() {
        if (nonNull(mEmptyText) && nonNull(mAdapter)) {
            mEmptyText.setVisibility(mAdapter.getRealItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void notifyDataAdding(int position, int count) {
        if (nonNull(mAdapter)) {
            mAdapter.notifyItemRangeInserted(position, count);
            resolveEmptyTextVisibility();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (nonNull(mAdapter)) {
            mAdapter.notifyDataSetChanged();
            resolveEmptyTextVisibility();
        }
    }

    @Override
    public void configLoadMore(@LoadMoreState int loadmoreState) {
        if (nonNull(mLoadMoreHelper)) {
            mLoadMoreHelper.switchToState(loadmoreState);
        }
    }

    @Override
    public void showLinksDialog(int accountId, @NonNull Feedback notification) {
        FeedbackLinkDialog.newInstance(accountId, notification).show(getParentFragmentManager(), "feedback_links");
    }

    @NotNull
    @Override
    public IPresenterFactory<FeedbackPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new FeedbackPresenter(requireArguments().getInt(Extra.ACCOUNT_ID), saveInstanceState);
    }

    @Override
    public void onNotificationClick(Feedback notification) {
        getPresenter().fireItemClick(notification);
    }

    @Override
    public void onRefresh() {
        getPresenter().fireRefresh();
    }
}