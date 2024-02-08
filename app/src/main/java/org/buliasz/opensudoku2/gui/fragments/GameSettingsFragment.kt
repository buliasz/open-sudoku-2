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

package org.buliasz.opensudoku2.gui.fragments


import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.preference.DialogPreference.TargetFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.color.DynamicColors
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.utils.ThemeUtils

class GameSettingsFragment : PreferenceFragmentCompat(), TargetFragment, OnSharedPreferenceChangeListener {
	private inner class ThemeSummaryProvider : SummaryProvider<ListPreference?> {
		override fun provideSummary(preference: ListPreference): CharSequence? {
			return if (TextUtils.isEmpty(preference.entry)) {
				getString(R.string.theme_custom)
			} else {
				preference.entry
			}
		}
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.preferences, rootKey)
		val themePref = findPreference<ListPreference>("theme")!!
		themePref.summaryProvider = ThemeSummaryProvider()
		val themeName = themePref.value

		// Disable selection of the light/dark mode if the selected theme forces the mode
		if (ThemeUtils.isDarkTheme(themeName) || themeName == "custom" || themeName == "custom_light") {
			val uiModePref = findPreference<ListPreference>(getString(R.string.dark_mode_key))!!
			uiModePref.isEnabled = false
			uiModePref.summaryProvider = null
			uiModePref.setSummary(R.string.disabled_by_theme)
		}

		// Adjust the visibility of the dynamic colors option if the device supports it.
		// There's no point in disabling it -- that would still show the preference on a device
		// that, in its current state, is not capable of supporting the option.
		//
		// Note: Using dynamic color is a separate preference because it's not possible to
		// determine what the actual colors are, so it's impossible to create a preview of the
		// game board if this was enabled.
		val dynamicColorPref = findPreference<SwitchPreference>("use_dynamic_color")!!
		val dynamicColorsAvailable = DynamicColors.isDynamicColorAvailable()
		dynamicColorPref.isVisible = dynamicColorsAvailable

		// Disable theme selection if dynamic colors is enabled
		themePref.isEnabled = !dynamicColorPref.isChecked

		// Disable the custom theme colors preference if a custom theme is not selected.
		if (themeName != "custom" && themeName != "custom_light") {
			val screenCustomTheme = findPreference<Preference>("screen_custom_theme")!!
			screenCustomTheme.isEnabled = false
			screenCustomTheme.setSummary(R.string.screen_custom_theme_summary_disabled)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		requireActivity().setTitle(R.string.game_settings)
		return super.onCreateView(inflater, container, savedInstanceState)
	}

	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference.key != "theme") {
			super.onDisplayPreferenceDialog(preference)
			return
		}
		val f: ThemePreferenceDialogFragment = ThemePreferenceDialogFragment.newInstance(preference.key)

		@Suppress("DEPRECATION")    // known bug in Preferences library https://stackoverflow.com/a/74230035/7926219
		f.setTargetFragment(this, 0)

		f.show(parentFragmentManager, ThemePreferenceDialogFragment.TAG)
	}

	override fun <T : Preference?> findPreference(key: CharSequence): T? = super.findPreference(key)

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
		when (key) {
			getString(R.string.dark_mode_key) -> {
				val mode = sharedPreferences.getString(key, "system")
				if ("light" == mode) {
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
					return
				}
				if ("dark" == mode) {
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
					return
				}
				// Default behaviour (including if the value is unrecognised) is to follow
				// the system.
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
				return
			}

			"use_dynamic_color" -> {
				// Dynamic color overrides the current theme, so disable theme selection. Like
				// any other theme the timestamp must be updated and the activity recreated to
				// use the new colours.
				val useDynamicColor = sharedPreferences.getBoolean(key, false)
				val themePref = findPreference<ListPreference>("theme")!!
				themePref.isEnabled = !useDynamicColor
				ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
				ActivityCompat.recreate(requireActivity())
				return
			}

			"theme" -> {
				// A dark theme overrides the UI mode, and is always in night mode. Otherwise,
				// follow the user's preference.
				val themeName = sharedPreferences.getString(key, "opensudoku2")
				if (ThemeUtils.isDarkTheme(themeName)) {
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
				} else {
					when (sharedPreferences.getString("ui_mode", "system")) {
						"light" -> {
							AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
						}

						"dark" -> {
							AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
						}

						else -> { // default behaviour is to follow the system
							AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
						}
					}
				}

				val screenCustomTheme = findPreference<Preference>("screen_custom_theme")!!
				if (themeName == "custom" || themeName == "custom_light") {
					screenCustomTheme.isEnabled = true
					screenCustomTheme.summary = ""
				} else {
					screenCustomTheme.isEnabled = false
					screenCustomTheme.setSummary(R.string.screen_custom_theme_summary_disabled)
				}

				ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
			}
		}
	}

	override fun onResume() {
		super.onResume()
		preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
	}

	override fun onPause() {
		super.onPause()
		preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
	}
}
