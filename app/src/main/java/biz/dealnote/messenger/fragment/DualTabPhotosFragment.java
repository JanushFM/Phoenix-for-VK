package biz.dealnote.messenger.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.activity.ActivityUtils;
import biz.dealnote.messenger.adapter.MyFragmentStatePagerAdapter;
import biz.dealnote.messenger.fragment.base.BaseFragment;
import biz.dealnote.messenger.fragment.base.BaseMvpFragment;
import biz.dealnote.messenger.listener.BackPressCallback;
import biz.dealnote.messenger.model.selection.AbsSelectableSource;
import biz.dealnote.messenger.model.selection.FileManagerSelectableSource;
import biz.dealnote.messenger.model.selection.LocalGallerySelectableSource;
import biz.dealnote.messenger.model.selection.LocalPhotosSelectableSource;
import biz.dealnote.messenger.model.selection.LocalVideosSelectableSource;
import biz.dealnote.messenger.model.selection.Sources;
import biz.dealnote.messenger.model.selection.Types;
import biz.dealnote.messenger.model.selection.VkPhotosSelectableSource;

import static biz.dealnote.messenger.util.Objects.nonNull;

/**
 * Created by admin on 15.04.2017.
 * phoenix
 */
public class DualTabPhotosFragment extends BaseFragment implements BackPressCallback {

    public static DualTabPhotosFragment newInstance(Sources sources) {
        Bundle args = new Bundle();
        args.putParcelable(Extra.SOURCES, sources);

        DualTabPhotosFragment fragment = new DualTabPhotosFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private Sources mSources;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSources = requireArguments().getParcelable(Extra.SOURCES);

        if (nonNull(savedInstanceState)) {
            this.mCurrentTab = savedInstanceState.getInt("mCurrentTab");
        }
    }

    private Adapter mPagerAdapter;
    private int mCurrentTab;

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mCurrentTab", mCurrentTab);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_dual_tab_photos, container, false);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(root.findViewById(R.id.toolbar));

        TabLayout tabLayout = root.findViewById(R.id.tablayout);

        ViewPager viewPager = root.findViewById(R.id.view_pager);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentTab = position;
            }
        });

        mPagerAdapter = new Adapter(getChildFragmentManager(), mSources);
        viewPager.setAdapter(mPagerAdapter);

        tabLayout.setupWithViewPager(viewPager, true);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionBar = ActivityUtils.supportToolbarFor(this);
        if (nonNull(actionBar)) {
            actionBar.setTitle(R.string.multiply_poll);
            actionBar.setSubtitle(null);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (nonNull(mPagerAdapter)) {
            Fragment fragment = mPagerAdapter.findFragmentByPosition(mCurrentTab);

            return !(fragment instanceof BackPressCallback) || ((BackPressCallback) fragment).onBackPressed();
        }

        return true;
    }

    private class Adapter extends MyFragmentStatePagerAdapter {

        private final Sources mSources;

        public Adapter(FragmentManager fm, Sources mSources) {
            super(fm);
            this.mSources = mSources;
        }

        @Override
        public Fragment getItem(int position) {
            AbsSelectableSource source = mSources.get(position);

            if (source instanceof LocalPhotosSelectableSource) {
                Bundle args = new Bundle();
                args.putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true);
                LocalImageAlbumsFragment fragment = new LocalImageAlbumsFragment();
                fragment.setArguments(args);
                return fragment;
            }

            if (source instanceof LocalGallerySelectableSource) {
                Bundle args = new Bundle();
                args.putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true);
                LocalPhotosFragment fragment = LocalPhotosFragment.newInstance(10, null);
                fragment.setArguments(args);
                return fragment;
            }

            if (source instanceof LocalVideosSelectableSource) {
                Bundle args = new Bundle();
                args.putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true);
                LocalVideosFragment fragment = LocalVideosFragment.newInstance();
                fragment.setArguments(args);
                return fragment;
            }

            if (source instanceof VkPhotosSelectableSource) {
                final VkPhotosSelectableSource vksource = (VkPhotosSelectableSource) source;
                VKPhotoAlbumsFragment fragment = VKPhotoAlbumsFragment.newInstance(vksource.getAccountId(), vksource.getOwnerId(), null, null);
                fragment.requireArguments().putBoolean(BaseMvpFragment.EXTRA_HIDE_TOOLBAR, true);
                return fragment;
            }

            if (source instanceof FileManagerSelectableSource) {
                Bundle args = new Bundle();
                args.putInt(Extra.ACTION, FileManagerFragment.SELECT_FILE);
                args.putBoolean(FileManagerFragment.EXTRA_SHOW_CANNOT_READ, true);

                FileManagerFragment fileManagerFragment = new FileManagerFragment();
                fileManagerFragment.setArguments(args);
                return fileManagerFragment;
            }

            throw new UnsupportedOperationException();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            @Types
            int tabtype = mSources.get(position).getType();

            switch (tabtype) {
                case Types.LOCAL_PHOTOS:
                    return getString(R.string.local_photos_tab_title);

                    case Types.LOCAL_GALLERY:
                        return getString(R.string.local_gallery_tab_title);

                case Types.VIDEOS:
                    return getString(R.string.videos);

                case Types.VK_PHOTOS:
                    return getString(R.string.vk_photos_tab_title);

                case Types.FILES:
                    return getString(R.string.files_tab_title);
            }

            throw new UnsupportedOperationException();
        }

        @Override
        public int getCount() {
            return mSources.count();
        }
    }
}