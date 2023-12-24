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
package org.buliasz.opensudoku2.gui

import android.content.Context
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.utils.StringUtils

class SudokuListFilter(private val mContext: Context) {
	var showStateNotStarted = true
	var showStatePlaying = true
	var showStateCompleted = true
	override fun toString(): String {
		val visibleStates: MutableList<String?> = ArrayList()
		if (showStateNotStarted) {
			visibleStates.add(mContext.getString(R.string.not_started))
		}
		if (showStatePlaying) {
			visibleStates.add(mContext.getString(R.string.playing))
		}
		if (showStateCompleted) {
			visibleStates.add(mContext.getString(R.string.solved))
		}
		return StringUtils.join(visibleStates, ",")
	}
}
