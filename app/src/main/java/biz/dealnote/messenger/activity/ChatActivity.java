package biz.dealnote.messenger.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;

import biz.dealnote.messenger.Extra;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.fragment.AudioPlayerFragment;
import biz.dealnote.messenger.fragment.ChatFragment;
import biz.dealnote.messenger.fragment.GifPagerFragment;
import biz.dealnote.messenger.fragment.PhotoPagerFragment;
import biz.dealnote.messenger.fragment.StoryPagerFragment;
import biz.dealnote.messenger.listener.AppStyleable;
import biz.dealnote.messenger.model.Peer;
import biz.dealnote.messenger.place.Place;
import biz.dealnote.messenger.place.PlaceProvider;
import biz.dealnote.messenger.settings.CurrentTheme;
import biz.dealnote.messenger.util.AssertUtils;
import biz.dealnote.messenger.util.StatusbarUtil;
import biz.dealnote.messenger.util.Utils;
import biz.dealnote.messenger.util.ViewUtils;


public class ChatActivity extends NoMainActivity implements PlaceProvider, AppStyleable {

    public static final String ACTION_OPEN_PLACE = "biz.dealnote.messenger.activity.ChatActivity.openPlace";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (ACTION_OPEN_PLACE.equals(action)) {
            Place place = intent.getParcelableExtra(Extra.PLACE);
            openPlace(place);
        }
    }

    @Override
    public void openPlace(Place place) {
        final Bundle args = place.getArgs();
        switch (place.type) {
            case Place.CHAT:
                final Peer peer = args.getParcelable(Extra.PEER);
                AssertUtils.requireNonNull(peer);
                ChatFragment chatFragment = ChatFragment.Companion.newInstance(args.getInt(Extra.ACCOUNT_ID), args.getInt(Extra.OWNER_ID), peer);
                attachToFront(chatFragment);
                break;
            case Place.VK_PHOTO_ALBUM_GALLERY:

            case Place.FAVE_PHOTOS_GALLERY:

            case Place.SIMPLE_PHOTO_GALLERY:

            case Place.VK_PHOTO_TMP_SOURCE:
                attachToFront(PhotoPagerFragment.newInstance(place.type, args));
                break;
            case Place.GIF_PAGER:
                attachToFront(GifPagerFragment.newInstance(args));
                break;
            case Place.STORY_PLAYER:
                attachToFront(StoryPagerFragment.newInstance(args));
                break;
            case Place.PLAYER:
                Fragment player = getSupportFragmentManager().findFragmentByTag("audio_player");
                if (player instanceof AudioPlayerFragment)
                    ((AudioPlayerFragment) player).dismiss();
                AudioPlayerFragment.newInstance(args).show(getSupportFragmentManager(), "audio_player");
                break;
            default:
                Intent intent = new Intent(this, SwipebleActivity.class);
                intent.setAction(SwipebleActivity.ACTION_OPEN_PLACE);
                intent.putExtra(Extra.PLACE, place);
                SwipebleActivity.start(this, intent);
                break;
        }
    }

    private void attachToFront(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    @Override
    public void onDestroy() {
        ViewUtils.keyboardHide(this);
        super.onDestroy();
    }

    @Override
    public void hideMenu(boolean hide) {

    }

    @Override
    public void openMenu(boolean open) {

    }

    @Override
    public void setStatusbarColored(boolean colored, boolean invertIcons) {
        int statusbarNonColored = CurrentTheme.getStatusBarNonColored(this);
        int statusbarColored = CurrentTheme.getStatusBarColor(this);

        if (Utils.hasLollipop()) {
            Window w = getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(colored ? statusbarColored : statusbarNonColored);
            int navigationColor = colored ? CurrentTheme.getNavigationBarColor(this) : Color.BLACK;
            w.setNavigationBarColor(navigationColor);
        }

        if (Utils.hasMarshmallow()) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            if (invertIcons) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);

            StatusbarUtil.setCustomStatusbarDarkMode(this, invertIcons);
        }

        if (Utils.hasOreo()) {
            Window w = getWindow();
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            if (invertIcons) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                w.getDecorView().setSystemUiVisibility(flags);
                w.setNavigationBarColor(Color.WHITE);
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                w.getDecorView().setSystemUiVisibility(flags);
                @ColorInt
                int navigationColor = colored ?
                        CurrentTheme.getNavigationBarColor(this) : Color.BLACK;
                w.setNavigationBarColor(navigationColor);
            }
        }
    }
}
