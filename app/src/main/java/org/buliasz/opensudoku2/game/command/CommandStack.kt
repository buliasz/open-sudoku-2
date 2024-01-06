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
	var onEmptyChangeListener: (isEmpty: Boolean) -> Unit = {}
	var isEmpty = true
		set(value) {
			if (field != value) {
				field = value
				onEmptyChangeListener(value)
			}
		}

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
		if (!isEmpty) {
			pop().undo()
			validateCells()
		}
	}

	fun setCheckpoint() {
		if (!isEmpty) {
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
		var c: AbstractCommand
		while (!isEmpty) {
			c = pop()
			c.undo()
			if (c is CheckpointCommand) break
		}
		validateCells()
	}

	private fun hasMistakes(finalValues: ArrayList<IntArray>): Boolean {
		for (rowColVal in finalValues) {
			val row = rowColVal[0]
			val col = rowColVal[1]
			val value = rowColVal[2]
			val cell = mCells.getCell(row, col)
			if (cell.value != value && cell.value != 0) {
				return true
			}
		}
		return false
	}

	fun undoToSolvableState() {
		val solver = SudokuSolver()
		solver.setPuzzle(mCells)
		val finalValues = solver.solve()
		while (!isEmpty && hasMistakes(finalValues)) {
			pop().undo()
		}
		validateCells()
	}

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
		isEmpty = false
	}

	private fun pop(): AbstractCommand {
		val lastCommand = mCommandStack.pop()
		isEmpty = mCommandStack.isEmpty()
		return lastCommand
	}

	private fun validateCells() {
		mCells.validate()
	}

	fun deserialize(data: String?) {
		if (data == null || data == "") {
			return
		}
		val st = StringTokenizer(data, "|")
		val result = CommandStack(mCells)
		val stackSize = st.nextToken().toInt()
		for (i in 0..<stackSize) {
			val command: AbstractCommand = AbstractCommand.deserialize(st)
			result.push(command)
		}
	}
}
