package biz.dealnote.messenger.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biz.dealnote.messenger.util.Objects;
import biz.dealnote.messenger.util.Utils;

class OtherSettings implements ISettings.IOtherSettings {

    private static final String KEY_JSON_STATE = "json_list_state";

    private static final String KEY_DONATE = "donates";

    private final Context app;

    OtherSettings(Context context) {
        app = context.getApplicationContext();
    }

    @Override
    public String getFeedSourceIds(int accountId) {
        return PreferenceManager.getDefaultSharedPreferences(app)
                .getString("source_ids" + accountId, null);
    }

    @Override
    public void setFeedSourceIds(int accountId, String sourceIds) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putString("source_ids" + accountId, sourceIds)
                .apply();
    }

    @Override
    public void storeFeedScrollState(int accountId, String state) {
        if (Objects.nonNull(state)) {
            PreferenceManager
                    .getDefaultSharedPreferences(app)
                    .edit()
                    .putString("json_list_state" + accountId, state)
                    .apply();
        } else {
            PreferenceManager
                    .getDefaultSharedPreferences(app)
                    .edit()
                    .remove(KEY_JSON_STATE + accountId)
                    .apply();
        }
    }

    @Override
    public String restoreFeedScrollState(int accountId) {
        return PreferenceManager.getDefaultSharedPreferences(app).getString(KEY_JSON_STATE + accountId, null);
    }

    @Override
    public String restoreFeedNextFrom(int accountId) {
        return PreferenceManager
                .getDefaultSharedPreferences(app)
                .getString("next_from" + accountId, null);
    }

    @Override
    public void storeFeedNextFrom(int accountId, String nextFrom) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putString("next_from" + accountId, nextFrom)
                .apply();
    }

    @Override
    public boolean isAudioBroadcastActive() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("broadcast", false);
    }

    @Override
    public void setAudioBroadcastActive(boolean active) {
        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putBoolean("broadcast", active)
                .apply();
    }

    @Override
    public boolean isCommentsDesc() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("comments_desc", true);
    }

    @Override
    public boolean toggleCommentsDirection() {
        boolean descNow = isCommentsDesc();

        PreferenceManager.getDefaultSharedPreferences(app)
                .edit()
                .putBoolean("comments_desc", !descNow)
                .apply();

        return !descNow;
    }

    @Override
    public boolean isKeepLongpoll() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("keep_longpoll", false);
    }

    @Override
    public void setKeepLongpoll(boolean en) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putBoolean("keep_longpoll", en).apply();
    }

    @Override
    public void setDisableErrorFCM(boolean en) {
        PreferenceManager.getDefaultSharedPreferences(app).edit().putBoolean("disable_error_fcm", en).apply();
    }

    @Override
    public boolean isDisabledErrorFCM() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_error_fcm", false);
    }

    @Override
    public boolean isSettings_no_push() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("settings_no_push", false);
    }

    @Override
    public boolean isShow_audio_cover() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_audio_cover", true);
    }

    @Override
    public String get_Api_Domain() {
        return PreferenceManager.getDefaultSharedPreferences(app).getString("vk_api_domain", "api.vk.com").trim();
    }

    @Override
    public String get_Auth_Domain() {
        return PreferenceManager.getDefaultSharedPreferences(app).getString("vk_auth_domain", "oauth.vk.com").trim();
    }

    @Override
    public boolean isDebug_mode() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("debug_mode", false);
    }

    @Override
    public boolean isForce_cache() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("force_cache", false);
    }

    @Override
    public boolean isAuto_update() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("auto_update", true);
    }

    @Override
    public boolean isUse_old_vk_api() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_old_vk_api", false);
    }

    @Override
    public boolean isDisable_history() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_history", false);
    }

    @Override
    public boolean isShow_wall_cover() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_wall_cover", true);
    }

    @Override
    public int getColorChat() {
        return PreferenceManager.getDefaultSharedPreferences(app).getInt("custom_chat_color", Color.argb(255, 255, 255, 255));
    }

    @Override
    public int getSecondColorChat() {
        return PreferenceManager.getDefaultSharedPreferences(app).getInt("custom_chat_color_second", Color.argb(255, 255, 255, 255));
    }

    @Override
    public boolean isCustom_chat_color() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("custom_chat_color_usage", false);
    }

    @Override
    public int getColorMyMessage() {
        return PreferenceManager.getDefaultSharedPreferences(app).getInt("custom_message_color", Color.parseColor("#CBD438FF"));
    }

    @Override
    public int getSecondColorMyMessage() {
        return PreferenceManager.getDefaultSharedPreferences(app).getInt("custom_second_message_color", Color.parseColor("#BF6539DF"));
    }

    @Override
    public boolean isCustom_MyMessage() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("custom_message_color_usage", false);
    }

    @Override
    public boolean isInfo_reading() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("info_reading", true);
    }

    @Override
    public boolean isAuto_read() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("auto_read", false);
    }

    @Override
    public boolean isBe_online() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("be_online", false);
    }

    @Override
    public boolean isUse_stop_audio() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_stop_audio", false);
    }

    @Override
    public boolean isBlur_for_player() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("blur_for_player", false);
    }

    @Override
    public boolean isShow_mini_player() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_mini_player", true);
    }

    @Override
    public boolean isEnable_last_read() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("enable_last_read", true);
    }

    @Override
    public boolean isEnable_show_recent_dialogs() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_recent_dialogs", true);
    }

    @Override
    public boolean isEnable_show_audio_top() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("show_audio_top", false);
    }

    @Override
    public boolean isUse_internal_downloader() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("use_internal_downloader", true);
    }

    @Override
    public String getMusicDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("music_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("music_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public String getPhotoDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("photo_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Phoenix";
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("photo_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public String getVideoDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("video_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/Phoenix";
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("video_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public String getDocDir() {
        String ret = PreferenceManager.getDefaultSharedPreferences(app).getString("docs_dir", null);
        if (Utils.isEmpty(ret) || !new File(ret).exists()) {
            ret = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Phoenix";
            PreferenceManager.getDefaultSharedPreferences(app).edit().putString("docs_dir", ret).apply();
        }
        return ret;
    }

    @Override
    public boolean isPhoto_to_user_dir() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("photo_to_user_dir", true);
    }

    @Override
    public boolean isDelete_cache_images() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("delete_cache_images", false);
    }

    @Override
    public boolean isClick_next_track() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("click_next_track", true);
    }

    @Override
    public boolean isDisabled_encryption() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_encryption", false);
    }

    @Override
    public boolean isDownload_photo_tap() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("download_photo_tap", true);
    }

    @Override
    public boolean isAudio_save_mode_button() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("audio_save_mode_button", true);
    }

    @Override
    public boolean isHint_stickers() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("hint_stickers", true);
    }

    @Override
    public boolean isRunes_show() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("runes_show", false);
    }

    @Override
    public boolean isDisable_sensored_voice() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("disable_sensored_voice", false);
    }

    @Override
    public boolean isRunes_valknut() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("runes_valknut", false);
    }

    @Override
    public boolean isSymbolSelectShow() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean("symbol_select_show", false);
    }

    @Override
    public void setSymbolSelectShow(boolean show) {
        PreferenceManager
                .getDefaultSharedPreferences(app)
                .edit()
                .putBoolean("symbol_select_show", show)
                .apply();
    }

    @Override
    public void registerDonatesId(List<Integer> Ids) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        Set<String> uids = new HashSet<>(Ids.size());
        for (int i : Ids) {
            uids.add(String.valueOf(i));
        }
        preferences.edit().putStringSet(KEY_DONATE, uids).apply();
    }

    @NonNull
    @Override
    public List<Integer> getDonates() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(app);
        Set<String> uids = preferences.getStringSet(KEY_DONATE, new HashSet<>(0));

        List<Integer> ids = new ArrayList<>(uids.size());
        for (String stringuid : uids) {
            int uid = Integer.parseInt(stringuid);
            ids.add(uid);
        }

        return ids;
    }
}