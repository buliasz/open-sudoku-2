/*
 * Copyright (C) 2009 Roman Masek, Kotlin version 2023 Bart Uliasz
 *
 * This file is part of OpenSudoku2.
 *
 * OpenSudoku2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenSudoku2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenSudoku2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.buliasz.opensudoku2.gui

import android.content.Context
import android.os.Handler
import android.util.Log
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.gui.FolderDetailLoader.FolderDetailCallback
import java.util.concurrent.Executors

/**
 * Loads details of given folders on one single background thread.
 * Results are published on GUI thread via [FolderDetailCallback] interface.
 *
 *
 * Please note that instance of this class has to be created on GUI thread!
 *
 *
 * You should explicitly call [.destroy] when this object is no longer needed.
 *
 * @author romario, Kotlin version by buliasz
 */
class FolderDetailLoader(context: Context?) {
    private val mDatabase: SudokuDatabase
    private val mGuiHandler: Handler
    private val mLoaderService = Executors.newSingleThreadExecutor()

    init {
        mDatabase = SudokuDatabase(context!!)
        mGuiHandler = Handler()
    }

    fun loadDetailAsync(folderID: Long, loadedCallback: FolderDetailCallback) {
        mLoaderService.execute {
            try {
                val folderInfo = mDatabase.getFolderInfoFull(folderID)
                mGuiHandler.post { loadedCallback.onLoaded(folderInfo) }
            } catch (e: Exception) {    // this is unimportant, we can log an error and continue
                Log.e(TAG, "Error occurred while loading full folder info.", e)
            }
        }
    }

    fun destroy() {
        mLoaderService.shutdownNow()
        mDatabase.close()
    }

    interface FolderDetailCallback {
        fun onLoaded(folderInfo: FolderInfo?)
    }

    companion object {
        private const val TAG = "FolderDetailLoader"
    }
}
