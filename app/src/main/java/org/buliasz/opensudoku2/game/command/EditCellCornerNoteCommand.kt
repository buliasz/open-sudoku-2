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
import org.buliasz.opensudoku2.game.CellNote
import java.util.StringTokenizer

class EditCellCornerNoteCommand : AbstractSingleCellCommand {
	private lateinit var mNote: CellNote
	private lateinit var mOldNote: CellNote

	constructor(cell: Cell, note: CellNote) : super(cell) {
		mNote = note
		mOldNote = CellNote()
	}

	internal constructor()

	override fun serialize(data: StringBuilder) {
		super.serialize(data)
		mNote.serialize(data)
		mOldNote.serialize(data)
	}

	override fun deserialize(data: StringTokenizer) {
		super.deserialize(data)
		mNote = CellNote.deserialize(data.nextToken())
		mOldNote = CellNote.deserialize(data.nextToken())
	}

	override fun execute() {
		mOldNote = cell.cornerNote
		cell.cornerNote = mNote
	}

	override fun undo(): Cell {
		cell.cornerNote = mOldNote
		return cell
	}
}
