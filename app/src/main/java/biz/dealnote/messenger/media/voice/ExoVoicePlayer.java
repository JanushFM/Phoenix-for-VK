package biz.dealnote.messenger.media.voice;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.api.ProxyUtil;
import biz.dealnote.messenger.media.exo.CustomHttpDataSourceFactory;
import biz.dealnote.messenger.media.exo.ExoEventAdapter;
import biz.dealnote.messenger.media.exo.ExoUtil;
import biz.dealnote.messenger.model.ProxyConfig;
import biz.dealnote.messenger.model.VoiceMessage;
import biz.dealnote.messenger.player.util.MusicUtils;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Logger;
import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Optional;

import static biz.dealnote.messenger.util.Objects.isNull;
import static biz.dealnote.messenger.util.Objects.nonNull;

/**
 * Created by r.kolbasa on 28.11.2017.
 * Phoenix-for-VK
 */
public class ExoVoicePlayer implements IVoicePlayer {

    private final Context app;
    private final ProxyConfig proxyConfig;
    private SimpleExoPlayer exoPlayer;
    private int status;
    private AudioEntry playingEntry;
    private boolean supposedToBePlaying;
    private IPlayerStatusListener statusListener;
    private IErrorListener errorListener;

    public ExoVoicePlayer(Context context, ProxyConfig config) {
        this.app = context.getApplicationContext();
        this.proxyConfig = config;
        this.status = STATUS_NO_PLAYBACK;
    }

    @Override
    public boolean toggle(int id, VoiceMessage audio) {
        if (nonNull(playingEntry) && playingEntry.getId() == id) {
            setSupposedToBePlaying(!isSupposedToPlay());
            return false;
        }

        release();

        playingEntry = new AudioEntry(id, audio);
        supposedToBePlaying = true;

        preparePlayer();
        return true;
    }

    private void setStatus(int status) {
        if (this.status != status) {
            this.status = status;

            if (nonNull(statusListener)) {
                statusListener.onPlayerStatusChange(status);
            }
        }
    }

    private void preparePlayer() {
        setStatus(STATUS_PREPARING);

        exoPlayer = new SimpleExoPlayer.Builder(app).build();
        exoPlayer.setWakeMode(C.WAKE_MODE_NETWORK);

        // DefaultBandwidthMeter bandwidthMeterA = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        // DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "exoplayer2example"), bandwidthMeterA);
        // DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(App.getInstance(), Util.getUserAgent(App.getInstance(), "exoplayer2example"), bandwidthMeterA);

        Proxy proxy = null;
        if (nonNull(proxyConfig)) {
            proxy = new Proxy(Proxy.Type.HTTP, ProxyUtil.obtainAddress(proxyConfig));
            if (proxyConfig.isAuthEnabled()) {
                Authenticator authenticator = new Authenticator() {
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyConfig.getUser(), proxyConfig.getPass().toCharArray());
                    }
                };

                Authenticator.setDefault(authenticator);
            } else {
                Authenticator.setDefault(null);
            }
        }

        String userAgent = Constants.USER_AGENT(null);
        CustomHttpDataSourceFactory factory = new CustomHttpDataSourceFactory(userAgent, proxy);

        // This is the MediaSource representing the media to be played:
        // FOR SD CARD SOURCE:
        // MediaSource videoSource = new ExtractorMediaSource(mp4VideoUri, dataSourceFactory, extractorsFactory, null, null);
        // FOR LIVESTREAM LINK:

        String url = playingEntry.getAudio().getLinkMp3();

        MediaSource mediaSource = new ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url));
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        if (Settings.get().other().isUse_speach_voice())
            exoPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_SPEECH).setUsage(C.USAGE_VOICE_COMMUNICATION).build(), false);
        else
            exoPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true);
        exoPlayer.addListener(new ExoEventAdapter() {
            @Override
            public void onPlayerStateChanged(boolean b, int i) {
                onInternalPlayerStateChanged(i);
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                onExoPlayerException(error);
            }
        });

        if (Settings.get().other().isUse_speach_voice()) {
            if (MusicUtils.isPlaying() || MusicUtils.isPreparing())
                MusicUtils.playOrPause();
        }

        exoPlayer.setPlayWhenReady(supposedToBePlaying);
        exoPlayer.prepare(mediaSource);
    }

    private void onExoPlayerException(ExoPlaybackException e) {
        if (nonNull(errorListener)) {
            errorListener.onPlayError(new PrepareException(e));
        }
    }

    private void onInternalPlayerStateChanged(int state) {
        Logger.d("ExoVoicePlayer", "onInternalPlayerStateChanged, state: " + state);

        switch (state) {
            case Player.STATE_READY:
                setStatus(STATUS_PREPARED);
                break;
            case Player.STATE_ENDED:
                setSupposedToBePlaying(false);
                exoPlayer.seekTo(0);
                break;
        }
    }

    private void setSupposedToBePlaying(boolean supposedToBePlaying) {
        this.supposedToBePlaying = supposedToBePlaying;

        if (supposedToBePlaying) {
            ExoUtil.startPlayer(exoPlayer);
        } else {
            ExoUtil.pausePlayer(exoPlayer);
        }
    }

    @Override
    public float getProgress() {
        if (Objects.isNull(exoPlayer)) {
            return 0f;
        }

        if (status != STATUS_PREPARED) {
            return 0f;
        }

        //long duration = playingEntry.getAudio().getDuration() * 1000;
        long duration = exoPlayer.getDuration();
        long position = exoPlayer.getCurrentPosition();
        return (float) position / (float) duration;
    }

    @Override
    public void setCallback(@Nullable IPlayerStatusListener listener) {
        this.statusListener = listener;
    }

    @Override
    public void setErrorListener(@Nullable IErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    @Override
    public Optional<Integer> getPlayingVoiceId() {
        return isNull(playingEntry) ? Optional.empty() : Optional.wrap(playingEntry.getId());
    }

    @Override
    public boolean isSupposedToPlay() {
        return supposedToBePlaying;
    }

    @Override
    public void stop() {
        if (nonNull(exoPlayer)) {
            exoPlayer.stop();
        }
    }

    @Override
    public void release() {
        if (nonNull(exoPlayer)) {
            exoPlayer.stop();
            exoPlayer.release();
        }
    }
}