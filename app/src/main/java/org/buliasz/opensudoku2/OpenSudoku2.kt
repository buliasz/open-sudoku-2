package org.buliasz.opensudoku2

import android.app.Application
import android.content.SharedPreferences
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.preference.PreferenceManager

class OpenSudoku2 : Application() {
    init {
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(VmPolicy.Builder().detectLeakedClosableObjects().penaltyLog().build())
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Migrate shared preference keys from version to version.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val oldVersion = sharedPreferences.getInt("schema_version", 20220830)
        val newVersion = BuildConfig.VERSION_CODE
        if (oldVersion != newVersion) {
            upgradeSharedPreferences(sharedPreferences, oldVersion, newVersion)
        }
    }

    private fun upgradeSharedPreferences(
        sharedPreferences: SharedPreferences, oldVersion: Int, @Suppress("SameParameterValue") newVersion: Int
    ) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Upgrading shared preferences: $oldVersion -> $newVersion")
        val editor = sharedPreferences.edit()

        // Changes after 20220830
        if (oldVersion <= 20220830) {
            Log.d(TAG, "Upgrading past 20220830")

            // Rename custom_theme_backgroundColorSecondary to custom_theme_backgroundColorEven
            val color = sharedPreferences.getInt("custom_theme_backgroundColorSecondary", 0)
            editor.putInt("custom_theme_backgroundColorEven", color)
            editor.remove("custom_theme_backgroundColorSecondary")

            // Remove preferences deleted after this version
            editor.remove("custom_theme_colorPrimaryDark")
            editor.remove("custom_theme_colorButtonNormal")
        }
        editor.putInt("schema_version", newVersion)
        editor.apply()
    }

    companion object {
        private val TAG = OpenSudoku2::class.java.simpleName
    }
}
