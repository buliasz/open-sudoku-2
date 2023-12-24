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

import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.CellNote

class ClearAllNotesCommand : AbstractMultiNoteCommand() {
	override fun execute() {
		val cells = cells
		mOldCornerNotes.clear()
		for (r in 0..<CellCollection.SUDOKU_SIZE) {
			for (c in 0..<CellCollection.SUDOKU_SIZE) {
				val cell = cells.getCell(r, c)
				if (!cell.cornerNote.isEmpty) {
					mOldCornerNotes.add(NoteEntry(r, c, cell.cornerNote))
					cell.cornerNote = CellNote()
				}
				if (!cell.centerNote.isEmpty) {
					mOldCenterNotes.add(NoteEntry(r, c, cell.centerNote))
					cell.centerNote = CellNote()
				}
			}
		}
	}
}
