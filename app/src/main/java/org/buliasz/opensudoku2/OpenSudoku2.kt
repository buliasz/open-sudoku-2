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
		val oldVersion = sharedPreferences.getInt("schema_version", 20231221)
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
		editor.putInt("schema_version", newVersion)
		editor.apply()
	}

	companion object {
		private val TAG = OpenSudoku2::class.java.simpleName
	}
}
