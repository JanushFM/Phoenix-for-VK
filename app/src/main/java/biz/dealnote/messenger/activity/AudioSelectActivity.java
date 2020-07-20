package biz.dealnote.messenger.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.fragment.AudioSelectTabsFragment;
import biz.dealnote.messenger.fragment.search.SingleTabSearchFragment;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceProvider;
import biz.dealnote.messenger.util.Objects;

public class AudioSelectActivity extends NoMainActivity implements PlaceProvider {

    /**
     * @param accountId От чьего имени получать
     */
    public static Intent createIntent(Context context, int accountId) {
        return new Intent(context, AudioSelectActivity.class)
                .putExtra(Extra.ACCOUNT_ID, accountId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Objects.isNull(savedInstanceState)) {
            int accountId = getIntent().getExtras().getInt(Extra.ACCOUNT_ID);
            attachInitialFragment(accountId);
        }
    }

    private void attachInitialFragment(int accountId) {
        AudioSelectTabsFragment fragment = AudioSelectTabsFragment.newInstance(accountId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(getMainContainerViewId(), fragment)
                .addToBackStack("audio-select")
                .commit();
    }

    @Override
    public void openPlace(Place place) {
        if (place.type == Place.SINGLE_SEARCH) {
            SingleTabSearchFragment singleTabSearchFragment = SingleTabSearchFragment.newInstance(place.getArgs());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(getMainContainerViewId(), singleTabSearchFragment)
                    .addToBackStack("audio-search-select")
                    .commit();
        }
    }
}
