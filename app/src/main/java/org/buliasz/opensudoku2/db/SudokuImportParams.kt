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

package org.buliasz.opensudoku2.db

import org.buliasz.opensudoku2.game.SudokuGame

class SudokuImportParams {
	@JvmField
	var created: Long = 0

	@JvmField
	var state: Long = 0

	@JvmField
	var time: Long = 0

	@JvmField
	var lastPlayed: Long = 0

	@JvmField
	var cellsData: String = ""

	@JvmField
	var userNote: String = ""

	@JvmField
	var commandStack: String = ""
	fun clear() {
		created = 0
		state = SudokuGame.GAME_STATE_NOT_STARTED.toLong()
		time = 0
		lastPlayed = 0
		cellsData = ""
		userNote = ""
		commandStack = ""
	}
}
