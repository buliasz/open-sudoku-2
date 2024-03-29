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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.gui.fragments.AboutDialogFragment
import org.buliasz.opensudoku2.gui.fragments.AddFolderDialogFragment
import org.buliasz.opensudoku2.gui.fragments.DeleteFolderDialogFragment
import org.buliasz.opensudoku2.gui.fragments.RenameFolderDialogFragment

const val storagePermissionCode = 1

/**
 * List of puzzle's folder. This activity also serves as root activity of application.
 */
class FolderListActivity : ThemedActivity() {
	enum class MenuItems {
		ADD,
		RENAME,
		DELETE,
		ABOUT,
		EXPORT,
		EXPORT_ALL,
		IMPORT,
		SETTINGS;

		val id = ordinal + Menu.FIRST
	}

	private lateinit var importLauncher: ActivityResultLauncher<Intent>
	private lateinit var addFolderDialog: AddFolderDialogFragment
	private lateinit var renameFolderDialog: RenameFolderDialogFragment
	private lateinit var aboutDialog: AboutDialogFragment
	private lateinit var deleteFolderDialog: DeleteFolderDialogFragment
	private lateinit var mAdapter: FolderListRecyclerAdapter
	private lateinit var mDatabase: SudokuDatabase
	private lateinit var recyclerView: RecyclerView
	private lateinit var mMenu: Menu

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.folder_list)
		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT)
		findViewById<View>(R.id.get_more_puzzles_from_os1).setOnClickListener { openWebpage("https://opensudoku.moire.org/") }
		findViewById<View>(R.id.get_more_puzzles_from_sepb).setOnClickListener { openWebpage("https://github.com/grantm/sudoku-exchange-puzzle-bank") }
		findViewById<View>(R.id.get_more_puzzles_from_sudocue).setOnClickListener { openWebpage("https://www.sudocue.net/download.php") }
		mDatabase = SudokuDatabase(applicationContext, true)
		mAdapter = FolderListRecyclerAdapter(this, mDatabase.getFolderList()) { id: Long ->
			val i = Intent(applicationContext, PuzzleListActivity::class.java)
			i.putExtra(Names.FOLDER_ID, id)
			startActivity(i)
		}

		recyclerView = findViewById(R.id.folder_list_recycler)
		recyclerView.adapter = mAdapter
		recyclerView.layoutManager = LinearLayoutManager(this)
		registerForContextMenu(recyclerView)

		val factory = LayoutInflater.from(this)
		aboutDialog = AboutDialogFragment(factory)
		addFolderDialog = AddFolderDialogFragment(factory, mDatabase, ::updateList)
		deleteFolderDialog = DeleteFolderDialogFragment(mDatabase, ::updateList)
		renameFolderDialog = RenameFolderDialogFragment(factory, mDatabase, ::updateList)

		importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				val data: Intent? = result.data
				if (data != null) {
					val uri = data.data
					val i = Intent(this, PuzzleImportActivity::class.java)
					i.setData(uri)
					startActivity(i)
				}
			}
		}

		// show changelog on first run (case when auto folder opening is enabled and a new app version was released)
		Changelog(this).showOnFirstRun()
	}

	private fun openWebpage(url: String) {
		val webpage = Uri.parse(url)
		try {
			Intent(Intent.ACTION_VIEW, webpage).apply {
				startActivity(this)
			}
		} catch (_: ActivityNotFoundException) {
			val mHintClosed = DialogInterface.OnClickListener { _: DialogInterface?, _: Int -> }
			AlertDialog.Builder(this)
				.setIcon(R.drawable.ic_error)
				.setTitle(R.string.error)
				.setMessage("Cannot start Web activity. Please open the webpage manually: $webpage")
				.setPositiveButton(R.string.close, mHintClosed)
				.create()
				.show()
		}
	}

	override fun onStart() {
		super.onStart()
		updateList()
	}

	override fun onResume() {
		super.onResume()
		updateList()
	}

	override fun onDestroy() {
		super.onDestroy()
		mDatabase.close()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)

		// This is our one standard application action -- inserting a
		// new note into the list.
		menu.add(0, MenuItems.ADD.id, 0, R.string.add_folder)
			.setShortcut('3', 'a')
			.setIcon(R.drawable.ic_add)
		menu.add(0, MenuItems.IMPORT.id, 0, R.string.import_title)
			.setShortcut('8', 'i')
			.setIcon(R.drawable.ic_baseline_download)
		menu.add(0, MenuItems.EXPORT_ALL.id, 1, R.string.export_all_folders)
			.setShortcut('7', 'e')
			.setIcon(R.drawable.ic_share)
		menu.add(0, MenuItems.SETTINGS.id, 2, R.string.settings)
			.setShortcut('6', 's')
			.setIcon(R.drawable.ic_settings)
		menu.add(0, MenuItems.ABOUT.id, 2, R.string.about)
			.setShortcut('1', 'h')
			.setIcon(R.drawable.ic_info)


		// Generate any additional actions that can be performed on the
		// overall list.  In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		val intent = Intent(null, intent.data)
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
		menu.addIntentOptions(
			Menu.CATEGORY_ALTERNATIVE, 0, 0, ComponentName(this, FolderListActivity::class.java), null, intent, 0, null
		)
		mMenu = menu
		return true
	}

	override fun onContextItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			MenuItems.EXPORT.id -> {
				val intent = Intent()
				intent.setClass(this, PuzzleExportActivity::class.java)
				intent.putExtra(Names.FOLDER_ID, mAdapter.selectedFolderId)
				startActivity(intent)
				return true
			}

			MenuItems.RENAME.id -> {
				renameFolderDialog.mRenameFolderID = mAdapter.selectedFolderId
				renameFolderDialog.show(supportFragmentManager, "RenameFolderDialog")
				return true
			}

			MenuItems.DELETE.id -> {
				deleteFolderDialog.mDeleteFolderID = mAdapter.selectedFolderId
				deleteFolderDialog.show(supportFragmentManager, "DeleteFolderDialog")
				return true
			}
		}
		return false
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		val intent: Intent
		when (item.itemId) {
			MenuItems.ADD.id -> {
				addFolderDialog.show(supportFragmentManager, "AddFolderDialog")
				return true
			}

			MenuItems.IMPORT.id -> {
				intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
				intent.addCategory(Intent.CATEGORY_OPENABLE)
				intent.setType("*/*")
				importLauncher.launch(intent)
				return true
			}

			MenuItems.EXPORT_ALL.id -> {
				intent = Intent()
				intent.setClass(this, PuzzleExportActivity::class.java)
				intent.putExtra(Names.FOLDER_ID, PuzzleExportActivity.ALL_IDS)
				startActivity(intent)
				return true
			}

			MenuItems.SETTINGS.id -> {
				intent = Intent()
				intent.setClass(this, GameSettingsActivity::class.java)
				startActivity(intent)
				return true
			}

			MenuItems.ABOUT.id -> {
				aboutDialog.show(supportFragmentManager, "AboutDialog")
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (requestCode == storagePermissionCode) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				onOptionsItemSelected(mMenu.findItem(MenuItems.IMPORT.id))
			} else {
				Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show()
			}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}

	private fun updateList() {
		mAdapter.updateFoldersList(mDatabase.getFolderList(true))
	}
}
