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
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.fragments.DeletePuzzleDialogFragment
import org.buliasz.opensudoku2.gui.fragments.EditUserNoteDialogFragment
import org.buliasz.opensudoku2.gui.fragments.FilterDialogFragment
import org.buliasz.opensudoku2.gui.fragments.ResetAllDialogFragment
import org.buliasz.opensudoku2.gui.fragments.ResetPuzzleDialogFragment
import org.buliasz.opensudoku2.gui.fragments.SortDialogFragment

class SudokuListActivity : ThemedActivity() {
    private lateinit var editUserNoteDialog: EditUserNoteDialogFragment
    private lateinit var resetPuzzleDialog: ResetPuzzleDialogFragment
    private lateinit var deletePuzzleDialog: DeletePuzzleDialogFragment
    private lateinit var filterDialog: FilterDialogFragment
    private lateinit var sortDialog: SortDialogFragment
    private lateinit var resetAllDialog: ResetAllDialogFragment
    private var mFolderID: Long = 0

    // input parameters for dialogs
    private lateinit var mListFilter: SudokuListFilter
    private lateinit var mListSorter: SudokuListSorter

    private lateinit var mFilterStatus: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var mAdapter: SudokuListRecyclerAdapter

    private lateinit var mDatabase: SudokuDatabase
    private lateinit var mFolderDetailLoader: FolderDetailLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sudoku_list)
        mFilterStatus = findViewById(R.id.filter_status)
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT)
        mDatabase = SudokuDatabase(applicationContext)
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
        with(SudokuListFilter(applicationContext)) {
            showStateNotStarted = settings.getBoolean(FILTER_STATE_NOT_STARTED, true)
            showStatePlaying = settings.getBoolean(FILTER_STATE_PLAYING, true)
            showStateCompleted = settings.getBoolean(FILTER_STATE_SOLVED, true)
            mListFilter = this
        }

        with(SudokuListSorter()) {
            sortType = settings.getInt(SORT_TYPE, SudokuListSorter.SORT_BY_CREATED)
            isAscending = (settings.getBoolean(SORT_ORDER, false))
            mListSorter = this
        }

        updateTitle()
        updateFilterStatus()

        val games = mDatabase.getSudokuGameList(mFolderID, mListFilter, mListSorter)
        mAdapter = SudokuListRecyclerAdapter(this, games, ::playSudoku)

        recyclerView = findViewById(R.id.sudoku_list_recycler)
        recyclerView.adapter = mAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        registerForContextMenu(recyclerView)

        resetPuzzleDialog = ResetPuzzleDialogFragment(mDatabase, ::updateList)
        deletePuzzleDialog = DeletePuzzleDialogFragment(mDatabase, settings, ::updateList)
        filterDialog = FilterDialogFragment(mListFilter, settings, ::updateList)
        sortDialog = SortDialogFragment(mListSorter, settings, ::updateList)
        resetAllDialog = ResetAllDialogFragment(mDatabase, mFolderID, mListSorter, ::updateList)

        val factory = LayoutInflater.from(this)
        editUserNoteDialog = EditUserNoteDialogFragment(factory, mDatabase, ::updateList)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDatabase.close()
        mFolderDetailLoader.destroy()
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
        menu.add(0, MENU_ITEM_FOLDERS, 0, R.string.folders).setShortcut('1', 'f')
            .setIcon(R.drawable.ic_folder)
        menu.add(0, MENU_ITEM_INSERT, 1, R.string.add_sudoku).setShortcut('1', 'a')
            .setIcon(R.drawable.ic_add)
        menu.add(0, MENU_ITEM_FILTER, 2, R.string.filter).setShortcut('2', 'f')
            .setIcon(R.drawable.ic_view)
        menu.add(0, MENU_ITEM_SORT, 2, R.string.sort).setShortcut('2', 'o')
            .setIcon(R.drawable.ic_sort)
        menu.add(0, MENU_ITEM_RESET_ALL, 3, R.string.reset_all_puzzles).setShortcut('3', 'r')
            .setIcon(R.drawable.ic_undo)
        menu.add(0, MENU_ITEM_SETTINGS, 4, R.string.settings).setShortcut('4', 's')
            .setIcon(R.drawable.ic_settings)
        // I'm not sure this one is ready for release
        menu.add(0, MENU_ITEM_GENERATE, 3, R.string.generate_sudoku).setShortcut('4', 'g')
            .setIcon(R.drawable.ic_add)

        // Generate any additional actions that can be performed on the
        // overall list. In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        val intent = Intent(null, intent.data)
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
        menu.addIntentOptions(
            Menu.CATEGORY_ALTERNATIVE, 0, 0,
            ComponentName(this, SudokuListActivity::class.java), null,
            intent, 0, null
        )
        return true
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_ITEM_PLAY -> {
                playSudoku(mAdapter.selectedGameId)
                return true
            }

            MENU_ITEM_EDIT -> {
                val i = Intent(this, SudokuEditActivity::class.java)
                i.setAction(Intent.ACTION_EDIT)
                i.putExtra(SudokuEditActivity.EXTRA_SUDOKU_ID, mAdapter.selectedGameId)
                startActivity(i)
                return true
            }

            MENU_ITEM_DELETE -> {
                deletePuzzleDialog.puzzleID = mAdapter.selectedGameId
                deletePuzzleDialog.show(supportFragmentManager, "DeletePuzzleDialog")
                return true
            }

            MENU_ITEM_EDIT_NOTE -> {
                editUserNoteDialog.puzzleId = mAdapter.selectedGameId
                editUserNoteDialog.currentValue = mDatabase.getSudoku(editUserNoteDialog.puzzleId)?.userNote ?: ""
                editUserNoteDialog.show(supportFragmentManager, "EditUserNoteDialog")
                return true
            }

            MENU_ITEM_RESET -> {
                resetPuzzleDialog.puzzleID = mAdapter.selectedGameId
                resetPuzzleDialog.show(supportFragmentManager, "ResetPuzzleDialog")
                return true
            }
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val i: Intent
        when (item.itemId) {
            MENU_ITEM_INSERT -> {
                // Launch activity to insert a new item
                i = Intent(this, SudokuEditActivity::class.java)
                i.setAction(Intent.ACTION_INSERT)
                i.putExtra(Names.FOLDER_ID, mFolderID)
                startActivity(i)
                return true
            }

            MENU_ITEM_SETTINGS -> {
                i = Intent(this, GameSettingsActivity::class.java)
                startActivity(i)
                return true
            }

            MENU_ITEM_FILTER -> {
                filterDialog.show(supportFragmentManager, "FilterDialog")
                return true
            }

            MENU_ITEM_SORT -> {
                sortDialog.show(supportFragmentManager, "SortDialog")
                return true
            }

            MENU_ITEM_FOLDERS -> {
                i = Intent(this, FolderListActivity::class.java)
                startActivity(i)
                finish()
                return true
            }

            MENU_ITEM_RESET_ALL -> {
                resetAllDialog.show(supportFragmentManager, "ResetAllDialog")
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
        mAdapter.updateGameList(mDatabase.getSudokuGameList(mFolderID, mListFilter, mListSorter))
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
        title = folder?.name ?: "NO FOLDER NAME"
        mFolderDetailLoader.loadDetailAsync(mFolderID, object : FolderDetailLoader.FolderDetailCallback {
            override fun onLoaded(folderInfo: FolderInfo?) {
                if (folderInfo != null) title = folderInfo.name + " - " + folderInfo.getDetail(applicationContext)
            }
        })
    }

    private fun playSudoku(sudokuID: Long) {
        val i = Intent(this@SudokuListActivity, SudokuPlayActivity::class.java)
        i.putExtra(SudokuPlayActivity.EXTRA_SUDOKU_ID, sudokuID)
        startActivity(i)
    }

    companion object {
        const val MENU_ITEM_INSERT = Menu.FIRST
        const val MENU_ITEM_EDIT = Menu.FIRST + 1
        const val MENU_ITEM_DELETE = Menu.FIRST + 2
        const val MENU_ITEM_PLAY = Menu.FIRST + 3
        const val MENU_ITEM_RESET = Menu.FIRST + 4
        const val MENU_ITEM_RESET_ALL = Menu.FIRST + 5
        const val MENU_ITEM_EDIT_NOTE = Menu.FIRST + 6
        const val MENU_ITEM_FILTER = Menu.FIRST + 7
        const val MENU_ITEM_SORT = Menu.FIRST + 8
        const val MENU_ITEM_FOLDERS = Menu.FIRST + 9
        const val MENU_ITEM_SETTINGS = Menu.FIRST + 10
        const val MENU_ITEM_GENERATE = Menu.FIRST + 11
        const val FILTER_STATE_NOT_STARTED = "filter" + SudokuGame.GAME_STATE_NOT_STARTED
        const val FILTER_STATE_PLAYING = "filter" + SudokuGame.GAME_STATE_PLAYING
        const val FILTER_STATE_SOLVED = "filter" + SudokuGame.GAME_STATE_COMPLETED
        const val SORT_TYPE = "sort_type"
        const val SORT_ORDER = "sort_order"
        private const val TAG = "SudokuListActivity"
    }
}
