package biz.dealnote.messenger.api;

import com.google.gson.Gson;

import biz.dealnote.messenger.settings.Settings;


class CustomTokenVkApiInterceptor extends AbsVkApiInterceptor {

    private final String token;

    private final String type;

    private final Integer account_id;

    CustomTokenVkApiInterceptor(String token, String v, Gson gson, String type, Integer account_id) {
        super(v, gson);
        this.token = token;
        this.type = type;
        this.account_id = account_id;
    }

    @Override
    protected String getToken() {
        return token;
    }

    @Override
    protected String getType() {
        if (type == null && account_id == null) {
            return "vkofficial";
        }
        if (type == null) {
            return Settings.get().accounts().getType(account_id);
        }
        return type;
    }
}