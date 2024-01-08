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

import org.buliasz.opensudoku2.db.Names

class PuzzleListSorter @JvmOverloads constructor(sortType: Int = SORT_BY_CREATED, var isAscending: Boolean = false) {
	internal var sortType: Int = sortType
		set(value) {
			field = if (value in 0..<SORT_TYPE_OPTIONS_LENGTH) value else SORT_BY_CREATED
		}


	val sortOrder: String
		get() {
			val order = if (isAscending) " ASC" else " DESC"
			when (sortType) {
				SORT_BY_CREATED -> return Names.CREATED + order
				SORT_BY_TIME -> return Names.TIME + order
				SORT_BY_LAST_PLAYED -> return Names.LAST_PLAYED + order
			}
			return Names.CREATED + order
		}

	companion object {
		const val SORT_BY_CREATED = 0
		const val SORT_BY_TIME = 1
		const val SORT_BY_LAST_PLAYED = 2
		private const val SORT_TYPE_OPTIONS_LENGTH = 3
	}
}
