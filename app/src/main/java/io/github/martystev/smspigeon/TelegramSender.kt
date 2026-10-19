package io.github.martystev.smspigeon

import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramSender {
    fun sendMessage(token: String, chatId: String, message: String): Boolean {
        if (token.isBlank() || chatId.isBlank()) return false
        val url = URL("https://api.telegram.org/bot$token/sendMessage")
        val body = "chat_id=${urlEncode(chatId)}&text=${urlEncode(message)}"
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }

        return try {
            BufferedOutputStream(conn.outputStream).use { out ->
                out.write(body.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            val code = conn.responseCode
            if (code in 200..299) {
                true
            } else {
                BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
    }
}
