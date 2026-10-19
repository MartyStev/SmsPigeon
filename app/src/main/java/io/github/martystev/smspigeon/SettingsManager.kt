package io.github.martystev.smspigeon

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isForwardingEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setForwardingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun setToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token.trim()).apply()
    }

    fun getChatId(): String = prefs.getString(KEY_CHAT_ID, "") ?: ""

    fun setChatId(chatId: String) {
        prefs.edit().putString(KEY_CHAT_ID, chatId.trim()).apply()
    }

    fun getSim1Name(): String = prefs.getString(KEY_SIM1_NAME, "SIM 1") ?: "SIM 1"

    fun setSim1Name(name: String) {
        prefs.edit().putString(KEY_SIM1_NAME, name.trim()).apply()
    }

    fun getSim2Name(): String = prefs.getString(KEY_SIM2_NAME, "SIM 2") ?: "SIM 2"

    fun setSim2Name(name: String) {
        prefs.edit().putString(KEY_SIM2_NAME, name.trim()).apply()
    }

    fun isBlacklistMode(): Boolean = prefs.getBoolean(KEY_BLACKLIST_MODE, false)

    fun setBlacklistMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLACKLIST_MODE, enabled).apply()
    }

    fun getBlacklistRaw(): String = prefs.getString(KEY_BLACKLIST, "") ?: ""

    fun setBlacklistRaw(raw: String) {
        prefs.edit().putString(KEY_BLACKLIST, raw).apply()
    }

    fun getSimNameForSlot(slotIndex: Int): String {
        return when (slotIndex) {
            0 -> getSim1Name()
            1 -> getSim2Name()
            else -> "SIM ?"
        }
    }

    companion object {
        private const val PREFS_NAME = "smspigeon_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_TOKEN = "token"
        private const val KEY_CHAT_ID = "chat_id"
        private const val KEY_SIM1_NAME = "sim1_name"
        private const val KEY_SIM2_NAME = "sim2_name"
        private const val KEY_BLACKLIST_MODE = "blacklist_mode"
        private const val KEY_BLACKLIST = "blacklist"
    }
}
