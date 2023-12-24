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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.buliasz.opensudoku2.R

class SimpleDialog : DialogFragment(), DialogInterface.OnClickListener {
	@StringRes var messageId: Int = 0
	var message: String? = null
	@DrawableRes var icon: Int = 0
	@StringRes var title: Int = R.string.app_name
	var onOkCallback: (() -> Unit)? = null

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(icon)
			.setTitle(title)

		if (message == null) {
			builder.setMessage(messageId)
		} else {
			builder.setMessage(message)
		}

		if (onOkCallback != null) {
			builder.setPositiveButton(android.R.string.ok, this)
				.setNegativeButton(android.R.string.cancel, null)
		} else {
			builder.setPositiveButton(android.R.string.ok, null)
		}

		return builder.create()
	}

	fun show(fragmentManager: FragmentManager) {
		show(fragmentManager, this.javaClass.simpleName)
	}

	override fun onClick(dialog: DialogInterface?, which: Int) {
		onOkCallback!!()
	}
}
