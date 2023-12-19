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
    private var mBoard: SudokuBoardView? = null
    private var mCopyFromExistingThemeDialog: Dialog? = null
    private var mSharedPreferences: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_custom_theme, rootKey)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val themeCode = sharedPreferences.getString("theme", "custom")
        mSharedPreferences = sharedPreferences

        val uiModePref = findPreference<ListPreference>("custom_theme_ui_mode")
        if ("custom" == themeCode) {
            uiModePref!!.value = "dark"
        }
        if ("custom_light" == themeCode) {
            uiModePref!!.value = "light"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Customise the view to include the sudoku board preview. Do this by inflating the
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
        prepareSudokuPreviewView(mBoard)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun prepareSudokuPreviewView(board: SudokuBoardView?) {
        val highlightSimilarCells = mSharedPreferences!!.getBoolean("highlight_similar_cells", true)
        val highlightSimilarNotes = mSharedPreferences!!.getBoolean("highlight_similar_notes", true)
        if (highlightSimilarCells) {
            board!!.setHighlightSimilarCell(if (highlightSimilarNotes) HighlightMode.NUMBERS_AND_NOTES else HighlightMode.NUMBERS)
        }
        board!!.setOnCellSelectedListener { cell: Cell? -> board.setHighlightedValue(cell?.value ?: 0) }
        ThemeUtils.prepareSudokuPreviewView(board)
        updateThemePreview()
    }

    private fun updateThemePreview() {
        val themeName = mSharedPreferences!!.getString("theme", "opensudoku2")
        ThemeUtils.applyThemeToSudokuBoardViewFromContext(themeName!!, mBoard!!, requireContext())
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
            R.attr.lineColor,
            R.attr.sectorLineColor,
            R.attr.textColor,
            R.attr.textColorNote,
            R.attr.backgroundColor,
            R.attr.textColorReadOnly,
            R.attr.backgroundColorReadOnly,
            R.attr.textColorTouched,
            R.attr.textColorNoteTouched,  // *
            R.attr.backgroundColorTouched,
            R.attr.backgroundColorSelected,
            R.attr.textColorHighlighted,
            R.attr.textColorNoteHighlighted,  // *
            R.attr.backgroundColorHighlighted,
            R.attr.textColorInvalid,
            R.attr.backgroundColorInvalid,
            R.attr.textColorEven,
            R.attr.textColorNoteEven,
            R.attr.backgroundColorEven
        )

        // ... and set them as the value of these preferences. The 'attributes' and 'preferenceKeys'
        // arrays must be the same length, and the same order (i.e., the attribute at index 0 must
        // contain the value for the preference at index 0, and so on).
        val preferenceKeys = arrayOf(
            "custom_theme_colorPrimary",
            "custom_theme_colorAccent",
            "custom_theme_lineColor",
            "custom_theme_sectorLineColor",
            "custom_theme_textColor",
            "custom_theme_textColorNote",
            "custom_theme_backgroundColor",
            "custom_theme_textColorReadOnly",
            "custom_theme_backgroundColorReadOnly",
            "custom_theme_textColorTouched",
            "custom_theme_textColorNoteTouched",
            "custom_theme_backgroundColorTouched",
            "custom_theme_backgroundColorSelected",
            "custom_theme_textColorHighlighted",
            "custom_theme_textColorNoteHighlighted",
            "custom_theme_backgroundColorHighlighted",
            "custom_theme_textColorError",
            "custom_theme_backgroundColorError",
            "custom_theme_textColorEven",
            "custom_theme_textColorNoteEven",
            "custom_theme_backgroundColorEven"
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
        val textColor = if (isLightTheme) Color.BLACK else Color.WHITE
        val backgroundColor = if (isLightTheme) Color.WHITE else Color.BLACK
        (findPreference<Preference>("custom_theme_lineColor") as ColorPickerPreference?)!!.onColorChanged(colorPrimaryDark)
        (findPreference<Preference>("custom_theme_sectorLineColor") as ColorPickerPreference?)!!.onColorChanged(colorPrimaryDark)
        (findPreference<Preference>("custom_theme_textColor") as ColorPickerPreference?)!!.onColorChanged(textColor)
        (findPreference<Preference>("custom_theme_textColorNote") as ColorPickerPreference?)!!.onColorChanged(textColor)
        (findPreference<Preference>("custom_theme_backgroundColor") as ColorPickerPreference?)!!.onColorChanged(backgroundColor)
        val textColorReadOnly = colorOn(colorPrimary)
        (findPreference<Preference>("custom_theme_textColorReadOnly") as ColorPickerPreference?)!!.onColorChanged(textColorReadOnly)
        (findPreference<Preference>("custom_theme_backgroundColorReadOnly") as ColorPickerPreference?)!!.onColorChanged(colorPrimary)
        val textColorTouched = colorOn(colorAccent)
        (findPreference<Preference>("custom_theme_textColorTouched") as ColorPickerPreference?)!!.onColorChanged(textColorTouched)
        (findPreference<Preference>("custom_theme_textColorNoteTouched") as ColorPickerPreference?)!!.onColorChanged(textColorTouched)
        (findPreference<Preference>("custom_theme_backgroundColorTouched") as ColorPickerPreference?)!!.onColorChanged(colorAccent)
        (findPreference<Preference>("custom_theme_backgroundColorSelected") as ColorPickerPreference?)!!.onColorChanged(colorPrimaryDark)
        val textColorHighlighted = colorOn(colorPrimary)
        (findPreference<Preference>("custom_theme_textColorHighlighted") as ColorPickerPreference?)!!.onColorChanged(textColorHighlighted)
        (findPreference<Preference>("custom_theme_textColorNoteHighlighted") as ColorPickerPreference?)!!.onColorChanged(
            textColorHighlighted
        )
        (findPreference<Preference>("custom_theme_backgroundColorHighlighted") as ColorPickerPreference?)!!.onColorChanged(colorPrimary)
        (findPreference<Preference>("custom_theme_textColorEven") as ColorPickerPreference?)!!.onColorChanged(textColor)
        (findPreference<Preference>("custom_theme_textColorNoteEven") as ColorPickerPreference?)!!.onColorChanged(textColor)
        // Default to transparent
        (findPreference<Preference>("custom_theme_backgroundColorEven") as ColorPickerPreference?)!!.onColorChanged(backgroundColor)
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
        if ("theme" == key) {
            // This is the theme changing when custom_theme_ui_mode changes, and can be ignored.
            return
        }
        if ("custom_theme_ui_mode" == key) {
            setThemeCodeFromUiMode(sharedPreferences)
            val mode = sharedPreferences.getString("custom_theme_ui_mode", "system")
            ThemeUtils.sTimestampOfLastThemeUpdate = System.currentTimeMillis()
            if ("light" == mode) {
                val editor = sharedPreferences.edit()
                editor.putString("theme", "custom_light")
                editor.apply()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                return
            }
            if ("dark" == mode) {
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
        if ("light" == mode) {
            editor.putString("theme", "custom_light")
        }
        if ("dark" == mode) {
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
