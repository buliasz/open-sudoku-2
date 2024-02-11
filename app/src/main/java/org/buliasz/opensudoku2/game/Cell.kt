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
 * Sudoku cell. Every cell has value, some notes attached to it and some basic state (whether it is editable and valid).
 */
class Cell private constructor(value: Int, cornerNote: CellNote, centerNote: CellNote, editable: Boolean, valid: Boolean) {
	// final solution of this cell
	var solution: Int = 0

	// true if current value matches final solution (if exists)
	val matchesSolution: Boolean
		get() = solution == 0 || value == solution

	private val mCellCollectionLock = Any()

	// if cell is included in collection, here are some additional information
	// about collection and cell's position in it
	private var mCellCollection: CellCollection? = null

	/**
	 * Cell's row index within [CellCollection].
	 */
	var rowIndex = -1
		private set

	/**
	 * Cell's column index within [CellCollection].
	 */
	var columnIndex = -1
		private set

	/**
	 * Sector containing this cell. Sector is 3x3 group of cells.
	 */
	var sector: CellGroup? = null // sector containing this cell
		private set

	/**
	 * Returns row containing this cell.
	 */
	var row: CellGroup? = null // row containing this cell
		private set

	/**
	 * Returns column containing this cell.
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
	fun initCollection(cellCollection: CellCollection, rowIndex: Int, colIndex: Int, sector: CellGroup, row: CellGroup, column: CellGroup) {
		synchronized(mCellCollectionLock) { mCellCollection = cellCollection }
		this.rowIndex = rowIndex
		columnIndex = colIndex
		this.sector = sector
		this.row = row
		this.column = column
		sector.addCell(this)
		row.addCell(this)
		column.addCell(this)
	}

	/**
	 * Cell's value. Value can be 1-9 or 0 if cell is empty.
	 */
	var value: Int
		get() = mValue
		set(value) {
			require(!(value < 0 || value > 9)) { "Value must be between 0-9." }
			mValue = value
			onChange()
		}

	/**
	 * Corner note attached to the cell
	 */
	var cornerNote: CellNote
		get() = mCornerNote
		set(note) {
			mCornerNote = note
			onChange()
		}

	/**
	 * Center note attached to the cell.
	 */
	var centerNote: CellNote
		get() = mCenterNote
		set(note) {
			mCenterNote = note
			onChange()
		}

	/**
	 * True if cell can be edited.
	 */
	var isEditable: Boolean
		get() = mEditable || mValue == 0 // buggy imported puzzle from OS1 may have 0 value with editable disabled
		set(editable) {
			mEditable = editable
			onChange()
		}

	/**
	 * Returns true, if cell contains valid value according to Sudoku rules.
	 */
	var isValid: Boolean
		get() = mValid
		set(valid) {
			mValid = valid
			onChange()
		}

	/**
	 * Appends string representation of this object to the given `StringBuilder`
	 * in a given data format version.
	 * You can later recreate object from this string by calling [.deserialize].
	 *
	 * @param data A `StringBuilder` where to write data.
	 */
	fun serialize(data: StringBuilder, dataVersion: Int) {
		when (dataVersion) {
			CellCollection.DATA_VERSION_ORIGINAL -> {
				data.append(if (mEditable) "0" else mValue)
			}

			CellCollection.DATA_VERSION_PLAIN -> {
				data.append(mValue)
			}

			else -> {
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
	}

	/**
	 * Returns a string representation of this object in a default data format version.
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
		 * Creates instance from given string (string which has been created by [serialize] method).
		 */
		fun deserialize(cellData: String?): Cell {
			val data = StringTokenizer(cellData, "|")
			return deserialize(data, CellCollection.DATA_VERSION)
		}
	}
}
