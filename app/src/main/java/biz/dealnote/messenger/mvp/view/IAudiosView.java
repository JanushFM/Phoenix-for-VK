package biz.dealnote.messenger.mvp.view;

import java.util.List;

import biz.dealnote.messenger.api.model.VKApiAudioPlaylist;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.view.base.IAccountDependencyView;
import biz.dealnote.mvp.core.IMvpView;

/**
 * Created by admin on 1/4/2018.
 * Phoenix-for-VK
 */
public interface IAudiosView extends IMvpView, IErrorView, IAccountDependencyView {
    void displayList(List<Audio> audios);

    void notifyListChanged();

    void doesLoadCache();

    void ProvideReadCachedAudio();

    void displayRefreshing(boolean refresing);

    void updatePlaylists(List<VKApiAudioPlaylist> stories);
}