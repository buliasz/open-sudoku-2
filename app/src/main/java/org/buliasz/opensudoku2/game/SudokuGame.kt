/*
 * Copyright (C) 2009 Roman Masek, Kotlin version 2023 Bart Uliasz
 *
 * This file is part of OpenSudoku2.
 *
 * OpenSudoku2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenSudoku2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenSudoku2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.buliasz.opensudoku2.game

import android.os.Bundle
import android.os.SystemClock
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.game.command.AbstractCommand
import org.buliasz.opensudoku2.game.command.ClearAllNotesCommand
import org.buliasz.opensudoku2.game.command.CommandStack
import org.buliasz.opensudoku2.game.command.CommandStack.Companion.deserialize
import org.buliasz.opensudoku2.game.command.EditCellCenterNoteCommand
import org.buliasz.opensudoku2.game.command.EditCellCornerNoteCommand
import org.buliasz.opensudoku2.game.command.FillInNotesCommand
import org.buliasz.opensudoku2.game.command.FillInNotesWithAllValuesCommand
import org.buliasz.opensudoku2.game.command.SetCellValueAndRemoveNotesCommand
import org.buliasz.opensudoku2.game.command.SetCellValueCommand

class SudokuGame {
    var id: Long = 0
    var created: Long = 0
    var state: Int
    private var mTime: Long = 0
    var lastPlayed: Long = 0
    var userNote: String = ""
    private lateinit var mCells: CellCollection
    private var mSolver: SudokuSolver? = null
    private var mUsedSolver = false
    private var mRemoveNotesOnEntry = false
    private var mOnPuzzleSolvedListener: OnPuzzleSolvedListener? = null
    lateinit var commandStack: CommandStack

    // Time when current activity has become active.
    private var mActiveFromTime: Long = -1

    init {
        state = GAME_STATE_NOT_STARTED
    }

    fun saveState(outState: Bundle) {
        with(outState) {
            putLong(Names.ID, id)
            putLong(Names.CREATED, created)
            putInt(Names.STATE, state)
            putLong(Names.TIME, mTime)
            putLong(Names.LAST_PLAYED, lastPlayed)
            putString(Names.CELLS_DATA, mCells.serialize())
            putString(Names.USER_NOTE, userNote)
            putString(Names.COMMAND_STACK, commandStack.serialize())
        }
    }

    fun restoreState(inState: Bundle) {
        id = inState.getLong(Names.ID)
        created = inState.getLong(Names.CREATED)
        state = inState.getInt(Names.STATE)
        mTime = inState.getLong(Names.TIME)
        lastPlayed = inState.getLong(Names.LAST_PLAYED)
        mCells = CellCollection.deserialize(inState.getString(Names.CELLS_DATA) ?: "")
        userNote = inState.getString(Names.USER_NOTE) ?: ""
        commandStack = deserialize(inState.getString(Names.COMMAND_STACK) ?: "", mCells)
        validate()
    }

    fun setOnPuzzleSolvedListener(l: OnPuzzleSolvedListener?) {
        mOnPuzzleSolvedListener = l
    }

    /**
     * Time of game-play in milliseconds.
     */
    var time: Long
        get() = (if (mActiveFromTime != -1L) mTime + SystemClock.uptimeMillis() - mActiveFromTime else mTime)
        set(time) {
            mTime = time
        }
    var cells: CellCollection
        get() = mCells
        set(cells) {
            mCells = cells
            validate()
            commandStack = CommandStack(mCells)
        }

    fun setRemoveNotesOnEntry(removeNotesOnEntry: Boolean) {
        mRemoveNotesOnEntry = removeNotesOnEntry
    }

    /**
     * Sets value for the given cell. 0 means empty cell.
     */
    fun setCellValue(cell: Cell, value: Int) {
        require(!(value < 0 || value > 9)) { "Value must be between 0-9." }
        if (cell.isEditable) {
            if (mRemoveNotesOnEntry) {
                executeCommand(SetCellValueAndRemoveNotesCommand(cell, value))
            } else {
                executeCommand(SetCellValueCommand(cell, value))
            }
            validate()
            if (isCompleted) {
                finish()
                (mOnPuzzleSolvedListener ?: return).onPuzzleSolved()
            }
        }
    }

    /**
     * Sets corner note attached to the given cell.
     */
    fun setCellCornerNote(cell: Cell, note: CellNote) {
        if (cell.isEditable) {
            executeCommand(EditCellCornerNoteCommand(cell, note))
        }
    }

    /**
     * Sets center note attached to the given cell.
     */
    fun setCellCenterNote(cell: Cell, note: CellNote) {
        if (cell.isEditable) {
            executeCommand(EditCellCenterNoteCommand(cell, note))
        }
    }

    private fun executeCommand(c: AbstractCommand) {
        commandStack.execute(c)
    }

    /**
     * Undo last command.
     */
    fun undo() {
        commandStack.undo()
    }

    fun hasSomethingToUndo(): Boolean = commandStack.hasSomethingToUndo()

    fun setUndoCheckpoint() {
        commandStack.setCheckpoint()
    }

    fun undoToCheckpoint() {
        commandStack.undoToCheckpoint()
    }

    fun hasUndoCheckpoint(): Boolean = commandStack.hasCheckpoint()

    fun undoToBeforeMistake() {
        commandStack.undoToSolvableState()
    }

    val lastChangedCell: Cell?
        get() = commandStack.lastChangedCell

    /**
     * Start game-play.
     */
    fun start() {
        state = GAME_STATE_PLAYING
        resume()
    }

    fun resume() {
        // reset time we have spent playing so far, so time when activity was not active
        // will not be part of the game play time
        mActiveFromTime = SystemClock.uptimeMillis()
    }

    /**
     * Pauses game-play (for example if activity pauses).
     */
    fun pause() {
        // save time we have spent playing so far - it will be reset after resuming
        mTime += SystemClock.uptimeMillis() - mActiveFromTime
        mActiveFromTime = -1
        lastPlayed = System.currentTimeMillis()
    }

    val isSolvable: Boolean
        /**
         * Checks if a solution to the puzzle exists
         */
        get() {
            val finalValues = with(SudokuSolver()) {
                mSolver = this
                setPuzzle(mCells)
                solve()
            }
            return finalValues.isNotEmpty()
        }

    /**
     * Solves puzzle from original state
     */
    fun solve() {
        mUsedSolver = true
        val finalValues = with(SudokuSolver()) {
            mSolver = this
            setPuzzle(mCells)
            solve()
        }
        for (rowColVal in finalValues) {
            val row = rowColVal[0]
            val col = rowColVal[1]
            val `val` = rowColVal[2]
            val cell = mCells.getCell(row, col)
            setCellValue(cell, `val`)
        }
    }

    fun usedSolver(): Boolean = mUsedSolver

    /**
     * Solves puzzle and fills in correct value for selected cell
     */
    fun solveCell(cell: Cell) {
        val finalValues = with(SudokuSolver()) {
            mSolver = this
            setPuzzle(mCells)
            solve()
        }
        val row = cell.rowIndex
        val col = cell.columnIndex
        for (rowColVal in finalValues) {
            if (rowColVal[0] == row && rowColVal[1] == col) {
                val `val` = rowColVal[2]
                setCellValue(cell, `val`)
            }
        }
    }

    /**
     * Finishes game-play. Called when puzzle is solved.
     */
    private fun finish() {
        pause()
        state = GAME_STATE_COMPLETED
    }

    /**
     * Resets game.
     */
    fun reset() {
        for (r in 0..<CellCollection.SUDOKU_SIZE) {
            for (c in 0..<CellCollection.SUDOKU_SIZE) {
                val cell = mCells.getCell(r, c)
                if (cell.isEditable) {
                    cell.value = 0
                    cell.cornerNote = CellNote()
                    cell.centerNote = CellNote()
                }
            }
        }
        commandStack = CommandStack(mCells)
        validate()
        time = 0
        lastPlayed = 0
        state = GAME_STATE_NOT_STARTED
        mUsedSolver = false
    }

    private val isCompleted: Boolean
        /**
         * Returns true, if puzzle is solved. In order to know the current state, you have to
         * call validate first.
         *
         * @return
         */
        get() = mCells.isCompleted

    fun clearAllNotes() = executeCommand(ClearAllNotesCommand())

    /**
     * Fills in possible values which can be entered in each cell.
     */
    fun fillInNotes() = executeCommand(FillInNotesCommand())

    /**
     * Fills in all values which can be entered in each cell.
     */
    fun fillInNotesWithAllValues() = executeCommand(FillInNotesWithAllValuesCommand())

    private fun validate() {
        mCells.validate()
    }

    interface OnPuzzleSolvedListener {
        /**
         * Occurs when puzzle is solved.
         */
        fun onPuzzleSolved()
    }

    companion object {
        const val GAME_STATE_PLAYING = 0
        const val GAME_STATE_NOT_STARTED = 1
        const val GAME_STATE_COMPLETED = 2
        fun createEmptyGame(): SudokuGame {
            val game = SudokuGame()
            game.cells = CellCollection.createEmpty()
            // set creation time
            game.created = System.currentTimeMillis()
            return game
        }
    }
}
