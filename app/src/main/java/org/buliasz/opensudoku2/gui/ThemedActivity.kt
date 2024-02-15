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

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.buliasz.opensudoku2.utils.ThemeUtils

abstract class ThemedActivity : AppCompatActivity() {
	private var mTimestampWhenApplyingTheme: Long = 0
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ThemeUtils.setThemeFromPreferences(this)
		mTimestampWhenApplyingTheme = System.currentTimeMillis()
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
