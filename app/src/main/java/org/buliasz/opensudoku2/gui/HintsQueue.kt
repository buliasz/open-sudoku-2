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
import android.content.DialogInterface
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import org.buliasz.opensudoku2.R
import java.util.LinkedList
import java.util.Queue

class HintsQueue(private val mContext: Context) {
	private val mMessages: Queue<Message>
	private val mHintDialog: AlertDialog
	private val mPrefs: SharedPreferences = mContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
	private var mOneTimeHintsEnabled: Boolean

	init {
		val gameSettings = PreferenceManager.getDefaultSharedPreferences(mContext)
		gameSettings.registerOnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, key: String? ->
			if (key == "show_hints") {
				mOneTimeHintsEnabled = sharedPreferences.getBoolean("show_hints", true)
			}
		}
		mOneTimeHintsEnabled = gameSettings.getBoolean("show_hints", true)

		//processQueue();
		val mHintClosed = DialogInterface.OnClickListener { _: DialogInterface?, _: Int -> }
		mHintDialog = AlertDialog.Builder(mContext)
			.setIcon(R.drawable.ic_info)
			.setTitle(R.string.hint)
			.setMessage("")
			.setPositiveButton(R.string.close, mHintClosed).create()
		mHintDialog.setOnDismissListener { processQueue() }
		mMessages = LinkedList()
	}

	private fun addHint(hint: Message) {
		synchronized(mMessages) { mMessages.add(hint) }
		synchronized(mHintDialog) {
			if (!mHintDialog.isShowing) {
				processQueue()
			}
		}
	}

	private fun processQueue() {
		showHintDialog(synchronized(mMessages) { mMessages.poll() ?: return@processQueue })
	}

	private fun showHintDialog(hint: Message) {
		synchronized(mHintDialog) {
			mHintDialog.setTitle(mContext.getString(hint.titleResID))
			mHintDialog.setMessage(mContext.getText(hint.messageResID))
			mHintDialog.show()
		}
	}

	fun showHint(titleResID: Int, messageResID: Int) {
		val hint = Message()
		hint.titleResID = titleResID
		hint.messageResID = messageResID
		//hint.args = args;
		addHint(hint)
	}

	fun showOneTimeHint(key: String, titleResID: Int, messageResID: Int) {
		if (mOneTimeHintsEnabled) {

			// FIXME: remove in future versions
			// Before 1.0.0, hintKey was created from messageResID. This ID has in 1.0.0 changed.
			// From 1.0.0, hintKey is based on key, to be backward compatible, check for old
			// hint keys.
			if (legacyHintsWereDisplayed()) {
				return
			}
			val hintKey = "hint_$key"
			if (!mPrefs.getBoolean(hintKey, false)) {
				showHint(titleResID, messageResID)
				val editor = mPrefs.edit()
				editor.putBoolean(hintKey, true)
				editor.apply()
			}
		}
	}

	private fun legacyHintsWereDisplayed(): Boolean {
		return mPrefs.getBoolean("hint_2131099727", false) &&
				mPrefs.getBoolean("hint_2131099730", false) &&
				mPrefs.getBoolean("hint_2131099726", false) &&
				mPrefs.getBoolean("hint_2131099729", false) &&
				mPrefs.getBoolean("hint_2131099728", false)
	}

	private class Message {
		var titleResID = 0
		var messageResID = 0 //Object[] args;
	}

	companion object {
		private const val PREF_FILE_NAME = "hints"
	}
}
