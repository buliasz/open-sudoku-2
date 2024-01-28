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
import java.util.regex.Pattern

/**
 * Collection of Sudoku cells. This class in fact represents one Sudoku board (9x9).
 */
class CellCollection private constructor(val cells: Array<Array<Cell>>) {
	private var mOriginalValues: String? = null
	val originalValues: String
		get() {
			if (mOriginalValues == null) {
				mOriginalValues = serialize(DATA_VERSION_ORIGINAL)
			}
			return mOriginalValues!!
		}
	private val mChangeListeners: MutableList<() -> Unit> = ArrayList()

	// Helper arrays, contains references to the groups of cells, which should contain unique
	// numbers.
	private lateinit var mSectors: Array<CellGroup>
	private lateinit var mRows: Array<CellGroup>
	private lateinit var mColumns: Array<CellGroup>
	private var mOnChangeEnabled = true

	var solutionCount: Int = -1
		get() {
			if (field == -1) {
				with(SudokuSolver()) {
					setPuzzle(this@CellCollection)
					field = solve()
					if (field == 1) {
						setSolution(validSolution)
					}
				}
			}
			return field
		}
		private set

	private fun regenerateOriginalValues() {
		mOriginalValues = null
	}

	private fun setSolution(validSolution: java.util.ArrayList<Array<Int>>) {
		for (rowColumnValue in validSolution) {
			val row = rowColumnValue[0]
			val column = rowColumnValue[1]
			val value = rowColumnValue[2]
			val cell = getCell(row, column)
			cell.solution = value
		}
	}

	/**
	 * Wraps given array in this object.
	 */
	init {
		initCollection()
	}

	/**
	 * True if no value is entered to any of cells.
	 */
	val isEmpty: Boolean
		get() {
			for (row in 0..<SUDOKU_SIZE) {
				for (col in 0..<SUDOKU_SIZE) {
					val cell = cells[row][col]
					if (cell.value != 0) return false
				}
			}
			return true
		}

	val hasMistakes: Boolean
		get() {
			for (row in 0..<SUDOKU_SIZE) {
				for (col in 0..<SUDOKU_SIZE) {
					if (!cells[row][col].matchesSolution) return true
				}
			}
			return false
		}

	/**
	 * Gets cell at given position.
	 */
	fun getCell(rowIndex: Int, colIndex: Int): Cell = cells[rowIndex][colIndex]

	private fun markAllCellsAsValid() {
		mOnChangeEnabled = false
		for (r in 0..<SUDOKU_SIZE) {
			for (c in 0..<SUDOKU_SIZE) {
				cells[r][c].isValid = true
			}
		}
		mOnChangeEnabled = true
		onChange()
	}

	/**
	 * Validates numbers in collection according to the Sudoku rules. Cells with invalid
	 * values are marked - you can use getInvalid method of cell to find out whether cell
	 * contains valid value.
	 *
	 * @return True if validation is successful.
	 */
	fun validate(): Boolean {
		var valid = true

		// first set all cells as valid
		markAllCellsAsValid()
		mOnChangeEnabled = false
		// run validation in groups
		for (row in mRows) {
			if (!row.validate()) {
				valid = false
			}
		}
		for (column in mColumns) {
			if (!column.validate()) {
				valid = false
			}
		}
		for (sector in mSectors) {
			if (!sector.validate()) {
				valid = false
			}
		}
		mOnChangeEnabled = true
		onChange()
		return valid
	}

	val isCompleted: Boolean
		get() {
			for (r in 0..<SUDOKU_SIZE) {
				for (c in 0..<SUDOKU_SIZE) {
					val cell = cells[r][c]
					if (cell.value == 0 || !cell.isValid) {
						return false
					}
				}
			}
			return true
		}

	/**
	 * Marks all cells as editable.
	 */
	fun markAllCellsAsEditable() {
		for (r in 0..<SUDOKU_SIZE) {
			for (c in 0..<SUDOKU_SIZE) {
				val cell = cells[r][c]
				cell.isEditable = true
			}
		}
	}

	/**
	 * Marks all filled cells (cells with value other than 0) as not editable.
	 */
	fun markCellsWithValuesAsNotEditable() {
		for (r in 0..<SUDOKU_SIZE) {
			for (c in 0..<SUDOKU_SIZE) {
				val cell = cells[r][c]
				cell.isEditable = cell.value == 0
			}
		}
		solutionCount = -1 // find new solution on next get
		regenerateOriginalValues()
	}

	/**
	 * Fills in all valid center notes for all cells based on the values in each row, column,
	 * and sector. This is a destructive operation in that the existing notes are overwritten.
	 */
	fun fillInCenterNotes() {
		for (r in 0..<SUDOKU_SIZE) {
			for (c in 0..<SUDOKU_SIZE) {
				val cell = getCell(r, c)
				cell.centerNote = CellNote()
				val row = cell.row
				val column = cell.column
				val sector = cell.sector
				for (i in 1..SUDOKU_SIZE) {
					if (!row!!.contains(i) && !column!!.contains(i) && !sector!!.contains(i)) {
						cell.centerNote = cell.centerNote.addNumber(i)
					}
				}
			}
		}
	}

	/**
	 * Fills in center notes with all values for all cells.
	 * This is a destructive operation in that the existing notes are overwritten.
	 */
	fun fillInCenterNotesWithAllValues() {
		for (r in 0..<SUDOKU_SIZE) {
			for (c in 0..<SUDOKU_SIZE) {
				val cell = getCell(r, c)
				cell.centerNote = CellNote()
				for (i in 1..SUDOKU_SIZE) {
					cell.centerNote = cell.centerNote.addNumber(i)
				}
			}
		}
	}

	fun removeNotesForChangedCell(cell: Cell, number: Int) {
		if (number < 1 || number > 9) {
			return
		}
		val cells: MutableList<Cell?> = ArrayList()
		cells.addAll(listOf(*cell.row!!.cells))
		cells.addAll(listOf(*cell.column!!.cells))
		cells.addAll(listOf(*cell.sector!!.cells))
		for (c in cells) {
			c!!.cornerNote = c.cornerNote.removeNumber(number)
			c.centerNote = c.centerNote.removeNumber(number)
		}
	}

	val valuesUseCount: Map<Int, Int>
		/**
		 * Returns how many times each value is used in `CellCollection`.
		 * Returns map with entry for each value.
		 */
		get() {
			val valuesUseCount: MutableMap<Int, Int> = HashMap()
			for (value in 1..SUDOKU_SIZE) {
				valuesUseCount[value] = 0
			}
			for (r in 0..<SUDOKU_SIZE) {
				for (c in 0..<SUDOKU_SIZE) {
					val value = getCell(r, c).value
					if (value != 0) {
						valuesUseCount[value] = valuesUseCount[value]!! + 1
					}
				}
			}
			return valuesUseCount
		}

	/**
	 * Initializes collection, initialization has two steps:
	 * 1) Groups of cells which must contain unique numbers are created.
	 * 2) Row and column index for each cell is set.
	 */
	private fun initCollection() {
		mRows = Array(SUDOKU_SIZE) { CellGroup() }
		mColumns = Array(SUDOKU_SIZE) { CellGroup() }
		mSectors = Array(SUDOKU_SIZE) { CellGroup() }
		for (r in 0..<SUDOKU_SIZE) {
			for (c in 0..<SUDOKU_SIZE) {
				val cell = cells[r][c]
				cell.initCollection(
					this, r, c,
					mSectors[c / 3 * 3 + r / 3],
					mRows[c],
					mColumns[r]
				)
			}
		}
	}

	/**
	 * Returns a string representation of this collection in a default
	 * ([.DATA_PATTERN_VERSION_4]) format version.
	 *
	 * @see .serialize
	 * @return A string representation of this collection.
	 */
	fun serialize(): String {
		val sb = StringBuilder()
		serialize(sb, DATA_VERSION)
		return "$sb"
	}

	/**
	 * Returns a string representation of this collection in a given data format version.
	 *
	 * @see .serialize
	 * @return A string representation of this collection.
	 */
	fun serialize(dataVersion: Int): String {
		val sb = StringBuilder()
		serialize(sb, dataVersion)
		return "$sb"
	}
	/**
	 * Writes collection to given `StringBuilder` in a given data format version.
	 * You can later recreate object instance by calling [.deserialize] method.
	 *
	 * Supports only [.DATA_PATTERN_VERSION_PLAIN] and [.DATA_PATTERN_VERSION_4] formats.
	 * All the other data format versions are ignored and treated as
	 * [.DATA_PATTERN_VERSION_4] format.
	 *
	 * @see .DATA_PATTERN_VERSION_PLAIN
	 *
	 * @see .DATA_PATTERN_VERSION_4
	 */
	/**
	 * Writes collection to given `StringBuilder` in a default
	 * ([.DATA_PATTERN_VERSION_4]) data format version.
	 *
	 * @see .serialize
	 */
	@JvmOverloads
	fun serialize(data: StringBuilder, dataVersion: Int = DATA_VERSION) {
		if (dataVersion > DATA_VERSION_PLAIN) {
			data.append("version: ")
			data.append(dataVersion)
			data.append("\n")
		}
		for (r in 0..<SUDOKU_SIZE) {
			for (c in 0..<SUDOKU_SIZE) {
				val cell = cells[r][c]
				cell.serialize(data, dataVersion)
			}
		}
	}

	fun ensureOnChangeListener(listener: (() -> Unit)?) {
		requireNotNull(listener) { "The listener is null." }
		synchronized(mChangeListeners) {
			if (!mChangeListeners.contains(listener)) {
				mChangeListeners.add(listener)
			}
		}
	}

	/**
	 * Notify all registered listeners that something has changed.
	 */
	fun onChange() {
		if (mOnChangeEnabled) {
			synchronized(mChangeListeners) {
				for (listener in mChangeListeners) {
					listener()
				}
			}
		}
	}

	companion object {
		const val SUDOKU_SIZE = 9

		/**
		 * String is expected to be in format "00002343243202...", where each number represents
		 * cell value and only the original values (not editable) are stored.
		 */
		var DATA_VERSION_ORIGINAL = -1

		/**
		 * String is expected to be in format "00002343243202...", where each number represents
		 * cell value, no other information can be set using this method.
		 */
		var DATA_VERSION_PLAIN = 0

		/**
		 * See [.DATA_PATTERN_VERSION_1] and [.serialize].
		 * Notes stored as an array of numbers
		 */
		var DATA_VERSION_1 = 1

		/**
		 * Center notes
		 */
		var DATA_VERSION_4 = 4
		var DATA_VERSION = DATA_VERSION_4
		private val DATA_PATTERN_VERSION_PLAIN = Pattern.compile("^\\d{81}$")

		// version: <version:1..4>
		// <value:1..9>|<notes>|<editable:0,1> x81
		private val DATA_PATTERN_VERSION_1 = Pattern.compile(   // legacy OpenSudoku1 format
			"""^version: 1\n(?<nodeInfo>(?<value>\d)[|](?<cornerNotes>(?<note>\d,)+|-)[|](?<isEditable>[01])[|]){81}$"""
		)
		private val DATA_PATTERN_VERSION_2 = Pattern.compile(   // legacy OpenSudoku1 format
			"""^version: 2\n(?<nodeInfo>(?<value>\d)[|](?<cornerNotes>\d{1,3})[|]{1,2}(?<isEditable>[01])[|]){81}$"""
		)
		private val DATA_PATTERN_VERSION_3 = Pattern.compile(   // legacy OpenSudoku1 format
			"""^version: 3\n(?<nodeInfo>(?<value>\d)[|](?<cornerNotes>\d{1,3})[|](?<isEditable>[01])[|]){81}$"""
		)
		private val DATA_PATTERN_VERSION_4 = Pattern.compile(
			"""^version: 4\n(?<nodeInfo>(?<value>\d)[|](?<cornerNotes>\d{1,3})[|](?<centerNotes>\d{1,3})[|](?<isEditable>[01])[|]){81}$"""
		)

		/**
		 * Creates empty Sudoku board cell collection.
		 */
		fun createEmpty(): CellCollection {
			val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }
			for (r in 0..<SUDOKU_SIZE) {
				for (c in 0..<SUDOKU_SIZE) {
					cells[r][c] = Cell()
				}
			}
			return CellCollection(cells)
		}

		/**
		 * Generates debug game.
		 */
		fun createDebugGame(): CellCollection {
			val debugGame = CellCollection(
				arrayOf(
					arrayOf(Cell(), Cell(), Cell(), Cell(4), Cell(5), Cell(6), Cell(7), Cell(8), Cell(9)),
					arrayOf(Cell(), Cell(), Cell(), Cell(7), Cell(8), Cell(9), Cell(1), Cell(2), Cell(3)),
					arrayOf(Cell(), Cell(), Cell(), Cell(1), Cell(2), Cell(3), Cell(4), Cell(5), Cell(6)),
					arrayOf(Cell(2), Cell(3), Cell(4), Cell(), Cell(), Cell(), Cell(8), Cell(9), Cell(1)),
					arrayOf(Cell(5), Cell(6), Cell(7), Cell(), Cell(), Cell(), Cell(2), Cell(3), Cell(4)),
					arrayOf(Cell(8), Cell(9), Cell(1), Cell(), Cell(), Cell(), Cell(5), Cell(6), Cell(7)),
					arrayOf(Cell(3), Cell(4), Cell(5), Cell(6), Cell(7), Cell(8), Cell(9), Cell(1), Cell(2)),
					arrayOf(Cell(6), Cell(7), Cell(8), Cell(9), Cell(1), Cell(2), Cell(3), Cell(4), Cell(5)),
					arrayOf(Cell(9), Cell(1), Cell(2), Cell(3), Cell(4), Cell(5), Cell(6), Cell(7), Cell(8))
				)
			)
			debugGame.markCellsWithValuesAsNotEditable()
			return debugGame
		}

		/**
		 * Creates instance from given string created by [serialize] method or simple 81 digit string.
		 * earlier.
		 */
		fun deserialize(data: String): CellCollection {
			val lines = data.split("\n".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
			require(lines.isNotEmpty()) { "Cannot deserialize Sudoku, data corrupted." }
			val line = lines[0]
			return if (line.startsWith("version:")) {
				val kv = line.split(":".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
				val version = kv[1].trim { it <= ' ' }.toInt()
				val tokenizer = StringTokenizer(lines[1], "|")
				deserialize(tokenizer, version)
			} else {
				fromString(data)
			}
		}

		/**
		 * Creates collection instance from given string. String is expected to be in format "00002343243202..." (81 digits),
		 * where each digit represents cell value, no other information can be set using this method.
		 */
		private fun fromString(data: String): CellCollection {
			val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }
			var pos = 0
			for (r in 0..<SUDOKU_SIZE) {
				for (c in 0..<SUDOKU_SIZE) {
					var value = 0
					while (pos < data.length) {
						pos++
						if (data[pos - 1].isDigit()) {
							value = data[pos - 1].digitToInt()
							break
						}
					}
					val cell = Cell()
					cell.value = value
					cell.isEditable = value == 0
					cells[r][c] = cell
				}
			}
			return CellCollection(cells)
		}

		/**
		 * Creates instance from given `StringTokenizer`.
		 */
		fun deserialize(tokenizer: StringTokenizer, version: Int): CellCollection {
			val cells = Array(SUDOKU_SIZE) { Array(SUDOKU_SIZE) { Cell() } }
			var r = 0
			var c = 0
			while (tokenizer.hasMoreTokens() && r < 9) {
				cells[r][c] = Cell.deserialize(tokenizer, version)
				c++
				if (c == 9) {
					r++
					c = 0
				}
			}
			return CellCollection(cells)
		}

		/**
		 * Returns true, if given `data` conform to format of any version.
		 */
		fun isValid(data: String?): Boolean {
			return data != null && (
					DATA_PATTERN_VERSION_PLAIN.matcher(data).matches() ||
							DATA_PATTERN_VERSION_1.matcher(data).matches() ||
							DATA_PATTERN_VERSION_2.matcher(data).matches() ||
							DATA_PATTERN_VERSION_3.matcher(data).matches() ||
							DATA_PATTERN_VERSION_4.matcher(data).matches()
					)
		}
	}
}
