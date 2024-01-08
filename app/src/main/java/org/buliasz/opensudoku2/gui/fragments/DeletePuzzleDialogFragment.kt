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

package org.buliasz.opensudoku2.gui.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase

class DeletePuzzleDialogFragment(val mDatabase: SudokuDatabase, val settings: SharedPreferences, val updateList: () -> Unit) :
	DialogFragment() {
	var puzzleID: Long = 0

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(R.drawable.ic_delete)
			.setTitle("Puzzle")
			.setMessage(R.string.delete_puzzle_confirm)
			.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
				val mostRecentId = settings.getLong("most_recently_played_puzzle_id", 0)
				if (puzzleID == mostRecentId) {
					settings.edit().remove("most_recently_played_puzzle_id").apply()
				}
				mDatabase.deletePuzzle(puzzleID)
				updateList()
			}
			.setNegativeButton(android.R.string.cancel, null)

		return builder.create()
	}
}
