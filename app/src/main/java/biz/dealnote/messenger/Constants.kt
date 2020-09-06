package biz.dealnote.messenger

import android.content.res.Resources
import android.os.Build
import biz.dealnote.messenger.db.column.GroupColumns
import biz.dealnote.messenger.db.column.UserColumns
import biz.dealnote.messenger.settings.ISettings
import biz.dealnote.messenger.util.Utils
import java.util.*

object Constants {
    const val NEED_CHECK_UPDATE = true

    const val API_VERSION = "5.122"
    const val DATABASE_VERSION = 202
    const val VERSION_APK = BuildConfig.VERSION_CODE
    const val APK_ID = BuildConfig.APPLICATION_ID
    const val IS_HAS_LOGIN_WEB = false
    const val FILE_PROVIDER_AUTHORITY = BuildConfig.APPLICATION_ID + ".fileprovider"

    const val DEVICE_COUNTRY_CODE = "ru"
    private val KATE_USER_AGENT = String.format(Locale.US, "KateMobileAndroid/64 lite-475 (Android %s; SDK %d; %s; %s; %s; %s)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Utils.getDeviceName(), DEVICE_COUNTRY_CODE, SCREEN_RESOLUTION())
    const val VKANDROID_APP_VERSION = "5956"
    private val VKANDROID_USER_AGENT = String.format(Locale.US, "VKAndroidApp/6.11.1-%s (Android %s; SDK %d; %s; %s; %s; %s)", VKANDROID_APP_VERSION, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS[0], Utils.getDeviceName(), DEVICE_COUNTRY_CODE, SCREEN_RESOLUTION())
    const val API_ID = BuildConfig.VK_API_APP_ID
    const val SECRET = BuildConfig.VK_CLIENT_SECRET
    const val MAIN_OWNER_FIELDS = UserColumns.API_FIELDS + "," + GroupColumns.API_FIELDS
    const val SERVICE_TOKEN = BuildConfig.SERVICE_TOKEN
    const val PHOTOS_PATH = "DCIM/Phoenix"
    const val PIN_DIGITS_COUNT = 4
    const val PICASSO_TAG = "picasso_tag"
    val IS_DEBUG = BuildConfig.DEBUG
    private fun SCREEN_RESOLUTION(): String {
        val metrics = Resources.getSystem().displayMetrics ?: return "1920x1080"
        return metrics.heightPixels.toString() + "x" + metrics.widthPixels
    }

    @JvmStatic
    fun USER_AGENT(type: String?): String {
        if (type != null) {
            if (type == "kate") return KATE_USER_AGENT else if (type == "vkofficial" || type == "hacked") return VKANDROID_USER_AGENT
        }
        val account_id = Injection.provideSettings().accounts().current
        if (account_id == ISettings.IAccountsSettings.INVALID_ID) {
            return VKANDROID_USER_AGENT
        }
        return if (Injection.provideSettings().accounts().getType(account_id) == "kate") KATE_USER_AGENT else VKANDROID_USER_AGENT
    }
}