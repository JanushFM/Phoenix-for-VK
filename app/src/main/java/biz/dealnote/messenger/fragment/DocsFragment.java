package biz.dealnote.messenger.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityFeatures;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.activity.DualTabPhotoActivity;
import biz.dealnote.messenger.adapter.DocsAdapter;
import biz.dealnote.messenger.adapter.DocsAsImagesAdapter;
import biz.dealnote.messenger.adapter.DocsUploadAdapter;
import biz.dealnote.messenger.adapter.base.RecyclerBindableAdapter;
import biz.dealnote.messenger.adapter.horizontal.HorizontalOptionsAdapter;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.OnSectionResumeCallback;
import biz.dealnote.messenger.listener.PicassoPauseOnScrollListener;
import biz.dealnote.messenger.model.DocFilter;
import biz.dealnote.messenger.model.Document;
import biz.dealnote.messenger.model.LocalPhoto;
import biz.dealnote.messenger.model.selection.FileManagerSelectableSource;
import biz.dealnote.messenger.model.selection.LocalPhotosSelectableSource;
import biz.dealnote.messenger.model.selection.Sources;
import biz.dealnote.messenger.mvp.presenter.DocsListPresenter;
import biz.dealnote.messenger.mvp.view.IDocListView;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.upload.Upload;
import biz.dealnote.messenger.util.ViewUtils;
import biz.dealnote.mvp.core.IPresenterFactory;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.nonEmpty;

public class DocsFragment extends BaseMvpFragment<DocsListPresenter, IDocListView>
        implements IDocListView, DocsAdapter.ActionListener, DocsUploadAdapter.ActionListener, DocsAsImagesAdapter.ActionListener {

    private static final int PERM_REQUEST_READ_STORAGE = 17;
    private static final int REQUEST_CODE_FILE = 115;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerBindableAdapter mDocsAdapter;
    private DocsUploadAdapter mUploadAdapter;
    private HorizontalOptionsAdapter<DocFilter> mFiltersAdapter;
    private View mHeaderView;
    private RecyclerView mRecyclerView;
    private View mUploadRoot;
    private boolean mImagesOnly;

    public static Bundle buildArgs(int accountId, int ownerId, String action) {
        Bundle args = new Bundle();
        args.putInt(Extra.ACCOUNT_ID, accountId);
        args.putInt(Extra.OWNER_ID, ownerId);
        args.putString(Extra.ACTION, action);
        return args;
    }

    public static DocsFragment newInstance(Bundle args) {
        DocsFragment fragment = new DocsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static DocsFragment newInstance(int accountId, int ownerId, String action) {
        return newInstance(buildArgs(accountId, ownerId, action));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_docs, container, false);

        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        mSwipeRefreshLayout = root.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(() -> getPresenter().fireRefresh());

        ViewUtils.setupSwipeRefreshLayoutWithCurrentTheme(requireActivity(), mSwipeRefreshLayout);

        mRecyclerView = root.findViewById(R.id.recycler_view);

        // тут, значит, некая многоходовочка
        // Так как мы не знаем, какой тип данных мы показываем (фото или просто документы),
        // то при создании view мы просим presenter уведомить об этом типе.
        // Предполагается, что presenter НЕЗАМЕДЛИТЕЛЬНО вызовет у view метод setAdapterType(boolean imagesOnly)
        getPresenter().pleaseNotifyViewAboutAdapterType();
        // и мы дальше по коду можем использовать переменную mImagesOnly

        mRecyclerView.setLayoutManager(createLayoutManager(mImagesOnly));

        mDocsAdapter = createAdapter(mImagesOnly, Collections.emptyList());

        FloatingActionButton buttonAdd = root.findViewById(R.id.add_button);
        buttonAdd.setOnClickListener(v -> getPresenter().fireButtonAddClick());

        RecyclerView uploadRecyclerView = root.findViewById(R.id.uploads_recycler_view);
        uploadRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false));

        mUploadAdapter = new DocsUploadAdapter(Collections.emptyList(), this);

        uploadRecyclerView.setAdapter(mUploadAdapter);

        mHeaderView = View.inflate(requireActivity(), R.layout.header_feed, null);

        RecyclerView headerRecyclerView = mHeaderView.findViewById(R.id.header_list);
        headerRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false));

        mFiltersAdapter = new HorizontalOptionsAdapter<>(Collections.emptyList());
        mFiltersAdapter.setListener(entry -> getPresenter().fireFilterClick(entry));

        headerRecyclerView.setAdapter(mFiltersAdapter);

        mDocsAdapter.addHeader(mHeaderView);
        mRecyclerView.setAdapter(mDocsAdapter);

        mUploadRoot = root.findViewById(R.id.uploads_root);

        mRecyclerView.addOnScrollListener(new PicassoPauseOnScrollListener(Constants.PICASSO_TAG));
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FILE && resultCode == Activity.RESULT_OK) {
            String file = data.getStringExtra(FileManagerFragment.returnFileParameter);

            ArrayList<LocalPhoto> photos = data.getParcelableArrayListExtra(Extra.PHOTOS);

            if (nonEmpty(file)) {
                getPresenter().fireFileForUploadSelected(file);
            } else if (nonEmpty(photos)) {
                getPresenter().fireLocalPhotosForUploadSelected(photos);
            }
        }
    }

    private RecyclerView.LayoutManager createLayoutManager(boolean asImages) {
        if (asImages) {
            int columnCount = getResources().getInteger(R.integer.local_gallery_column_count);
            return new GridLayoutManager(requireActivity(), columnCount);
        } else {
            return new LinearLayoutManager(requireActivity());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void displayData(List<Document> documents, boolean asImages) {
        mImagesOnly = asImages;

        if (isNull(mRecyclerView)) {
            return;
        }

        if (asImages && mDocsAdapter instanceof DocsAsImagesAdapter) {
            mDocsAdapter.setItems(documents);
            return;
        }

        if (!asImages && mDocsAdapter instanceof DocsAdapter) {
            mDocsAdapter.setItems(documents);
            return;
        }

        if (asImages) {
            DocsAsImagesAdapter docsAsImagesAdapter = new DocsAsImagesAdapter(documents);
            docsAsImagesAdapter.setActionListener(this);
            mDocsAdapter = docsAsImagesAdapter;
        } else {
            DocsAdapter docsAdapter = new DocsAdapter(documents);
            docsAdapter.setActionListener(this);
            mDocsAdapter = docsAdapter;
        }

        mRecyclerView.setLayoutManager(createLayoutManager(asImages));

        mDocsAdapter = createAdapter(asImages, documents);
        mDocsAdapter.addHeader(mHeaderView);

        mRecyclerView.setAdapter(mDocsAdapter);
    }

    private RecyclerBindableAdapter createAdapter(boolean asImages, List<Document> documents) {
        if (asImages) {
            DocsAsImagesAdapter docsAsImagesAdapter = new DocsAsImagesAdapter(documents);
            docsAsImagesAdapter.setActionListener(this);
            return docsAsImagesAdapter;
        } else {
            DocsAdapter docsAdapter = new DocsAdapter(documents);
            docsAdapter.setActionListener(this);
            return docsAdapter;
        }
    }

    @Override
    public void showRefreshing(boolean refreshing) {
        if (nonNull(mSwipeRefreshLayout)) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(refreshing));
        }
    }

    @Override
    public void notifyDataSetChanged() {
        if (nonNull(mDocsAdapter)) {
            mDocsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataAdd(int position, int count) {
        if (nonNull(mDocsAdapter)) {
            mDocsAdapter.notifyItemRangeInserted(position, count);
        }
    }

    @Override
    public void openDocument(int accountId, @NonNull Document document) {
        PlaceFactory.getDocPreviewPlace(accountId, document).tryOpenWith(requireActivity());
    }

    @Override
    public void returnSelection(ArrayList<Document> docs) {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(Extra.ATTACHMENTS, docs);
        requireActivity().setResult(Activity.RESULT_OK, intent);
        requireActivity().finish();
    }

    @Override
    public void goToGifPlayer(int accountId, @NonNull ArrayList<Document> gifs, int selected) {
        PlaceFactory.getGifPagerPlace(accountId, gifs, selected).tryOpenWith(requireActivity());
    }

    @Override
    public void requestReadExternalStoragePermission() {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERM_REQUEST_READ_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQUEST_READ_STORAGE) {
            getPresenter().fireReadPermissionResolved();
        }
    }

    @Override
    public void startSelectUploadFileActivity(int accountId) {
        Sources sources = new Sources()
                .with(new FileManagerSelectableSource())
                .with(new LocalPhotosSelectableSource());

        Intent intent = DualTabPhotoActivity.createIntent(requireActivity(), 10, sources);
        startActivityForResult(intent, REQUEST_CODE_FILE);
    }

    @Override
    public void setUploadDataVisible(boolean visible) {
        if (nonNull(mUploadRoot)) {
            mUploadRoot.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void displayUploads(List<Upload> data) {
        if (nonNull(mUploadAdapter)) {
            mUploadAdapter.setData(data);
        }
    }

    @Override
    public void notifyUploadDataChanged() {
        if (nonNull(mUploadAdapter)) {
            mUploadAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyUploadItemsAdded(int position, int count) {
        if (nonNull(mUploadAdapter)) {
            mUploadAdapter.notifyItemRangeInserted(position, count);
        }
    }

    @Override
    public void notifyUploadItemChanged(int position) {
        if (nonNull(mUploadAdapter)) {
            mUploadAdapter.notifyItemChanged(position);
        }
    }

    @Override
    public void notifyUploadItemRemoved(int position) {
        if (nonNull(mUploadAdapter)) {
            mUploadAdapter.notifyItemRemoved(position);
        }
    }

    @Override
    public void notifyUploadProgressChanged(int position, int progress, boolean smoothly) {
        if (nonNull(mUploadAdapter)) {
            mUploadAdapter.changeUploadProgress(position, progress, smoothly);
        }
    }

    @Override
    public void displayFilterData(List<DocFilter> filters) {
        if (nonNull(mFiltersAdapter)) {
            mFiltersAdapter.setItems(filters);
        }
    }

    @Override
    public void notifyFiltersChanged() {
        if (nonNull(mFiltersAdapter)) {
            mFiltersAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void setAdapterType(boolean imagesOnly) {
        mImagesOnly = imagesOnly;
    }

    @NotNull
    @Override
    public IPresenterFactory<DocsListPresenter> getPresenterFactory(@Nullable Bundle saveInstanceState) {
        return () -> new DocsListPresenter(
                requireArguments().getInt(Extra.ACCOUNT_ID),
                requireArguments().getInt(Extra.OWNER_ID),
                requireArguments().getString(Extra.ACTION),
                saveInstanceState
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.get().ui().notifyPlaceResumed(Place.DOCS);

        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (actionBar != null) {
            actionBar.setTitle(R.string.documents);
            actionBar.setSubtitle(null);
        }

        if (requireActivity() instanceof OnSectionResumeCallback) {
            ((OnSectionResumeCallback) requireActivity()).onSectionResume(AdditionalNavigationFragment.SECTION_ITEM_DOCS);
        }

        new ActivityFeatures.Builder()
                .begin()
                .setHideNavigationMenu(false)
                .setBarsColored(requireActivity(), true)
                .build()
                .apply(requireActivity());
    }

    @Override
    public void onDocClick(int index, @NonNull Document doc) {
        getPresenter().fireDocClick(doc);
    }

    @Override
    public boolean onDocLongClick(int index, @NonNull Document doc) {
        return false;
    }

    @Override
    public void onRemoveClick(Upload upload) {
        getPresenter().fireRemoveClick(upload);
    }
}