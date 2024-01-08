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
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.buliasz.opensudoku2.BuildConfig
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.db.SudokuInvalidFormatException
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.fragments.SimpleDialog
import org.buliasz.opensudoku2.utils.Const

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
	private lateinit var mDatabase: SudokuDatabase
	private lateinit var mFolder: FolderInfo // currently processed folder
	private lateinit var mContext: Context
	private var mFolderCount = 0 // count of processed folders
	private var mImportError: String? = null
	private var mImportSuccessful = false

	suspend fun doInBackground(context: Context, onImportFinished: OnImportFinishedListener, supportFragmentManager: FragmentManager) {
		mContext = context
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
		if (isSuccess) {
			if (mFolderCount == 1) {
				Toast.makeText(
					mContext, mContext.getString(R.string.puzzles_saved, mFolder.name),
					Toast.LENGTH_LONG
				).show()
			} else if (mFolderCount > 1) {
				Toast.makeText(
					mContext, mContext.getString(R.string.folders_created, mFolderCount),
					Toast.LENGTH_LONG
				).show()
			}
		} else {
			with(SimpleDialog()) {
				message = mImportError
				show(supportFragmentManager)
			}
		}

		val folderId = if (mFolderCount == 1) mFolder.id else -1
		onImportFinished.onImportFinished(isSuccess, folderId)
	}

	private fun processImportInternal(): Boolean {
		mImportSuccessful = true
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
		return mImportSuccessful
	}

	/**
	 * Subclasses should do all import work in this method.
	 */
	@Throws(SudokuInvalidFormatException::class)
	protected abstract fun processImport(context: Context)

	/**
	 * Creates new folder and starts appending puzzles to this folder.
	 */
	protected fun importFolder(name: String, created: Long = System.currentTimeMillis()): Long {
		mFolderCount++
		mFolder = mDatabase.insertFolder(name, created)
		return mFolder.id
	}

	/**
	 * Imports game with all its fields.
	 */
	@Throws(SudokuInvalidFormatException::class)
	protected fun importGame(newPuzzle: SudokuGame) {
		if (isPuzzleValid(newPuzzle)) {
			mDatabase.insertPuzzle(newPuzzle)
		}
	}

	private fun isPuzzleValid(newPuzzle: SudokuGame): Boolean {
		if (newPuzzle.solutionCount == 0) {
			setError(mContext.getString(R.string.puzzle_has_no_solution))
			return false
		}
		if (newPuzzle.solutionCount > 1) {
			setError(mContext.getString(R.string.puzzle_has_multiple_solutions))
			return false
		}
		val existingPuzzle = mDatabase.puzzleExists(newPuzzle.cells)
		if (existingPuzzle != null) {
			if (newPuzzle.folderId == existingPuzzle.folderId && existingPuzzle.state == SudokuGame.GAME_STATE_NOT_STARTED) {
				mDatabase.deletePuzzle(existingPuzzle.id)  // delete not started game to insert the imported instance
			} else {
				setError(mContext.getString(R.string.puzzle_already_exists, mDatabase.getFolderInfo(existingPuzzle.folderId)?.name))
				return false
			}
		}
		return true
	}

	protected fun setError(error: String?) {
		mImportError = error
		mImportSuccessful = false
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
