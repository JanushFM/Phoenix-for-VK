package biz.dealnote.messenger.fragment.conversation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.fragment.base.PlaceSupportMvpFragment;
import biz.dealnote.messenger.listener.EndlessRecyclerOnScrollListener;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.mvp.presenter.conversations.BaseChatAttachmentsPresenter;
import biz.dealnote.messenger.mvp.view.conversations.IBaseChatAttachmentsView;
import biz.dealnote.messenger.util.ViewUtils;

import static biz.dealnote.messenger.util.Objects.nonNull;

public abstract class AbsChatAttachmentsFragment<T, P extends BaseChatAttachmentsPresenter<T, V>, V extends IBaseChatAttachmentsView<T>>
        extends PlaceSupportMvpFragment<P, V> implements IBaseChatAttachmentsView<T> {

    public static final String TAG = ConversationPhotosFragment.class.getSimpleName();

    protected View root;
    protected RecyclerView mRecyclerView;
    protected TextView mEmpty;
    protected SwipeRefreshLayout mSwipeRefreshLayout;
    protected RecyclerView.Adapter mAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_photos, container, false);

        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        mRecyclerView = root.findViewById(android.R.id.list);
        mEmpty = root.findViewById(R.id.empty);

        RecyclerView.LayoutManager manager = createLayoutManager();
        mRecyclerView.setLayoutManager(manager);

        mRecyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onScrollToLastElement() {
                getPresenter().fireScrollToEnd();
            }
        });

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(getPresenter()::fireRefresh);

        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout, true);

        mAdapter = createAdapter();
        mRecyclerView.setAdapter(mAdapter);
        return root;
    }

    protected RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    protected abstract RecyclerView.LayoutManager createLayoutManager();

    public abstract RecyclerView.Adapter createAdapter();

    @Override
    public void notifyDataAdded(int position, int count) {
        if (nonNull(mAdapter)) {
            mAdapter.notifyItemRangeInserted(position, count);
        }
    }

    @Override
    public void notifyDatasetChanged() {
        if (nonNull(mAdapter)) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void showLoading(boolean loading) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.setRefreshing(loading);
        }
    }

    @Override
    public void setEmptyTextVisible(boolean visible) {
        if (nonNull(mEmpty)) {
            mEmpty.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setToolbarTitle(String title) {
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (nonNull(actionBar)) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public void setToolbarSubtitle(String subtitle) {
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (nonNull(actionBar)) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }
}
