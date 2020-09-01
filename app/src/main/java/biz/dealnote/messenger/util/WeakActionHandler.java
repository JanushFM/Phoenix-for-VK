package biz.dealnote.messenger.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

public class WeakActionHandler<T> extends Handler {

    private final WeakReference<T> ref;
    private Action<T> action;

    public WeakActionHandler(T object) {
        super(Looper.getMainLooper());
        ref = new WeakReference<>(object);
    }

    @Override
    public final void handleMessage(@NotNull Message msg) {
        T object = ref.get();
        if (Objects.nonNull(action)) {
            action.doAction(msg.what, object);
        }
    }

    public WeakActionHandler<T> setAction(Action<T> action) {
        this.action = action;
        return this;
    }

    public interface Action<T> {
        void doAction(int what, T object);
    }
}
