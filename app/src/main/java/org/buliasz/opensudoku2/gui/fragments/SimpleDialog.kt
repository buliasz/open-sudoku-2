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
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.buliasz.opensudoku2.R

class SimpleDialog(private val fragmentManager: FragmentManager) : DialogFragment(), DialogInterface.OnClickListener {
	@StringRes var messageId: Int = 0
	var message: String? = null
	@DrawableRes var iconId: Int = 0
	@StringRes var titleId: Int = R.string.app_name
	var positiveButtonCallback: (() -> Unit)? = null
	@StringRes var positiveButtonString: Int = android.R.string.ok
	var negativeButtonCallback: (() -> Unit)? = null
	@StringRes var negativeButtonString: Int = android.R.string.cancel
	@StringRes var neutralButtonString: Int = android.R.string.cancel
	var onDismiss: (() -> Unit)? = null
	var dialogView: View? = null

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val builder = AlertDialog.Builder(requireActivity())
			.setIcon(iconId)
			.setTitle(titleId)

		if (dialogView != null) {
			builder.setView(dialogView)
		} else if (message != null) {
			builder.setMessage(message)
		} else {
			builder.setMessage(messageId)
		}

		if (negativeButtonCallback != null) {
			builder.setPositiveButton(positiveButtonString, this)
				.setNegativeButton(negativeButtonString, this)
				.setNeutralButton(neutralButtonString, null)
		} else if (positiveButtonCallback != null) {
			builder.setPositiveButton(positiveButtonString, this)
				.setNegativeButton(negativeButtonString, null)
		} else {
			builder.setPositiveButton(positiveButtonString, null)
		}

		return builder.create()
	}

	fun show() {
		show(fragmentManager, this.javaClass.simpleName)
	}

	fun show(@StringRes messageId: Int) {
		this.messageId = messageId
		show()
	}

	fun show(message: String) {
		this.message = message
		show()
	}

	override fun onClick(dialog: DialogInterface?, whichButton: Int) {
		if (whichButton == BUTTON_POSITIVE) {
			positiveButtonCallback?.invoke()
		} else if (whichButton == BUTTON_NEGATIVE) {
			negativeButtonCallback?.invoke()
		}
	}

	override fun onDismiss(dialog: DialogInterface) {
		super.onDismiss(dialog)
		onDismiss?.invoke()
	}
}
