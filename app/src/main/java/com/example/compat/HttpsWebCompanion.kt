package com.example.compat

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Strict HTTPS-only companion helper enforcing TLS URLs (`https://`) across Android 5–17
 * and matching `network_security_config.xml` (`cleartextTrafficPermitted="false"`).
 */
object HttpsWebCompanion {

    const val DEFAULT_STUDIO_WEB_URL = "https://pixelforge.studio"

    fun validateHttpsUrl(rawUrl: String?): String? {
        if (!LegacyCompatBridge.isStrictHttpsUrl(rawUrl)) {
            return null
        }
        return rawUrl?.trim()
    }

    fun openHttpsUrl(context: Context, rawUrl: String): Boolean {
        val validated = validateHttpsUrl(rawUrl) ?: return false
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validated)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
