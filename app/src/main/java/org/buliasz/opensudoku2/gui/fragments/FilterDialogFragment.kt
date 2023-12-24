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
import org.buliasz.opensudoku2.gui.SudokuListFilter

class FilterDialogFragment(private val mListFilter: SudokuListFilter, val settings: SharedPreferences, val updateList: () -> Unit) :
	DialogFragment() {
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(R.drawable.ic_view)
			.setTitle(R.string.filter_by_gamestate)
			.setMultiChoiceItems(
				R.array.game_states, booleanArrayOf(
					mListFilter.showStateNotStarted,
					mListFilter.showStatePlaying,
					mListFilter.showStateCompleted
				)
			) { _: DialogInterface?, whichButton: Int, isChecked: Boolean ->
				when (whichButton) {
					0 -> mListFilter.showStateNotStarted = isChecked
					1 -> mListFilter.showStatePlaying = isChecked
					2 -> mListFilter.showStateCompleted = isChecked
				}
			}
			.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
				settings.edit()
					.putBoolean(SudokuListActivity.FILTER_STATE_NOT_STARTED, mListFilter.showStateNotStarted)
					.putBoolean(SudokuListActivity.FILTER_STATE_PLAYING, mListFilter.showStatePlaying)
					.putBoolean(SudokuListActivity.FILTER_STATE_SOLVED, mListFilter.showStateCompleted)
					.apply()
				updateList()
			}
			.setNegativeButton(android.R.string.cancel) { _: DialogInterface?, _: Int -> }
		return builder.create()
	}
}
