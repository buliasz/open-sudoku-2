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
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import java.io.Closeable

class ProgressUpdater(private val context: Context, private val progressBar: ProgressBar) : Closeable {
	var titleTextView: TextView? = null
	@StringRes var titleStringRes: Int = 0
	var titleParam: String? = null
		set(value) {
			if (field != value) {
				field = value
				isUpdateNeeded = true
				if (!isProgressStarted) startUpdatingProgress()
			}
		}

	var maxValue: Int = Int.MIN_VALUE
		set(value) {
			if (field != value) {
				field = value
				isUpdateNeeded = true
				if (!isProgressStarted) startUpdatingProgress()
			}
		}

	var currentValue: Int = Int.MIN_VALUE
		set(value) {
			if (field != value) {
				field = value
				isUpdateNeeded = true
				if (!isProgressStarted) startUpdatingProgress()
			}
		}

	var progressTextView: TextView? = null
	@StringRes var progressStringRes: Int = 0
	@StringRes var progressStringResMaxOnly: Int = 0
	@StringRes var progressStringResNoValues: Int = 0

	var intervalMillis: Long = 300

	private val handler = Handler(Looper.getMainLooper())
	private var isProgressStarted = false
	private var isUpdateNeeded: Boolean = false

	private fun startUpdatingProgress() {
		isProgressStarted = true
		handler.postDelayed(object : Runnable {
			override fun run() {
				progressUpdate()
				handler.postDelayed(this, intervalMillis)
			}
		}, intervalMillis)
	}

	internal fun progressUpdate() {
		if (!isUpdateNeeded) return

		titleTextView?.text = context.getString(titleStringRes, titleParam ?: "")
		isUpdateNeeded = false
		if (currentValue >= 0) {
			progressBar.isIndeterminate = false
			progressBar.max = maxValue
			progressBar.progress = currentValue
			progressTextView?.text = context.getString(progressStringRes, currentValue, maxValue)
		} else {
			progressBar.isIndeterminate = true
			if (maxValue > 0) {
				progressTextView?.text = context.getString(progressStringResMaxOnly, maxValue)
			} else {
				progressTextView?.text = context.getString(progressStringResNoValues)
			}
		}
	}

	private fun stopUpdatingProgress() {
		handler.removeCallbacksAndMessages(null)
	}

	override fun close() {
		stopUpdatingProgress()
	}
}
