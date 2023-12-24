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
import org.buliasz.opensudoku2.gui.SudokuListActivity
import org.buliasz.opensudoku2.gui.SudokuListSorter

class SortDialogFragment(
	private var mListSorter: SudokuListSorter,
	private val settings: SharedPreferences,
	private val updateList: () -> Unit
) :
	DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(R.drawable.ic_sort)
			.setTitle(R.string.sort_puzzles_by)
			.setSingleChoiceItems(
				R.array.game_sort,
				mListSorter.sortType
			) { _: DialogInterface?, whichButton: Int -> mListSorter.sortType = whichButton }
			.setPositiveButton(R.string.sort_order_ascending) { _: DialogInterface?, _: Int ->
				mListSorter.isAscending = (true)
				settings.edit()
					.putInt(SudokuListActivity.SORT_TYPE, mListSorter.sortType)
					.putBoolean(SudokuListActivity.SORT_ORDER, true)
					.apply()
				updateList()
			}
			.setNegativeButton(R.string.sort_order_descending) { _: DialogInterface?, _: Int ->
				mListSorter.isAscending = (false)
				settings.edit()
					.putInt(SudokuListActivity.SORT_TYPE, mListSorter.sortType)
					.putBoolean(SudokuListActivity.SORT_ORDER, false)
					.apply()
				updateList()
			}
			.setNeutralButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
		return builder.create()
	}
}
