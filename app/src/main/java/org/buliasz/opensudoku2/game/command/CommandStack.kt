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

package org.buliasz.opensudoku2.game.command

import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.SudokuSolver
import java.util.Stack
import java.util.StringTokenizer

class CommandStack(private val mCells: CellCollection) {

	private val mCommandStack = Stack<AbstractCommand>()

	fun serialize(): String {
		val sb = StringBuilder()
		serialize(sb)
		return "$sb"
	}

	fun serialize(data: StringBuilder) {
		data.append(mCommandStack.size).append("|")
		for (i in mCommandStack.indices) {
			val command = mCommandStack[i]
			command.serialize(data)
		}
	}

	fun execute(command: AbstractCommand) {
		push(command)
		command.execute()
	}

	fun undo() {
		if (!mCommandStack.empty()) {
			val c = pop()
			c.undo()
			validateCells()
		}
	}

	fun setCheckpoint() {
		if (!mCommandStack.empty()) {
			val c = mCommandStack.peek()
			if (c is CheckpointCommand) return
		}
		push(CheckpointCommand())
	}

	fun hasCheckpoint(): Boolean {
		for (c in mCommandStack) {
			if (c is CheckpointCommand) return true
		}
		return false
	}

	fun undoToCheckpoint() {
		/*
		 * I originally planned to just call undo but this way it doesn't need to
		 * validateCells() until the run is complete
		 */
		var c: AbstractCommand
		while (!mCommandStack.empty()) {
			c = mCommandStack.pop()
			c.undo()
			if (c is CheckpointCommand) break
		}
		validateCells()
	}

	private fun hasMistakes(finalValues: ArrayList<IntArray>): Boolean {
		for (rowColVal in finalValues) {
			val row = rowColVal[0]
			val col = rowColVal[1]
			val `val` = rowColVal[2]
			val cell = mCells.getCell(row, col)
			if (cell.value != `val` && cell.value != 0) {
				return true
			}
		}
		return false
	}

	fun undoToSolvableState() {
		val solver = SudokuSolver()
		solver.setPuzzle(mCells)
		val finalValues = solver.solve()
		while (!mCommandStack.empty() && hasMistakes(finalValues)) {
			mCommandStack.pop().undo()
		}
		validateCells()
	}

	fun hasSomethingToUndo(): Boolean = mCommandStack.size != 0

	val lastChangedCell: Cell?
		get() {
			val iterator: ListIterator<AbstractCommand> = mCommandStack.listIterator(mCommandStack.size)
			while (iterator.hasPrevious()) {
				val o = iterator.previous()
				if (o is AbstractSingleCellCommand) {
					return o.cell
				} else if (o is SetCellValueAndRemoveNotesCommand) {
					return o.cell
				}
			}
			return null
		}

	private fun push(command: AbstractCommand) {
		if (command is AbstractCellCommand) {
			command.cells = mCells
		}
		mCommandStack.push(command)
	}

	private fun pop(): AbstractCommand = mCommandStack.pop()

	private fun validateCells() {
		mCells.validate()
	}

	companion object {
		@JvmStatic
		fun deserialize(data: String, cells: CellCollection): CommandStack {
			val st = StringTokenizer(data, "|")
			return deserialize(st, cells)
		}

		fun deserialize(data: StringTokenizer, cells: CellCollection): CommandStack {
			val result = CommandStack(cells)
			val stackSize = data.nextToken().toInt()
			for (i in 0..<stackSize) {
				val command: AbstractCommand = AbstractCommand.deserialize(data)
				result.push(command)
			}
			return result
		}
	}
}
