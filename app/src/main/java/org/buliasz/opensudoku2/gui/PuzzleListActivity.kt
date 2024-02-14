/*
 * This file is part of Open Sudoku 2 - an open-source Sudoku game.
 * Copyright (C) 2009-2024 by original authors.
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

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.fragments.DeletePuzzleDialogFragment
import org.buliasz.opensudoku2.gui.fragments.EditUserNoteDialogFragment
import org.buliasz.opensudoku2.gui.fragments.FilterDialogFragment
import org.buliasz.opensudoku2.gui.fragments.ResetAllDialogFragment
import org.buliasz.opensudoku2.gui.fragments.ResetPuzzleDialogFragment
import org.buliasz.opensudoku2.gui.fragments.SortDialogFragment

class PuzzleListActivity : ThemedActivity() {
	enum class MenuItems {
		INSERT,
		EDIT,
		DELETE,
		PLAY,
		RESET,
		RESET_ALL,
		EDIT_NOTE,
		FILTER,
		SORT,
		FOLDERS,
		SETTINGS,
		EXPORT_GAME,
		EXPORT_FOLDER;

		val id = ordinal + Menu.FIRST
	}

	private lateinit var editUserNoteDialog: EditUserNoteDialogFragment
	private lateinit var resetPuzzleDialog: ResetPuzzleDialogFragment
	private lateinit var deletePuzzleDialog: DeletePuzzleDialogFragment
	private lateinit var filterDialog: FilterDialogFragment
	private lateinit var sortDialog: SortDialogFragment
	private lateinit var resetAllDialog: ResetAllDialogFragment
	private var mFolderID: Long = 0

	// input parameters for dialogs
	private lateinit var mListFilter: PuzzleListFilter
	private lateinit var mListSorter: PuzzleListSorter

	private lateinit var mFilterStatus: TextView
	private lateinit var recyclerView: RecyclerView
	private lateinit var mAdapter: PuzzleListRecyclerAdapter

	private lateinit var mDatabase: SudokuDatabase
	private lateinit var mFolderDetailLoader: FolderDetailLoader

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.sudoku_list)
		mFilterStatus = findViewById(R.id.filter_status)
		setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT)
		mDatabase = SudokuDatabase(applicationContext, true)
		mFolderDetailLoader = FolderDetailLoader(applicationContext)
		val intent = intent

		mFolderID = if (intent.hasExtra(Names.FOLDER_ID)) {
			intent.getLongExtra(Names.FOLDER_ID, 0)
		} else {
			Log.d(TAG, "No '${Names.FOLDER_ID}' extra provided, exiting.")
			finish()
			return
		}

		val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
		with(PuzzleListFilter(applicationContext)) {
			showStateNotStarted = settings.getBoolean(FILTER_STATE_NOT_STARTED, true)
			showStatePlaying = settings.getBoolean(FILTER_STATE_PLAYING, true)
			showStateCompleted = settings.getBoolean(FILTER_STATE_SOLVED, true)
			mListFilter = this
		}

		with(PuzzleListSorter()) {
			sortType = settings.getInt(SORT_TYPE, PuzzleListSorter.SORT_BY_CREATED)
			isAscending = (settings.getBoolean(SORT_ORDER, false))
			mListSorter = this
		}

		updateTitle()
		updateFilterStatus()

		val puzzlesCursor = mDatabase.getPuzzleListCursor(mFolderID, mListFilter, mListSorter.sortOrder)
		mAdapter = PuzzleListRecyclerAdapter(this, puzzlesCursor, ::playSudoku)

		recyclerView = findViewById(R.id.puzzle_list_recycler)
		recyclerView.adapter = mAdapter
		recyclerView.layoutManager = LinearLayoutManager(this)
		registerForContextMenu(recyclerView)

		resetPuzzleDialog = ResetPuzzleDialogFragment(mDatabase, ::updateList)
		deletePuzzleDialog = DeletePuzzleDialogFragment(mDatabase, settings, ::updateList)
		filterDialog = FilterDialogFragment(mListFilter, settings, ::updateList)
		sortDialog = SortDialogFragment(mListSorter, settings, ::updateList)
		resetAllDialog = ResetAllDialogFragment(mDatabase, mFolderID, ::updateList)

		val factory = LayoutInflater.from(this)
		editUserNoteDialog = EditUserNoteDialogFragment(factory, mDatabase, ::updateList)
	}

	override fun onDestroy() {
		super.onDestroy()
		mDatabase.close()
		mFolderDetailLoader.close()
		mAdapter.close()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putLong("DeletePuzzleID", deletePuzzleDialog.puzzleID)
		outState.putLong("ResetPuzzleID", resetPuzzleDialog.puzzleID)
		outState.putLong("EditNotePuzzleID", editUserNoteDialog.puzzleId)
	}

	override fun onRestoreInstanceState(state: Bundle) {
		super.onRestoreInstanceState(state)
		deletePuzzleDialog.puzzleID = state.getLong("DeletePuzzleID")
		resetPuzzleDialog.puzzleID = state.getLong("ResetPuzzleID")
		editUserNoteDialog.puzzleId = state.getLong("EditNotePuzzleID")
	}

	override fun onResume() {
		super.onResume()
		updateTitle()
		updateList()
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		// if there is no activity in history and back button was pressed, go
		// to FolderListActivity, which is the root activity.
		if (isTaskRoot && keyCode == KeyEvent.KEYCODE_BACK) {
			val i = Intent()
			i.setClass(this, FolderListActivity::class.java)
			startActivity(i)
			finish()
			return true
		}
		return super.onKeyDown(keyCode, event)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		super.onCreateOptionsMenu(menu)

		// This is our one standard application action -- inserting a
		// new note into the list.
		menu.add(0, MenuItems.FOLDERS.id, 0, R.string.folders).setShortcut('1', 'f')
			.setIcon(R.drawable.ic_folder)
		menu.add(0, MenuItems.INSERT.id, 1, R.string.add_puzzle).setShortcut('2', 'a')
			.setIcon(R.drawable.ic_add)
		menu.add(0, MenuItems.FILTER.id, 2, R.string.filter).setShortcut('3', 'f')
			.setIcon(R.drawable.ic_view)
		menu.add(0, MenuItems.SORT.id, 3, R.string.sort).setShortcut('4', 'o')
			.setIcon(R.drawable.ic_sort)
		menu.add(0, MenuItems.RESET_ALL.id, 4, R.string.reset_all_puzzles).setShortcut('5', 'r')
			.setIcon(R.drawable.ic_undo)
		menu.add(0, MenuItems.EXPORT_FOLDER.id, 5, R.string.export_folder).setShortcut('6', 'e')
			.setIcon(R.drawable.ic_share)
		menu.add(0, MenuItems.SETTINGS.id, 6, R.string.settings).setShortcut('7', 's')
			.setIcon(R.drawable.ic_settings)

		// Generate any additional actions that can be performed on the
		// overall list. In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		val intent = Intent(null, intent.data)
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
		menu.addIntentOptions(
			Menu.CATEGORY_ALTERNATIVE, 0, 0,
			ComponentName(this, PuzzleListActivity::class.java), null,
			intent, 0, null
		)
		return true
	}

	override fun onContextItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			MenuItems.PLAY.id -> {
				playSudoku(mAdapter.selectedGameId)
				return true
			}

			MenuItems.EDIT.id -> {
				val i = Intent(this, PuzzleEditActivity::class.java)
				i.setAction(Intent.ACTION_EDIT)
				i.putExtra(Names.PUZZLE_ID, mAdapter.selectedGameId)
				startActivity(i)
				return true
			}

			MenuItems.DELETE.id -> {
				deletePuzzleDialog.puzzleID = mAdapter.selectedGameId
				deletePuzzleDialog.show(supportFragmentManager, "DeletePuzzleDialog")
				return true
			}

			MenuItems.EDIT_NOTE.id -> {
				editUserNoteDialog.puzzleId = mAdapter.selectedGameId
				editUserNoteDialog.currentValue = mDatabase.getPuzzle(editUserNoteDialog.puzzleId)!!.userNote
				editUserNoteDialog.show(supportFragmentManager, "EditUserNoteDialog")
				return true
			}

			MenuItems.RESET.id -> {
				resetPuzzleDialog.puzzleID = mAdapter.selectedGameId
				resetPuzzleDialog.show(supportFragmentManager, "ResetPuzzleDialog")
				return true
			}

			MenuItems.EXPORT_GAME.id -> {
				val intent = Intent()
				intent.setClass(this, PuzzleExportActivity::class.java)
				intent.putExtra(Names.FOLDER_ID, mFolderID)
				intent.putExtra(Names.ID, mAdapter.selectedGameId)
				startActivity(intent)
				return true
			}
		}
		return false
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		val i: Intent
		when (item.itemId) {
			MenuItems.INSERT.id -> {
				// Launch activity to insert a new item
				i = Intent(this, PuzzleEditActivity::class.java)
				i.setAction(Intent.ACTION_INSERT)
				i.putExtra(Names.FOLDER_ID, mFolderID)    // we need to know folder in which the new puzzle will be stored
				startActivity(i)
				return true
			}

			MenuItems.SETTINGS.id -> {
				i = Intent(this, GameSettingsActivity::class.java)
				startActivity(i)
				return true
			}

			MenuItems.FILTER.id -> {
				filterDialog.show(supportFragmentManager, "FilterDialog")
				return true
			}

			MenuItems.SORT.id -> {
				sortDialog.show(supportFragmentManager, "SortDialog")
				return true
			}

			MenuItems.FOLDERS.id -> {
				i = Intent(this, FolderListActivity::class.java)
				startActivity(i)
				finish()
				return true
			}

			MenuItems.RESET_ALL.id -> {
				resetAllDialog.show(supportFragmentManager, "ResetAllDialog")
				return true
			}

			MenuItems.EXPORT_FOLDER.id -> {
				val intent = Intent()
				intent.setClass(this, PuzzleExportActivity::class.java)
				intent.putExtra(Names.FOLDER_ID, mFolderID)
				startActivity(intent)
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	/**
	 * Updates whole list.
	 */
	private fun updateList() {
		updateTitle()
		updateFilterStatus()
		mAdapter.updateGameList(mDatabase.getPuzzleListCursor(mFolderID, mListFilter, mListSorter.sortOrder))
	}

	private fun updateFilterStatus() {
		if (mListFilter.showStateCompleted && mListFilter.showStateNotStarted && mListFilter.showStatePlaying) {
			mFilterStatus.visibility = View.GONE
		} else {
			mFilterStatus.text = getString(R.string.filter_active, mListFilter)
			mFilterStatus.visibility = View.VISIBLE
		}
	}

	private fun updateTitle() {
		val folder = mDatabase.getFolderInfo(mFolderID)
		title = folder?.name ?: ""
		mFolderDetailLoader.loadDetailAsync(mFolderID) { folderInfo ->
			runOnUiThread { title = folderInfo.name + ": " + folderInfo.getDetail(applicationContext) }
		}
	}

	private fun playSudoku(puzzleID: Long) {
		val i = Intent(this@PuzzleListActivity, SudokuPlayActivity::class.java)
		i.putExtra(Names.PUZZLE_ID, puzzleID)
		startActivity(i)
	}

	companion object {
		const val FILTER_STATE_NOT_STARTED = "filter" + SudokuGame.GAME_STATE_NOT_STARTED
		const val FILTER_STATE_PLAYING = "filter" + SudokuGame.GAME_STATE_PLAYING
		const val FILTER_STATE_SOLVED = "filter" + SudokuGame.GAME_STATE_COMPLETED
		const val SORT_TYPE = "sort_type"
		const val SORT_ORDER = "sort_order"
		private const val TAG = "PuzzleListActivity"
	}
}
