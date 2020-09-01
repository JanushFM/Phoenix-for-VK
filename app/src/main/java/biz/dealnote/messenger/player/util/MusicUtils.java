package biz.dealnote.messenger.player.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import biz.dealnote.messenger.R;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.player.IAudioPlayerService;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.Logger;
import biz.dealnote.messenger.util.Optional;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;


public final class MusicUtils {

    private static final WeakHashMap<Context, ServiceBinder> mConnectionMap;
    private static final PublishSubject<Optional<IAudioPlayerService>> SERVICE_BIND_PUBLISHER = PublishSubject.create();
    private static final String TAG = MusicUtils.class.getSimpleName();
    public static IAudioPlayerService mService;
    public static HashMap<Integer, ArrayList<Audio>> Audios = new HashMap<>();
    public static boolean SuperCloseMiniPlayer;
    public static Set<String> CachedAudios = new ArraySet<>();
    public static Set<String> RemoteAudios = new ArraySet<>();
    private static int sForegroundActivities;

    static {
        mConnectionMap = new WeakHashMap<>();
    }

    /* This class is never initiated */
    private MusicUtils() {
    }

    public static ServiceToken bindToServiceWithoutStart(Activity realActivity, ServiceConnection callback) {
        ContextWrapper contextWrapper = new ContextWrapper(realActivity);
        ServiceBinder binder = new ServiceBinder(callback);

        if (contextWrapper.bindService(new Intent().setClass(contextWrapper, MusicPlaybackService.class), binder, 0)) {
            mConnectionMap.put(contextWrapper, binder);
            return new ServiceToken(contextWrapper);
        }

        return null;
    }

    /**
     * @param token The {@link ServiceToken} to unbind from
     */
    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            return;
        }

        ContextWrapper mContextWrapper = token.mWrappedContext;
        ServiceBinder mBinder = mConnectionMap.remove(mContextWrapper);
        if (mBinder == null) {
            return;
        }

        mContextWrapper.unbindService(mBinder);

        if (mConnectionMap.isEmpty()) {
            mService = null;
        }
    }

    public static Observable<Optional<IAudioPlayerService>> observeServiceBinding() {
        return SERVICE_BIND_PUBLISHER;
    }

    public static String makeTimeString(Context context, long secs) {
        long hours, mins;

        hours = secs / 3600;
        secs -= hours * 3600;
        mins = secs / 60;
        secs -= mins * 60;

        String durationFormat = context.getResources().getString(
                hours == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        return String.format(durationFormat, hours, mins, secs);
    }

    /**
     * Changes to the next track
     */
    public static void next() {
        try {
            if (mService != null) {
                mService.next();
            }
        } catch (RemoteException ignored) {
        }
    }

    public static boolean isInitialized() {
        if (mService != null) {
            try {
                return mService.isInitialized();
            } catch (RemoteException ignored) {
            }
        }

        return false;
    }

    public static boolean isPreparing() {
        if (mService != null) {
            try {
                return mService.isPreparing();
            } catch (RemoteException ignored) {
            }
        }

        return false;
    }

    /**
     * Changes to the previous track.
     */
    public static void previous(Context context) {
        Intent previous = new Intent(context, MusicPlaybackService.class);
        previous.setAction(MusicPlaybackService.PREVIOUS_ACTION);
        context.startService(previous);
    }

    /**
     * Plays or pauses the music.
     */
    public static void playOrPause() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.pause();
                } else {
                    mService.play();
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void stop() {
        try {
            if (mService != null) {
                mService.stop();
            }
        } catch (Exception ignored) {
        }
    }

    public static void closeMiniPlayer() {
        try {
            if (mService != null) {
                mService.closeMiniPlayer();
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean getMiniPlayerVisibility() {
        if (!Settings.get().other().isShow_mini_player())
            return false;
        try {
            if (mService != null) {
                return mService.getMiniplayerVisibility();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void setMiniPlayerVisibility(boolean visiable) {
        SuperCloseMiniPlayer = !visiable;
        try {
            if (mService != null) {
                mService.setMiniPlayerVisibility(visiable);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Cycles through the repeat options.
     */
    public static void cycleRepeat() {
        try {
            if (mService != null) {
                switch (mService.getRepeatMode()) {
                    case MusicPlaybackService.REPEAT_NONE:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        break;
                    case MusicPlaybackService.REPEAT_ALL:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
                        if (mService.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
                            mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        }
                        break;
                    default:
                        mService.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
                        break;
                }
            }
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Cycles through the shuffle options.
     */
    public static void cycleShuffle() {
        try {
            if (mService != null) {
                switch (mService.getShuffleMode()) {
                    case MusicPlaybackService.SHUFFLE_NONE:
                        mService.setShuffleMode(MusicPlaybackService.SHUFFLE);
                        if (mService.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
                            mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
                        }
                        break;
                    case MusicPlaybackService.SHUFFLE:
                        mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
                        break;
                    default:
                        break;
                }
            }
        } catch (RemoteException ignored) {
        }
    }

    /**
     * @return True if we're playing music, false otherwise.
     */
    public static boolean isPlaying() {
        if (mService != null) {
            try {
                return mService.isPlaying();
            } catch (RemoteException ignored) {
            }
        }
        return false;
    }

    public static boolean isPaused() {
        if (mService != null) {
            try {
                return mService.isPaused();
            } catch (RemoteException ignored) {
            }
        }
        return false;
    }

    /**
     * @return The current shuffle mode.
     */
    public static int getShuffleMode() {
        if (mService != null) {
            try {
                return mService.getShuffleMode();
            } catch (RemoteException ignored) {
            }
        }

        return 0;
    }

    /**
     * @return The current repeat mode.
     */
    public static int getRepeatMode() {
        if (mService != null) {
            try {
                return mService.getRepeatMode();
            } catch (RemoteException ignored) {
            }
        }

        return 0;
    }

    @Nullable
    public static Audio getCurrentAudio() {
        if (mService != null) {
            try {
                return mService.getCurrentAudio();
            } catch (RemoteException ignored) {
            }
        }

        return null;
    }

    /**
     * @return The current track name.
     */
    @Nullable
    public static String getTrackName() {
        if (mService != null) {
            try {
                return mService.getTrackName();
            } catch (RemoteException ignored) {
            }
        }

        return null;
    }

    /**
     * @return The current album name.
     */
    @Nullable
    public static String getAlbumName() {
        if (mService != null) {
            try {
                return mService.getAlbumName();
            } catch (RemoteException ignored) {
            }
        }

        return null;
    }

    /**
     * @return The current artist name.
     */
    @Nullable
    public static String getArtistName() {
        if (mService != null) {
            try {
                return mService.getArtistName();
            } catch (RemoteException ignored) {
            }
        }
        return null;
    }

    @Nullable
    public static String getAlbumCoverBig() {
        if (mService != null) {
            try {
                return mService.getAlbumCover();
            } catch (RemoteException ignored) {
            }
        }
        return null;
    }

    /**
     * @return The current song Id.
     */
    public static int getAudioSessionId() {
        if (mService != null) {
            try {
                return mService.getAudioSessionId();
            } catch (RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The queue.
     */
    public static List<Audio> getQueue() {
        try {
            if (mService != null) {
                return mService.getQueue();
            }
        } catch (RemoteException ignored) {
        }

        return Collections.emptyList();
    }

    /**
     * Called when one of the lists should refresh or requery.
     */
    public static void refresh() {
        try {
            if (mService != null) {
                mService.refresh();
            }
        } catch (RemoteException ignored) {
        }
    }

    /**
     * Seeks the current track to a desired position
     *
     * @param position The position to seek to
     */
    public static void seek(long position) {
        if (mService != null) {
            try {
                mService.seek(position);
            } catch (RemoteException ignored) {
            }
        }
    }

    /**
     * @return The current position time of the track
     */
    public static long position() {
        if (mService != null) {
            try {
                return mService.position();
            } catch (RemoteException ignored) {
            }
        }

        return 0;
    }

    /**
     * @return The total length of the current track
     */
    public static long duration() {
        if (mService != null) {
            try {
                return mService.duration();
            } catch (RemoteException ignored) {
            }
        }
        return 0;
    }

    /**
     * @return The total length of the current track
     */
    public static int bufferPercent() {
        if (mService != null) {
            try {
                return mService.getBufferPercent();
            } catch (RemoteException ignored) {
            }
        }
        return 0;
    }

    public static boolean isNowPlayingOrPreparing(Audio audio) {
        return audio.equals(getCurrentAudio()) && (isPreparing() || isPlaying());
    }

    public static boolean isNowPlayingOrPreparingOrPaused(Audio audio) {
        return audio.equals(getCurrentAudio()) && (isPreparing() || isPlaying() || isPaused());
    }

    public static Integer AudioStatus(Audio audio) {
        if (!audio.equals(getCurrentAudio()))
            return -1;
        if (audio.equals(getCurrentAudio()) && (isPaused()))
            return 2;
        if (audio.equals(getCurrentAudio()) && (isPreparing() || isPlaying()))
            return 1;
        return 0;
    }

    public static boolean isNowPaused(Audio audio) {
        return audio.equals(getCurrentAudio()) && (isPaused());
    }

    /**
     * Used to build and show a notification when player is sent into the
     * background
     *
     * @param context The {@link Context} to use.
     */
    public static void notifyForegroundStateChanged(Context context, boolean inForeground) {
        int old = sForegroundActivities;
        if (inForeground) {
            sForegroundActivities++;
        } else {
            sForegroundActivities--;
            if (sForegroundActivities < 0)
                sForegroundActivities = 0;
        }

        if (old == 0 || sForegroundActivities == 0) {
            boolean nowInForeground = sForegroundActivities != 0;
            Logger.d(TAG, "notifyForegroundStateChanged, nowInForeground: " + nowInForeground);

            Intent intent = new Intent(context, MusicPlaybackService.class);
            intent.setAction(MusicPlaybackService.FOREGROUND_STATE_CHANGED);
            intent.putExtra(MusicPlaybackService.NOW_IN_FOREGROUND, nowInForeground);
            context.startService(intent);
        }
    }

    public static final class ServiceBinder implements ServiceConnection {

        private final ServiceConnection mCallback;


        public ServiceBinder(ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IAudioPlayerService.Stub.asInterface(service);

            SERVICE_BIND_PUBLISHER.onNext(Optional.wrap(mService));

            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }

            mService = null;

            SERVICE_BIND_PUBLISHER.onNext(Optional.empty());
        }
    }

    public static final class ServiceToken {

        public ContextWrapper mWrappedContext;

        public ServiceToken(ContextWrapper context) {
            mWrappedContext = context;
        }
    }

}
