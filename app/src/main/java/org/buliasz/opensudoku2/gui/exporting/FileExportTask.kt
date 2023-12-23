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

package org.buliasz.opensudoku2.gui.exporting

import android.content.Context
import android.os.Handler
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.buliasz.opensudoku2.BuildConfig
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.utils.Const
import org.xmlpull.v1.XmlSerializer
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer

class FileExportTask {
    private val mGuiHandler = Handler()
    var onExportFinishedListener: OnExportFinishedListener? = null

    internal suspend fun exportToFile(context: Context, vararg params: FileExportTaskParams?) {
        withContext(Dispatchers.IO) {
            for (par in params) {
                val result = saveToFile(par!!, context)
                mGuiHandler.post {
                    onExportFinishedListener?.onExportFinished(result)
                }
            }
        }
    }

    private fun saveToFile(exportParams: FileExportTaskParams, context: Context): FileExportTaskResult {
        require(!(exportParams.folderID == null && exportParams.sudokuID == null)) { "Exactly one of folderID or sudokuID must be set." }
        require(!(exportParams.folderID != null && exportParams.sudokuID != null)) { "Only one of folderID or sudokuID must be set." }
        requireNotNull(exportParams.fileOutputStream) { "Filename must be set." }
        val start = System.currentTimeMillis()
        val result = FileExportTaskResult()
        result.isSuccess = true
        result.filename = exportParams.filename
        var writer: Writer? = null
        try {
            val serializer = Xml.newSerializer()
            writer = BufferedWriter(OutputStreamWriter(exportParams.fileOutputStream))
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "opensudoku2")
            serializer.attribute("", "version", FILE_EXPORT_VERSION)

            SudokuDatabase(context).use { db ->
                if (exportParams.folderID != null) {
                    serializeFolders(db, serializer, exportParams.folderID!!)
                } else {
                    serializeGame(serializer, db.getSudoku(exportParams.sudokuID!!)!!)
                }
            }

            serializer.endTag("", "opensudoku2")
            serializer.endDocument()
        } catch (e: IOException) {
            Log.e(Const.TAG, "Error while exporting file.", e)
            result.isSuccess = false
            return result
        } finally {
            if (writer != null) {
                try {
                    writer.close()
                } catch (e: IOException) {
                    Log.e(Const.TAG, "Error while exporting file.", e)
                    result.isSuccess = false
                }
            }
        }
        val end = System.currentTimeMillis()
        if (BuildConfig.DEBUG) Log.i(Const.TAG, String.format("Exported in %f seconds.", (end - start) / 1000f))
        return result
    }

    private fun serializeFolders(db: SudokuDatabase, serializer: XmlSerializer, folderID: Long) {
        val folderList: List<FolderInfo> = if (folderID == -1L) db.getFolderList() else listOf(db.getFolderInfo(folderID)!!)
        for (folder in folderList) {
            serializer.startTag("", Names.FOLDER)
            serializer.attribute("", Names.FOLDER_NAME, folder.name ?: "")
            serializer.attribute("", Names.FOLDER_CREATED, folder.created.toString())
            val sudokuList = db.getSudokuGameList(folder.id, null, null)
            for (game in sudokuList) {
                serializeGame(serializer, game)
            }
            serializer.endTag("", Names.FOLDER)
        }
    }

    private fun serializeGame(serializer: XmlSerializer, game: SudokuGame) {
        serializer.startTag("", Names.GAME)
        serializer.attribute("", Names.CREATED, game.created.toString())
        serializer.attribute("", Names.STATE, game.state.toString())
        serializer.attribute("", Names.TIME, game.time.toString())
        serializer.attribute("", Names.LAST_PLAYED, game.lastPlayed.toString())
        serializer.attribute("", Names.CELLS_DATA, game.cells.serialize())
        serializer.attribute("", Names.USER_NOTE, game.userNote)
        serializer.attribute("", Names.COMMAND_STACK, game.commandStack.serialize())
        serializer.endTag("", Names.GAME)
    }

    interface OnExportFinishedListener {
        /**
         * Occurs when export is finished.
         *
         * @param result The result of the export
         */
        fun onExportFinished(result: FileExportTaskResult?)
    }

    companion object {
        val FILE_EXPORT_VERSION: String get() = "3"
    }
}
