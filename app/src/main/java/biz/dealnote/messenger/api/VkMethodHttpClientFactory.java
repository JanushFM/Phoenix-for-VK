package biz.dealnote.messenger.api;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import biz.dealnote.messenger.Constants;
import biz.dealnote.messenger.model.ProxyConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class VkMethodHttpClientFactory implements IVkMethodHttpClientFactory {

    @Override
    public OkHttpClient createDefaultVkHttpClient(int accountId, Gson gson, ProxyConfig config) {
        return createDefaultVkApiOkHttpClient(new DefaultVkApiInterceptor(accountId, Constants.API_VERSION, gson), config);
    }

    @Override
    public OkHttpClient createCustomVkHttpClient(int accountId, String token, Gson gson, ProxyConfig config) {
        return createDefaultVkApiOkHttpClient(new CustomTokenVkApiInterceptor(token, Constants.API_VERSION, gson, null, accountId), config);
    }

    @Override
    public OkHttpClient createServiceVkHttpClient(Gson gson, ProxyConfig config) {
        return createDefaultVkApiOkHttpClient(new CustomTokenVkApiInterceptor(Constants.SERVICE_TOKEN, Constants.API_VERSION, gson, "vkofficial", null), config);
    }

    private OkHttpClient createDefaultVkApiOkHttpClient(AbsVkApiInterceptor interceptor, ProxyConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(HttpLogger.DEFAULT_LOGGING_INTERCEPTOR)
                .readTimeout(40, TimeUnit.SECONDS)
                .connectTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS).addInterceptor(chain -> {
                    Request request = chain.request().newBuilder().addHeader("User-Agent", Constants.USER_AGENT(interceptor.getType())).build();
                    return chain.proceed(request);
                });

        ProxyUtil.applyProxyConfig(builder, config);
        return builder.build();
    }
}