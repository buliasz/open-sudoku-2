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
import org.buliasz.opensudoku2.db.originalValues
import org.buliasz.opensudoku2.utils.getFileName
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

/**
 * Handles import of Sudoku Exchange "Puzzle Bank" files (see https://github.com/grantm/sudoku-exchange-puzzle-bank)
 */
val SepbRegex = """\s*\w+\s+(?<cellValues>\d{81})\s+\d+.\d+\s*""".toRegex()

class SepbImportTask(private val mUri: Uri) : AbstractImportTask() {
	private lateinit var folderName: String
	var folderId: Long = 0L
		get() {
			if (field == 0L) field = importFolder(folderName)
			return field
		}

	@Throws(SudokuInvalidFormatException::class)
	override suspend fun processImport(context: Context) {
		folderName = mUri.getFileName(context.contentResolver) ?: "Imported Puzzles"
		val isr: InputStreamReader = if (mUri.scheme == "content") {
			val contentResolver = context.contentResolver
			InputStreamReader(contentResolver.openInputStream(mUri))
		} else {
			val url = URL("$mUri")
			InputStreamReader(url.openStream())
		}

		val newPuzzles = HashSet<String>()
		BufferedReader(isr).useLines { lineSequence ->
			lineSequence.forEach { inputLine ->
				SepbRegex.find(inputLine)?.also { newPuzzles.add(it.groups["cellValues"]?.value!!) }
				mProgressUpdate.maxValue = newPuzzles.size
			}
		}

		// skip existing puzzles and count duplicates
		mDatabase.getPuzzleListCursor().forEach { c -> if (newPuzzles.remove(c.originalValues)) duplicatesCount += 1 }

		mProgressUpdate.currentValue = 0
		mProgressUpdate.maxValue = newPuzzles.size
		for (values in newPuzzles) {
			mProgressUpdate.currentValue += 1
			mDatabase.insertPuzzle(values, folderId)
			importedCount += 1
		}
	}
}
