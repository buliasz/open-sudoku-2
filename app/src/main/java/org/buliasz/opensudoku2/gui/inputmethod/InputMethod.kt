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
package org.buliasz.opensudoku2.gui.inputmethod

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.Button
import androidx.annotation.ColorInt
import com.google.android.material.color.MaterialColors
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.HintsQueue
import org.buliasz.opensudoku2.gui.SudokuBoardView
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanelStatePersister.StateBundle
import kotlin.math.ceil

/**
 * Base class for several input methods used to edit puzzle contents.
 */
abstract class InputMethod {
	abstract val switchModeButton: Button

	protected lateinit var mContext: Context
	private lateinit var mControlPanel: IMControlPanel
	protected lateinit var mGame: SudokuGame
	protected lateinit var mBoard: SudokuBoardView
	private var mHintsQueue: HintsQueue? = null
	private var mInputMethodView: View? = null
	protected var mActive = false

	/**
	 * This should be unique name of input method.
	 */
	var inputMethodName: String? = null
		private set
	private var mEnabled = true
	open fun initialize(context: Context, controlPanel: IMControlPanel, game: SudokuGame, board: SudokuBoardView, hintsQueue: HintsQueue?) {
		mContext = context
		mControlPanel = controlPanel
		mGame = game
		mBoard = board
		mHintsQueue = hintsQueue
		inputMethodName = this.javaClass.simpleName
	}

	val isInputMethodViewCreated: Boolean
		get() = mInputMethodView != null
	val inputMethodView: View
		get() = mInputMethodView!!

	fun createInputMethodView(): View {
		val inputMethodView = createControlPanelView(abbrName)
		mInputMethodView = inputMethodView
		onControlPanelCreated()
		return inputMethodView
	}

	/**
	 * This should be called when activity is paused (so InputMethod can do some cleanup,
	 * for example properly dismiss dialogs because of WindowLeaked exception).
	 */
	fun pause() {
		onPause()
	}

	protected open fun onPause() {}
	abstract val nameResID: Int
	abstract val helpResID: Int

	/**
	 * Gets abbreviated name of input method, which will be displayed on input method switch button.
	 */
	abstract val abbrName: String
	var isEnabled: Boolean
		get() = mEnabled
		set(enabled) {
			mEnabled = enabled
			if (!enabled) {
				mControlPanel.activateNextInputMethod()
			}
		}

	fun activate() {
		mActive = true
		onActivated()
	}

	fun deactivate() {
		mActive = false
		onDeactivated()
	}

	protected abstract fun createControlPanelView(abbrName: String): View
	private fun onControlPanelCreated() {}
	protected open fun onActivated() {}
	protected open fun onDeactivated() {}

	/**
	 * Called when cell is selected. Please note that cell selection can
	 * change without direct user interaction.
	 */
	open fun onCellSelected(cell: Cell?) {}

	/**
	 * Called when cell is tapped.
	 */
	open fun onCellTapped(cell: Cell) {}
	open fun onSaveState(outState: StateBundle) {}
	open fun onRestoreState(savedState: StateBundle) {}

	companion object {
		const val MODE_EDIT_VALUE = 0
		const val MODE_EDIT_CORNER_NOTE = 1
		const val MODE_EDIT_CENTER_NOTE = 2

		/**
		 * Generates a [ColorStateList] using colors from boardView suitable
		 * for use as text colors on a button.
		 *
		 *
		 * An XML color state list file can not be used because the colors may be
		 * changed at runtime instead of coming from a fixed theme.
		 *
		 * @param boardView the view to derive colors from
		 * @return suitable colors
		 * @see .makeBackgroundColorStateList
		 */
		// Note: It's tempting to make this part of NumberButton, but it's useful for other buttons
		// that are not NumberButton (e.g., the delete and mode switch buttons).
		fun makeTextColorStateList(boardView: SudokuBoardView): ColorStateList {
			val states = arrayOf(
				intArrayOf(R.attr.all_numbers_placed, android.R.attr.state_enabled),
				intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
				intArrayOf(android.R.attr.state_enabled),
				intArrayOf()
			)

			// No more of this number should be entered, use the same color as given digits
			val allNumbersPlacedColor = boardView.textColorReadOnly

			// The number being entered, or highlighted, so use the same colour as highlighted digits
			val checkedColor = boardView.textColorHighlighted
			val enabledColor = MaterialColors.getColor(boardView, com.google.android.material.R.attr.colorOnBackground)
			val disabledColor: Int = setAlpha(MaterialColors.getColor(boardView, com.google.android.material.R.attr.colorOnSurface), 0.12f)
			val colors = intArrayOf(
				allNumbersPlacedColor, checkedColor, enabledColor, disabledColor
			)
			return ColorStateList(states, colors)
		}

		/**
		 * Generates a [ColorStateList] using colors from boardView suitable
		 * for use as background colors on a button.
		 *
		 *
		 * An XML color state list file can not be used because the colors may be
		 * changed at runtime instead of coming from a fixed theme.
		 *
		 * @param boardView the view to derive colors from
		 * @return suitable colors
		 * @see .makeTextColorStateList
		 */
		fun makeBackgroundColorStateList(boardView: SudokuBoardView): ColorStateList {
			val states = arrayOf(
				intArrayOf(R.attr.all_numbers_placed, android.R.attr.state_enabled, -android.R.attr.state_checked),
				intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
				intArrayOf(android.R.attr.state_enabled),
				intArrayOf()
			)

			// No more of this number should be entered, so use the same color as given digits
			val allNumbersPlacedColor = boardView.backgroundColorReadOnly

			// The number being entered, or highlighted, so use the same colour as highlighted digits
			val checkedColor = boardView.backgroundColorHighlighted
			val enabledColor = MaterialColors.getColor(boardView, android.R.attr.colorBackground)
			val disabledColor: Int = setAlpha(MaterialColors.getColor(boardView, com.google.android.material.R.attr.colorOnSurface), 0.12f)
			val colors = intArrayOf(
				allNumbersPlacedColor, checkedColor, enabledColor, disabledColor
			)
			return ColorStateList(states, colors)
		}

		/**
		 * Adjusts the alpha channel of the given color.
		 *
		 * @param color color to adjust
		 * @param alphaPct percentage to adjust it by
		 * @return The adjusted color
		 */
		@ColorInt
		private fun setAlpha(@ColorInt color: Int, @Suppress("SameParameterValue") alphaPct: Float): Int {
			var alpha = color ushr 24
			alpha = ceil((alpha * alphaPct).toDouble()).toInt()
			return color and 0x00ffffff or (alpha shl 24)
		}
	}
}
