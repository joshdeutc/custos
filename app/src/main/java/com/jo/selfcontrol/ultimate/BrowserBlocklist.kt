package com.jo.selfcontrol.ultimate

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object BrowserBlocklist {

    val ALL_BROWSERS = setOf(
        "com.android.chrome",
        "com.google.android.apps.chrome",
        "com.ecosia.android",
        "com.opera.preinstall",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.gx",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "org.mozilla.focus",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.duckduckgo.mobile.android",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "com.mi.globalbrowser",
        "com.android.browser",
        "com.kiwibrowser.browser",
        "com.vivaldi.browser",
        "com.torproject.torbrowser",
        "mark.via.gp",
        "acr.browser.barebones",
        "com.cloudmosa.puffinFree",
        "com.ucmobile.intl",
        "com.yandex.browser",
        "com.huawei.browser"
    )

    fun isBrowser(ctx: Context, pkg: String): Boolean {
        if (pkg in ALL_BROWSERS) return true
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(pkg)
            }
            val list = ctx.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            list.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
