package org.buliasz.opensudoku2.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.gui.SudokuBoardView
import kotlin.math.abs

object ThemeUtils {
    private val TAG = ThemeUtils::class.java.simpleName
    private val MATERIAL_COLORS = intArrayOf(
        -0x21f24,
        -0x64245,
        -0x96678,
        -0xc93a0,
        -0x17b1c0,
        -0x1ae3dd,
        -0x22e6e3,
        -0x2fe8ea,
        -0x3bebef,
        -0x4fedf6,
        -0x8669,
        -0xae89,
        -0xd291,
        -0x1fffce,
        -0x31b14,
        -0x74430,
        -0xb704f,
        -0xf9d6e,
        -0x13bf86,
        -0x16e19d,
        -0x27e4a0,
        -0x3de7a5,
        -0x52eba9,
        -0x77f1b1,
        -0x7f55,
        -0xbf7f,
        -0xaffa9,
        -0x3aee9e,
        -0xc1a0b,
        -0x1e4119,
        -0x316c28,
        -0x459738,
        -0x54b844,
        -0x63d850,
        -0x71db56,
        -0x84e05e,
        -0x95e466,
        -0xb5eb74,
        -0x157f04,
        -0x1fbf05,
        -0x2aff07,
        -0x55ff01,
        -0x12180a,
        -0x2e3b17,
        -0x4c6225,
        -0x6a8a33,
        -0x81a83e,
        -0x98c549,
        -0xa1ca4f,
        -0xaed258,
        -0xbad860,
        -0xcee46e,
        -0x4c7701,
        -0x83b201,
        -0x9ae001,
        -0x9dff16,
        -0x17150a,
        -0x3a3517,
        -0x605726,
        -0x867935,
        -0xa39440,
        -0xc0ae4b,
        -0xc6b655,
        -0xcfc061,
        -0xd7ca6d,
        -0xe5dc82,
        -0x736101,
        -0xac9202,
        -0xc2a502,
        -0xcfb002,
        -0x181603,
        -0x2f2601,
        -0x504001,
        -0x6e5801,
        -0x8c7002,
        -0xa98804,
        -0xb19311,
        -0xbaa122,
        -0xc4af32,
        -0xd5c94f,
        -0x594501,
        -0x977601,
        -0xb28c01,
        -0xb29601,
        -0x1e0a02,
        -0x4c1a04,
        -0x7e2b06,
        -0xb03c09,
        -0xd6490a,
        -0xfc560c,
        -0xfc641b,
        -0xfd772f,
        -0xfd8843,
        -0xfea865,
        -0x7f2701,
        -0xbf3b01,
        -0xff4f01,
        -0xff6e16,
        -0x1f0806,
        -0x4d140e,
        -0x7f2116,
        -0xb22f1f,
        -0xd93926,
        -0xff432c,
        -0xff533f,
        -0xff6859,
        -0xff7c71,
        -0xff9f9c,
        -0x7b0001,
        -0xe70001,
        -0xff1a01,
        -0xff472c,
        -0x1f0d0f,
        -0x4d2025,
        -0x7f343c,
        -0xb24954,
        -0xd95966,
        -0xff6978,
        -0xff7685,
        -0xff8695,
        -0xff96a4,
        -0xffb2c0,
        -0x580015,
        -0x9b0026,
        -0xe2164a,
        -0xff405b,
        -0x2f0732,
        -0x5c165c,
        -0x8d2a8e,
        -0xbd42bf,
        -0xd450d5,
        -0xda64dc,
        -0xf570f8,
        -0xf581f9,
        -0xfa9100,
        -0xf2acfe,
        -0x5d0873,
        -0xa50ea8,
        -0xeb18eb,
        -0xed3900,
        -0xe0717,
        -0x231238,
        -0x3a1e5b,
        -0x512a7f,
        -0x63339b,
        -0x743cb6,
        -0x834cbe,
        -0x9760c8,
        -0xaa74d1,
        -0xcc96e2,
        -0x330070,
        -0x4d00a7,
        -0x8900fd,
        -0x9b22e9,
        -0x60419,
        -0xf0b3d,
        -0x191164,
        -0x23188b,
        -0x2b1ea9,
        -0x3223c7,
        -0x3f35cd,
        -0x504bd5,
        -0x6162dc,
        -0x7d88e9,
        -0xb007f,
        -0x1100bf,
        -0x390100,
        -0x511600,
        -0x219,
        -0x63c,
        -0xa63,
        -0xe8a,
        -0x11a8,
        -0x14c5,
        -0x227cb,
        -0x43fd3,
        -0x657db,
        -0xa80e9,
        -0x73,
        -0x100,
        -0x1600,
        -0x2a00,
        -0x71f,
        -0x134d,
        -0x1f7e,
        -0x2ab1,
        -0x35d8,
        -0x3ef9,
        -0x4d00,
        -0x6000,
        -0x7100,
        -0x9100,
        -0x1a81,
        -0x28c0,
        -0x3c00,
        -0x5500,
        -0xc20,
        -0x1f4e,
        -0x3380,
        -0x48b3,
        -0x58da,
        -0x6800,
        -0x47400,
        -0xa8400,
        -0x109400,
        -0x19af00,
        -0x2e80,
        -0x54c0,
        -0x6f00,
        -0x9300,
        -0x41619,
        -0x3344,
        -0x546f,
        -0x759b,
        -0x8fbd,
        -0xa8de,
        -0xbaee2,
        -0x19b5e7,
        -0x27bceb,
        -0x40c9f4,
        -0x6180,
        -0x91c0,
        -0xc300,
        -0x22d400,
        -0x101417,
        -0x283338,
        -0x43555c,
        -0x5e7781,
        -0x72919d,
        -0x86aab8,
        -0x92b3bf,
        -0xa2bfc9,
        -0xb1cbd2,
        -0xc1d8dd,
        -0x50506,
        -0xa0a0b,
        -0x111112,
        -0x1f1f20,
        -0x424243,
        -0x616162,
        -0x8a8a8b,
        -0x9e9e9f,
        -0xbdbdbe,
        -0xdededf,
        -0x1000000,
        -0x1,
        -0x13100f,
        -0x302724,
        -0x4f413b,
        -0x6f5b52,
        -0x876f64,
        -0x9f8275,
        -0xab9186,
        -0xbaa59c,
        -0xc8b8b1,
        -0xd9cdc8
    )
    var sTimestampOfLastThemeUpdate: Long = 0
    fun getThemeResourceIdFromString(theme: String?): Int {
        return when (theme) {
            "default" -> R.style.AppTheme_Default
            "amoled" -> R.style.AppTheme_AMOLED
            "latte" -> R.style.AppTheme_Latte
            "espresso" -> R.style.AppTheme_Espresso
            "sunrise" -> R.style.AppTheme_Sunrise
            "honeybee" -> R.style.AppTheme_HoneyBee
            "crystal" -> R.style.AppTheme_Crystal
            "midnight_blue" -> R.style.AppTheme_MidnightBlue
            "emerald" -> R.style.AppTheme_Emerald
            "forest" -> R.style.AppTheme_Forest
            "amethyst" -> R.style.AppTheme_Amethyst
            "ruby" -> R.style.AppTheme_Ruby
            "paper" -> R.style.AppTheme_Paper
            "graphpaper" -> R.style.AppTheme_GraphPaper
            "light" -> R.style.AppTheme_Light
            "paperlight" -> R.style.AppTheme_PaperLight
            "graphpaperlight" -> R.style.AppTheme_GraphPaperLight
            "highcontrast" -> R.style.AppTheme_HighContrast
            "invertedhighcontrast" -> R.style.AppTheme_InvertedHighContrast
            "custom" -> R.style.AppTheme_OpenSudoku2
            "custom_light" -> R.style.AppTheme_OpenSudoku2
            "opensudoku2" -> R.style.AppTheme_OpenSudoku2
            else -> R.style.AppTheme_OpenSudoku2
        }
    }

    fun isDarkTheme(themeCode: String?): Boolean {
        return when (themeCode) {
            "default", "amoled", "espresso", "honeybee", "midnight_blue", "forest", "ruby", "paper", "graphpaper", "highcontrast",
            "custom" -> true

            "opensudoku2", "latte", "sunrise", "crystal", "emerald", "amethyst", "light", "paperlight", "graphpaperlight",
            "invertedhighcontrast", "custom_light" -> false

            else -> false
        }
    }

    fun getCurrentThemeFromPreferences(context: Context?): String {
        val gameSettings = PreferenceManager.getDefaultSharedPreferences(context!!)
        return gameSettings.getString("theme", "opensudoku2")!!
    }

    fun setThemeFromPreferences(activity: Activity) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        // If dynamic colors are enabled then use them, overriding other settings
        val useDynamicColor = sharedPreferences.getBoolean("use_dynamic_color", false)
        if (useDynamicColor) {
            Log.d(TAG, "Using dynamic colors and returning")
            DynamicColors.applyToActivityIfAvailable(activity)
            return
        }
        val themeCode = sharedPreferences.getString("theme", "opensudoku2")
        val customTheme = "custom" == themeCode || "custom_light" == themeCode
        val themeId: Int = if (customTheme) {
            val light = "light" == sharedPreferences.getString("custom_theme_ui_mode", "light")
            if (light) {
                getThemeResourceIdFromString("custom_light")
            } else {
                getThemeResourceIdFromString("custom")
            }
        } else {
            getThemeResourceIdFromString(themeCode)
        }
        activity.setTheme(themeId)
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()

        // A dark theme overrides the UI mode, and is always in night mode. Otherwise,
        // follow the user's preference.
        //
        // If the UI mode has changed then signal the caller to recreate.
        val newNightMode: Int = if (isDarkTheme(themeCode)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            when (sharedPreferences.getString("ui_mode", "system")) {
                "light" -> {
                    AppCompatDelegate.MODE_NIGHT_NO
                }

                "dark" -> {
                    AppCompatDelegate.MODE_NIGHT_YES
                }

                else -> {
                    // Default behaviour (including if the value is unrecognised) is to follow
                    // the system.
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            }
        }
        if (newNightMode != currentNightMode) {
            AppCompatDelegate.setDefaultNightMode(newNightMode)
        }

        // https://issuetracker.google.com/issues/123835106 -- calling applyStyle does not work
        // if the night mode has changed, and the new night mode is "MODE_NIGHT_YES". The activity
        // has to recreate again, the night mode does not change, and then the call to applyStyle
        // a few lines below here will work.
        //
        // This does cause a flicker when the activity recreates, but it only occurs if the user
        // toggle from light to dark mode, and has a custom theme set, which is a limited set of
        // circumstances.
        if (newNightMode != currentNightMode && newNightMode == AppCompatDelegate.MODE_NIGHT_YES && customTheme) {
            ActivityCompat.recreate(activity)
        }
        if (customTheme) {
            val themeResource = activity.theme
            val colorPrimaryResourceId = getColorPrimaryResourceId(
                activity,
                findClosestMaterialColor(sharedPreferences.getInt("custom_theme_colorPrimary", -0xb24954))
            )
            themeResource.applyStyle(colorPrimaryResourceId, true)
            val colorAccentResourceId = getColorAccentResourceId(
                activity,
                findClosestMaterialColor(sharedPreferences.getInt("custom_theme_colorAccent", -0x9a9a9b))
            )
            themeResource.applyStyle(colorAccentResourceId, true)
        }
    }

    fun getCurrentThemeColor(context: Context, colorAttribute: Int): Int {
        val attributes = intArrayOf(colorAttribute)
        val themeColors = context.theme.obtainStyledAttributes(attributes)
        return themeColors.getColor(0, Color.BLACK)
    }

    /**
     * Updates the colors of `board` to use colors defined by the `custom_theme_...` preferences.
     * @param context
     * @param board
     */
    fun applyCustomThemeToSudokuBoardViewFromSharedPreferences(context: Context?, board: SudokuBoardView) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context!!)
        board.setLineColor(
            sharedPreferences.getInt("custom_theme_lineColor", ContextCompat.getColor(context, R.color.default_lineColor))
        )
        board.setSectorLineColor(
            sharedPreferences.getInt("custom_theme_sectorLineColor", ContextCompat.getColor(context, R.color.default_sectorLineColor))
        )
        board.setTextColor(
            sharedPreferences.getInt("custom_theme_textColor", ContextCompat.getColor(context, R.color.default_textColor))
        )
        board.setTextColorNote(
            sharedPreferences.getInt("custom_theme_textColorNote", ContextCompat.getColor(context, R.color.default_textColorNote))
        )
        board.setBackgroundColor(
            sharedPreferences.getInt(
                "custom_theme_backgroundColor",
                ContextCompat.getColor(context, R.color.default_backgroundColor)
            )
        )
        board.textColorReadOnly =
            sharedPreferences.getInt("custom_theme_textColorReadOnly", ContextCompat.getColor(context, R.color.default_textColorReadOnly))
        board.backgroundColorReadOnly =
            sharedPreferences.getInt(
                "custom_theme_backgroundColorReadOnly",
                ContextCompat.getColor(context, R.color.default_backgroundColorReadOnly)
            )
        board.setTextColorTouched(
            sharedPreferences.getInt(
                "custom_theme_textColorTouched",
                ContextCompat.getColor(context, R.color.default_textColorTouched)
            )
        )
        board.setTextColorNoteTouched(
            sharedPreferences.getInt(
                "custom_theme_textColorNoteTouched",
                ContextCompat.getColor(context, R.color.default_textColorNoteTouched)
            )
        )
        board.setBackgroundColorTouched(
            sharedPreferences.getInt(
                "custom_theme_backgroundColorTouched",
                ContextCompat.getColor(context, R.color.default_backgroundColorTouched)
            )
        )
        board.setBackgroundColorSelected(
            sharedPreferences.getInt(
                "custom_theme_backgroundColorSelected",
                ContextCompat.getColor(context, R.color.default_backgroundColorSelected)
            )
        )
        board.textColorHighlighted =
            sharedPreferences.getInt(
                "custom_theme_textColorHighlighted",
                ContextCompat.getColor(context, R.color.default_textColorHighlighted)
            )
        board.setTextColorNoteHighlighted(
            sharedPreferences.getInt(
                "custom_theme_textColorNoteHighlighted",
                ContextCompat.getColor(context, R.color.default_textColorNoteHighlighted)
            )
        )
        board.backgroundColorHighlighted =
            sharedPreferences.getInt(
                "custom_theme_backgroundColorHighlighted",
                ContextCompat.getColor(context, R.color.default_backgroundColorHighlighted)
            )
        board.setTextColorEven(
            sharedPreferences.getInt(
                "custom_theme_textColorEven",
                ContextCompat.getColor(context, R.color.default_textColor)
            )
        )
        board.setTextColorNoteEven(
            sharedPreferences.getInt(
                "custom_theme_textColorNoteEven",
                ContextCompat.getColor(context, R.color.default_textColor)
            )
        )
        board.setBackgroundColorEven(
            sharedPreferences.getInt(
                "custom_theme_backgroundColorEven",
                ContextCompat.getColor(context, R.color.default_backgroundColor)
            )
        )
        board.setTextColorError(
            sharedPreferences.getInt(
                "custom_theme_textColorError",
                ContextCompat.getColor(context, R.color.default_textColorError)
            )
        )
        board.setBackgroundColorError(
            sharedPreferences.getInt(
                "custom_theme_backgroundColorError",
                ContextCompat.getColor(context, R.color.default_backgroundColorError)
            )
        )
    }

    fun applyThemeToSudokuBoardViewFromContext(theme: String, board: SudokuBoardView, context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (theme == "custom" || theme == "custom_light") {
            applyCustomThemeToSudokuBoardViewFromSharedPreferences(context, board)
        } else {
            // If the theme implies dark mode then show the dark mode colours. Do this
            // by constructing a new context with a `Configuration` that forces UI_MODE_NIGHT_YES.
            // This will cause the later colour lookups to use the version of the theme from
            // values-night/.
            //
            // Doing this with AppCompatDelegate.setDefaultNightMode() is destructive -- the
            // activity/fragment/dialog would be destroyed and recreated whenever the user previews
            // a new theme.
            val mode = sharedPreferences.getString("ui_mode", "system")
            val configContext: Context

            // Previewing a dark theme, or the user's preference is for dark mode, show it in
            // dark mode
            if (isDarkTheme(theme) || "dark" == mode) {
                val config = context.resources.configuration
                config.uiMode = Configuration.UI_MODE_NIGHT_YES or (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
                configContext = context.createConfigurationContext(config)
            } else if ("light" == mode) {
                val config = context.resources.configuration
                config.uiMode = Configuration.UI_MODE_NIGHT_NO or (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
                configContext = context.createConfigurationContext(config)
            } else {
                val config = context.applicationContext.resources.configuration
                configContext = context.createConfigurationContext(config)
            }
            val themeWrapper = ContextThemeWrapper(configContext, getThemeResourceIdFromString(theme))
            board.setAllColorsFromThemedContext(themeWrapper)
        }
        board.invalidate()
    }

    fun prepareSudokuPreviewView(board: SudokuBoardView) {
        board.isFocusable = false

        // Create a sample game by starting with the debug game, removing an extra box (sector),
        // adding in notes, and filling in the first 3 clues, an an invalid digit. This provides a
        // sample of an in-progress game that will demonstrate all of the possible scenarios that
        // have different theme colors applied to them.
        val cells: CellCollection = CellCollection.createDebugGame()
        cells.getCell(0, 3).value = 0
        cells.getCell(0, 4).value = 0
        cells.getCell(1, 3).value = 0
        cells.getCell(1, 5).value = 0
        cells.getCell(2, 4).value = 0
        cells.getCell(2, 5).value = 0
        cells.getCell(2, 2).value = 2
        cells.markAllCellsAsEditable()
        cells.markFilledCellsAsNotEditable()
        cells.getCell(0, 0).value = 1
        cells.getCell(0, 1).value = 2 // Invalid value
        cells.getCell(0, 2).value = 3
        cells.fillInCenterNotes()
        cells.validate()
        board.cells = cells
        board.setHighlightWrongValues(true)
    }

    fun findClosestMaterialColor(color: Int): Int {
        var minDifference = Int.MAX_VALUE
        var selectedIndex = 0
        var difference: Int
        var rdiff: Int
        var gdiff: Int
        var bdiff: Int
        for (i in MATERIAL_COLORS.indices) {
            if (color == MATERIAL_COLORS[i]) {
                return color
            }
            rdiff = abs(Color.red(color) - Color.red(MATERIAL_COLORS[i]))
            gdiff = abs(Color.green(color) - Color.green(MATERIAL_COLORS[i]))
            bdiff = abs(Color.blue(color) - Color.blue(MATERIAL_COLORS[i]))
            difference = rdiff + gdiff + bdiff
            if (difference < minDifference) {
                minDifference = difference
                selectedIndex = i
            }
        }
        return MATERIAL_COLORS[selectedIndex]
    }

    private fun getColorResourceIdHelper(context: Context, style: String, color: Int): Int {
        val colorAsString = String.format("%1$06x", findClosestMaterialColor(color) and 0x00FFFFFF)
        return context.resources.getIdentifier(style + colorAsString, "style", context.packageName)
    }

    private fun getColorPrimaryResourceId(context: Context, color: Int): Int = getColorResourceIdHelper(context, "colorPrimary_", color)

    private fun getColorAccentResourceId(context: Context, color: Int): Int = getColorResourceIdHelper(context, "colorAccent_", color)
}
