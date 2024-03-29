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
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.CellNote

class IMPopup(val parent: ViewGroup) : InputMethod() {

	/**
	 * If set to true, buttons for numbers, which occur in [CellCollection]
	 * more than [CellCollection.SUDOKU_SIZE]-times, will be highlighted.
	 */
	internal var highlightCompletedValues = true
	private var mEditCellDialog: IMPopupDialog? = null
	private lateinit var mSelectedCell: Cell
	private lateinit var mSwitchModeButton: Button

	/**
	 * Occurs when user selects number in EditCellDialog.
	 * Occurs when user edits note in EditCellDialog
	 */
	private fun mOnCellUpdate(value: Int, cornerNotes: Array<Int>, centerNotes: Array<Int>) {
		var manualRecorded = mGame.setCellCornerNote(mSelectedCell, CellNote.fromIntArray(cornerNotes), true)
		manualRecorded = mGame.setCellCenterNote(mSelectedCell, CellNote.fromIntArray(centerNotes), !manualRecorded) || manualRecorded
		if (value != -1) {
			mGame.setCellValue(mSelectedCell, value, !manualRecorded)
			mBoard.highlightedValue = value
		}
	}

	/**
	 * Occurs when popup dialog is closed.
	 */
	private val mOnPopupDismissedListener = DialogInterface.OnDismissListener { _: DialogInterface? -> mBoard.hideTouchedCellHint() }

	private fun ensureEditCellDialog() {
		if (mEditCellDialog == null) {
			mEditCellDialog = with(IMPopupDialog(parent, mContext, mBoard)) {
				cellUpdateCallback = ::mOnCellUpdate
				setOnDismissListener(mOnPopupDismissedListener)
				setShowNumberTotals(showDigitCount)
				setHighlightCompletedValues(highlightCompletedValues)
				mDigitButtons = mNumberButtons
				this
			}
		}
	}

	override fun onActivated() {
		mBoard.autoHideTouchedCellHint = false
	}

	override fun onDeactivated() {
		mBoard.autoHideTouchedCellHint = true
	}

	override fun onCellTapped(cell: Cell) {
		mSelectedCell = cell
		if (cell.isEditable) {
			ensureEditCellDialog()
			mEditCellDialog!!.resetState()
			mEditCellDialog!!.setNumber(cell.value)
			mEditCellDialog!!.setCornerNotes(cell.cornerNote.notedNumbers)
			mEditCellDialog!!.setCenterNotes(cell.centerNote.notedNumbers)
			val valuesUseCount = mGame.cells.valuesUseCount
			mEditCellDialog!!.setValueCount(valuesUseCount)
			mEditCellDialog!!.show()
		} else {
			mBoard.hideTouchedCellHint()
		}
	}

	override fun onCellSelected(cell: Cell?) {
		super.onCellSelected(cell)
		mBoard.highlightedValue = cell?.value ?: 0
	}

	override fun onPause() {
		// release dialog resource (otherwise WindowLeaked exception is logged)
		if (mEditCellDialog != null) {
			mEditCellDialog!!.cancel()
		}
	}

	override val nameResID: Int
		get() = R.string.popup
	override val helpResID: Int
		get() = R.string.im_popup_hint
	override val abbrName: String
		get() = mContext.getString(R.string.popup_abbr)
	override val switchModeButton: Button
		get() = mSwitchModeButton

	override fun createControlPanelView(abbrName: String): View {
		val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val controlPanel = inflater.inflate(R.layout.im_popup, parent, false)

		mSwitchModeButton = controlPanel.findViewById(R.id.popup_switch_input_mode)
		mSwitchModeButton.text = abbrName

		return controlPanel
	}
}
