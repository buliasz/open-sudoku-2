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

package org.buliasz.opensudoku2.game

import java.util.StringTokenizer

/**
 * Sudoku cell. Every cell has value, some notes attached to it and some basic
 * state (whether it is editable and valid).
 */
class Cell private constructor(value: Int, cornerNote: CellNote, centerNote: CellNote, editable: Boolean, valid: Boolean) {
	private val mCellCollectionLock = Any()

	// if cell is included in collection, here are some additional information
	// about collection and cell's position in it
	private var mCellCollection: CellCollection? = null

	/**
	 * Gets cell's row index within [CellCollection].
	 *
	 * @return Cell's row index within CellCollection.
	 */
	var rowIndex = -1
		private set

	/**
	 * Gets cell's column index within [CellCollection].
	 *
	 * @return Cell's column index within CellCollection.
	 */
	var columnIndex = -1
		private set

	/**
	 * Returns sector containing this cell. Sector is 3x3 group of cells.
	 *
	 * @return Sector containing this cell.
	 */
	var sector: CellGroup? = null // sector containing this cell
		private set

	/**
	 * Returns row containing this cell.
	 *
	 * @return Row containing this cell.
	 */
	var row: CellGroup? = null // row containing this cell
		private set

	/**
	 * Returns column containing this cell.
	 *
	 * @return Column containing this cell.
	 */
	var column: CellGroup? = null // column containing this cell
		private set
	private var mValue: Int
	private var mCornerNote: CellNote
	private var mCenterNote: CellNote
	private var mEditable: Boolean
	private var mValid: Boolean

	/**
	 * Creates empty editable cell.
	 */
	constructor() : this(0, CellNote(), CellNote(), true, true)

	/**
	 * Creates empty editable cell containing given value.
	 *
	 * @param value Value of the cell.
	 */
	constructor(value: Int) : this(value, CellNote(), CellNote(), true, true)

	init {
		require(!(value < 0 || value > 9)) { "Value must be between 0-9." }
		mValue = value
		mCornerNote = cornerNote
		mCenterNote = centerNote
		mEditable = editable
		mValid = valid
	}

	/**
	 * Called when `Cell` is added to [CellCollection].
	 *
	 * @param rowIndex Cell's row index within collection.
	 * @param colIndex Cell's column index within collection.
	 * @param sector   Reference to sector group in which cell is included.
	 * @param row      Reference to row group in which cell is included.
	 * @param column   Reference to column group in which cell is included.
	 */
	fun initCollection(
		cellCollection: CellCollection?, rowIndex: Int, colIndex: Int, sector: CellGroup?, row: CellGroup?, column: CellGroup?
	) {
		synchronized(mCellCollectionLock) { mCellCollection = cellCollection }
		this.rowIndex = rowIndex
		columnIndex = colIndex
		this.sector = sector
		this.row = row
		this.column = column
		sector!!.addCell(this)
		row!!.addCell(this)
		column!!.addCell(this)
	}

	var value: Int
		/**
		 * Gets cell's value. Value can be 1-9 or 0 if cell is empty.
		 *
		 * @return Cell's value. Value can be 1-9 or 0 if cell is empty.
		 */
		get() = mValue
		/**
		 * Sets cell's value. Value can be 1-9 or 0 if cell should be empty.
		 *
		 * @param value 1-9 or 0 if cell should be empty.
		 */
		set(value) {
			require(!(value < 0 || value > 9)) { "Value must be between 0-9." }
			mValue = value
			onChange()
		}
	var cornerNote: CellNote
		/**
		 * Gets corner note attached to the cell.
		 *
		 * @return Note attached to the cell.
		 */
		get() = mCornerNote
		/**
		 * Sets corner note attached to the cell
		 *
		 * @param note Corner note attached to the cell
		 */
		set(note) {
			mCornerNote = note
			onChange()
		}
	var centerNote: CellNote
		/**
		 * Gets center note attached to the cell.
		 *
		 * @return Center note attached to the cell.
		 */
		get() = mCenterNote
		/**
		 * Sets center note attached to the cell
		 *
		 * @param note Center note attached to the cell
		 */
		set(note) {
			mCenterNote = note
			onChange()
		}
	val notedNumbers: List<Int?>
		/**
		 * @return All notes associated with the cell, irrespective of whether
		 * they are corner notes or center notes.
		 */
		get() {
			val notes = cornerNote.notedNumbers
			notes.addAll(centerNote.notedNumbers)
			return notes
		}
	var isEditable: Boolean
		/**
		 * Returns whether cell can be edited.
		 *
		 * @return True if cell can be edited.
		 */
		get() = mEditable
		/**
		 * Sets whether cell can be edited.
		 *
		 * @param editable True, if cell should allow editing.
		 */
		set(editable) {
			mEditable = editable
			onChange()
		}
	var isValid: Boolean
		/**
		 * Returns true, if cell contains valid value according to sudoku rules.
		 *
		 * @return True, if cell contains valid value according to sudoku rules.
		 */
		get() = mValid
		/**
		 * Sets whether cell contains valid value according to sudoku rules.
		 *
		 * @param valid
		 */
		set(valid) {
			mValid = valid
			onChange()
		}

	/**
	 * Appends string representation of this object to the given `StringBuilder`
	 * in a given data format version.
	 * You can later recreate object from this string by calling [.deserialize].
	 *
	 * @see CellCollection.serialize
	 * @param data A `StringBuilder` where to write data.
	 */
	fun serialize(data: StringBuilder, dataVersion: Int) {
		if (dataVersion == CellCollection.DATA_VERSION_PLAIN) {
			data.append(mValue)
		} else {
			data.append(mValue).append("|")
			if (mCornerNote.isEmpty) {
				data.append("0").append("|")
			} else {
				mCornerNote.serialize(data)
			}
			if (mCenterNote.isEmpty) {
				data.append("0").append("|")
			} else {
				mCenterNote.serialize(data)
			}
			data.append(if (mEditable) "1" else "0").append("|")
		}
	}

	/**
	 * Returns a string representation of this object in a default data format version.
	 *
	 * @see .serialize
	 * @return A string representation of this object.
	 */
	fun serialize(): String {
		val sb = StringBuilder()
		serialize(sb, CellCollection.DATA_VERSION)
		return "$sb"
	}

	/**
	 * Returns a string representation of this object in a given data format version.
	 *
	 * @see .serialize
	 * @param dataVersion A version of data format.
	 * @return A string representation of this object.
	 */
	fun serialize(dataVersion: Int): String {
		val sb = StringBuilder()
		serialize(sb, dataVersion)
		return "$sb"
	}

	/**
	 * Notify CellCollection that something has changed.
	 */
	private fun onChange() {
		synchronized(mCellCollectionLock) {
			if (mCellCollection != null) {
				mCellCollection!!.onChange()
			}
		}
	}

	companion object {
		/**
		 * Creates instance from given `StringTokenizer`.
		 */
		fun deserialize(data: StringTokenizer, version: Int): Cell {
			val cell = Cell()
			cell.value = data.nextToken().toInt()
			cell.cornerNote = CellNote.deserialize(data.nextToken(), version)
			if (version >= CellCollection.DATA_VERSION_4) {
				cell.centerNote = CellNote.deserialize(data.nextToken(), version)
			}
			cell.isEditable = data.nextToken() == "1"
			return cell
		}

		/**
		 * Creates instance from given string (string which has been
		 * created by [.serialize] or [.serialize] method).
		 * earlier.
		 */
		fun deserialize(cellData: String?): Cell {
			val data = StringTokenizer(cellData, "|")
			return deserialize(data, CellCollection.DATA_VERSION)
		}
	}
}
