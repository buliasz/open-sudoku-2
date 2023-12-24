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

/**
 * Represents group of cells which must each contain unique number.
 *
 *
 * Typical examples of instances are sudoku row, column or sector (3x3 group of cells).
 */
class CellGroup {
    val cells = Array(CellCollection.SUDOKU_SIZE) { Cell() }
    private var mPos = 0
    fun addCell(cell: Cell) {
        cells[mPos] = cell
        mPos++
    }

    /**
     * Validates numbers in given sudoku group - numbers must be unique. Cells with invalid
     * numbers are marked (see [Cell.isValid]).
     *
     *
     * Method expects that cell's invalid properties has been set to false
     * ([CellCollection.validate] does this).
     *
     * @return True if validation is successful.
     */
    fun validate(): Boolean {
        var valid = true
        val cellsByValue: MutableMap<Int, Cell> = HashMap()
        for (cell in cells) {
            val value = cell.value
            if (cellsByValue[value] != null) {
                cell.isValid = false
                cellsByValue[value]!!.isValid = (false)
                valid = false
            } else {
                cellsByValue[value] = cell
                // we cannot set cell as valid here, because same cell can be invalid
                // as part of another group
            }
        }
        return valid
    }

    operator fun contains(value: Int): Boolean {
        for (mCell in cells) {
            if (mCell.value == value) {
                return true
            }
        }
        return false
    }
}
