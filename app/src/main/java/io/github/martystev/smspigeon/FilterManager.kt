package io.github.martystev.smspigeon

import android.content.Context

class FilterManager(context: Context) {
    private val settings = SettingsManager(context)

    fun isAllowed(sender: String): Boolean {
        if (!settings.isBlacklistMode()) {
            return true
        }
        val blacklist = parseBlacklist(settings.getBlacklistRaw())
        val normalized = normalize(sender)
        return !blacklist.contains(normalized)
    }

    private fun parseBlacklist(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        val tokens = raw.split("\n", ",", ";", " ")
        val result = HashSet<String>()
        for (t in tokens) {
            val n = normalize(t)
            if (n.isNotEmpty()) {
                result.add(n)
            }
        }
        return result
    }

    private fun normalize(value: String): String {
        return value.replace(" ", "").replace("-", "").trim()
    }
}
