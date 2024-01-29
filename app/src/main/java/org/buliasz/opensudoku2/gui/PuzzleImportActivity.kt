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

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.gui.importing.AbstractImportTask
import org.buliasz.opensudoku2.gui.importing.OpenSudoku1ImportTask
import org.buliasz.opensudoku2.gui.importing.OpenSudoku2ImportTask
import org.buliasz.opensudoku2.gui.importing.SdmImportTask
import org.buliasz.opensudoku2.gui.importing.SepbImportTask
import org.buliasz.opensudoku2.gui.importing.SepbRegex
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException

/**
 * This activity is responsible for importing puzzles from various sources
 * (web, file, .opensudoku2, .sdm, extras).
 */
class PuzzleImportActivity : ThemedActivity() {
	private val mOnImportFinishedListener = object : AbstractImportTask.OnImportFinishedListener {
		override fun onImportFinished(importSuccessful: Boolean, folderId: Long) {
			if (importSuccessful) {
				if (folderId != -1L) {
					// one folder was imported, show this folder automatically
					val i = Intent(this@PuzzleImportActivity, PuzzleListActivity::class.java)
					i.putExtra(Names.FOLDER_ID, folderId)
					startActivity(i)
				}
			}
			finish()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportRequestWindowFeature(Window.FEATURE_LEFT_ICON)
		setContentView(R.layout.import_puzzle)
		setTitle(R.string.import_title)
		window.setFeatureDrawableResource(
			Window.FEATURE_LEFT_ICON,
			R.mipmap.ic_launcher_icon
		)
		val importTask: AbstractImportTask
		val intent = intent
		val action = intent.action

		val dataUri: Uri? = if (action == null) {
			intent.data
		} else if (action.equals("android.intent.action.SEND", ignoreCase = true)) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
			} else {
				@Suppress("DEPRECATION")
				intent.extras!![Intent.EXTRA_STREAM] as Uri?
			}
		} else if (action.equals("android.intent.action.VIEW", ignoreCase = true)) {
			intent.data
		} else {
			finish()
			return
		}

		if (dataUri == null) {
			Log.e(TAG, "No data provided, exiting.")
			finish()
			return
		}

		Log.v(TAG, "$dataUri")
		var streamReader: InputStreamReader? = null
		if (dataUri.scheme == "content") {
			try {
				streamReader = InputStreamReader(contentResolver.openInputStream(dataUri))
			} catch (e: FileNotFoundException) {
				e.printStackTrace()
			}
		} else {
			val juri: URI
			Log.v(TAG, "$dataUri")
			try {
				juri = URI(
					dataUri.scheme, dataUri.schemeSpecificPart, dataUri.fragment
				)
				streamReader = InputStreamReader(juri.toURL().openStream())
			} catch (e: URISyntaxException) {
				e.printStackTrace()
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}

		if (streamReader == null) {
			return
		}

		val cBuffer = CharArray(512)
		val numberOfCharactersRead: Int
		try {
			// read first 512 bytes to check the type of file
			numberOfCharactersRead = streamReader.read(cBuffer, 0, 512)
			streamReader.close()
		} catch (e: IOException) {
			return
		}

		if (numberOfCharactersRead < 81) {
			// At least one full 9x9 game needed in case of SDM
			return
		}

		val cBufferStr = String(cBuffer)
		@Suppress("RegExpSimplifiable")
		importTask = if (cBufferStr.contains("<opensudoku2")) { // Recognized OpenSudoku2 file
			OpenSudoku2ImportTask(dataUri)
		} else if (cBufferStr.contains("<opensudoku")) { // Recognized OpenSudoku1 file
			OpenSudoku1ImportTask(dataUri)
		} else if (cBufferStr.matches("""[.0-9\n\r]{$numberOfCharactersRead}""".toRegex())) { // Recognized Sudoku SDM file
			SdmImportTask(dataUri)
		} else if (SepbRegex.containsMatchIn(cBufferStr)) { // Recognized Sudoku Exchange "Puzzle Bank" file
			SepbImportTask(dataUri)
		} else {
			Log.e(TAG, "Unknown type of data provided (mime-type=${intent.type}; uri=$dataUri), exiting.")
			Toast.makeText(this, R.string.invalid_format, Toast.LENGTH_LONG).show()
			finish()
			return
		}

		with(ProgressUpdater(applicationContext, findViewById(R.id.progressBar))) {
			progressTextView = findViewById(R.id.progressText)
			progressStringRes = R.string.importing
			progressStringResMaxOnly = R.string.import_puzzles_found
			progressStringResNoValues = R.string.checking_duplicates
			this
		}.use { progressUpdater ->
			CoroutineScope(Dispatchers.IO).launch {
				importTask.doInBackground(applicationContext, mOnImportFinishedListener, supportFragmentManager, progressUpdater)
			}
		}
	}

	companion object {
		private const val TAG = "ImportPuzzleActivity"
	}
}
