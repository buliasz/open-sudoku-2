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
package org.buliasz.opensudoku2.game

import android.content.Context
import org.buliasz.opensudoku2.R

/**
 * Some information about folder, used in FolderListActivity.
 */
class FolderInfo {
	/**
	 * Primary key of folder.
	 */
	var id: Long = 0

	/**
	 * Name of the folder.
	 */
	lateinit var name: String

	var created: Long = 0

	/**
	 * Total count of puzzles in the folder.
	 */
	var puzzleCount = 0

	/**
	 * Count of solved puzzles in the folder.
	 */
	var solvedCount = 0

	/**
	 * Count of puzzles in "playing" state in the folder.
	 */
	var playingCount = 0

	constructor()
	constructor(id: Long, name: String) {
		this.id = id
		this.name = name
	}

	fun getDetail(c: Context): String {
		val sb = StringBuilder()
		sb.append(c.getString(R.string.n_puzzles, puzzleCount))
		if (puzzleCount > 0) {
			// there are some puzzles
			val unsolvedCount = puzzleCount - solvedCount

			// if there are any playing or unsolved puzzles, add info about them
			if (playingCount != 0 || unsolvedCount != 0) {
				sb.append(" (")
				if (playingCount != 0) {
					sb.append(c.getString(R.string.n_playing, playingCount))
					if (unsolvedCount != 0) {
						sb.append(", ")
					}
				}
				if (unsolvedCount != 0) {
					sb.append(c.getString(R.string.n_unsolved, unsolvedCount))
				}
				sb.append(")")
			}

			// maybe all puzzles are solved?
			if (unsolvedCount == 0 && puzzleCount != 0) {
				sb.append(" (").append(c.getString(R.string.all_solved)).append(")")
			}
		}
		return "$sb"
	}
}
