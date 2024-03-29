/*
 * This file is part of Open Sudoku 2 - an open-source Sudoku game.
 * Copyright (C) 2009-2024 by original authors.
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
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.gui.fragments.GameSettingsFragment

class GameSettingsActivity : ThemedActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, FragmentManager.OnBackStackChangedListener {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.preferences_host)
		supportFragmentManager.addOnBackStackChangedListener(this)
		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.replace(R.id.preferences_content, GameSettingsFragment())
				.commit()
		}
	}

	override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val args = pref.extras
		val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment ?: return true)
		fragment.arguments = args

		@Suppress("DEPRECATION")    // known bug in Preferences library https://stackoverflow.com/a/74230035/7926219
		fragment.setTargetFragment(caller, 0)

		supportFragmentManager.beginTransaction()
			.replace(R.id.preferences_content, fragment)
			.addToBackStack(null)
			.commit()
		return true
	}

	override fun onBackStackChanged() {
		// One of the nested fragments (e.g., CustomThemeFragment) may have changed a preference
		// that changes the color/theme. Check whenever the backstack changes, so that when the
		// user returns from a nested fragment the activity will recreate with the new colors
		// if appropriate.
		recreateActivityIfThemeChanged()
	}
}
