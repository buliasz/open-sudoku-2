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
import android.database.Cursor
import android.os.Handler
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.buliasz.opensudoku2.BuildConfig
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
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
        require(!(exportParams.folderID == null && exportParams.sudokuID == null)) { "Exactly one of folderID and sudokuID must be set." }
        require(!(exportParams.folderID != null && exportParams.sudokuID != null)) { "Exactly one of folderID and sudokuID must be set." }
        requireNotNull(exportParams.fileOutputStream) { "Filename must be set." }
        val start = System.currentTimeMillis()
        val result = FileExportTaskResult()
        result.isSuccess = true
        result.filename = exportParams.filename
        var database: SudokuDatabase? = null
        var cursor: Cursor? = null
        var writer: Writer? = null
        try {
            database = SudokuDatabase(context)
            val generateFolders: Boolean
            if (exportParams.folderID != null) {
                cursor = database.exportFolder(exportParams.folderID!!)
                generateFolders = true
            } else {
                cursor = database.exportFolder(exportParams.sudokuID!!)
                generateFolders = false
            }
            val serializer = Xml.newSerializer()
            writer = BufferedWriter(OutputStreamWriter(exportParams.fileOutputStream))
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "opensudoku2")
            serializer.attribute("", "version", FILE_EXPORT_VERSION)
            var currentFolderId: Long = -1
            while (cursor.moveToNext()) {
                if (generateFolders && currentFolderId != cursor.getLong(cursor.getColumnIndexOrThrow(Names.FOLDER_ID))) {
                    // next folder
                    if (currentFolderId != -1L) {
                        serializer.endTag("", Names.FOLDER)
                    }
                    currentFolderId = cursor.getLong(cursor.getColumnIndexOrThrow(Names.FOLDER_ID))
                    serializer.startTag("", Names.FOLDER)
                    attribute(serializer, cursor, Names.FOLDER_NAME)
                    attribute(serializer, cursor, Names.FOLDER_CREATED)
                }
                val data = cursor.getString(cursor.getColumnIndexOrThrow(Names.CELLS_DATA))
                if (data != null) {
                    serializer.startTag("", Names.GAME)
                    attribute(serializer, cursor, Names.CREATED)
                    attribute(serializer, cursor, Names.STATE)
                    attribute(serializer, cursor, Names.TIME)
                    attribute(serializer, cursor, Names.LAST_PLAYED)
                    attribute(serializer, cursor, Names.CELLS_DATA)
                    attribute(serializer, cursor, Names.USER_NOTE)
                    attribute(serializer, cursor, Names.COMMAND_STACK)
                    serializer.endTag("", Names.GAME)
                }
            }
            if (generateFolders && currentFolderId != -1L) {
                serializer.endTag("", Names.FOLDER)
            }
            serializer.endTag("", "opensudoku2")
            serializer.endDocument()
        } catch (e: IOException) {
            Log.e(Const.TAG, "Error while exporting file.", e)
            result.isSuccess = false
            return result
        } finally {
            cursor?.close()
            database?.close()
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

    @Throws(IllegalArgumentException::class, IllegalStateException::class, IOException::class)
    private fun attribute(serializer: XmlSerializer, cursor: Cursor, columnName: String) {
        val value = cursor.getString(cursor.getColumnIndexOrThrow(columnName))
        if (value != null) {
            serializer.attribute("", columnName, value)
        }
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
        val FILE_EXPORT_VERSION: String = "3"
    }
}
