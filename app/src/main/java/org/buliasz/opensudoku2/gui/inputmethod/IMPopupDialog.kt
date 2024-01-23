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

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.gui.NumberButton
import org.buliasz.opensudoku2.gui.SudokuBoardView

/**
 * Dialog for selecting and entering numbers and notes.
 *
 * When entering a number, the dialog automatically closes.
 *
 * When entering a note the dialog remains open, to allow multiple notes to be entered at once.
 */
class IMPopupDialog(val parent: ViewGroup, mContext: Context, mBoard: SudokuBoardView) : Dialog(mContext) {
	private val mInflater: LayoutInflater
	private var mEditMode = InputMethod.MODE_EDIT_VALUE
	val mNumberButtons: MutableMap<Int, NumberButton> = HashMap()

	// selected number on "Select number" tab (0 if nothing is selected).
	private var mSelectedNumber = 0
	private val mCornerNoteSelectedNumbers: MutableSet<Int> = HashSet()
	private val mCenterNoteSelectedNumbers: MutableSet<Int> = HashSet()
	private var mShowNumberTotals = false

	/** True if buttons with completed values should be highlighted  */
	private var mHighlightCompletedValues = false
	private val mEnterNumberButton: MaterialButton
	private val mCornerNoteButton: MaterialButton
	private val mCenterNoteButton: MaterialButton

	/**
	 * Registers a callback to be invoked when number is selected.
	 */
	internal var onNumberEditListener: OnNumberEditListener? = null

	/**
	 * Register a callback to be invoked when note is edited.
	 */
	internal var onNoteEditListener: OnNoteEditListener? = null
	private val mValueCount: MutableMap<Int, Int> = HashMap()

	private val mNumberButtonClicked = View.OnClickListener { v: View ->
		val number = v.tag as Int
		when (mEditMode) {
			InputMethod.MODE_EDIT_VALUE -> {
				mSelectedNumber = number
				syncAndDismiss()
			}

			InputMethod.MODE_EDIT_CORNER_NOTE -> if ((v as MaterialButton).isChecked) {
				mCornerNoteSelectedNumbers.add(number)
			} else {
				mCornerNoteSelectedNumbers.remove(number)
			}

			InputMethod.MODE_EDIT_CENTER_NOTE -> if ((v as MaterialButton).isChecked) {
				mCenterNoteSelectedNumbers.add(number)
			} else {
				mCenterNoteSelectedNumbers.remove(number)
			}
		}
	}

	/**
	 * Occurs when user presses "Clear" button.
	 */
	private val clearButtonListener = View.OnClickListener { v ->
		(v as MaterialButton).isChecked = false
		when (mEditMode) {
			InputMethod.MODE_EDIT_VALUE -> {
				mSelectedNumber = 0
				syncAndDismiss()
			}

			InputMethod.MODE_EDIT_CORNER_NOTE ->                     // Clear the corner notes. Dialog should stay visible
				setCornerNotes(emptyList())

			InputMethod.MODE_EDIT_CENTER_NOTE ->                     // Clear the center notes. Dialog should stay visible
				setCenterNotes(emptyList())
		}
		update()
	}

	/**
	 * Occurs when user presses "Close" button.
	 */
	private val closeButtonListener = View.OnClickListener { _: View? -> syncAndDismiss() }

	/** Synchronises state with the hosting activity and dismisses the dialog  */
	private fun syncAndDismiss() {
		if (onNumberEditListener != null) {
			onNumberEditListener!!.onNumberEdit(mSelectedNumber)
		}
		if (onNoteEditListener != null) {
			onNoteEditListener!!.onCornerNoteEdit(mCornerNoteSelectedNumbers.toTypedArray())
			onNoteEditListener!!.onCenterNoteEdit(mCenterNoteSelectedNumbers.toTypedArray())
		}
		dismiss()
	}

	init {
		mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val keypad = mInflater.inflate(R.layout.im_popup_edit_value, null)

		mNumberButtons[1] = keypad.findViewById(R.id.button_1)
		mNumberButtons[2] = keypad.findViewById(R.id.button_2)
		mNumberButtons[3] = keypad.findViewById(R.id.button_3)
		mNumberButtons[4] = keypad.findViewById(R.id.button_4)
		mNumberButtons[5] = keypad.findViewById(R.id.button_5)
		mNumberButtons[6] = keypad.findViewById(R.id.button_6)
		mNumberButtons[7] = keypad.findViewById(R.id.button_7)
		mNumberButtons[8] = keypad.findViewById(R.id.button_8)
		mNumberButtons[9] = keypad.findViewById(R.id.button_9)

		val textColor: ColorStateList = InputMethod.makeTextColorStateList(mBoard)
		val backgroundColor: ColorStateList = InputMethod.makeBackgroundColorStateList(mBoard)

		for ((key, b) in mNumberButtons) {
			b.tag = key
			b.setOnClickListener(mNumberButtonClicked)
			b.enableAllNumbersPlaced = mHighlightCompletedValues
			b.backgroundTintList = backgroundColor
			b.setTextColor(textColor)
		}

		val clearButton = keypad.findViewById<MaterialButton>(R.id.button_clear)
		clearButton.tag = 0
		clearButton.setOnClickListener(clearButtonListener)
		clearButton.backgroundTintList = backgroundColor
		clearButton.iconTint = textColor

		/* Switch mode, and update the UI */
		val modeButtonClicked = View.OnClickListener { v: View ->
			mEditMode = v.tag as Int
			update()
		}

		mEnterNumberButton = keypad.findViewById(R.id.enter_number)
		mEnterNumberButton.tag = InputMethod.MODE_EDIT_VALUE
		mEnterNumberButton.setOnClickListener(modeButtonClicked)
		mEnterNumberButton.backgroundTintList = backgroundColor
		mEnterNumberButton.iconTint = textColor

		mCornerNoteButton = keypad.findViewById(R.id.corner_note)
		mCornerNoteButton.tag = InputMethod.MODE_EDIT_CORNER_NOTE
		mCornerNoteButton.setOnClickListener(modeButtonClicked)
		mCornerNoteButton.backgroundTintList = backgroundColor
		mCornerNoteButton.iconTint = textColor

		mCenterNoteButton = keypad.findViewById(R.id.center_note)
		mCenterNoteButton.tag = InputMethod.MODE_EDIT_CENTER_NOTE
		mCenterNoteButton.setOnClickListener(modeButtonClicked)
		mCenterNoteButton.backgroundTintList = backgroundColor
		mCenterNoteButton.iconTint = textColor

		val closeButton = keypad.findViewById<View>(R.id.button_close)
		closeButton.setOnClickListener(closeButtonListener)
		setContentView(keypad)
	}

	private fun update() {
		// Determine which buttons to check, based on the value / notes in the selected cell
		val buttonsToCheck: List<Int>
		when (mEditMode) {
			InputMethod.MODE_EDIT_VALUE -> {
				mEnterNumberButton.isChecked = true
				mCornerNoteButton.isChecked = false
				mCenterNoteButton.isChecked = false
				buttonsToCheck = listOf(mSelectedNumber)
			}

			InputMethod.MODE_EDIT_CORNER_NOTE -> {
				mEnterNumberButton.isChecked = false
				mCornerNoteButton.isChecked = true
				mCenterNoteButton.isChecked = false
				buttonsToCheck = ArrayList(mCornerNoteSelectedNumbers)
			}

			InputMethod.MODE_EDIT_CENTER_NOTE -> {
				mEnterNumberButton.isChecked = false
				mCornerNoteButton.isChecked = false
				mCenterNoteButton.isChecked = true
				buttonsToCheck = ArrayList(mCenterNoteSelectedNumbers)
			}

			else ->                 // Can't happen
				buttonsToCheck = ArrayList()
		}
		for (button in mNumberButtons.values) {
			val tag = button.tag as Int
			button.mode = mEditMode

			// Check the button if necessary
			button.isChecked = buttonsToCheck.contains(tag)

			// Update the count of numbers placed
			if (mValueCount.isNotEmpty()) {
				button.setNumbersPlaced(mValueCount[tag]!!)
			}
		}
	}

	fun setShowNumberTotals(showNumberTotals: Boolean) {
		if (mShowNumberTotals == showNumberTotals) {
			return
		}
		mShowNumberTotals = showNumberTotals
		for (button in mNumberButtons.values) {
			button.showNumbersPlaced = mShowNumberTotals
		}
	}

	fun setHighlightCompletedValues(highlightCompletedValues: Boolean) {
		if (mHighlightCompletedValues == highlightCompletedValues) {
			return
		}
		mHighlightCompletedValues = highlightCompletedValues
		for (b in mNumberButtons.values) {
			b.enableAllNumbersPlaced = mHighlightCompletedValues
		}
	}

	/**
	 * Reset most of the state of the dialog (selected values, notes, etc).
	 *
	 * DO NOT reset the edit mode, for compatibility with the previous code that used a tab.
	 * The selected tab (which was the edit mode) was retained if the dialog was dismissed on
	 * one cell and opened on another.
	 */
	fun resetState() {
		mSelectedNumber = 0
		mCornerNoteSelectedNumbers.clear()
		mCenterNoteSelectedNumbers.clear()
		mValueCount.clear()
		update()
	}

	fun setNumber(number: Int) {
		mSelectedNumber = number
		update()
	}

	fun setCornerNotes(numbers: List<Int>?) {
		mCornerNoteSelectedNumbers.clear()
		mCornerNoteSelectedNumbers.addAll(numbers!!)
		update()
	}

	fun setCenterNotes(numbers: List<Int>?) {
		mCenterNoteSelectedNumbers.clear()
		mCenterNoteSelectedNumbers.addAll(numbers!!)
		update()
	}

	fun setValueCount(count: Map<Int, Int>?) {
		mValueCount.clear()
		mValueCount.putAll(count!!)
		update()
	}

	/**
	 * Interface definition for a callback to be invoked, when user selects a number, which
	 * should be entered in the Sudoku cell.
	 */
	fun interface OnNumberEditListener {
		fun onNumberEdit(number: Int): Boolean
	}

	/**
	 * Interface definition for a callback to be invoked, when user selects new note
	 * content.
	 */
	interface OnNoteEditListener {
		fun onCornerNoteEdit(numbers: Array<Int>): Boolean
		fun onCenterNoteEdit(numbers: Array<Int>): Boolean
	}
}
