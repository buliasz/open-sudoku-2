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
import org.buliasz.opensudoku2.gui.inputmethod.IMPopupDialog.OnNoteEditListener
import org.buliasz.opensudoku2.gui.inputmethod.IMPopupDialog.OnNumberEditListener

class IMPopup(val parent: ViewGroup) : InputMethod() {
	private var mHighlightCompletedValues = true
	private var mShowNumberTotals = false
	private var mEditCellDialog: IMPopupDialog? = null
	private var mSelectedCell: Cell? = null
	private lateinit var mSwitchModeButton: Button

	/**
	 * Occurs when user selects number in EditCellDialog.
	 */
	private val mOnNumberEditListener = OnNumberEditListener { number ->
		if (number != -1 && mSelectedCell != null) {
			mGame!!.setCellValue(mSelectedCell!!, number)
			mBoard!!.setHighlightedValue(number)
		}
		true
	}

	/**
	 * Occurs when user edits note in EditCellDialog
	 */
	private val mOnNoteEditListener: OnNoteEditListener = object : OnNoteEditListener {
		override fun onCornerNoteEdit(numbers: Array<Int>): Boolean {
			if (mSelectedCell != null) {
				mGame!!.setCellCornerNote(mSelectedCell!!, CellNote.fromIntArray(numbers))
			}
			return true
		}

		override fun onCenterNoteEdit(numbers: Array<Int>): Boolean {
			if (mSelectedCell != null) {
				mGame!!.setCellCenterNote(mSelectedCell!!, CellNote.fromIntArray(numbers))
			}
			return true
		}
	}

	/**
	 * Occurs when popup dialog is closed.
	 */
	private val mOnPopupDismissedListener = DialogInterface.OnDismissListener { _: DialogInterface? -> mBoard!!.hideTouchedCellHint() }

	/**
	 * If set to true, buttons for numbers, which occur in [CellCollection]
	 * more than [CellCollection.SUDOKU_SIZE]-times, will be highlighted.
	 */
	fun setHighlightCompletedValues(highlightCompletedValues: Boolean) {
		mHighlightCompletedValues = highlightCompletedValues
	}

	fun setShowNumberTotals(showNumberTotals: Boolean) {
		mShowNumberTotals = showNumberTotals
	}

	private fun ensureEditCellDialog() {
		if (mEditCellDialog == null) {
			with(IMPopupDialog(parent, mContext!!, mBoard!!)) {
				setOnNumberEditListener(mOnNumberEditListener)
				setOnNoteEditListener(mOnNoteEditListener)
				setOnDismissListener(mOnPopupDismissedListener)
				setShowNumberTotals(mShowNumberTotals)
				setHighlightCompletedValues(mHighlightCompletedValues)
				mEditCellDialog = this
			}
		}
	}

	override fun onActivated() {
		mBoard!!.setAutoHideTouchedCellHint(false)
	}

	override fun onDeactivated() {
		mBoard!!.setAutoHideTouchedCellHint(true)
	}

	override fun onCellTapped(cell: Cell) {
		mSelectedCell = cell
		if (cell.isEditable) {
			ensureEditCellDialog()
			mEditCellDialog!!.resetState()
			mEditCellDialog!!.setNumber(cell.value)
			mEditCellDialog!!.setCornerNotes(cell.cornerNote.notedNumbers)
			mEditCellDialog!!.setCenterNotes(cell.centerNote.notedNumbers)
			val valuesUseCount = mGame!!.cells.valuesUseCount
			mEditCellDialog!!.setValueCount(valuesUseCount)
			mEditCellDialog!!.show()
		} else {
			mBoard!!.hideTouchedCellHint()
		}
	}

	override fun onCellSelected(cell: Cell?) {
		super.onCellSelected(cell)
		mBoard!!.setHighlightedValue(cell?.value ?: 0)
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
		get() = mContext!!.getString(R.string.popup_abbr)
	override val switchModeButton: Button
		get() = mSwitchModeButton

	override fun createControlPanelView(abbrName: String): View {
		val inflater = mContext!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val controlPanel = inflater.inflate(R.layout.im_popup, null)

		mSwitchModeButton = controlPanel.findViewById(R.id.popup_switch_input_mode)
		mSwitchModeButton.text = abbrName

		return controlPanel
	}
}
