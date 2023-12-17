/*
 * Copyright (C) 2009 Roman Masek, Kotlin version 2023 Bart Uliasz
 *
 * This file is part of OpenSudoku2.
 *
 * OpenSudoku2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenSudoku2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenSudoku2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.buliasz.opensudoku2.gui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import androidx.preference.PreferenceManager
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.fragments.DeletePuzzleDialogFragment
import org.buliasz.opensudoku2.gui.fragments.EditUserNoteDialogFragment
import org.buliasz.opensudoku2.gui.fragments.FilterDialogFragment
import org.buliasz.opensudoku2.gui.fragments.ResetAllDialogFragment
import org.buliasz.opensudoku2.gui.fragments.ResetPuzzleDialogFragment
import org.buliasz.opensudoku2.gui.fragments.SortDialogFragment
import org.buliasz.opensudoku2.utils.ThemeUtils
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * List of puzzles in folder.
 *
 * @author romario, Kotlin version by buliasz
 */
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
    private lateinit var mAdapter: SimpleCursorAdapter
    private var mCursor: Cursor? = null
    private lateinit var mDatabase: SudokuDatabase
    private lateinit var mFolderDetailLoader: FolderDetailLoader
    private lateinit var mListView: ListView
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
        mAdapter = SimpleCursorAdapter(
            this,
            R.layout.sudoku_list_item,
            null,
            arrayOf(Names.CELLS_DATA, Names.STATE, Names.TIME, Names.LAST_PLAYED, Names.CREATED, Names.USER_NOTE),
            intArrayOf(R.id.cells_data, R.id.state, R.id.time, R.id.last_played, R.id.created, R.id.user_note)
        )
        mAdapter.viewBinder = SudokuListViewBinder(this)
        updateList()
        val listView = findViewById<ListView>(android.R.id.list)
        mListView = listView
        listView.adapter = mAdapter
        listView.setOnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, id: Long -> playSudoku(id) }
        registerForContextMenu(listView)

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
        // the puzzle list is naturally refreshed when the window
        // regains focus, so we only need to update the title
        updateTitle()
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

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo) {
        val info: AdapterContextMenuInfo = try {
            menuInfo as AdapterContextMenuInfo
        } catch (e: ClassCastException) {
            Log.e(TAG, "bad menuInfo", e)
            return
        }
        mListView.adapter.getItem(info.position)
        menu.setHeaderTitle("Puzzle")

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_PLAY, 0, R.string.play_puzzle)
        menu.add(0, MENU_ITEM_EDIT_NOTE, 1, R.string.edit_note)
        menu.add(0, MENU_ITEM_RESET, 2, R.string.reset_puzzle)
        menu.add(0, MENU_ITEM_EDIT, 3, R.string.edit_puzzle)
        menu.add(0, MENU_ITEM_DELETE, 4, R.string.delete_puzzle)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info: AdapterContextMenuInfo = try {
            item.menuInfo as AdapterContextMenuInfo
        } catch (e: ClassCastException) {
            Log.e(TAG, "bad menuInfo", e)
            return false
        }
        when (item.itemId) {
            MENU_ITEM_PLAY -> {
                playSudoku(info.id)
                return true
            }

            MENU_ITEM_EDIT -> {
                val i = Intent(this, SudokuEditActivity::class.java)
                i.setAction(Intent.ACTION_EDIT)
                i.putExtra(SudokuEditActivity.EXTRA_SUDOKU_ID, info.id)
                startActivity(i)
                return true
            }

            MENU_ITEM_DELETE -> {
                deletePuzzleDialog.puzzleID = info.id
                deletePuzzleDialog.show(supportFragmentManager, "DeletePuzzleDialog")
                return true
            }

            MENU_ITEM_EDIT_NOTE -> {
                editUserNoteDialog.puzzleId = info.id
                editUserNoteDialog.currentValue = mDatabase.getSudoku(editUserNoteDialog.puzzleId)?.userNote ?: ""
                editUserNoteDialog.show(supportFragmentManager, "EditUserNoteDialog")
                return true
            }

            MENU_ITEM_RESET -> {
                resetPuzzleDialog.puzzleID = info.id
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
        if (mCursor != null) {
            stopManagingCursor(mCursor)
        }
        mCursor = mDatabase.getSudokuList(mFolderID, mListFilter, mListSorter)
        startManagingCursor(mCursor)
        mAdapter.changeCursor(mCursor)
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

    private class SudokuListViewBinder(private val mContext: Context) : SimpleCursorAdapter.ViewBinder {
        private val mGameTimeFormatter = GameTimeFormat()
        private val mDateTimeFormatter = DateFormat.getDateTimeInstance(
            DateFormat.SHORT, DateFormat.SHORT
        )
        private val mTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

        override fun setViewValue(view: View, c: Cursor, columnIndex: Int): Boolean {
            val state = c.getInt(c.getColumnIndexOrThrow(Names.STATE))
            val label: TextView
            when (view.id) {
                R.id.cells_data -> {
                    val data = c.getString(columnIndex)
                    // TODO: still can be faster, I don't have to call initCollection and read notes
                    val cells: CellCollection = try {
                        CellCollection.deserialize(data)
                    } catch (e: Exception) {
                        val id = c.getLong(c.getColumnIndexOrThrow(Names.ID))
                        Log.e(TAG, "Exception occurred when deserializing puzzle with id $id.", e)
                        CellCollection.createEmpty()
                    }
                    val board = view as SudokuBoardView
                    board.isReadOnly = true
                    board.isFocusable = false
                    view.cells = cells
                    ThemeUtils.applyThemeToSudokuBoardViewFromContext(
                        ThemeUtils.getCurrentThemeFromPreferences(mContext),
                        board,
                        mContext
                    )
                }

                R.id.state -> {
                    label = view as TextView
                    var stateString: String? = null
                    when (state) {
                        SudokuGame.GAME_STATE_COMPLETED -> stateString = mContext.getString(R.string.solved)
                        SudokuGame.GAME_STATE_PLAYING -> stateString = mContext.getString(R.string.playing)
                    }
                    label.visibility = if (stateString == null) View.GONE else View.VISIBLE
                    label.text = stateString
                    if (state == SudokuGame.GAME_STATE_COMPLETED) {
                        label.setTextColor(ThemeUtils.getCurrentThemeColor(view.getContext(), android.R.attr.colorAccent))
                    } else {
                        label.setTextColor(ThemeUtils.getCurrentThemeColor(view.getContext(), android.R.attr.textColorPrimary))
                    }
                }

                R.id.time -> {
                    val time = c.getLong(columnIndex)
                    label = view as TextView
                    var timeString: String? = null
                    if (time != 0L) {
                        timeString = mGameTimeFormatter.format(time)
                    }
                    label.visibility = if (timeString == null) View.GONE else View.VISIBLE
                    label.text = timeString
                    if (state == SudokuGame.GAME_STATE_COMPLETED) {
                        label.setTextColor(ThemeUtils.getCurrentThemeColor(view.getContext(), android.R.attr.colorAccent))
                    } else {
                        label.setTextColor(ThemeUtils.getCurrentThemeColor(view.getContext(), android.R.attr.textColorPrimary))
                    }
                }

                R.id.last_played -> {
                    val lastPlayed = c.getLong(columnIndex)
                    label = view as TextView
                    var lastPlayedString: String? = null
                    if (lastPlayed != 0L) {
                        lastPlayedString = mContext.getString(
                            R.string.last_played_at,
                            getDateAndTimeForHumans(lastPlayed)
                        )
                    }
                    label.visibility = if (lastPlayedString == null) View.GONE else View.VISIBLE
                    label.text = lastPlayedString
                }

                R.id.created -> {
                    val created = c.getLong(columnIndex)
                    label = view as TextView
                    var createdString: String? = null
                    if (created != 0L) {
                        createdString = mContext.getString(
                            R.string.created_at,
                            getDateAndTimeForHumans(created)
                        )
                    }
                    // TODO: when GONE, note is not correctly aligned below last_played
                    label.visibility = if (createdString == null) View.GONE else View.VISIBLE
                    label.text = createdString
                }

                R.id.user_note -> {
                    val note = c.getString(columnIndex)
                    label = view as TextView
                    if (note == null || note.trim { it <= ' ' } == "") {
                        view.setVisibility(View.GONE)
                    } else {
                        view.text = note
                    }
                    label.visibility = if (note == null || note.trim { it <= ' ' } == "") View.GONE else View.VISIBLE
                    label.text = note
                }
            }
            return true
        }

        private fun getDateAndTimeForHumans(utcEpochSeconds: Long): String {
            val dateTime = LocalDateTime.ofEpochSecond(utcEpochSeconds, 0, ZoneOffset.UTC)
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            return if (dateTime.isAfter(today.atStartOfDay())) {
                mContext.getString(R.string.at_time, dateTime.format(mTimeFormatter))
            } else if (dateTime.isAfter(yesterday.atStartOfDay())) {
                mContext.getString(R.string.yesterday_at_time, dateTime.format(mTimeFormatter))
            } else {
                mContext.getString(R.string.on_date, mDateTimeFormatter.format(dateTime))
            }
        }
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
