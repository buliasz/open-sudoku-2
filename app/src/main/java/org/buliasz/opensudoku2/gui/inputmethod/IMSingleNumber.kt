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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.button.MaterialButton
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.CellNote
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.HintsQueue
import org.buliasz.opensudoku2.gui.NumberButton
import org.buliasz.opensudoku2.gui.SudokuBoardView
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanelStatePersister.StateBundle

/**
 * This class represents following type of number input workflow: Number buttons are displayed
 * in the sidebar, user selects one number and then fill values by tapping the cells.
 */
class IMSingleNumber(val parent: ViewGroup) : InputMethod() {
	/**
	 * If set to true, buttons for numbers, which occur in [CellCollection]
	 * more than [CellCollection.SUDOKU_SIZE]-times, will be highlighted.
	 */
	internal var highlightCompletedValues = true
	internal var showNumberTotals = false
	internal var bidirectionalSelection = true
	internal var highlightSimilar = true
	private var mSelectedNumber = 1
	private var mEditMode: Int = MODE_EDIT_VALUE
	private var mNumberButtons: MutableMap<Int, NumberButton>? = null

	// Conceptually these behave like RadioButtons. However, it's difficult to style a RadioButton
	// without re-implementing all the drawables, and they would require a custom parent layout
	// to work properly in a ConstraintLayout, so it's simpler and more consistent in the UI to
	// handle the toggle logic in the code here.
	private var mEnterNumberButton: MaterialButton? = null
	private var mCornerNoteButton: MaterialButton? = null
	private var mCenterNoteButton: MaterialButton? = null
	private lateinit var mSwitchModeButton: Button
	internal var onSelectedNumberChangedListener: ((Int) -> Unit)? = null
	private val mNumberButtonTouched = OnTouchListener { view: View, _: MotionEvent? ->
		view.performClick()
		mSelectedNumber = view.tag as Int
		onSelectedNumberChanged()
		update()
		true
	}

	private val mNumberButtonClicked = View.OnClickListener { v: View ->
		mSelectedNumber = v.tag as Int
		onSelectedNumberChanged()
		update()
	}

	private val mOnCellsChangeListener = {
		if (mActive) {
			update()
		}
	}

	private val mModeButtonClicked = View.OnClickListener { v: View ->
		mEditMode = v.tag as Int
		update()
	}

	override fun initialize(
		context: Context?, controlPanel: IMControlPanel?, game: SudokuGame, board: SudokuBoardView, hintsQueue: HintsQueue?
	) {
		super.initialize(context, controlPanel, game, board, hintsQueue)
		game.cells.ensureOnChangeListener(mOnCellsChangeListener)
	}

	override val nameResID: Int
		get() = R.string.single_number
	override val helpResID: Int
		get() = R.string.im_single_number_hint
	override val abbrName: String
		get() = mContext!!.getString(R.string.single_number_abbr)

	override val switchModeButton: Button
		get() = mSwitchModeButton

	override fun createControlPanelView(abbrName: String): View {
		val inflater = mContext!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val controlPanel = inflater.inflate(R.layout.im_single_number, null)
		val numberButtons = HashMap<Int, NumberButton>()
		numberButtons[1] = controlPanel.findViewById(R.id.button_1)
		numberButtons[2] = controlPanel.findViewById(R.id.button_2)
		numberButtons[3] = controlPanel.findViewById(R.id.button_3)
		numberButtons[4] = controlPanel.findViewById(R.id.button_4)
		numberButtons[5] = controlPanel.findViewById(R.id.button_5)
		numberButtons[6] = controlPanel.findViewById(R.id.button_6)
		numberButtons[7] = controlPanel.findViewById(R.id.button_7)
		numberButtons[8] = controlPanel.findViewById(R.id.button_8)
		numberButtons[9] = controlPanel.findViewById(R.id.button_9)
		val textColor: ColorStateList = makeTextColorStateList(mBoard)
		val backgroundColor: ColorStateList = makeBackgroundColorStateList(mBoard)
		for ((key, b) in numberButtons) {
			with(b) {
				tag = key
				setOnClickListener(mNumberButtonClicked)
				setOnTouchListener(mNumberButtonTouched)
				showNumbersPlaced = showNumberTotals
				enableAllNumbersPlaced = highlightCompletedValues
				backgroundTintList = backgroundColor
				setTextColor(textColor)
			}
		}
		mNumberButtons = numberButtons

		with(controlPanel.findViewById<MaterialButton>(R.id.button_clear)) {
			tag = 0
			setOnClickListener(mNumberButtonClicked)
			setOnTouchListener(mNumberButtonTouched)
			backgroundTintList = backgroundColor
			iconTint = textColor
		}

		with(controlPanel.findViewById<MaterialButton>(R.id.enter_number)) {
			tag = MODE_EDIT_VALUE
			setOnClickListener(mModeButtonClicked)
			backgroundTintList = backgroundColor
			iconTint = textColor
			mEnterNumberButton = this
		}

		with(controlPanel.findViewById<MaterialButton>(R.id.corner_note)) {
			tag = MODE_EDIT_CORNER_NOTE
			setOnClickListener(mModeButtonClicked)
			backgroundTintList = backgroundColor
			iconTint = textColor
			mCornerNoteButton = this
		}

		with(controlPanel.findViewById<MaterialButton>(R.id.center_note)) {
			tag = MODE_EDIT_CENTER_NOTE
			setOnClickListener(mModeButtonClicked)
			backgroundTintList = backgroundColor
			iconTint = textColor
			mCenterNoteButton = this
		}

		mSwitchModeButton = controlPanel.findViewById(R.id.single_number_switch_input_mode)
		mSwitchModeButton.text = abbrName

		return controlPanel
	}

	private fun update() {
		when (mEditMode) {
			MODE_EDIT_VALUE -> {
				mEnterNumberButton!!.isChecked = true
				mCornerNoteButton!!.isChecked = false
				mCenterNoteButton!!.isChecked = false
			}

			MODE_EDIT_CORNER_NOTE -> {
				mEnterNumberButton!!.isChecked = false
				mCornerNoteButton!!.isChecked = true
				mCenterNoteButton!!.isChecked = false
			}

			MODE_EDIT_CENTER_NOTE -> {
				mEnterNumberButton!!.isChecked = false
				mCornerNoteButton!!.isChecked = false
				mCenterNoteButton!!.isChecked = true
			}
		}
		val valuesUseCount = mGame!!.cells.valuesUseCount
		for (button in mNumberButtons!!.values) {
			val tag = button.tag as Int
			button.mode = mEditMode
			if (mSelectedNumber == tag) {
				button.isChecked = true
				button.requestFocus()
			} else {
				button.isChecked = false
			}

			// Update the count of numbers placed
			button.setNumbersPlaced(valuesUseCount[tag] ?: 0)
		}
		mBoard.highlightedValue = if (mBoard.isReadOnly) 0 else mSelectedNumber
	}

	override fun onActivated() {
		update()
	}

	override fun onCellSelected(cell: Cell?) {
		super.onCellSelected(cell)
		if (bidirectionalSelection && cell != null) {
			val v = cell.value
			if (v != 0 && v != mSelectedNumber) {
				mSelectedNumber = cell.value
				update()
			}
		}
		mBoard.highlightedValue = mSelectedNumber
	}

	private fun onSelectedNumberChanged() {
		if (bidirectionalSelection && highlightSimilar && onSelectedNumberChangedListener != null && !mBoard.isReadOnly) {
			onSelectedNumberChangedListener!!(mSelectedNumber)
			mBoard.highlightedValue = mSelectedNumber
		}
	}

	override fun onCellTapped(cell: Cell) {
		var selNumber = mSelectedNumber
		when (mEditMode) {
			MODE_EDIT_CORNER_NOTE -> if (selNumber == 0) {
				mGame!!.setCellCornerNote(cell, CellNote.EMPTY)
			} else if (selNumber in 1..9) {
				val newNote = cell.cornerNote.toggleNumber(selNumber)
				mGame!!.setCellCornerNote(cell, newNote)
				// if we toggled the note off we want to de-select the cell
				if (!newNote.hasNumber(selNumber)) {
					mBoard.clearCellSelection()
				}
			}

			MODE_EDIT_CENTER_NOTE -> if (selNumber == 0) {
				mGame!!.setCellCenterNote(cell, CellNote.EMPTY)
			} else if (selNumber in 1..9) {
				val newNote = cell.centerNote.toggleNumber(selNumber)
				mGame!!.setCellCenterNote(cell, newNote)
				if (!newNote.hasNumber(selNumber)) {
					mBoard.clearCellSelection()
				}
			}

			MODE_EDIT_VALUE -> {
				// Normal flow, just set the value (or clear it if it is repeated touch)
				if (selNumber == cell.value) {
					selNumber = 0
					mBoard.clearCellSelection()
				}
				mGame!!.setCellValue(cell, selNumber)
			}
		}
	}

	override fun onSaveState(outState: StateBundle) {
		outState.putInt("selectedNumber", mSelectedNumber)
		outState.putInt("editMode", mEditMode)
	}

	override fun onRestoreState(savedState: StateBundle) {
		mSelectedNumber = savedState.getInt("selectedNumber", 1)
		mEditMode = savedState.getInt("editMode", MODE_EDIT_VALUE)
		if (isInputMethodViewCreated) {
			update()
		}
	}
}
