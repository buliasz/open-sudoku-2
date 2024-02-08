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

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.margaritov.preference.colorpicker.ColorPickerDialog
import net.margaritov.preference.colorpicker.ColorPickerPreference
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.gui.SudokuBoardView
import org.buliasz.opensudoku2.gui.SudokuBoardView.HighlightMode
import org.buliasz.opensudoku2.utils.ThemeUtils

/**
 * Preview and set a custom app theme.
 */
class CustomThemeFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener, MenuProvider {
	private lateinit var mBoard: SudokuBoardView
	private var mCopyFromExistingThemeDialog: Dialog? = null
	private var mSharedPreferences: SharedPreferences? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.preferences_custom_theme, rootKey)
		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
		val themeCode = sharedPreferences.getString("theme", "custom")
		mSharedPreferences = sharedPreferences

		val uiModePref = findPreference<ListPreference>("custom_theme_ui_mode")
		if (themeCode == "custom") {
			uiModePref!!.value = "dark"
		}
		if (themeCode == "custom_light") {
			uiModePref!!.value = "light"
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		// Customise the view to include the game board preview. Do this by inflating the
		// default view and the desired view (as a ViewGroup), and then adding the default view
		// to the view group.
		val defaultView = super.onCreateView(inflater, container, savedInstanceState)
		val viewGroup = inflater.inflate(R.layout.preference_custom_theme, container, false) as ViewGroup
		viewGroup.addView(defaultView)

		// The normal preferences layout (i.e., the layout that defaultView is using) forces the
		// width to match the parent. In landscape mode this shrinks the width of the board to 0.
		//
		// To fix, wait until after initialView has been added to viewGroup (so the layout params
		// are the correct type), then override the width and weight to take up 50% of the screen.
		if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			val layoutParams = defaultView.layoutParams as LinearLayout.LayoutParams
			layoutParams.width = 0
			layoutParams.weight = 1f
			defaultView.layoutParams = layoutParams
		}
		requireActivity().setTitle(R.string.screen_custom_theme)
		return viewGroup
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		mBoard = view.findViewById(R.id.board_view)
		prepareGamePreviewView(mBoard)
		val menuHost: MenuHost = requireActivity()
		menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
	}

	private fun prepareGamePreviewView(board: SudokuBoardView) {
		val highlightSimilarCells = mSharedPreferences!!.getBoolean("highlight_similar_cells", true)
		val highlightSimilarNotes = mSharedPreferences!!.getBoolean("highlight_similar_notes", true)
		if (highlightSimilarCells) {
			board.highlightSimilarCells = if (highlightSimilarNotes) HighlightMode.NUMBERS_AND_NOTES else HighlightMode.NUMBERS
		}
		board.onCellSelectedListener = { cell: Cell? -> board.highlightedValue = cell?.value ?: 0 }
		ThemeUtils.prepareBoardPreviewView(board)
		updateThemePreview()
	}

	private fun updateThemePreview() {
		val themeName = mSharedPreferences!!.getString("theme", "opensudoku2")
		ThemeUtils.applyThemeToSudokuBoardViewFromContext(themeName!!, mBoard, requireContext())
	}

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.custom_theme, menu)
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.copy_from_theme -> {
				showCopyFromExistingThemeDialog()
				true
			}

			R.id.create_from_color -> {
				showCreateFromSingleColorDialog()
				true
			}

			else -> false
		}
	}

	private fun showCopyFromExistingThemeDialog() {
		val builder = AlertDialog.Builder(context)
		builder.setTitle(R.string.select_theme)
		builder.setNegativeButton(android.R.string.cancel, null)
		val themeNames = requireContext().resources.getStringArray(R.array.theme_names)
		val themeNamesWithoutCustomTheme = themeNames.copyOfRange(0, themeNames.size - 1)
		builder.setItems(themeNamesWithoutCustomTheme) { _: DialogInterface?, which: Int ->
			copyFromExistingThemeIndex(which)
			mCopyFromExistingThemeDialog!!.dismiss()
		}
		val copyFromExistingThemeDialog = builder.create()
		copyFromExistingThemeDialog.setOnDismissListener({ mCopyFromExistingThemeDialog = null })
		copyFromExistingThemeDialog.show()
		mCopyFromExistingThemeDialog = copyFromExistingThemeDialog
	}

	private fun showCreateFromSingleColorDialog() {
		val colorDialog = ColorPickerDialog(context, mSharedPreferences!!.getInt("custom_theme_colorPrimary", Color.WHITE), "XXX")
		colorDialog.alphaSliderVisible = false
		colorDialog.hexValueEnabled = true
		colorDialog.setOnColorChangedListener { colorPrimary: Int -> createCustomThemeFromSingleColor(colorPrimary) }
		colorDialog.show()
	}

	private fun copyFromExistingThemeIndex(which: Int) {
		val context = requireContext()
		val themeCode = context.resources.getStringArray(R.array.theme_codes)[which]

		// Copy attributes that correspond to the light or dark context for the theme by preparing
		// a ConfigurationContext that uses the appropriate UI mode.
		val isDarkTheme = ThemeUtils.isDarkTheme(themeCode)
		val configContext: Context
		if (isDarkTheme) {
			val config = context.resources.configuration
			config.uiMode = Configuration.UI_MODE_NIGHT_YES or (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
			configContext = context.createConfigurationContext(config)
		} else {
			val config = context.resources.configuration
			config.uiMode = Configuration.UI_MODE_NIGHT_NO or (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
			configContext = context.createConfigurationContext(config)
		}
		val themeWrapper = ContextThemeWrapper(configContext, ThemeUtils.getThemeResourceIdFromString(themeCode))
		val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
		val editor = sharedPreferences.edit()
		if (isDarkTheme) {
			editor.putString("custom_theme_ui_mode", "dark")
		} else {
			editor.putString("custom_theme_ui_mode", "light")
		}
		editor.apply()

		// Copy these attributes from a theme...
		val attributes = intArrayOf(
			androidx.appcompat.R.attr.colorPrimary,
			androidx.appcompat.R.attr.colorPrimaryDark,
			R.attr.colorLine,
			R.attr.colorSectorLine,
			R.attr.colorText,
			R.attr.colorNoteText,
			R.attr.colorBackground,
			R.attr.colorReadOnlyText,
			R.attr.colorReadOnlyBackground,
			R.attr.colorTouchedText,
			R.attr.colorTouchedNoteText,  // *
			R.attr.colorTouchedBackground,
			R.attr.colorSelectedBackground,
			R.attr.colorHighlightedText,
			R.attr.colorHighlightedNoteText,  // *
			R.attr.colorHighlightedBackground,
			R.attr.colorInvalidText,
			R.attr.colorInvalidBackground,
			R.attr.colorEvenText,
			R.attr.colorEvenNoteText,
			R.attr.colorEvenBackground
		)

		// ... and set them as the value of these preferences. The 'attributes' and 'preferenceKeys'
		// arrays must be the same length, and the same order (i.e., the attribute at index 0 must
		// contain the value for the preference at index 0, and so on).
		val preferenceKeys = arrayOf(
			"custom_theme_colorPrimary",
			"custom_theme_colorAccent",
			"custom_theme_colorLine",
			"custom_theme_colorSectorLine",
			"custom_theme_colorText",
			"custom_theme_colorNoteText",
			"custom_theme_colorBackground",
			"custom_theme_colorReadOnlyText",
			"custom_theme_colorReadOnlyBackground",
			"custom_theme_colorTouchedText",
			"custom_theme_colorTouchedNoteText",
			"custom_theme_colorTouchedBackground",
			"custom_theme_colorSelectedBackground",
			"custom_theme_colorHighlightedText",
			"custom_theme_colorHighlightedNoteText",
			"custom_theme_colorHighlightedBackground",
			"custom_theme_colorTextError",
			"custom_theme_colorBackgroundError",
			"custom_theme_colorEvenText",
			"custom_theme_colorEvenNoteText",
			"custom_theme_colorEvenBackground"
		)
		assert(attributes.size == preferenceKeys.size)
		val themeColors = themeWrapper.theme.obtainStyledAttributes(attributes)
		for (i in attributes.indices) {
			(findPreference<Preference>(preferenceKeys[i]) as ColorPickerPreference?)!!.onColorChanged(themeColors.getColor(i, Color.GRAY))
		}
		themeColors.recycle()
	}

	private fun createCustomThemeFromSingleColor(@ColorInt colorPrimary: Int) {
		val whiteContrast = ColorUtils.calculateContrast(colorPrimary, Color.WHITE)
		val blackContrast = ColorUtils.calculateContrast(colorPrimary, Color.BLACK)
		val isLightTheme = whiteContrast < blackContrast
		val colorAsHSL = FloatArray(3)
		ColorUtils.colorToHSL(colorPrimary, colorAsHSL)
		var tempHSL = colorAsHSL.clone()
		tempHSL[0] = (colorAsHSL[0] + 180f) % 360.0f
		val colorAccent = ColorUtils.HSLToColor(tempHSL)
		tempHSL = colorAsHSL.clone()
		tempHSL[2] += if (isLightTheme) -0.1f else 0.1f
		val colorPrimaryDark = ColorUtils.HSLToColor(tempHSL)
		val colorText = if (isLightTheme) Color.BLACK else Color.WHITE
		val colorBackground = if (isLightTheme) Color.WHITE else Color.BLACK
		(findPreference<Preference>("custom_theme_colorLine") as ColorPickerPreference?)!!.onColorChanged(colorPrimaryDark)
		(findPreference<Preference>("custom_theme_colorSectorLine") as ColorPickerPreference?)!!.onColorChanged(colorPrimaryDark)
		(findPreference<Preference>("custom_theme_colorText") as ColorPickerPreference?)!!.onColorChanged(colorText)
		(findPreference<Preference>("custom_theme_colorNoteText") as ColorPickerPreference?)!!.onColorChanged(colorText)
		(findPreference<Preference>("custom_theme_colorBackground") as ColorPickerPreference?)!!.onColorChanged(colorBackground)
		val colorReadOnlyText = colorOn(colorPrimary)
		(findPreference<Preference>("custom_theme_colorReadOnlyText") as ColorPickerPreference?)!!.onColorChanged(colorReadOnlyText)
		(findPreference<Preference>("custom_theme_colorReadOnlyBackground") as ColorPickerPreference?)!!.onColorChanged(colorPrimary)
		val colorTouchedText = colorOn(colorAccent)
		(findPreference<Preference>("custom_theme_colorTouchedText") as ColorPickerPreference?)!!.onColorChanged(colorTouchedText)
		(findPreference<Preference>("custom_theme_colorTouchedNoteText") as ColorPickerPreference?)!!.onColorChanged(colorTouchedText)
		(findPreference<Preference>("custom_theme_colorTouchedBackground") as ColorPickerPreference?)!!.onColorChanged(colorAccent)
		(findPreference<Preference>("custom_theme_colorSelectedBackground") as ColorPickerPreference?)!!.onColorChanged(colorPrimaryDark)
		val colorHighlightedText = colorOn(colorPrimary)
		(findPreference<Preference>("custom_theme_colorHighlightedText") as ColorPickerPreference?)!!.onColorChanged(colorHighlightedText)
		(findPreference<Preference>("custom_theme_colorHighlightedNoteText") as ColorPickerPreference?)!!.onColorChanged(
			colorHighlightedText
		)
		(findPreference<Preference>("custom_theme_colorHighlightedBackground") as ColorPickerPreference?)!!.onColorChanged(colorPrimary)
		(findPreference<Preference>("custom_theme_colorEvenText") as ColorPickerPreference?)!!.onColorChanged(colorText)
		(findPreference<Preference>("custom_theme_colorEvenNoteText") as ColorPickerPreference?)!!.onColorChanged(colorText)
		// Default to transparent
		(findPreference<Preference>("custom_theme_colorEvenBackground") as ColorPickerPreference?)!!.onColorChanged(colorBackground)
		(findPreference<Preference>("custom_theme_colorPrimary") as ColorPickerPreference?)!!.onColorChanged(colorPrimary)
		(findPreference<Preference>("custom_theme_colorAccent") as ColorPickerPreference?)!!.onColorChanged(colorAccent)
	}

	/**
	 * @param background background color
	 * @return a color suitable for using "on" the given background color.
	 */
	@ColorInt
	private fun colorOn(@ColorInt background: Int): Int {
		val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, background)
		val blackContrast = ColorUtils.calculateContrast(Color.BLACK, background)
		return if (whiteContrast >= blackContrast) Color.WHITE else Color.BLACK
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
		if (key == "theme") {
			// This is the theme changing when custom_theme_ui_mode changes, and can be ignored.
			return
		}
		if (key == "custom_theme_ui_mode") {
			setThemeCodeFromUiMode(sharedPreferences)
			val mode = sharedPreferences.getString("custom_theme_ui_mode", "system")
			ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
			if (mode == "light") {
				val editor = sharedPreferences.edit()
				editor.putString("theme", "custom_light")
				editor.apply()
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
				return
			}
			if (mode == "dark") {
				val editor = sharedPreferences.edit()
				editor.putString("theme", "custom")
				editor.apply()
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
				return
			}
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
			return
		}
		if (key!!.contains("custom_theme_color")) {
			quantizeCustomAppColorPreferences()
		}
		updateThemePreview()
		ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
	}

	/**
	 * Edits the "theme" preference to "custom" or "custom_light" based on the value of
	 * "custom_theme_ui_mode" and applies the change.
	 *
	 * @param sharedPreferences shared preferences to edit
	 */
	private fun setThemeCodeFromUiMode(sharedPreferences: SharedPreferences) {
		val mode = sharedPreferences.getString("custom_theme_ui_mode", "system")
		val editor = sharedPreferences.edit()
		if (mode == "light") {
			editor.putString("theme", "custom_light")
		}
		if (mode == "dark") {
			editor.putString("theme", "custom")
		}
		editor.apply()
		ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
	}

	private fun quantizeCustomAppColorPreferences() {
		val settingsEditor = mSharedPreferences!!.edit()
		settingsEditor.putInt(
			"custom_theme_colorPrimary",
			ThemeUtils.findClosestMaterialColor(mSharedPreferences!!.getInt("custom_theme_colorPrimary", Color.GRAY))
		)
		settingsEditor.putInt(
			"custom_theme_colorAccent",
			ThemeUtils.findClosestMaterialColor(mSharedPreferences!!.getInt("custom_theme_colorAccent", Color.WHITE))
		)
		settingsEditor.apply()
	}

	override fun onResume() {
		super.onResume()
		mSharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
	}

	override fun onPause() {
		super.onPause()
		mSharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
	}
}
