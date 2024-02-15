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

package org.buliasz.opensudoku2.gui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
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
		AlertDialog.Builder(mContext)
			.setIcon(R.drawable.ic_info)
			.setTitle(R.string.what_is_new)
			.setMessage(HtmlCompat.fromHtml(changelog, HtmlCompat.FROM_HTML_MODE_COMPACT))
			.setPositiveButton(R.string.close, null)
			.create()
			.show()
	}

	private val changelogFromResources: String
		get() {
			var inputStream: InputStream? = null
			try {
				inputStream = mContext.resources.openRawResource(R.raw.changelog)
				val buffer = CharArray(0x10000)
				val out = StringBuilder()
				val reader: Reader
				reader = InputStreamReader(inputStream, StandardCharsets.UTF_8)
				var read: Int
				do {
					read = reader.read(buffer, 0, buffer.size)
					if (read > 0) {
						out.appendRange(buffer, 0, read)
					}
				} while (read >= 0)
				return "$out"
			} catch (e: IOException) {
				e.printStackTrace()
				Log.e(TAG, "Error when reading changelog from raw resources.", e)
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close()
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
