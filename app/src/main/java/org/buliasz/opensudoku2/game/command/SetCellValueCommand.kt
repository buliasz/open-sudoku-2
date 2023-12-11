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
import java.util.StringTokenizer

class SetCellValueCommand : AbstractSingleCellCommand {
    private var mValue = 0
    private var mOldValue = 0

    constructor(cell: Cell, value: Int) : super(cell) {
        mValue = value
    }

    internal constructor()

    override fun serialize(data: StringBuilder) {
        super.serialize(data)
        data.append(mValue).append("|")
        data.append(mOldValue).append("|")
    }

    override fun deserialize(data: StringTokenizer) {
        super.deserialize(data)
        mValue = data.nextToken().toInt()
        mOldValue = data.nextToken().toInt()
    }

    override fun execute() {
        val cell = cell
        mOldValue = cell.value
        cell.value = mValue
    }

    override fun undo() {
        val cell = cell
        cell.value = mOldValue
    }
}
