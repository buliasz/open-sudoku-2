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
import org.buliasz.opensudoku2.BuildConfig
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.db.SudokuInvalidFormatException
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.gui.fragments.SimpleDialog
import org.buliasz.opensudoku2.utils.Const
import kotlin.reflect.KSuspendFunction2

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
	private var mFolderCount = 0 // count of processed folders
	private lateinit var mImportError: String
	protected var importedCount = 0
	protected var duplicatesCount = 0
	protected var updatedCount = 0
	lateinit var mProgressUpdate: KSuspendFunction2<Int, Int, Unit>
		private set

	suspend fun doInBackground(
		context: Context,
		onImportFinished: OnImportFinishedListener,
		supportFragmentManager: FragmentManager,
		progressUpdate: KSuspendFunction2<Int, Int, Unit>
	) {
		mContext = context
		mProgressUpdate = progressUpdate
		var isSuccess = false
		withContext(Dispatchers.IO) {
			try {
				isSuccess = processImportInternal()
			} catch (e: Exception) {
				Log.e(Const.TAG, "Exception occurred during import.", e)
				setError(context.getString(R.string.unknown_import_error))
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
			if (mFolderCount == 1) {
				resultMessage = mContext.getString(R.string.puzzles_saved, mFolder.name)
			} else if (mFolderCount > 1) {
				resultMessage = mContext.getString(R.string.folders_created, mFolderCount)
			}
			if (importedCount > 0) resultMessage += "\nImported $importedCount new puzzles."
			if (duplicatesCount > 0) resultMessage += "\nSkipped $duplicatesCount already existing puzzles."
			if (updatedCount > 0) resultMessage += "\nUpdated $updatedCount existing puzzles."
		} else {
			resultMessage = mImportError
		}

		val folderId = if (mFolderCount == 1) mFolder.id else -1
		with(SimpleDialog(supportFragmentManager)) {
			titleId = R.string.importing
			message = resultMessage
			onDismiss = {
				onImportFinished.onImportFinished(isSuccess, folderId)
			}
			show()
		}
	}

	private suspend fun processImportInternal(): Boolean {
		val start = System.currentTimeMillis()
		SudokuDatabase(mContext).use { database ->
			try {
				mDatabase = database
				processImport(mContext)  // let subclass handle the import
			} catch (e: SudokuInvalidFormatException) {
				Log.e(this.javaClass.name, "Invalid format", e)
				setError(mContext.getString(R.string.invalid_format))
			}
		}

		if (mFolderCount == 0) {
			setError(mContext.getString(R.string.no_puzzles_found))
			return false
		}

		val end = System.currentTimeMillis()
		if (BuildConfig.DEBUG) Log.i(Const.TAG, String.format("Imported in %f seconds.", (end - start) / 1000f))
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
	protected fun importFolder(name: String, created: Long = System.currentTimeMillis()): Long {
		mFolderCount++
		mFolder = mDatabase.insertFolder(name, created)
		return mFolder.id
	}

	protected fun setError(error: String) {
		mImportError = error
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
