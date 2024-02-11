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

package org.buliasz.opensudoku2.gui.importing

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.db.SudokuInvalidFormatException
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.gui.ProgressUpdater
import org.buliasz.opensudoku2.gui.fragments.SimpleDialog
import org.buliasz.opensudoku2.utils.Const
import java.time.Instant

/**
 * To add support for new import source, do following:
 *
 * 1) Subclass this class. Any input parameters specific for your import should be put
 * in constructor of your class.
 * 2) In [.processImport] method process your data source (parse file or maybe download
 * data from some other source) and save puzzles by calling
 * [.importFolder] and [.importGame] methods. Note
 * that `importFolder` must be called first, otherwise `importGame`
 * doesn't know where to put puzzles.
 * 3) Add code to [org.buliasz.opensudoku2.gui.PuzzleImportActivity] which creates instance of your new class and
 * passes it input parameters.
 */
abstract class AbstractImportTask {
	private lateinit var mContext: Context
	protected lateinit var mDatabase: SudokuDatabase
	private lateinit var mFolder: FolderInfo // currently processed folder
	private var mFoldersUsed = HashSet<String>() // count of processed folders
	protected lateinit var importError: String
	protected var importedCount = 0
	protected var duplicatesCount = 0
	protected var updatedCount = 0
	lateinit var mProgressUpdate: ProgressUpdater
		private set

	suspend fun doInBackground(
		context: Context,
		onImportFinished: OnImportFinishedListener,
		supportFragmentManager: FragmentManager,
		progressUpdate: ProgressUpdater
	) {
		mContext = context
		mProgressUpdate = progressUpdate
		var isSuccess = false
		withContext(Dispatchers.IO) {
			try {
				isSuccess = processImportInternal()
			} catch (e: Exception) {
				Log.e(Const.TAG, "Exception occurred during import.", e)
				importError = context.getString(R.string.unknown_import_error)
			}
			withContext(Dispatchers.Main) {
				onPostExecute(isSuccess, onImportFinished, supportFragmentManager)
			}
		}
	}

	private fun onPostExecute(
		isSuccess: Boolean,
		onImportFinished: OnImportFinishedListener,
		supportFragmentManager: FragmentManager
	) {
		var resultMessage = ""
		if (isSuccess) {
			if (mFoldersUsed.size > 0) resultMessage += mContext.getString(R.string.puzzles_saved, mFoldersUsed.joinToString(", "))
			if (importedCount > 0) resultMessage += "\nImported $importedCount new puzzles."
			if (duplicatesCount > 0) resultMessage += "\nSkipped $duplicatesCount already existing puzzles."
			if (updatedCount > 0) resultMessage += "\nUpdated $updatedCount existing puzzles with saved in-progress games."
		} else {
			resultMessage = importError
		}

		val folderId = if (mFoldersUsed.size == 1) mFolder.id else -1
		with(SimpleDialog(supportFragmentManager)) {
			titleId = R.string.import_title
			message = resultMessage
			onDismiss = {
				onImportFinished.onImportFinished(isSuccess, folderId)
			}
			show()
		}
	}

	private suspend fun processImportInternal(): Boolean {
		SudokuDatabase(mContext, false).use { database ->
			try {
				mDatabase = database
				processImport(mContext)  // let subclass handle the import
			} catch (e: Exception) {
				Log.e(this.javaClass.name, "Invalid format", e)
				importError = mContext.getString(R.string.invalid_format)
			}
		}

		if (importedCount + duplicatesCount + updatedCount == 0) {
			importError = mContext.getString(R.string.no_puzzles_found)
			return false
		}

		return true
	}

	/**
	 * Subclasses should do all import work in this method.
	 */
	@Throws(SudokuInvalidFormatException::class)
	protected abstract suspend fun processImport(context: Context)

	/**
	 * Creates new folder to append later puzzles to this folder.
	 *
	 * @return folder ID
	 */
	protected fun importFolder(name: String, created: Long = 0): Long {
		mFolder = mDatabase.insertFolder(name, if (created > 0) created else Instant.now().epochSecond)
		mFoldersUsed.add(mFolder.name)
		return mFolder.id
	}

	interface OnImportFinishedListener {
		/**
		 * Occurs when import is finished.
		 *
		 * @param importSuccessful Indicates whether import was successful.
		 * @param folderId         Contains id of imported folder, or -1 if multiple folders were imported.
		 */
		fun onImportFinished(importSuccessful: Boolean, folderId: Long)
	}
}
