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
import org.buliasz.opensudoku2.db.SudokuInvalidFormatException
import org.buliasz.opensudoku2.db.forEach
import org.buliasz.opensudoku2.db.getPuzzleListCursor
import org.buliasz.opensudoku2.db.insertPuzzle
import org.buliasz.opensudoku2.db.originalValues
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**
 * Handles import of Sudoku Exchange "Puzzle Bank" files (see https://github.com/grantm/sudoku-exchange-puzzle-bank)
 */
val SepbRegex = """\s*\w+\s+(?<cellValues>\d{81})\s+\d+.\d+\s*""".toRegex()

class SepbImportTask(private val mUri: Uri) : AbstractImportTask() {
	@Throws(SudokuInvalidFormatException::class)
	override suspend fun processImport(context: Context) {
		val folderId = importFolder(mUri.lastPathSegment ?: "UNKNOWN")
		val isr: InputStreamReader = if (mUri.scheme == "content") {
			val contentResolver = context.contentResolver
			InputStreamReader(contentResolver.openInputStream(mUri))
		} else {
			val url = URL("$mUri")
			InputStreamReader(url.openStream())
		}

		val newPuzzleMap = HashMap<String, Boolean>()
		BufferedReader(isr).useLines {
			it.forEach { inputLine ->
				SepbRegex.find(inputLine)?.also { newPuzzleMap[it.groups["cellValues"]?.value!!] = true }
				mProgressUpdate(0, newPuzzleMap.size)
			}
		}

		mDatabase.writable.use { db ->
			db.getPuzzleListCursor().forEach { c -> newPuzzleMap[c.originalValues] = false }
			var index = 0
			for ((values, isNew) in newPuzzleMap) {
				index += 1
				mProgressUpdate(index, newPuzzleMap.size)
				if (isNew) {
					db.insertPuzzle(values, folderId)
					importedCount += 1
				} else {
					duplicatesCount += 1
				}
			}
		}
	}
}
