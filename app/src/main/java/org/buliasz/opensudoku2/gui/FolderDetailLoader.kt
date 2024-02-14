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
import android.util.Log
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.FolderInfo
import java.io.Closeable
import java.util.concurrent.Executors

/**
 * Loads details of given folders on one single background thread.
 * Results are published on UI.
 *
 * Please note that instance of this class has to be created on GUI thread!
 */
class FolderDetailLoader(context: Context) : Closeable {
	private val mDatabase: SudokuDatabase
	private val executorService = Executors.newSingleThreadExecutor()

	init {
		mDatabase = SudokuDatabase(context, true)
	}

	fun loadDetailAsync(folderId: Long, loadedCallback: (FolderInfo) -> Unit) {
		executorService.execute {
			try {
				val folderInfo = mDatabase.getFolderInfoWithCounts(folderId)
				loadedCallback(folderInfo)
			} catch (e: Exception) {    // this is unimportant, we can log an error and continue
				Log.e(TAG, "Error occurred while loading full folder info.", e)
			}
		}
	}

	override fun close() {
		executorService.shutdownNow()
		mDatabase.close()
	}

	companion object {
		private const val TAG = "FolderDetailLoader"
	}
}
