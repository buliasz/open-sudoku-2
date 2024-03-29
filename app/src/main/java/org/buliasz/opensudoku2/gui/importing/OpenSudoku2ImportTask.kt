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
import android.net.Uri
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuInvalidFormatException
import org.buliasz.opensudoku2.db.forEach
import org.buliasz.opensudoku2.db.id
import org.buliasz.opensudoku2.db.originalValues
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.exporting.FileExportTask
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI

/**
 * Handles import of application/x-opensudoku2 or .opensudoku2 files.
 */
class OpenSudoku2ImportTask(private val mUri: Uri) : AbstractImportTask() {
	@Throws(SudokuInvalidFormatException::class)
	override suspend fun processImport(context: Context) {
		val streamReader: InputStreamReader = if (mUri.scheme == "content") {
			val contentResolver = context.contentResolver
			InputStreamReader(contentResolver.openInputStream(mUri))
		} else {
			val juri = URI(mUri.scheme, mUri.schemeSpecificPart, mUri.fragment)
			InputStreamReader(juri.toURL().openStream())
		}
		streamReader.use { importFromXml(context, it) }
	}


	@Throws(SudokuInvalidFormatException::class)
	private suspend fun importFromXml(context: Context, reader: Reader) {
		val inBR = BufferedReader(reader)
		val parserFactory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
		parserFactory.isNamespaceAware = false
		val parser: XmlPullParser = parserFactory.newPullParser()
		parser.setInput(inBR)
		var eventType = parser.eventType
		var rootTag: String

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				rootTag = parser.name
				if (rootTag == "opensudoku2") {
					when (parser.getAttributeValue(null, "version")) {
						FileExportTask.FILE_EXPORT_VERSION -> {
							importOpenSudoku2Puzzles(parser) // exceptions are caught in the main import method
						}

						else -> {
							importError = context.getString(R.string.invalid_format)
						}
					}
				} else {
					importError = context.getString(R.string.invalid_format)
					return
				}
			}
			eventType = parser.next()
		}
	}

	@Throws(XmlPullParserException::class, IOException::class, SudokuInvalidFormatException::class)
	private suspend fun importOpenSudoku2Puzzles(parser: XmlPullParser) {
		var eventType = parser.eventType
		var lastTag: String
		var lastFolderId: Long = 0
		val existingPuzzles = HashMap<String, Long>()

		mDatabase.getPuzzleListCursor().forEach { c -> existingPuzzles[c.originalValues] = c.id }
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				lastTag = parser.name
				if (lastTag == Names.FOLDER) {
					val name = parser.getAttributeValue(null, Names.FOLDER_NAME)
					val created = parser.getAttributeValue(null, Names.FOLDER_CREATED).toLong()
					lastFolderId = importFolder(name, created)
				} else if (lastTag == Names.GAME) {
					with(SudokuGame()) {
						created = parser.getAttributeValue(null, Names.CREATED).toLong()
						cells = CellCollection.deserialize(parser.getAttributeValue(null, Names.CELLS_DATA))
						lastPlayed = parser.getAttributeValue(null, Names.LAST_PLAYED).toLong()
						state = parser.getAttributeValue(null, Names.STATE).toInt()
						time = parser.getAttributeValue(null, Names.TIME).toLong()
						userNote = parser.getAttributeValue(null, Names.USER_NOTE)
						folderId = lastFolderId
						commandStack.deserialize(parser.getAttributeValue(null, Names.COMMAND_STACK))
						id = existingPuzzles[cells.originalValues] ?: -1
						if (id < 0L) {
							mDatabase.insertPuzzle(this)
							importedCount += 1
						} else if (state != SudokuGame.GAME_STATE_NOT_STARTED) {
							mDatabase.updatePuzzle(this)    // those saved may be in progress, update makes sense for puzzles exported by by this app
							updatedCount += 1
						} else {
							duplicatesCount += 1
						}
						mProgressUpdate.maxValue = importedCount + updatedCount + duplicatesCount
					}
				}
			}
			eventType = parser.next()
		}
	}
}
