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
import java.util.Stack
import java.util.StringTokenizer

class CommandStack(private val mCells: CellCollection) {

	private val mCommandStack = Stack<AbstractCommand>()
	var onEmptyChangeListener: (isEmpty: Boolean) -> Unit = {}
	var isEmpty = true // true if no commands are recorded on this CommandStack
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

	fun execute(command: AbstractCommand, isManual: Boolean) {
		if (isManual) {
			push(ManualActionCommand())
		}
		push(command)
		command.execute()
	}

	fun undo(): Cell? {
		if (isEmpty) {
			return null
		}

		var lastCommand: AbstractCommand
		var cellUndone: Cell? = null

		do {
			lastCommand = pop()
			if (lastCommand !is ManualActionCommand && lastCommand !is CheckpointCommand) {
				cellUndone = lastCommand.undo()
			}
		} while (!isEmpty && lastCommand !is ManualActionCommand)

		mCells.validate()
		return cellUndone
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
		mCells.validate()
	}

	fun undoToSolvableState() {
		require(mCells.solutionCount == 1) { "This puzzle has " + mCells.solutionCount + " solutions" }
		while (!isEmpty && mCells.hasMistakes) {
			undo()
		}
		mCells.validate()
	}

	val lastCommandCell: Cell?
		get() {
			val iterator: ListIterator<AbstractCommand> = mCommandStack.listIterator(mCommandStack.size)
			while (iterator.hasPrevious()) {
				when (val o = iterator.previous()) {
					is AbstractSingleCellCommand -> {
						return o.cell
					}

					is SetCellValueAndRemoveNotesCommand -> {
						return o.cell
					}
				}
			}
			return null
		}

	private fun push(command: AbstractCommand) {
		if (command is AbstractCellCommand) {
			command.cells = mCells
		}
		mCommandStack.push(command)
		if (command !is CheckpointCommand) {
			isEmpty = false
		}
	}

	private fun pop(): AbstractCommand {
		val lastCommand = mCommandStack.pop()
		isEmpty = mCommandStack.isEmpty()
		return lastCommand
	}

	fun deserialize(data: String?) {
		if (data == null || data == "") {
			return
		}
		val st = StringTokenizer(data, "|")
		clean()
		val stackSize = st.nextToken().toInt()
		for (i in 0..<stackSize) {
			val command: AbstractCommand = AbstractCommand.deserialize(st)
			push(command)
		}
	}

	fun clean() {
		mCommandStack.empty()
		isEmpty = true
	}
}
