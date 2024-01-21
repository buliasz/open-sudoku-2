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
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.button.MaterialButton
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.CellNote
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.HintsQueue
import org.buliasz.opensudoku2.gui.IconButton
import org.buliasz.opensudoku2.gui.NumberButton
import org.buliasz.opensudoku2.gui.SudokuBoardView
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanelStatePersister.StateBundle

class IMSelectOnTap(val parent: ViewGroup) : InputMethod() {
	var isMoveCellSelectionOnPress = true

	/**
	 * If set to true, buttons for numbers, which occur in [CellCollection]
	 * more than [CellCollection.SUDOKU_SIZE]-times, will be highlighted.
	 */
	internal var highlightCompletedValues = true
	internal var showNumberTotals = false
	private var mSelectedCell: Cell? = null
	private var mClearButton: IconButton? = null
	private var mEditMode = MODE_EDIT_VALUE
	private var mNumberButtons: MutableMap<Int, NumberButton>? = null

	// Conceptually these behave like RadioButtons. However, it's difficult to style a RadioButton
	// without re-implementing all the drawables, and they would require a custom parent layout
	// to work properly in a ConstraintLayout, so it's simpler and more consistent in the UI to
	// handle the toggle logic in the code here.
	private var mEnterNumberButton: MaterialButton? = null
	private var mCornerNoteButton: MaterialButton? = null
	private var mCenterNoteButton: MaterialButton? = null
	private lateinit var mSwitchModeButton: Button

	private val mNumberButtonClicked = View.OnClickListener { v: View ->
		val selNumber = v.tag as Int
		val selCell = mSelectedCell
		if (selCell != null) {
			when (mEditMode) {
				MODE_EDIT_VALUE -> if (selNumber in 0..9) {
					mGame.setCellValue(selCell, selNumber)
					mBoard.highlightedValue = selNumber
					if (isMoveCellSelectionOnPress) {
						mBoard.moveCellSelectionRight()
					}
				}

				MODE_EDIT_CORNER_NOTE -> if (selNumber == 0) {
					mGame.setCellCornerNote(selCell, CellNote.EMPTY)
				} else if (selNumber in 1..9) {
					mGame.setCellCornerNote(selCell, selCell.cornerNote.toggleNumber(selNumber))
				}

				MODE_EDIT_CENTER_NOTE -> if (selNumber == 0) {
					mGame.setCellCenterNote(selCell, CellNote.EMPTY)
				} else if (selNumber in 1..9) {
					mGame.setCellCenterNote(selCell, selCell.centerNote.toggleNumber(selNumber))
				}
			}
		}
	}
	private val mModeButtonClicked = View.OnClickListener { v: View ->
		mEditMode = v.tag as Int
		update()
	}
	private val mOnCellsChangeListener = { if (mActive) update() }

	override fun initialize(context: Context, controlPanel: IMControlPanel, game: SudokuGame, board: SudokuBoardView, hintsQueue: HintsQueue?) {
		super.initialize(context, controlPanel, game, board, hintsQueue)
		game.cells.ensureOnChangeListener(mOnCellsChangeListener)
	}

	override fun createControlPanelView(abbrName: String): View {
		val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val controlPanel = inflater.inflate(R.layout.im_select_on_tap, null)

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
		for (num in numberButtons.keys) {
			val button = numberButtons[num]
			button!!.tag = num
			button.setOnClickListener(mNumberButtonClicked)
			button.showNumbersPlaced = showNumberTotals
			button.enableAllNumbersPlaced = highlightCompletedValues
			button.backgroundTintList = backgroundColor
			button.setTextColor(textColor)
		}
		mNumberButtons = numberButtons

		val clearButton = controlPanel.findViewById<IconButton>(R.id.button_clear)
		clearButton.tag = 0
		clearButton.setOnClickListener(mNumberButtonClicked)
		clearButton.backgroundTintList = backgroundColor
		clearButton.iconTint = textColor
		mClearButton = clearButton

		val enterNumberButton = controlPanel.findViewById<MaterialButton>(R.id.enter_number)
		enterNumberButton.tag = MODE_EDIT_VALUE
		enterNumberButton.setOnClickListener(mModeButtonClicked)
		enterNumberButton.backgroundTintList = backgroundColor
		enterNumberButton.iconTint = textColor
		mEnterNumberButton = enterNumberButton

		val cornerNoteButton = controlPanel.findViewById<MaterialButton>(R.id.corner_note)
		cornerNoteButton.tag = MODE_EDIT_CORNER_NOTE
		cornerNoteButton.setOnClickListener(mModeButtonClicked)
		cornerNoteButton.backgroundTintList = backgroundColor
		cornerNoteButton.iconTint = textColor
		mCornerNoteButton = cornerNoteButton

		val centerNoteButton = controlPanel.findViewById<MaterialButton>(R.id.center_note)
		centerNoteButton.tag = MODE_EDIT_CENTER_NOTE
		centerNoteButton.setOnClickListener(mModeButtonClicked)
		centerNoteButton.backgroundTintList = backgroundColor
		centerNoteButton.iconTint = textColor
		mCenterNoteButton = centerNoteButton

		mSwitchModeButton = controlPanel.findViewById(R.id.sot_switch_input_mode)
		mSwitchModeButton.text = abbrName

		return controlPanel
	}

	override val nameResID: Int
		get() = R.string.select_on_tap
	override val helpResID: Int
		get() = R.string.im_select_on_tap_hint
	override val abbrName: String
		get() = mContext.getString(R.string.select_on_tap_abbr)
	override val switchModeButton: Button
		get() = mSwitchModeButton

	override fun onActivated() {
		onCellSelected(if (mBoard.isReadOnly) null else mBoard.selectedCell)
	}

	override fun onCellSelected(cell: Cell?) {
		mBoard.highlightedValue = cell?.value ?: 0
		mSelectedCell = cell
		update()
	}

	private fun update() {
		val editable = mSelectedCell != null && mSelectedCell!!.isEditable
		mClearButton!!.isEnabled = editable

		// Determine which buttons to check, based on the value / notes in the selected cell
		var buttonsToCheck: MutableList<Int> = ArrayList()
		when (mEditMode) {
			MODE_EDIT_VALUE -> {
				mEnterNumberButton!!.isChecked = true
				mCornerNoteButton!!.isChecked = false
				mCenterNoteButton!!.isChecked = false
				if (mSelectedCell != null) {
					buttonsToCheck.add(mSelectedCell!!.value)
				}
			}

			MODE_EDIT_CORNER_NOTE -> {
				mEnterNumberButton!!.isChecked = false
				mCornerNoteButton!!.isChecked = true
				mCenterNoteButton!!.isChecked = false
				if (mSelectedCell != null) {
					buttonsToCheck = mSelectedCell!!.cornerNote.notedNumbers
				}
			}

			MODE_EDIT_CENTER_NOTE -> {
				mEnterNumberButton!!.isChecked = false
				mCornerNoteButton!!.isChecked = false
				mCenterNoteButton!!.isChecked = true
				if (mSelectedCell != null) {
					buttonsToCheck = mSelectedCell!!.centerNote.notedNumbers
				}
			}
		}
		val valuesUseCount = mGame.cells.valuesUseCount
		for (button in mNumberButtons!!.values) {
			val tag = button.tag as Int

			// Enable / disable the button, depending on the editable state of the selected cell
			// This has to come first, calling setChecked() later doesn't work if the button is
			// not editable when setChecked() is called.
			button.isEnabled = editable
			button.mode = mEditMode

			// Check the button if necessary
			button.isChecked = buttonsToCheck.contains(tag)

			// Update the count of numbers placed
			button.setNumbersPlaced(valuesUseCount[tag] ?: 0)
		}
	}

	override fun onSaveState(outState: StateBundle) {
		outState.putInt("editMode", mEditMode)
	}

	override fun onRestoreState(savedState: StateBundle) {
		mEditMode = savedState.getInt("editMode", MODE_EDIT_VALUE)
		if (isInputMethodViewCreated) {
			update()
		}
	}
}
