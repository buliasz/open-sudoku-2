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

package org.buliasz.opensudoku2.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.util.Log
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.PuzzleListFilter
import java.io.Closeable
import java.time.Instant
import java.util.LinkedList

/**
 * Wrapper around opensudoku2's database.
 * You have to close connection when you're done with the database.
 */
class SudokuDatabase(context: Context, readOnly: Boolean) : Closeable {
	private val mOpenHelper: DatabaseHelper = DatabaseHelper(context)
	private val db: SQLiteDatabase = if (readOnly) mOpenHelper.readableDatabase else mOpenHelper.writableDatabase

	/**
	 * Returns list of puzzle folders.
	 */
	fun getFolderList(withCounts: Boolean = false): List<FolderInfo> {
		val qb = SQLiteQueryBuilder()
		val folderList: MutableList<FolderInfo> = LinkedList()
		qb.tables = Names.FOLDER
		qb.query(db, null, null, null, null, null, null).forEach { cursor ->
			val folderInfo = FolderInfo()
			folderInfo.id = cursor.id
			folderInfo.name = cursor.getString(cursor.getColumnIndexOrThrow(Names.FOLDER_NAME))
			folderInfo.created = cursor.getLong(cursor.getColumnIndexOrThrow(Names.FOLDER_CREATED))
			if (withCounts) {
				folderList.add(getFolderInfoWithCounts(folderInfo.id))
			} else {
				folderList.add(folderInfo)
			}
		}
		return folderList
	}

	/**
	 * Returns the folder info.
	 *
	 * @param folderId Primary key of folder.
	 */
	fun getFolderInfo(folderId: Long): FolderInfo? {
		val qb = SQLiteQueryBuilder()
		qb.tables = Names.FOLDER
		qb.query(db, null, Names.ID + "=" + folderId, null, null, null, null).use { cursor ->
			return@getFolderInfo if (cursor.moveToFirst()) {
				val id = cursor.id
				val name = cursor.getString(cursor.getColumnIndexOrThrow(Names.FOLDER_NAME))
				val folderInfo = FolderInfo()
				folderInfo.id = id
				folderInfo.name = name
				folderInfo
			} else {
				null
			}
		}
	}

	/**
	 * Returns the folder info.
	 *
	 * @param folderName Name of the folder to get info.
	 */
	private fun getFolderInfo(folderName: String): FolderInfo? {
		val qb = SQLiteQueryBuilder()
		qb.tables = Names.FOLDER
		qb.query(db, null, Names.FOLDER_NAME + "=?", arrayOf(folderName), null, null, null).use { cursor ->
			return@getFolderInfo if (cursor.moveToFirst()) {
				val id = cursor.id
				val name = cursor.getString(cursor.getColumnIndexOrThrow(Names.FOLDER_NAME))
				FolderInfo(id, name)
			} else {
				null
			}
		}
	}

	/**
	 * Returns the full folder info - this includes count of games in particular states.
	 *
	 * @param folderId Primary key of folder.
	 * @return folder info
	 */
	fun getFolderInfoWithCounts(folderId: Long): FolderInfo {
		val folder = FolderInfo(folderId, "")
		val q = "select f.${Names.ID} as ${Names.ID}, f.${Names.FOLDER_NAME} as ${Names.FOLDER_NAME}, " +
			"g.${Names.STATE} as ${Names.STATE}, count(g.${Names.STATE}) as ${Names.COUNT} " +
			"from ${Names.FOLDER} f left join ${Names.GAME} g on f.${Names.ID} = g.${Names.FOLDER_ID} " +
			"where f.${Names.ID} = $folderId " +
			"group by g.${Names.STATE}"
		db.rawQuery(q, null).use { cursor ->
			while (cursor.moveToNext()) {
				val state = cursor.getInt(cursor.getColumnIndexOrThrow(Names.STATE))
				val count = cursor.getInt(cursor.getColumnIndexOrThrow(Names.COUNT))
				if (folder.name.isBlank()) {
					folder.name = cursor.getString(cursor.getColumnIndexOrThrow(Names.FOLDER_NAME))
				}
				folder.puzzleCount += count
				if (state == SudokuGame.GAME_STATE_COMPLETED) {
					folder.solvedCount += count
				} else if (state == SudokuGame.GAME_STATE_PLAYING) {
					folder.playingCount += count
				}
			}
		}
		return folder
	}

	/**
	 * Inserts new puzzle folder into the database.
	 *
	 * @param name    Name of the folder.
	 * @param created Time of folder creation.
	 */
	fun insertFolder(name: String, created: Long): FolderInfo {
		val existingFolder = getFolderInfo(name)
		if (existingFolder != null) {
			return existingFolder
		}
		val values = ContentValues()
		values.put(Names.FOLDER_CREATED, created)
		values.put(Names.FOLDER_NAME, name)
		val rowId = db.insert(Names.FOLDER, Names.ID, values)
		if (rowId < 0) {
			throw SQLException("Failed to insert folder '$name'.")
		}
		return FolderInfo(rowId, name)
	}

	/**
	 * Renames existing folder.
	 *
	 * @param folderId Primary key of folder.
	 * @param name     New name for the folder.
	 */
	fun renameFolder(folderId: Long, name: String) {
		val values = ContentValues()
		values.put(Names.FOLDER_NAME, name)
		db.update(Names.FOLDER, values, Names.ID + "=" + folderId, null)
	}

	/**
	 * Deletes given folder.
	 *
	 * @param folderId Primary key of folder.
	 */
	fun deleteFolder(folderId: Long) {
		// delete all puzzles in folder we are going to delete
		db.delete(Names.GAME, Names.FOLDER_ID + "=" + folderId, null)
		// delete the folder
		db.delete(Names.FOLDER, Names.ID + "=" + folderId, null)
	}

	/**
	 * Deletes given puzzle from the database.
	 */
	fun deletePuzzle(puzzleID: Long) {
		db.delete(Names.GAME, Names.ID + "=" + puzzleID, null)
	}

	fun findPuzzle(cells: CellCollection): SudokuGame? {
		with(SQLiteQueryBuilder()) {
			tables = Names.GAME
			query(
				db, null, Names.ORIGINAL_VALUES + "=?", arrayOf(cells.originalValues), null, null, null
			).use { cursor ->
				if (cursor.moveToFirst()) return@findPuzzle extractSudokuGameFromCursorRow(cursor)
			}
		}
		return null
	}

	override fun close() {
		mOpenHelper.close()
	}

	companion object {
		const val DATABASE_NAME = "opensudoku2"
	}


	/**
	 * Returns sudoku game object.
	 *
	 * @param gameID Primary key of folder.
	 */
	internal fun getPuzzle(gameID: Long): SudokuGame? {
		with(SQLiteQueryBuilder()) {
			tables = Names.GAME
			query(db, null, Names.ID + "=" + gameID, null, null, null, null).use { cursor ->
				if (cursor.moveToFirst()) {
					return@getPuzzle extractSudokuGameFromCursorRow(cursor)
				}
			}
		}
		return null
	}

	internal fun insertPuzzle(originalValues: String, folderId: Long) {
		with(ContentValues()) {
			put(Names.ORIGINAL_VALUES, originalValues)
			put(Names.CREATED, Instant.now().epochSecond)
			put(Names.LAST_PLAYED, 0)
			put(Names.STATE, SudokuGame.GAME_STATE_NOT_STARTED)
			put(Names.TIME, 0)
			put(Names.USER_NOTE, "")
			put(Names.COMMAND_STACK, "")
			put(Names.FOLDER_ID, folderId)
			val rowId = db.insert(Names.GAME, null, this)
			if (rowId < 0) {
				throw SQLException("Failed to insert puzzle.")
			}
		}
	}

	internal fun insertPuzzle(newGame: SudokuGame): Long {
		val rowId = db.insert(Names.GAME, null, newGame.contentValues)
		if (rowId < 0) {
			throw SQLException("Failed to insert puzzle.")
		}
		return rowId
	}

	internal fun updatePuzzle(game: SudokuGame): Int = db.update(Names.GAME, game.contentValues, "${Names.ID}=${game.id}", null)

	internal fun resetAllPuzzles(folderID: Long) {
		with(ContentValues()) {
			putNull(Names.CELLS_DATA)
			put(Names.LAST_PLAYED, 0)
			put(Names.STATE, SudokuGame.GAME_STATE_NOT_STARTED)
			put(Names.TIME, 0)
			put(Names.USER_NOTE, "")
			put(Names.COMMAND_STACK, "")
			val rowsCount = db.update(Names.GAME, this, "${Names.FOLDER_ID}=$folderID", null)
			if (rowsCount <= 0) {
				throw SQLException("Failed to insert puzzle.")
			}
		}
	}

	/**
	 * Returns list of sudoku game objects
	 *
	 * @param folderId Primary key of folder.
	 */
	internal fun getPuzzleListCursor(folderId: Long = 0L, filter: PuzzleListFilter? = null, sortOrder: String? = null): Cursor {
		val qb = SQLiteQueryBuilder()
		qb.tables = Names.GAME
		if (folderId != 0L) {
			qb.appendWhere(Names.FOLDER_ID + "=" + folderId)
		}
		if (filter != null) {
			if (!filter.showStateCompleted) {
				qb.appendWhere(" and " + Names.STATE + "!=" + SudokuGame.GAME_STATE_COMPLETED)
			}
			if (!filter.showStateNotStarted) {
				qb.appendWhere(" and " + Names.STATE + "!=" + SudokuGame.GAME_STATE_NOT_STARTED)
			}
			if (!filter.showStatePlaying) {
				qb.appendWhere(" and " + Names.STATE + "!=" + SudokuGame.GAME_STATE_PLAYING)
			}
		}
		return qb.query(db, null, null, null, null, null, sortOrder)
	}

	fun folderExists(folderName: String): Boolean = getFolderInfo(folderName) != null
}

internal fun extractSudokuGameFromCursorRow(cursor: Cursor): SudokuGame {
	val game = SudokuGame()
	try {
		with(game) {
			id = cursor.id
			created = cursor.getLong(cursor.getColumnIndexOrThrow(Names.CREATED))
			val cellsDataIndex = cursor.getColumnIndexOrThrow(Names.CELLS_DATA)
			cells = CellCollection.deserialize(
				cursor.getString(
					if (!cursor.isNull(cellsDataIndex)) cellsDataIndex else cursor.getColumnIndexOrThrow(Names.ORIGINAL_VALUES),
				),
			)
			lastPlayed = cursor.getLong(cursor.getColumnIndexOrThrow(Names.LAST_PLAYED))
			state = cursor.getInt(cursor.getColumnIndexOrThrow(Names.STATE))
			time = cursor.getLong(cursor.getColumnIndexOrThrow(Names.TIME))
			userNote = cursor.getString(cursor.getColumnIndexOrThrow(Names.USER_NOTE))
			folderId = cursor.getLong(cursor.getColumnIndexOrThrow(Names.FOLDER_ID))
			commandStack.deserialize(cursor.getString(cursor.getColumnIndexOrThrow(Names.COMMAND_STACK)))
		}
	} catch (e: Exception) {    // this shouldn't normally happen, db corrupted
		Log.e(DB_TAG, "Error extracting SudokuGame from cursor", e)
	}
	return game
}

internal fun Cursor.forEach(callback: ((Cursor) -> Unit)) {
	if (moveToFirst()) {
		while (!isAfterLast) {
			callback(this)
			moveToNext()
		}
	}
	close()
}

internal val Cursor.originalValues: String
	get() = getString(getColumnIndexOrThrow(Names.ORIGINAL_VALUES))

internal val Cursor.id: Long
	get() = getLong(getColumnIndexOrThrow(Names.ID))

const val DB_TAG = "SudokuDatabase"