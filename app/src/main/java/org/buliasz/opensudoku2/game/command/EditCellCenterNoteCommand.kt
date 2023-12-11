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
package org.buliasz.opensudoku2.game.command

import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.game.CellNote
import java.util.StringTokenizer

class EditCellCenterNoteCommand : AbstractSingleCellCommand {
    private lateinit var mNote: CellNote
    private lateinit var mOldNote: CellNote

    constructor(cell: Cell, note: CellNote) : super(cell) {
        mNote = note
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
        val cell = cell
        mOldNote = cell.centerNote
        cell.centerNote = mNote
    }

    override fun undo() {
        val cell = cell
        cell.centerNote = mOldNote
    }
}
