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
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.db.extractSudokuGameFromCursorRow
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.PuzzleExportActivity.Companion.ALL_IDS
import org.buliasz.opensudoku2.utils.Const
import org.xmlpull.v1.XmlSerializer
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer

class FileExportTask {
	/**
	 * Occurs when export is finished.
	 */
	var onExportFinishedListener: (suspend (FileExportTaskResult?) -> Unit)? = null

	internal suspend fun exportToFile(context: Context, vararg params: FileExportTaskParams?) {
		withContext(Dispatchers.IO) {
			for (param in params) {
				val result = saveToFile(param!!, context)
				launch {
					onExportFinishedListener?.invoke(result)
				}
			}
		}
	}

	private fun saveToFile(exportParams: FileExportTaskParams, context: Context): FileExportTaskResult {
		require(exportParams.folderId != null) { "'folderId' param must be set" }
		requireNotNull(exportParams.fileOutputStream) { "Output stream cannot be null" }
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
				serializeFolders(db, serializer, exportParams.folderId!!, exportParams.puzzleId ?: ALL_IDS)
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
		return result
	}

	private fun serializeFolders(db: SudokuDatabase, serializer: XmlSerializer, folderId: Long, gameId: Long = ALL_IDS) {
		val folderList: List<FolderInfo> = if (folderId == -1L) db.getFolderList() else listOf(db.getFolderInfo(folderId)!!)
		for (folder in folderList) {
			serializer.startTag("", Names.FOLDER)
			serializer.attribute("", Names.FOLDER_NAME, folder.name)
			serializer.attribute("", Names.FOLDER_CREATED, folder.created.toString())

			if (gameId == ALL_IDS) {
				db.getPuzzleListCursor(folder.id, null, null).use { cursor ->
					if (cursor.moveToFirst()) {
						while (!cursor.isAfterLast) {
							serializeGame(serializer, extractSudokuGameFromCursorRow(cursor))
							cursor.moveToNext()
						}
					}
				}
			} else {
				serializeGame(serializer, db.getPuzzle(gameId)!!)
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

	companion object {
		val FILE_EXPORT_VERSION: String get() = "3"
	}
}
