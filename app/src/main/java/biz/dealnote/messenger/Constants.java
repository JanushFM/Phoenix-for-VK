package biz.dealnote.messenger;

import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import java.util.Locale;

import biz.dealnote.messenger.db.column.GroupColumns;
import biz.dealnote.messenger.db.column.UserColumns;
import biz.dealnote.messenger.settings.ISettings;
import biz.dealnote.messenger.util.Utils;

public class Constants {
    public static final boolean NEED_CHECK_UPDATE = true;

    public static final String API_VERSION = "5.122";
    public static final int DATABASE_VERSION = 202;
    public static final int VERSION_APK = BuildConfig.VERSION_CODE;
    public static final String APK_ID = BuildConfig.APPLICATION_ID;

    public static final boolean IS_HAS_LOGIN_WEB = false;

    public static final String FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider";

    //public static final String DEVICE_COUNTRY_CODE = Injection.provideApplicationContext().getResources().getConfiguration().locale.getCountry().toLowerCase();
    public static final String DEVICE_COUNTRY_CODE = "ru";
    public static final String KATE_USER_AGENT = String.format(Locale.US, "KateMobileAndroid/64 lite-475 (Android %s; SDK %d; %s; %s; %s; %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Utils.getDeviceName(), DEVICE_COUNTRY_CODE, SCREEN_RESOLUTION());
    public static final String VKANDROID_APP_VERSION = "5935";
    public static final String VKANDROID_USER_AGENT = String.format(Locale.US, "VKAndroidApp/6.11-%s (Android %s; SDK %d; %s; %s; %s; %s)", VKANDROID_APP_VERSION, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Utils.getDeviceName(), DEVICE_COUNTRY_CODE, SCREEN_RESOLUTION());
    public static final int API_ID = BuildConfig.VK_API_APP_ID;
    public static final String SECRET = BuildConfig.VK_CLIENT_SECRET;
    public static final String MAIN_OWNER_FIELDS = UserColumns.API_FIELDS + "," + GroupColumns.API_FIELDS;
    public static final String SERVICE_TOKEN = BuildConfig.SERVICE_TOKEN;
    public static final String PHOTOS_PATH = "DCIM/Phoenix";
    public static final int PIN_DIGITS_COUNT = 4;
    public static final String PICASSO_TAG = "picasso_tag";
    public static final boolean IS_DEBUG = BuildConfig.DEBUG;

    public static String SCREEN_RESOLUTION() {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        if (metrics == null) {
            return "1920x1080";
        }
        return metrics.heightPixels + "x" + metrics.widthPixels;
    }

    public static String USER_AGENT(String type) {
        if (type != null) {
            if (type.equals("kate"))
                return KATE_USER_AGENT;
            else if (type.equals("vkofficial") || type.equals("hacked"))
                return VKANDROID_USER_AGENT;
        }
        int account_id = Injection.provideSettings().accounts().getCurrent();
        if (account_id == ISettings.IAccountsSettings.INVALID_ID) {
            return VKANDROID_USER_AGENT;
        }
        if (Injection.provideSettings().accounts().getType(account_id).equals("kate"))
            return KATE_USER_AGENT;
        return VKANDROID_USER_AGENT;
    }
}
