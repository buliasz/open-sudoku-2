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

/**
 * List of puzzle's folder. This activity also serves as root activity of application.
 *
 * @author romario, Kotlin version by buliasz
 */
class FolderListActivity : ThemedActivity() {
    private lateinit var addFolderDialog: AddFolderDialogFragment
    private lateinit var renameFolderDialog: RenameFolderDialogFragment
    private lateinit var aboutDialog: AboutDialogFragment
    private lateinit var deleteFolderDialog: DeleteFolderDialogFragment
    private lateinit var mAdapter: FolderListRecyclerAdapter
    private val storagePermissionCoe = 1
    private lateinit var mDatabase: SudokuDatabase
    private lateinit var recyclerView: RecyclerView
    private var mMenu: Menu? = null

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
        mAdapter = FolderListRecyclerAdapter(this, mDatabase.getFolderList()) { id: Long ->
            val i = Intent(applicationContext, SudokuListActivity::class.java)
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

        // show changelog on first run
        val changelog = Changelog(this)
        changelog.showOnFirstRun()
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

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_ITEM_EXPORT -> {
                val intent = Intent()
                intent.setClass(this, SudokuExportActivity::class.java)
                intent.putExtra(Names.FOLDER_ID, mAdapter.selectedFolderId)
                startActivity(intent)
                return true
            }

            MENU_ITEM_RENAME -> {
                renameFolderDialog.mRenameFolderID = mAdapter.selectedFolderId
                renameFolderDialog.show(supportFragmentManager, "RenameFolderDialog")
                return true
            }

            MENU_ITEM_DELETE -> {
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
            MENU_ITEM_ADD -> {
                addFolderDialog.show(supportFragmentManager, "AddFolderDialog")
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
                aboutDialog.show(supportFragmentManager, "AboutDialog")
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
        mAdapter.updateFoldersList(mDatabase.getFolderList())
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
    }
}
