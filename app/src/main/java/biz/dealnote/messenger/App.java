package biz.dealnote.messenger;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import org.conscrypt.Conscrypt;

import java.security.Security;

import biz.dealnote.messenger.api.PicassoInstance;
import biz.dealnote.messenger.domain.Repository;
import biz.dealnote.messenger.service.ErrorLocalizer;
import biz.dealnote.messenger.service.KeepLongpollService;
import biz.dealnote.messenger.settings.Settings;
import biz.dealnote.messenger.util.PhoenixToast;
import ealvatag.tag.TagOptionSingleton;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

import static biz.dealnote.messenger.longpoll.NotificationHelper.tryCancelNotificationForPeer;
import static biz.dealnote.messenger.util.RxUtils.ignore;

public class App extends Application {

    private static App sInstanse;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @NonNull
    public static App getInstance() {
        if (sInstanse == null) {
            throw new IllegalStateException("App instance is null!!! WTF???");
        }

        return sInstanse;
    }

    @Override
    public void onCreate() {
        sInstanse = this;
        AppCompatDelegate.setDefaultNightMode(Settings.get().ui().getNightMode());
        TagOptionSingleton.getInstance().setAndroid(true);
        Security.addProvider(Conscrypt.newProvider());

        super.onCreate();

        PicassoInstance.init(this, Injection.provideProxySettings());

        if (Settings.get().other().isKeepLongpoll()) {
            KeepLongpollService.start(this);
        }

        compositeDisposable.add(Repository.INSTANCE.getMessages()
                .observePeerUpdates()
                .flatMap(Flowable::fromIterable)
                .subscribe(update -> {
                    if (update.getReadIn() != null) {
                        tryCancelNotificationForPeer(App.this, update.getAccountId(), update.getPeerId());
                    }
                }, ignore()));

        compositeDisposable.add(Repository.INSTANCE.getMessages()
                .observeSentMessages()
                .subscribe(sentMsg -> tryCancelNotificationForPeer(App.this, sentMsg.getAccountId(), sentMsg.getPeerId()), ignore()));

        compositeDisposable.add(Repository.INSTANCE.getMessages()
                .observeMessagesSendErrors()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(throwable -> PhoenixToast.CreatePhoenixToast(App.this).showToastError(ErrorLocalizer.localizeThrowable(App.this, throwable)), ignore()));
    }
}