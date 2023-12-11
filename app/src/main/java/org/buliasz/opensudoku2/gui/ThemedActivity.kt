package org.buliasz.opensudoku2.gui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.buliasz.opensudoku2.utils.ThemeUtils

abstract class ThemedActivity : AppCompatActivity() {
    private var mTimestampWhenApplyingTheme: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.setThemeFromPreferences(this)
        mTimestampWhenApplyingTheme = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
    }

    protected fun recreateActivityIfThemeChanged() {
        if (ThemeUtils.sTimestampOfLastThemeUpdate > mTimestampWhenApplyingTheme) {
            Log.d(TAG, "Theme changed, recreating activity")
            ActivityCompat.recreate(this)
        }
    }

    override fun onResume() {
        super.onResume()
        recreateActivityIfThemeChanged()
    }

    companion object {
        private val TAG = ThemedActivity::class.java.simpleName
    }
}
