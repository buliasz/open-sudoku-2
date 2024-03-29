/*
 * This file is part of Open Sudoku 2 - an open-source Sudoku game.
 * Copyright (C) 2009-2023 by original authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.buliasz.opensudoku2.utils

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.File

object AndroidUtils {
	/**
	 * Returns version code of OpenSudoku2.
	 */
	fun getAppVersionCode(context: Context): Long {
		return try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
			} else {
				@Suppress("DEPRECATION")
				context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
			}
		} catch (e: PackageManager.NameNotFoundException) {
			throw RuntimeException(e)
		}
	}

	/**
	 * Returns version name of OpenSudoku2.
	 */
	fun getAppVersionName(context: Context): String {
		return try {
			context.packageManager.getPackageInfo(context.packageName, 0).versionName
		} catch (e: PackageManager.NameNotFoundException) {
			throw RuntimeException(e)
		}
	}
}

internal fun Uri.getFileName(contentResolver: ContentResolver): String? {
	contentResolver.query(this, null, null, null, null)?.use { cursor ->
		if (cursor.moveToFirst()) {
			return@getFileName File(cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))).name
		}
	}
	return lastPathSegment
}
