package biz.dealnote.messenger.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;
import java.util.Objects;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.Injection;
import biz.dealnote.messenger.R;
import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.PolyTransformation;
import biz.dealnote.messenger.util.RoundTransformation;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.player.util.MusicUtils.mService;
import static biz.dealnote.messenger.player.util.MusicUtils.observeServiceBinding;
import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;
import static biz.dealnote.messenger.util.Utils.firstNonEmptyString;

public class MiniPlayerView extends FrameLayout implements SeekBar.OnSeekBarChangeListener {

    private static final int REFRESH_TIME = 1;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private int mAccountId;
    private PlaybackStatus mPlaybackStatus;
    private LottieAnimationView visual;
    private ImageView play_cover;
    private TextView Title;
    private SeekBar mProgress;
    private boolean mFromTouch;
    private long mPosOverride = -1;
    private View lnt;
    private TimeHandler mTimeHandler;
    private long mLastSeekEventTime;

    public MiniPlayerView(Context context) {
        super(context);
        init();
    }

    public MiniPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiniPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        View root = LayoutInflater.from(getContext()).inflate(R.layout.mini_player, this);
        View play = root.findViewById(R.id.item_audio_play);
        play_cover = root.findViewById(R.id.item_audio_play_cover);
        visual = root.findViewById(R.id.item_audio_visual);
        lnt = root.findViewById(R.id.miniplayer_layout);
        lnt.setVisibility(MusicUtils.getMiniPlayerVisibility() ? View.VISIBLE : View.GONE);
        ImageButton mPClosePlay = root.findViewById(R.id.close_player);
        mPClosePlay.setOnClickListener(v -> {
                    MusicUtils.closeMiniPlayer();
                    lnt.setVisibility(View.GONE);
                }
        );
        play.setOnClickListener(v -> {
            MusicUtils.playOrPause();
            if (MusicUtils.isPlaying()) {
                Utils.doAnimateLottie(visual, true, 104);
                play_cover.setColorFilter(Color.parseColor("#44000000"));
            } else {
                Utils.doAnimateLottie(visual, false, 104);
                play_cover.clearColorFilter();
            }
        });
        play.setOnLongClickListener(v -> {
            MusicUtils.next();
            return true;
        });
        ImageButton mOpenPlayer = root.findViewById(R.id.open_player);
        mOpenPlayer.setOnClickListener(v -> PlaceFactory.getPlayerPlace(mAccountId).tryOpenWith(getContext()));
        Title = root.findViewById(R.id.mini_artist);
        Title.setSelected(true);
        mProgress = root.findViewById(R.id.SeekBar01);
        mProgress.setOnSeekBarChangeListener(this);

    }

    private void queueNextRefresh(long delay) {
        Message message = mTimeHandler.obtainMessage(REFRESH_TIME);

        mTimeHandler.removeMessages(REFRESH_TIME);
        mTimeHandler.sendMessageDelayed(message, delay);
    }

    private Transformation TransformCover() {
        return Settings.get().main().Ismini_player_audio_round_icon() ? new RoundTransformation() : new PolyTransformation();
    }

    @DrawableRes
    private int getAudioCoverSimple() {
        return Settings.get().main().Ismini_player_audio_round_icon() ? R.drawable.audio_button : R.drawable.audio_button_material;
    }

    private void updatePlaybackControls() {
        if (nonNull(play_cover)) {
            Audio audio = MusicUtils.getCurrentAudio();
            if (audio != null && !Utils.isEmpty(audio.getThumb_image_little())) {
                PicassoInstance.with()
                        .load(audio.getThumb_image_little())
                        .placeholder(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), getAudioCoverSimple(), getContext().getTheme())))
                        .transform(TransformCover())
                        .tag(Constants.PICASSO_TAG)
                        .into(play_cover);
            } else {
                PicassoInstance.with().cancelRequest(play_cover);
                play_cover.setImageResource(getAudioCoverSimple());
            }

            if (MusicUtils.isPlaying()) {
                visual.setRepeatCount(ValueAnimator.INFINITE);
                visual.playAnimation();
                play_cover.setColorFilter(Color.parseColor("#44000000"));
            } else {
                if (visual.isAnimating()) {
                    visual.setFrame(104);
                }
                visual.setRepeatCount(0);
                play_cover.clearColorFilter();
            }
        }
    }

    private void onServiceBindEvent() {
        updatePlaybackControls();
        updateNowPlayingInfo();
    }

    @SuppressLint("SetTextI18n")
    private void updateNowPlayingInfo() {
        lnt.setVisibility(MusicUtils.getMiniPlayerVisibility() ? View.VISIBLE : View.GONE);
        String artist = MusicUtils.getArtistName();
        String trackName = MusicUtils.getTrackName();
        Title.setText(firstNonEmptyString(artist, " ") + " - " + firstNonEmptyString(trackName, " "));
        //queueNextRefresh(1);
    }

    private void resolveControlViews() {
        if (mProgress == null) return;

        boolean preparing = MusicUtils.isPreparing();
        boolean initialized = MusicUtils.isInitialized();
        mProgress.setEnabled(!preparing && initialized);
        //mProgress.setIndeterminate(preparing);
    }

    private long refreshCurrentTime() {
        if (mService == null) {
            return 500;
        }

        try {
            long pos = mPosOverride < 0 ? MusicUtils.position() : mPosOverride;
            long duration = MusicUtils.duration();

            if (pos >= 0 && duration > 0) {
                int progress = (int) (1000 * pos / duration);

                mProgress.setProgress(progress);

                int bufferProgress = (int) ((float) MusicUtils.bufferPercent() * 10F);
                mProgress.setSecondaryProgress(bufferProgress);

                if (mFromTouch) {
                    return 500;
                } else if (!MusicUtils.isPlaying()) {
                    return 500;
                }
            } else {
                mProgress.setProgress(0);
                return 500;
            }

            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            long remaining = duration - pos % duration;

            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mProgress.getWidth();
            if (width == 0) {
                width = 320;
            }

            long smoothrefreshtime = duration / width;
            if (smoothrefreshtime > remaining) {
                return remaining;
            }

            if (smoothrefreshtime < 20) {
                return 20;
            }

            return smoothrefreshtime;
        } catch (Exception ignored) {
        }

        return 500;
    }

    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
        if (!fromuser || mService == null) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;

            refreshCurrentTime();

            if (!mFromTouch) {
                // refreshCurrentTime();
                mPosOverride = -1;
            }
        }

        mPosOverride = MusicUtils.duration() * progress / 1000;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mFromTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mPosOverride != -1) {
            MusicUtils.seek(mPosOverride);
            mPosOverride = -1;
        }

        mFromTouch = false;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTimeHandler = new TimeHandler(this);

        mAccountId = Settings.get()
                .accounts()
                .getCurrent();
        mPlaybackStatus = new PlaybackStatus(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        filter.addAction(MusicPlaybackService.META_CHANGED);
        filter.addAction(MusicPlaybackService.PREPARED);
        filter.addAction(MusicPlaybackService.REFRESH);
        filter.addAction(MusicPlaybackService.MINIPLAYER_SUPER_VIS_CHANGED);
        getContext().registerReceiver(mPlaybackStatus, filter);
        long next = refreshCurrentTime();
        queueNextRefresh(next);
        mCompositeDisposable.add(observeServiceBinding()
                .observeOn(Injection.provideMainThreadScheduler())
                .subscribe(ignore -> onServiceBindEvent()));
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCompositeDisposable.dispose();

        mTimeHandler.removeMessages(REFRESH_TIME);

        // Unregister the receiver
        try {
            getContext().unregisterReceiver(mPlaybackStatus);
        } catch (Throwable ignored) {
            //$FALL-THROUGH$
        }
    }

    private static final class TimeHandler extends Handler {

        private final WeakReference<MiniPlayerView> mAudioPlayer;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        TimeHandler(MiniPlayerView player) {
            super(Looper.getMainLooper());
            mAudioPlayer = new WeakReference<>(player);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == REFRESH_TIME) {
                if (mAudioPlayer.get() == null)
                    return;
                long next = mAudioPlayer.get().refreshCurrentTime();
                mAudioPlayer.get().queueNextRefresh(next);
            }
        }
    }

    private static final class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<MiniPlayerView> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(MiniPlayerView activity) {
            mReference = new WeakReference<>(activity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MiniPlayerView fragment = mReference.get();
            if (isNull(fragment) || isNull(action)) return;

            switch (action) {
                case MusicPlaybackService.MINIPLAYER_SUPER_VIS_CHANGED:
                    fragment.updateNowPlayingInfo();
                    break;
                case MusicPlaybackService.META_CHANGED:
                case MusicPlaybackService.PREPARED:
                    // Current info
                    fragment.updateNowPlayingInfo();
                    fragment.resolveControlViews();
                    break;
                case MusicPlaybackService.PLAYSTATE_CHANGED:
                    fragment.updatePlaybackControls();
                    fragment.resolveControlViews();
                    break;

            }
        }
    }
}
