package biz.dealnote.messenger.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.api.adapters.LongpollUpdateAdapter;
import biz.dealnote.messenger.api.adapters.LongpollUpdatesAdapter;
import biz.dealnote.messenger.api.model.longpoll.AbsLongpollEvent;
import biz.dealnote.messenger.api.model.longpoll.VkApiLongpollUpdates;
import biz.dealnote.messenger.settings.IProxySettings;
import biz.dealnote.messenger.util.Objects;
import io.reactivex.Single;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static biz.dealnote.messenger.util.Objects.nonNull;

/**
 * Created by Ruslan Kolbasa on 31.07.2017.
 * phoenix
 */
public class OtherVkRetrofitProvider implements IOtherVkRetrofitProvider {

    private final IProxySettings proxySettings;
    private final Object longpollRetrofitLock = new Object();
    private RetrofitWrapper longpollRetrofitInstance;

    public OtherVkRetrofitProvider(IProxySettings proxySettings) {
        this.proxySettings = proxySettings;
        this.proxySettings.observeActive()
                .subscribe(ignored -> onProxySettingsChanged());
    }

    private void onProxySettingsChanged() {
        synchronized (longpollRetrofitLock) {
            if (nonNull(longpollRetrofitInstance)) {
                longpollRetrofitInstance.cleanup();
                longpollRetrofitInstance = null;
            }
        }
    }

    @Override
    public Single<RetrofitWrapper> provideAuthRetrofit() {
        return Single.fromCallable(() -> {

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(HttpLogger.DEFAULT_LOGGING_INTERCEPTOR).addInterceptor(chain -> {
                        Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(null)).build();
                        return chain.proceed(request);
                    });

            ProxyUtil.applyProxyConfig(builder, proxySettings.getActiveProxy());
            Gson gson = new GsonBuilder().create();

            final Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://oauth.vk.com/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(builder.build())
                    .build();

            return RetrofitWrapper.wrap(retrofit, false);
        });
    }

    @Override
    public Single<RetrofitWrapper> provideAuthServiceRetrofit() {
        return Single.fromCallable(() -> {

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(HttpLogger.DEFAULT_LOGGING_INTERCEPTOR).addInterceptor(chain -> {
                        Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(null)).build();
                        return chain.proceed(request);
                    });

            ProxyUtil.applyProxyConfig(builder, proxySettings.getActiveProxy());
            Gson gson = new GsonBuilder().create();

            final Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.vk.com/method/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(builder.build())
                    .build();

            return RetrofitWrapper.wrap(retrofit, false);
        });
    }

    private Retrofit createLongpollRetrofitInstance() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(HttpLogger.DEFAULT_LOGGING_INTERCEPTOR).addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(null)).build();
                        return chain.proceed(request);
                    }
                });

        ProxyUtil.applyProxyConfig(builder, proxySettings.getActiveProxy());

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(VkApiLongpollUpdates.class, new LongpollUpdatesAdapter())
                .registerTypeAdapter(AbsLongpollEvent.class, new LongpollUpdateAdapter())
                .create();

        return new Retrofit.Builder()
                .baseUrl("https://api.vk.com/method/") // dummy
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(builder.build())
                .build();
    }

    @Override
    public Single<RetrofitWrapper> provideLongpollRetrofit() {
        return Single.fromCallable(() -> {

            if (Objects.isNull(longpollRetrofitInstance)) {
                synchronized (longpollRetrofitLock) {
                    if (Objects.isNull(longpollRetrofitInstance)) {
                        longpollRetrofitInstance = RetrofitWrapper.wrap(createLongpollRetrofitInstance());
                    }
                }
            }

            return longpollRetrofitInstance;
        });
    }
}