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
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase

class DeleteFolderDialogFragment(private val mDatabase: SudokuDatabase, val updateList: () -> Unit) : DialogFragment() {

	var mDeleteFolderID: Long = 0

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val folderInfo = mDatabase.getFolderInfo(mDeleteFolderID)
		val folderName = folderInfo?.name ?: ""

		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(R.drawable.ic_delete)
			.setTitle(getString(R.string.delete_folder_title, folderName))
			.setMessage(R.string.delete_folder_confirm)
			.setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
				mDatabase.deleteFolder(mDeleteFolderID)
				updateList()
			}
			.setNegativeButton(android.R.string.cancel, null)

		return builder.create()
	}
}
