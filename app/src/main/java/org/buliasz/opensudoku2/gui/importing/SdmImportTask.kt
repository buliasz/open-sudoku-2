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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL

/**
 * Handles import of .sdm files (see http://sudocue.net/download.php).
 */
class SdmImportTask(private val mUri: Uri) : AbstractImportTask() {
	@Throws(SudokuInvalidFormatException::class)
	override fun processImport(context: Context) {
		importFolder(mUri.lastPathSegment)
		val isr: InputStreamReader
		try {
			isr = if (mUri.scheme == "content") {
				val contentResolver = context.contentResolver
				InputStreamReader(contentResolver.openInputStream(mUri))
			} else {
				val url = URL(mUri.toString())
				InputStreamReader(url.openStream())
			}
			BufferedReader(isr).use { br ->
				var s: String
				while (br.readLine().also { s = it } != null) {
					if (s != "") {
						if (s.contains(".")) {
							s = s.replace(".", "0")
						}
						importGame(s)
					}
				}
			}
		} catch (e: MalformedURLException) {
			throw RuntimeException(e)
		} catch (e: IOException) {
			throw RuntimeException(e)
		}
	}
}
