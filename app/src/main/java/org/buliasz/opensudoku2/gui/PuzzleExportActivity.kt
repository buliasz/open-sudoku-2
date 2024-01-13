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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.gui.exporting.FileExportTask
import org.buliasz.opensudoku2.gui.exporting.FileExportTaskParams
import java.io.FileNotFoundException
import java.util.Date

class PuzzleExportActivity : ThemedActivity() {
	private lateinit var mFileExportTask: FileExportTask
	private lateinit var mExportParams: FileExportTaskParams

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.sudoku_export)
		mFileExportTask = FileExportTask()
		mExportParams = FileExportTaskParams()
		var intent = intent
		if (!intent.hasExtra(Names.FOLDER_ID)) {
			Log.d(TAG, "No 'FOLDER_ID' extra provided, exiting.")
			finish()
			return
		}
		mExportParams.folderId = intent.getLongExtra(Names.FOLDER_ID, ALL_IDS)
		mExportParams.puzzleId = intent.getLongExtra(Names.ID, ALL_IDS)
		val timestamp = DateFormat.format("yyyy-MM-dd-HH-mm-ss", Date()).toString()

		val fileName = if (mExportParams.folderId == -1L) {
			"all-folders-$timestamp"
		} else {
			val folderName = SudokuDatabase(applicationContext).use { database ->
				val folderId = mExportParams.folderId ?: database.getPuzzle(mExportParams.puzzleId!!)!!.folderId
				val folder = database.getFolderInfo(folderId)
				if (folder == null) {
					Log.e(TAG, "Folder with id ${mExportParams.folderId} not found, exiting.")
					finish()
					return@onCreate
				}
				folder.name
			}
			if (mExportParams.puzzleId != null) "$folderName-${mExportParams.puzzleId}-$timestamp" else "$folderName-$timestamp"
		}

		intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
		intent.addCategory(Intent.CATEGORY_OPENABLE)
		intent.setType("application/x-opensudoku2")
		intent.putExtra(Intent.EXTRA_TITLE, "$fileName.opensudoku2")
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				val data: Intent? = result.data
				if (data != null) {
					val uri = data.data
					startExportToFileTask(uri)
				}
			} else if (result.resultCode == RESULT_CANCELED) {
				finish()
			}
		}.launch(intent)
	}

	private fun startExportToFileTask(uri: Uri?) {
		mFileExportTask.onExportFinishedListener = { result ->
			withContext(Dispatchers.Main) {
				if (result!!.isSuccess) {
					Toast.makeText(
						this@PuzzleExportActivity,
						getString(R.string.puzzles_have_been_exported, result.filename),
						Toast.LENGTH_SHORT
					).show()
				} else {
					Toast.makeText(
						this@PuzzleExportActivity, getString(R.string.unknown_export_error), Toast.LENGTH_LONG
					).show()
				}
			}
			finish()
		}
		try {
			mExportParams.fileOutputStream = contentResolver.openOutputStream(uri!!)
			contentResolver.query(uri, null, null, null, null)?.use { cursor ->
				if (cursor.moveToFirst()) {
					mExportParams.filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
				}
			}
		} catch (e: FileNotFoundException) {
			Toast.makeText(this@PuzzleExportActivity, getString(R.string.unknown_export_error), Toast.LENGTH_LONG).show()
		}
		CoroutineScope(Dispatchers.IO).launch {
			mFileExportTask.exportToFile(this@PuzzleExportActivity, mExportParams)
		}
	}

	companion object {
		/**
		 * Id of folder to export. If -1, all folders will be exported.
		 */
		const val ALL_IDS: Long = -1
		private val TAG = PuzzleExportActivity::class.java.simpleName
	}
}
