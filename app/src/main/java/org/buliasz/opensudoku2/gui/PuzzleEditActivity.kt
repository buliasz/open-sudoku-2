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

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuDatabase
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.fragments.SimpleDialog
import org.buliasz.opensudoku2.gui.inputmethod.IMControlPanel
import java.time.Instant

/**
 * Activity for editing content of puzzle.
 */
class PuzzleEditActivity : ThemedActivity() {
	private var originalValues: String = ""
	private lateinit var mDatabase: SudokuDatabase
	private lateinit var newPuzzle: SudokuGame
	private lateinit var mRootLayout: ViewGroup
	private lateinit var mClipboard: ClipboardManager
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.sudoku_edit)
		mRootLayout = findViewById(R.id.root_layout)
		val mBoard = findViewById<SudokuBoardView>(R.id.board_view)
		mDatabase = SudokuDatabase(applicationContext, false)
		val intent = intent
		val action = intent.action
		val mPuzzleID: Long
		when (action) {
			Intent.ACTION_EDIT -> {
				// Requested to edit: set that state, and the data being edited.
				mPuzzleID = intent.getLongExtra(Names.PUZZLE_ID, -1L)
				require(mPuzzleID >= 0L) { "Extra with key PUZZLE_ID is required." }
			}

			Intent.ACTION_INSERT -> {
				mPuzzleID = -1L
			}

			else -> {
				// Whoops, unknown action!  Bail.
				Log.e(TAG, "Unknown action, exiting.")
				finish()
				return
			}
		}
		if (savedInstanceState != null) {
			newPuzzle = SudokuGame()
			newPuzzle.restoreState(savedInstanceState)
		} else {
			if (mPuzzleID != -1L) { // existing puzzle, read it from database
				newPuzzle = mDatabase.getPuzzle(mPuzzleID)!!
				originalValues = newPuzzle.cells.originalValues
				newPuzzle.reset()
				newPuzzle.cells.markAllCellsAsEditable()
			} else {
				newPuzzle = SudokuGame.createEmptyGame()
				newPuzzle.folderId = intent.getLongExtra(Names.FOLDER_ID, -1L)
				require(newPuzzle.folderId >= 0L) { "Extra with key FOLDER_ID is required." }
			}
		}
		mBoard.setGame(newPuzzle)
		val mInputMethods = findViewById<IMControlPanel>(R.id.input_methods)
		mInputMethods.initialize(mBoard, newPuzzle, null)

		// only SelectOnTap input method will be enabled
		for (im in mInputMethods.inputMethods) {
			im.isEnabled = false
		}
		mInputMethods.imSelectOnTap.isEnabled = true
		mInputMethods.activateInputMethod(IMControlPanel.INPUT_METHOD_SELECT_ON_TAP)
		mClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (isModified()) showSaveDialogAndOrFinish(true) else finish()
			}
		})
	}

	override fun onDestroy() {
		super.onDestroy()
		mDatabase.close()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		newPuzzle.saveState(outState)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		// This is our one standard application action -- inserting a
		// new note into the list.
		menu.add(0, MenuItems.COPY.id, 0, android.R.string.copy)
		menu.add(0, MenuItems.PASTE.id, 1, android.R.string.paste)
		menu.add(0, MenuItems.CHECK_VALIDITY.id, 2, R.string.check_validity)
		menu.add(0, MenuItems.SAVE.id, 3, R.string.save).setShortcut('1', 's').setIcon(R.drawable.ic_save)
		menu.add(0, MenuItems.DISCARD.id, 4, R.string.discard).setShortcut('3', 'c').setIcon(R.drawable.ic_close)

		// Generate any additional actions that can be performed on the
		// overall list.  In a normal install, there are no additional
		// actions found here, but this allows other applications to extend
		// our menu with their own actions.
		val intent = Intent(null, intent.data)
		intent.addCategory(Intent.CATEGORY_ALTERNATIVE)
		menu.addIntentOptions(
			Menu.CATEGORY_ALTERNATIVE, 0, 0, ComponentName(this, PuzzleEditActivity::class.java), null, intent, 0, null
		)
		return true
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		super.onPrepareOptionsMenu(menu)
		if (!mClipboard.hasPrimaryClip()) {
			// If the clipboard doesn't contain data, disable the paste menu item.
			menu.findItem(MenuItems.PASTE.id).setVisible(false)
		} else if (!(mClipboard.primaryClipDescription!!.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || mClipboard.primaryClipDescription!!.hasMimeType(
				ClipDescription.MIMETYPE_TEXT_HTML
			))
		) {
			// This disables the paste menu item, since the clipboard has data but it is not plain text
			Toast.makeText(applicationContext, mClipboard.primaryClipDescription!!.getMimeType(0), Toast.LENGTH_SHORT).show()
			menu.findItem(MenuItems.PASTE.id).setVisible(false)
		} else {
			// This enables the paste menu item, since the clipboard contains plain text.
			menu.findItem(MenuItems.PASTE.id).setVisible(true)
		}
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			MenuItems.COPY.id -> {
				copyToClipboard()
				return true
			}

			MenuItems.PASTE.id -> {
				pasteFromClipboard()
				return true
			}

			MenuItems.CHECK_VALIDITY.id -> {
				when (getNumberOfSolutions()) {
					1 -> {
						SimpleDialog(supportFragmentManager).show(R.string.puzzle_solvable)
					}

					0 -> {
						SimpleDialog(supportFragmentManager).show(R.string.puzzle_has_no_solution)
					}

					else -> {
						SimpleDialog(supportFragmentManager).show(R.string.puzzle_has_multiple_solutions)
					}
				}
				return true
			}

			MenuItems.SAVE.id -> {
				showSaveDialogAndOrFinish(false)
				return true
			}

			MenuItems.DISCARD.id -> {
				finish()
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	private fun getNumberOfSolutions(): Int {
		newPuzzle.cells.markCellsWithValuesAsNotEditable()
		val numberOfSolutions = newPuzzle.solutionCount
		newPuzzle.cells.markAllCellsAsEditable()
		return numberOfSolutions
	}

	fun isModified(): Boolean {
		if (newPuzzle.cells.isEmpty) {
			return false
		}
		if (newPuzzle.id < 0) {
			return true
		}
		newPuzzle.cells.markCellsWithValuesAsNotEditable()
		val isOriginalModified = newPuzzle.cells.originalValues != originalValues
		newPuzzle.cells.markAllCellsAsEditable()
		return isOriginalModified
	}

	private fun savePuzzle() {
		newPuzzle.cells.markCellsWithValuesAsNotEditable()
		newPuzzle.reset()
		newPuzzle.created = Instant.now().epochSecond
		if (newPuzzle.id < 0L) {
			mDatabase.insertPuzzle(newPuzzle)
			Toast.makeText(applicationContext, R.string.puzzle_inserted, Toast.LENGTH_SHORT).show()
		} else {
			mDatabase.updatePuzzle(newPuzzle)
			Toast.makeText(applicationContext, R.string.puzzle_updated, Toast.LENGTH_SHORT).show()
		}

		originalValues = newPuzzle.cells.originalValues
	}

	internal fun showSaveDialogAndOrFinish(askAndFinish: Boolean) {
		val dialog = with(SimpleDialog(supportFragmentManager)) {
			positiveButtonString = R.string.save
			positiveButtonCallback = {
				savePuzzle()
				if (askAndFinish) {
					finish()
				} else {
					newPuzzle.cells.markAllCellsAsEditable()
				}
			}
			if (askAndFinish) {
				negativeButtonString = R.string.discard
				negativeButtonCallback = ::finish
			}
			this
		}

		// check number of solutions
		val numberOfSolutions = getNumberOfSolutions()
		if (numberOfSolutions == 0) {
			dialog.show(
				applicationContext.getString(R.string.puzzle_has_no_solution) + "\n" + applicationContext.getString(R.string.do_you_want_to_save_anyway)
			)
			return
		} else if (numberOfSolutions > 1) {
			dialog.show(
				applicationContext.getString(R.string.puzzle_has_multiple_solutions) + "\n" + applicationContext.getString(R.string.do_you_want_to_save_anyway)
			)
			return
		}

		// check already existing puzzles
		val existingPuzzle = mDatabase.findPuzzle(newPuzzle.cells)
		if (existingPuzzle != null) {
			dialog.show(
				applicationContext.getString(
					R.string.puzzle_already_exists,
					mDatabase.getFolderInfo(existingPuzzle.folderId)?.name
				) + "\n" + applicationContext.getString(R.string.do_you_want_to_save_anyway)
			)
			return
		}

		if (askAndFinish) {
			dialog.show(applicationContext.getString(R.string.press_ok_to_save))
			return
		}

		// no dialog necessary, save and finish is invoked
		dialog.positiveButtonCallback?.invoke()
	}

	/**
	 * Copies puzzle to primary clipboard in a plain text format (81 character string).
	 *
	 * @see CellCollection.serialize
	 */
	private fun copyToClipboard() {
		val cells = newPuzzle.cells
		val serializedCells: String = cells.serialize(CellCollection.DATA_VERSION_PLAIN)
		val clipData = ClipData.newPlainText("Sudoku Puzzle", serializedCells)
		mClipboard.setPrimaryClip(clipData)
		Toast.makeText(applicationContext, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
	}

	/**
	 * Pastes puzzle from primary clipboard in any of the supported formats.
	 *
	 * @see CellCollection.serialize
	 */
	private fun pasteFromClipboard() {
		if (!mClipboard.hasPrimaryClip() || (
				mClipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) != true &&
				mClipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) != true
			)
		) {
			Toast.makeText(applicationContext, R.string.invalid_mime_type, Toast.LENGTH_LONG).show()
			return
		}

		val clipDataItem = mClipboard.primaryClip!!.getItemAt(0)
		val clipDataText = clipDataItem.text.toString()
		if (CellCollection.isValid(clipDataText)) {
			val cells: CellCollection = try {
				CellCollection.deserialize(clipDataText)
			} catch (e: Exception) {
				Toast.makeText(applicationContext, R.string.invalid_puzzle_format, Toast.LENGTH_LONG).show()
				return
			}

			newPuzzle.cells = cells
			(mRootLayout.getChildAt(0) as SudokuBoardView).cells = cells
			Toast.makeText(applicationContext, R.string.pasted_from_clipboard, Toast.LENGTH_SHORT).show()
		} else {
			Toast.makeText(applicationContext, R.string.invalid_puzzle_format, Toast.LENGTH_LONG).show()
		}
	}

	companion object {
		enum class MenuItems {
			CHECK_VALIDITY,
			SAVE,
			DISCARD,
			COPY,
			PASTE;

			val id = ordinal + Menu.FIRST
		}

		private const val TAG = "PuzzleEditActivity"
	}
}
