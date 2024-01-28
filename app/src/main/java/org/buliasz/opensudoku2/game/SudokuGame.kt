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

import android.content.ContentValues
import android.os.Bundle
import android.os.SystemClock
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.game.CellCollection.Companion.SUDOKU_SIZE
import org.buliasz.opensudoku2.game.command.AbstractCommand
import org.buliasz.opensudoku2.game.command.ClearAllNotesCommand
import org.buliasz.opensudoku2.game.command.CommandStack
import org.buliasz.opensudoku2.game.command.EditCellCenterNoteCommand
import org.buliasz.opensudoku2.game.command.EditCellCornerNoteCommand
import org.buliasz.opensudoku2.game.command.FillInNotesCommand
import org.buliasz.opensudoku2.game.command.FillInNotesWithAllValuesCommand
import org.buliasz.opensudoku2.game.command.SetCellValueAndRemoveNotesCommand
import org.buliasz.opensudoku2.game.command.SetCellValueCommand
import java.time.Instant

class SudokuGame {
	var id: Long = -1
	var folderId: Long = -1
	var created: Long = 0
	var state: Int
	private var mTime: Long = 0
	var lastPlayed: Long = 0
	var userNote: String = ""
	private lateinit var mCells: CellCollection
	private var mUsedSolver = false
	internal var removeNotesOnEntry = true
	internal var onPuzzleSolvedListener: (() -> Unit)? = null
	internal var onDigitFinishedManuallyListener: ((Int) -> Unit)? = null
	var onHasUndoChangedListener: (isEmpty: Boolean) -> Unit
		get() = commandStack.onEmptyChangeListener
		set(value) {
			commandStack.onEmptyChangeListener = value
		}
	lateinit var commandStack: CommandStack

	// Time when current activity has become active.
	private var mActiveFromTime: Long = -1

	val contentValues: ContentValues
		get() {
			with(ContentValues()) {
				put(Names.ORIGINAL_VALUES, cells.originalValues)
				put(Names.CELLS_DATA, cells.serialize())
				put(Names.CREATED, created)
				put(Names.LAST_PLAYED, lastPlayed)
				put(Names.STATE, state)
				put(Names.TIME, time)
				put(Names.USER_NOTE, userNote)
				put(Names.COMMAND_STACK, if (state == GAME_STATE_PLAYING) commandStack.serialize() else "")
				put(Names.FOLDER_ID, folderId)
				return this
			}
		}

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
			putLong(Names.FOLDER_ID, folderId)
		}
	}

	fun restoreState(inState: Bundle) {
		id = inState.getLong(Names.ID)
		created = inState.getLong(Names.CREATED)
		state = inState.getInt(Names.STATE)
		mTime = inState.getLong(Names.TIME)
		lastPlayed = inState.getLong(Names.LAST_PLAYED)
		cells = CellCollection.deserialize(inState.getString(Names.CELLS_DATA) ?: "")
		userNote = inState.getString(Names.USER_NOTE) ?: ""
		commandStack.deserialize(inState.getString(Names.COMMAND_STACK))
		folderId = inState.getLong(Names.FOLDER_ID)
		mCells.validate()
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
			mCells.validate()
			commandStack = CommandStack(mCells)
		}

	/**
	 * Sets value for the given cell. 0 means empty cell.
	 */
	fun setCellValue(cell: Cell, value: Int, isManual: Boolean) {
		if (!cell.isEditable || cell.value == value) return
		require(!(value < 0 || value > 9)) { "Value must be between 0-9." }

		if (removeNotesOnEntry) {
			executeCommand(SetCellValueAndRemoveNotesCommand(cell, value), isManual)
		} else {
			executeCommand(SetCellValueCommand(cell, value), isManual)
		}
		if (mCells.validate()) {
			if (isManual && value > 0 && cells.valuesUseCount[value] == 9) {
				onDigitFinishedManuallyListener?.invoke(value)
			}
		}
		if (isCompleted) {
			finish()
			onPuzzleSolvedListener?.invoke()
		}
	}

	/**
	 * Sets corner note attached to the given cell.
	 */
	fun setCellCornerNote(cell: Cell, note: CellNote, isManual: Boolean): Boolean {
		if (cell.isEditable && cell.cornerNote != note) {
			executeCommand(EditCellCornerNoteCommand(cell, note), isManual)
			return true
		}
		return false
	}

	/**
	 * Sets center note attached to the given cell.
	 */
	fun setCellCenterNote(cell: Cell, note: CellNote, isManual: Boolean): Boolean {
		if (cell.isEditable && cell.centerNote != note) {
			executeCommand(EditCellCenterNoteCommand(cell, note), isManual)
			return true
		}
		return false
	}

	private fun executeCommand(c: AbstractCommand, isManual: Boolean) {
		commandStack.execute(c, isManual)
	}

	/**
	 * Undo last command.
	 */
	fun undo(): Cell? = commandStack.undo()

	fun hasSomethingToUndo(): Boolean = !commandStack.isEmpty

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

	val lastCommandCell: Cell?
		get() = commandStack.lastCommandCell

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
		lastPlayed = Instant.now().epochSecond
	}

	val solutionCount: Int
		get() = mCells.solutionCount

	/**
	 * Solves puzzle from original state
	 */
	fun solve(): Int {
		mUsedSolver = true
		if (mCells.solutionCount != 1) {
			return mCells.solutionCount
		}
		for (row in 0..<SUDOKU_SIZE) {
			for (col in 0..<SUDOKU_SIZE) {
				val cell = mCells.getCell(row, col)
				setCellValue(cell, cell.solution, false)
			}
		}
		commandStack.clean()
		return 1
	}

	fun usedSolver(): Boolean = mUsedSolver

	/**
	 * Solves puzzle and fills in correct value for selected cell
	 */
	fun solveCell(cell: Cell) {
		require(mCells.solutionCount == 1) { "This puzzle has " + mCells.solutionCount + " solutions" }
		setCellValue(cell, cell.solution, true)
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
		for (r in 0..<SUDOKU_SIZE) {
			for (c in 0..<SUDOKU_SIZE) {
				val cell = mCells.getCell(r, c)
				if (cell.isEditable) {
					cell.value = 0
					cell.cornerNote = CellNote()
					cell.centerNote = CellNote()
				}
			}
		}
		commandStack = CommandStack(mCells)
		mCells.validate()
		time = 0
		lastPlayed = 0
		state = GAME_STATE_NOT_STARTED
		mUsedSolver = false
	}

	/**
	 * Returns true, if puzzle is solved. In order to know the current state, you have to
	 * call validate first.
	 */
	private val isCompleted: Boolean
		get() = mCells.isCompleted

	fun clearAllNotesManual() = executeCommand(ClearAllNotesCommand(), true)

	/**
	 * Fills in possible values which can be entered in each cell.
	 */
	fun fillInNotesManual() = executeCommand(FillInNotesCommand(), true)

	/**
	 * Fills in all values which can be entered in each cell.
	 */
	fun fillInNotesWithAllValuesManual() = executeCommand(FillInNotesWithAllValuesCommand(), true)

	companion object {
		const val GAME_STATE_PLAYING = 0
		const val GAME_STATE_NOT_STARTED = 1
		const val GAME_STATE_COMPLETED = 2
		fun createEmptyGame(): SudokuGame {
			val game = SudokuGame()
			game.cells = CellCollection.createEmpty()
			// set creation time
			game.created = Instant.now().epochSecond
			return game
		}
	}
}
