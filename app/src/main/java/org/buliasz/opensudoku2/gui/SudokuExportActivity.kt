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
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.DateFormat
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.gui.exporting.FileExportTask
import org.buliasz.opensudoku2.gui.exporting.FileExportTask.OnExportFinishedListener
import org.buliasz.opensudoku2.gui.exporting.FileExportTaskParams
import org.buliasz.opensudoku2.gui.exporting.FileExportTaskResult
import java.io.FileNotFoundException
import java.util.Date

class SudokuExportActivity : ThemedActivity() {
    private var mFileExportTask: FileExportTask? = null
    private var mExportParams: FileExportTaskParams? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sudoku_export)
        mFileExportTask = FileExportTask()
        mExportParams = FileExportTaskParams()
        var intent = intent
        if (intent.hasExtra(Names.FOLDER_ID)) {
            mExportParams!!.folderID = intent.getLongExtra(Names.FOLDER_ID, ALL_FOLDERS)
        } else {
            Log.d(TAG, "No 'FOLDER_ID' extra provided, exiting.")
            finish()
            return
        }
        val fileName: String
        val timestamp = DateFormat.format("yyyy-MM-dd-HH-mm-ss", Date()).toString()
        if (mExportParams!!.folderID == -1L) {
            fileName = "all-folders-$timestamp"
        } else {
            val database = SudokuDatabase(applicationContext)
            val folder = database.getFolderInfo(mExportParams!!.folderID!!)
            if (folder == null) {
                Log.d(TAG, "Folder with id ${mExportParams!!.folderID} not found, exiting.")
                finish()
                return
            }
            fileName = folder.name + "-" + timestamp
            database.close()
        }
        intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/x-opensudoku2")
        intent.putExtra(Intent.EXTRA_TITLE, "$fileName.opensudoku2")
        startActivityForResult(intent, CREATE_FILE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                val uri = data.data
                startExportToFileTask(uri)
            }
        } else if (requestCode == CREATE_FILE && resultCode == RESULT_CANCELED) {
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startExportToFileTask(uri: Uri?) {
        mFileExportTask!!.onExportFinishedListener = object : OnExportFinishedListener {
            override fun onExportFinished(result: FileExportTaskResult?) {
                if (result!!.isSuccess) {
                    Toast.makeText(
                        this@SudokuExportActivity, getString(
                            R.string.puzzles_have_been_exported, result.filename
                        ), Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@SudokuExportActivity, getString(
                            R.string.unknown_export_error
                        ), Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }
        }
        try {
            mExportParams!!.fileOutputStream = contentResolver.openOutputStream(uri!!)
            val cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                mExportParams!!.filename = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                cursor.close()
            }
        } catch (e: FileNotFoundException) {
            Toast.makeText(
                this@SudokuExportActivity, getString(
                    R.string.unknown_export_error
                ), Toast.LENGTH_LONG
            ).show()
        }
        CoroutineScope(Dispatchers.IO).launch {
            mFileExportTask!!.exportToFile(this@SudokuExportActivity, mExportParams)
        }
    }

    companion object {
        /**
         * Id of folder to export. If -1, all folders will be exported.
         */
        const val ALL_FOLDERS: Long = -1
        private val TAG = SudokuExportActivity::class.java.simpleName
        private const val CREATE_FILE = 1
    }
}
