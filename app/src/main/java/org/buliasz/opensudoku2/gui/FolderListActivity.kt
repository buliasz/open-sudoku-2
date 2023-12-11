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

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.utils.AndroidUtils

/**
 * List of puzzle's folder. This activity also serves as root activity of application.
 *
 * @author romario, Kotlin version by buliasz
 */
class FolderListActivity : ThemedActivity() {
    private val storagePermissionCoe = 1
    private var mCursor: Cursor? = null
    private var mDatabase: SudokuDatabase? = null
    private var mFolderListBinder: FolderListViewBinder? = null
    private var mListView: ListView? = null
    private var mMenu: Menu? = null

    // input parameters for dialogs
    private var mAddFolderNameInput: TextView? = null
    private var mRenameFolderNameInput: TextView? = null
    private var mRenameFolderID: Long = 0
    private var mDeleteFolderID: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.folder_list)
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT)
        val getMorePuzzles = findViewById<View>(R.id.get_more_puzzles)
        getMorePuzzles.setOnClickListener {
            val webpage = Uri.parse("https://github.com/grantm/sudoku-exchange-puzzle-bank")
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
        mDatabase = SudokuDatabase(applicationContext)
        mCursor = mDatabase!!.folderList
        startManagingCursor(mCursor)
        val adapter = SimpleCursorAdapter(
            this,
            R.layout.folder_list_item,
            mCursor,
            arrayOf(Names.FOLDER_NAME, Names.ID),
            intArrayOf(R.id.name, R.id.detail)
        )
        mFolderListBinder = FolderListViewBinder(this)
        adapter.viewBinder = mFolderListBinder
        val listView = findViewById<ListView>(android.R.id.list)
        listView.adapter = adapter
        listView.setOnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, id: Long ->
            val i = Intent(applicationContext, SudokuListActivity::class.java)
            i.putExtra(Names.FOLDER_ID, id)
            startActivity(i)
        }
        registerForContextMenu(listView)
        mListView = listView

        // show changelog on first run
        val changelog = Changelog(this)
        changelog.showOnFirstRun()
    }

    override fun onStart() {
        super.onStart()
        updateList()
    }

    override fun onDestroy() {
        super.onDestroy()
        mDatabase!!.close()
        mFolderListBinder!!.destroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("mRenameFolderID", mRenameFolderID)
        outState.putLong("mDeleteFolderID", mDeleteFolderID)
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
        mRenameFolderID = state.getLong("mRenameFolderID")
        mDeleteFolderID = state.getLong("mDeleteFolderID")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        // This is our one standard application action -- inserting a
        // new note into the list.
        menu.add(0, MENU_ITEM_ADD, 0, R.string.add_folder)
            .setShortcut('3', 'a')
            .setIcon(R.drawable.ic_add)
        menu.add(0, MENU_ITEM_IMPORT, 0, R.string.import_file)
            .setShortcut('8', 'i')
            .setIcon(R.drawable.ic_baseline_download_24)
        menu.add(0, MENU_ITEM_EXPORT_ALL, 1, R.string.export_all_folders)
            .setShortcut('7', 'e')
            .setIcon(R.drawable.ic_share)
        menu.add(0, MENU_ITEM_SETTINGS, 2, R.string.settings)
            .setShortcut('6', 's')
            .setIcon(R.drawable.ic_settings)
        menu.add(0, MENU_ITEM_ABOUT, 2, R.string.about)
            .setShortcut('1', 'h')
            .setIcon(R.drawable.ic_info)


        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        val intent = Intent(null, intent.data)
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
        menu.addIntentOptions(
            Menu.CATEGORY_ALTERNATIVE, 0, 0,
            ComponentName(this, FolderListActivity::class.java), null, intent, 0, null
        )
        mMenu = menu
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo) {
        val info: AdapterContextMenuInfo = try {
            menuInfo as AdapterContextMenuInfo
        } catch (e: ClassCastException) {
            Log.e(TAG, "bad menuInfo", e)
            return
        }
        val cursor = mListView!!.adapter.getItem(info.position) as Cursor
        menu.setHeaderTitle(cursor.getString(cursor.getColumnIndexOrThrow(Names.FOLDER_NAME)))
        menu.add(0, MENU_ITEM_EXPORT, 0, R.string.export_folder)
        menu.add(0, MENU_ITEM_RENAME, 1, R.string.rename_folder)
        menu.add(0, MENU_ITEM_DELETE, 2, R.string.delete_folder)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(id: Int): Dialog {
        val factory = LayoutInflater.from(this)
        when (id) {
            DIALOG_ABOUT -> {
                val aboutView = factory.inflate(R.layout.about, null)
                val versionLabel = aboutView.findViewById<TextView>(R.id.version_label)
                val versionName = AndroidUtils.getAppVersionName(applicationContext)
                versionLabel.text = getString(R.string.version, versionName)
                return AlertDialog.Builder(this)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.app_name)
                    .setView(aboutView)
                    .setPositiveButton("OK", null)
                    .create()
            }

            DIALOG_ADD_FOLDER -> {
                val addFolderView = factory.inflate(R.layout.folder_name, null)
                val addFolderNameInput = addFolderView.findViewById<TextView>(R.id.name)
                mAddFolderNameInput = addFolderNameInput
                return AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_add)
                    .setTitle(R.string.add_folder)
                    .setView(addFolderView)
                    .setPositiveButton(R.string.save) { _: DialogInterface?, _: Int ->
                        mDatabase!!.insertFolder(
                            addFolderNameInput.text.toString().trim { it <= ' ' },
                            System.currentTimeMillis()
                        )
                        updateList()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
            }

            DIALOG_RENAME_FOLDER -> {
                val renameFolderView = factory.inflate(R.layout.folder_name, null)
                val renameFolderNameInput = renameFolderView.findViewById<TextView>(R.id.name)
                mRenameFolderNameInput = renameFolderNameInput
                return AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_edit_grey)
                    .setTitle(R.string.rename_folder_title)
                    .setView(renameFolderView)
                    .setPositiveButton(R.string.save) { _: DialogInterface?, _: Int ->
                        mDatabase!!.updateFolder(
                            mRenameFolderID,
                            renameFolderNameInput.text.toString().trim { it <= ' ' })
                        updateList()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()
            }

            DIALOG_DELETE_FOLDER -> return AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.delete_folder_title)
                .setMessage(R.string.delete_folder_confirm)
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    // TODO: this could take a while, I should show progress dialog
                    mDatabase!!.deleteFolder(mDeleteFolderID)
                    updateList()
                }
                .setNegativeButton(android.R.string.no, null)
                .create()
        }
        throw Exception("Unknown dialog $id")
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareDialog(id: Int, dialog: Dialog) {
        super.onPrepareDialog(id, dialog)
        when (id) {
            DIALOG_ADD_FOLDER -> {}
            DIALOG_RENAME_FOLDER -> {
                val folder = mDatabase!!.getFolderInfo(mRenameFolderID)
                val folderName = if (folder != null) folder.name else ""
                dialog.setTitle(getString(R.string.rename_folder_title, folderName))
                mRenameFolderNameInput!!.text = folderName
            }

            DIALOG_DELETE_FOLDER -> {
                val folder = mDatabase!!.getFolderInfo(mDeleteFolderID)
                val folderName = if (folder != null) folder.name else ""
                dialog.setTitle(getString(R.string.delete_folder_title, folderName))
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info: AdapterContextMenuInfo? = try {
            item.menuInfo as AdapterContextMenuInfo?
        } catch (e: ClassCastException) {
            Log.e(TAG, "bad menuInfo", e)
            return false
        }
        when (item.itemId) {
            MENU_ITEM_EXPORT -> {
                val intent = Intent()
                intent.setClass(this, SudokuExportActivity::class.java)
                intent.putExtra(Names.FOLDER_ID, info!!.id)
                startActivity(intent)
                return true
            }

            MENU_ITEM_RENAME -> {
                mRenameFolderID = info!!.id
                showDialog(DIALOG_RENAME_FOLDER)
                return true
            }

            MENU_ITEM_DELETE -> {
                mDeleteFolderID = info!!.id
                showDialog(DIALOG_DELETE_FOLDER)
                return true
            }
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            MENU_ITEM_ADD -> {
                showDialog(DIALOG_ADD_FOLDER)
                return true
            }

            MENU_ITEM_IMPORT -> {
                intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("*/*")
                startActivityForResult(intent, OPEN_FILE)
                return true
            }

            MENU_ITEM_EXPORT_ALL -> {
                intent = Intent()
                intent.setClass(this, SudokuExportActivity::class.java)
                intent.putExtra(
                    Names.FOLDER_ID,
                    SudokuExportActivity.ALL_FOLDERS
                )
                startActivity(intent)
                return true
            }

            MENU_ITEM_SETTINGS -> {
                intent = Intent()
                intent.setClass(this, GameSettingsActivity::class.java)
                startActivity(intent)
                return true
            }

            MENU_ITEM_ABOUT -> {
                showDialog(DIALOG_ABOUT)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OPEN_FILE && resultCode == RESULT_OK) {
            val uri: Uri?
            if (data != null) {
                uri = data.data
                val i = Intent(this, SudokuImportActivity::class.java)
                i.setData(uri)
                startActivity(i)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == storagePermissionCoe) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onOptionsItemSelected(mMenu!!.findItem(MENU_ITEM_IMPORT))
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun updateList() {
        mCursor!!.requery()
    }

    private class FolderListViewBinder(private val mContext: Context) :
        SimpleCursorAdapter.ViewBinder {
        private val mDetailLoader: FolderDetailLoader = FolderDetailLoader(mContext)

        override fun setViewValue(view: View, c: Cursor, columnIndex: Int): Boolean {
            when (view.id) {
                R.id.name -> (view as TextView).text = c.getString(columnIndex)
                R.id.detail -> {
                    val folderID = c.getLong(columnIndex)
                    val detailView = view as TextView
                    detailView.text = mContext.getString(R.string.loading)
                    mDetailLoader.loadDetailAsync(folderID, object :
                        FolderDetailLoader.FolderDetailCallback {
                        override fun onLoaded(folderInfo: FolderInfo?) {
                            if (folderInfo != null) detailView.text = folderInfo.getDetail(mContext)
                        }
                    })
                }
            }
            return true
        }

        fun destroy() {
            mDetailLoader.destroy()
        }
    }

    companion object {
        const val MENU_ITEM_ADD = Menu.FIRST
        const val MENU_ITEM_RENAME = Menu.FIRST + 1
        const val MENU_ITEM_DELETE = Menu.FIRST + 2
        const val MENU_ITEM_ABOUT = Menu.FIRST + 3
        const val MENU_ITEM_EXPORT = Menu.FIRST + 4
        const val MENU_ITEM_EXPORT_ALL = Menu.FIRST + 5
        const val MENU_ITEM_IMPORT = Menu.FIRST + 6
        const val MENU_ITEM_SETTINGS = Menu.FIRST + 7
        private const val OPEN_FILE = 1
        private const val DIALOG_ABOUT = 0
        private const val DIALOG_ADD_FOLDER = 1
        private const val DIALOG_RENAME_FOLDER = 2
        private const val DIALOG_DELETE_FOLDER = 3
        private const val TAG = "FolderListActivity"
    }
}
