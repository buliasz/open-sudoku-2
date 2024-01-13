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

package org.buliasz.opensudoku2.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.CellCollection.Companion.DATA_VERSION_ORIGINAL
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.gui.PuzzleListFilter
import java.io.Closeable
import java.time.Instant
import java.util.LinkedList

/**
 * Wrapper around opensudoku2's database.
 *
 * You have to pass application context when creating instance:
 * `SudokuDatabase db = new SudokuDatabase(getApplicationContext());`
 *
 * You have to explicitly close connection when you're done with database (see [.close]).
 *
 * This class supports database transactions using [.beginTransaction], \
 * [.setTransactionSuccessful] and [.endTransaction].
 * See [SQLiteDatabase] for details on how to use them.
 */
class SudokuDatabase(context: Context) : Closeable {
	private val mOpenHelper: DatabaseHelper = DatabaseHelper(context)
	val writable: SQLiteDatabase
		get() = mOpenHelper.writableDatabase

	private val readable: SQLiteDatabase
		get() = mOpenHelper.readableDatabase

	/**
	 * Returns list of puzzle folders.
	 */
	fun getFolderList(withCounts: Boolean = false): List<FolderInfo> {
		val qb = SQLiteQueryBuilder()
		val folderList: MutableList<FolderInfo> = LinkedList()
		qb.tables = Names.FOLDER
		readable.use { db ->
			qb.query(db, null, null, null, null, null, null).forEach { cursor ->
				val folderInfo = FolderInfo()
				folderInfo.id = cursor.getLong(cursor.getColumnIndexOrThrow(Names.ID))
				folderInfo.name = cursor.getString(cursor.getColumnIndexOrThrow(Names.FOLDER_NAME))
				folderInfo.created = cursor.getLong(cursor.getColumnIndexOrThrow(Names.FOLDER_CREATED))
				if (withCounts) {
					folderList.add(getFolderInfoWithCounts(folderInfo.id))
				} else {
					folderList.add(folderInfo)
				}
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
		mOpenHelper.readableDatabase.use { db ->
			qb.query(db, null, Names.ID + "=" + folderId, null, null, null, null).use { cursor ->
				return@getFolderInfo if (cursor.moveToFirst()) {
					val id = cursor.getLong(cursor.getColumnIndexOrThrow(Names.ID))
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
	}

	/**
	 * Returns the folder info.
	 *
	 * @param folderName Name of the folder to get info.
	 */
	private fun getFolderInfo(folderName: String): FolderInfo? {
		val qb = SQLiteQueryBuilder()
		qb.tables = Names.FOLDER
		mOpenHelper.readableDatabase.use { db ->
			qb.query(db, null, Names.FOLDER_NAME + "=?", arrayOf(folderName), null, null, null).use { cursor ->
				return@getFolderInfo if (cursor.moveToFirst()) {
					val id = cursor.getLong(cursor.getColumnIndexOrThrow(Names.ID))
					val name = cursor.getString(cursor.getColumnIndexOrThrow(Names.FOLDER_NAME))
					FolderInfo(id, name)
				} else {
					null
				}
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
		mOpenHelper.readableDatabase.use { db ->
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
		val rowId: Long
		writable.use { db ->
			rowId = db.insert(Names.FOLDER, Names.ID, values)
			if (rowId > 0) {
				return@insertFolder FolderInfo(rowId, name)
			}
		}
		throw SQLException("Failed to insert folder '$name'.")
	}

	/**
	 * Renames existing folder.
	 *
	 * @param folderId Primary key of folder.
	 * @param name     New name for the folder.
	 */
	fun renameFolder(folderId: Long, name: String) {
		val existingFolder = getFolderInfo(name)
		if (existingFolder != null) {
			throw SQLException("Folder of this name already exists.")
		}
		val values = ContentValues()
		values.put(Names.FOLDER_NAME, name)
		writable.use { db ->
			db.update(Names.FOLDER, values, Names.ID + "=" + folderId, null)
		}
	}

	/**
	 * Deletes given folder.
	 *
	 * @param folderId Primary key of folder.
	 */
	fun deleteFolder(folderId: Long) {
		writable.use { db ->
			// delete all puzzles in folder we are going to delete
			db.delete(Names.GAME, Names.FOLDER_ID + "=" + folderId, null)
			// delete the folder
			db.delete(Names.FOLDER, Names.ID + "=" + folderId, null)
		}
	}

	fun insertPuzzle(newPuzzle: SudokuGame) {
		writable.use { it.insertPuzzle(newPuzzle) }
	}

	fun updatePuzzle(game: SudokuGame) {
		writable.use { it.updatePuzzle(game) }
	}

	/**
	 * Deletes given puzzle from the database.
	 */
	fun deletePuzzle(puzzleID: Long) {
		writable.use { db ->
			db.delete(Names.GAME, Names.ID + "=" + puzzleID, null)
		}
	}

	fun findPuzzle(cells: CellCollection): SudokuGame? {
		readable.use { db ->
			with(SQLiteQueryBuilder()) {
				tables = Names.GAME
				query(
					db,
					null,
					Names.ORIGINAL_VALUES + "=?",
					arrayOf(cells.serialize(DATA_VERSION_ORIGINAL)),
					null,
					null,
					null
				).use { cursor ->
					if (cursor.moveToFirst()) return@findPuzzle extractSudokuGameFromCursorRow(cursor)
				}
			}
		}

		return null
	}

	override fun close() {
		mOpenHelper.close()
	}

	fun getPuzzleListCursor(folderID: Long, filter: PuzzleListFilter?, sortOrder: String?): Cursor =
		readable.getPuzzleListCursor(folderID, filter, sortOrder)

	fun getPuzzle(puzzleId: Long): SudokuGame? = readable.use { it.getPuzzle(puzzleId) }

	companion object {
		const val DATABASE_NAME = "opensudoku2"
	}
}

internal fun SQLiteDatabase.updatePuzzle(game: SudokuGame): Int = update(Names.GAME, game.contentValues, Names.ID + "=" + game.id, null)

/**
 * Returns sudoku game object.
 *
 * @param gameID Primary key of folder.
 */
internal fun SQLiteDatabase.getPuzzle(gameID: Long): SudokuGame? {
	with(SQLiteQueryBuilder()) {
		tables = Names.GAME
		query(this@getPuzzle, null, Names.ID + "=" + gameID, null, null, null, null).use { cursor ->
			if (cursor.moveToFirst()) {
				return@getPuzzle extractSudokuGameFromCursorRow(cursor)
			}
		}
	}
	return null
}

internal fun SQLiteDatabase.puzzleExists(originalValues: String): Boolean {
	with(SQLiteQueryBuilder()) {
		tables = Names.GAME
		query(this@puzzleExists, null, Names.ORIGINAL_VALUES + "=?", arrayOf(originalValues), null, null, null).use { cursor ->
			if (cursor.moveToFirst()) return@puzzleExists true
		}
	}
	return false
}

internal fun SQLiteDatabase.insertPuzzle(originalValues: String, folderId: Long): Boolean {
	with(ContentValues()) {
		put(Names.ORIGINAL_VALUES, originalValues)
		put(Names.CREATED, Instant.now().epochSecond)
		put(Names.LAST_PLAYED, 0)
		put(Names.STATE, SudokuGame.GAME_STATE_NOT_STARTED)
		put(Names.TIME, 0)
		put(Names.USER_NOTE, "")
		put(Names.COMMAND_STACK, "")
		put(Names.FOLDER_ID, folderId)
		val rowId = insert(Names.GAME, null, this)
		if (rowId > 0) {
			return@insertPuzzle true
		}
	}
	throw SQLException("Failed to insert puzzle.")
}

internal fun SQLiteDatabase.insertPuzzle(newGame: SudokuGame) {
	val rowId = insert(Names.GAME, null, newGame.contentValues)
	if (rowId <= 0) {
		throw SQLException("Failed to insert puzzle.")
	}
}

/**
 * Returns list of sudoku game objects
 *
 * @param folderId Primary key of folder.
 */
internal fun SQLiteDatabase.getPuzzleListCursor(folderId: Long = 0L, filter: PuzzleListFilter? = null, sortOrder: String? = null): Cursor {
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
	return qb.query(this@getPuzzleListCursor, null, null, null, null, null, sortOrder)
}

internal fun extractSudokuGameFromCursorRow(cursor: Cursor): SudokuGame {
	val game = SudokuGame()
	with(game) {
		id = cursor.getLong(cursor.getColumnIndexOrThrow(Names.ID))
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
		if (state == SudokuGame.GAME_STATE_PLAYING) {
			commandStack.deserialize(cursor.getString(cursor.getColumnIndexOrThrow(Names.COMMAND_STACK)))
		}
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
