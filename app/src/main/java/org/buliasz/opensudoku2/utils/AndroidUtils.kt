package org.buliasz.opensudoku2.utils

import android.content.Context
import android.content.pm.PackageManager

object AndroidUtils {
    /**
     * Returns version code of OpenSudoku2.
     *
     * @return
     */
    fun getAppVersionCode(context: Context): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Returns version name of OpenSudoku2.
     *
     * @return
     */
    fun getAppVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }
}
