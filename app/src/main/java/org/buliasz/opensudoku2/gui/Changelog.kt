package org.buliasz.opensudoku2.gui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.utils.AndroidUtils
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets

class Changelog(private val mContext: Context) {
    private val mPrefs: SharedPreferences = mContext.getSharedPreferences(PREF_FILE_CHANGELOG, Context.MODE_PRIVATE)

    fun showOnFirstRun() {
        val versionKey = "changelog_" + AndroidUtils.getAppVersionCode(mContext)
        if (!mPrefs.getBoolean(versionKey, false)) {
            showChangelogDialog()
            val editor = mPrefs.edit()
            editor.putBoolean(versionKey, true)
            editor.apply()
        }
    }

    private fun showChangelogDialog() {
        val changelog = changelogFromResources
        val webView = WebView(mContext)
        webView.loadData(changelog, "text/html", "utf-8")
        val changelogDialog = AlertDialog.Builder(mContext)
            .setIcon(R.drawable.ic_info)
            .setTitle(R.string.what_is_new)
            .setView(webView)
            .setPositiveButton(R.string.close, null).create()
        changelogDialog.show()
    }

    private val changelogFromResources: String
        get() {
            var `is`: InputStream? = null
            try {
                `is` = mContext.resources.openRawResource(R.raw.changelog)
                val buffer = CharArray(0x10000)
                val out = StringBuilder()
                val `in`: Reader
                `in` = InputStreamReader(`is`, StandardCharsets.UTF_8)
                var read: Int
                do {
                    read = `in`.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        out.appendRange(buffer, 0, read)
                    }
                } while (read >= 0)
                return "$out"
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Error when reading changelog from raw resources.", e)
            } finally {
                if (`is` != null) {
                    try {
                        `is`.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e(TAG, "Error when reading changelog from raw resources.", e)
                    }
                }
            }
            return ""
        }

    companion object {
        private const val TAG = "Changelog"
        private const val PREF_FILE_CHANGELOG = "changelog"
    }
}
