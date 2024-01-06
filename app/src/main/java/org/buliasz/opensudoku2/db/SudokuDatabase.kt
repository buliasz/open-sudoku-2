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
import org.buliasz.opensudoku2.gui.SudokuListFilter
import java.io.Closeable
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

	/**
	 * Returns list of puzzle folders.
	 */
	fun getFolderList(withCounts: Boolean = false): List<FolderInfo> {
		val qb = SQLiteQueryBuilder()
		val folderList: MutableList<FolderInfo> = LinkedList()
		qb.tables = Names.FOLDER
		mOpenHelper.readableDatabase.use { db ->
			qb.query(db, null, null, null, null, null, null).use { cursor ->
				if (cursor.moveToFirst()) {
					while (!cursor.isAfterLast) {
						val folderInfo = FolderInfo()
						folderInfo.id = cursor.getLong(cursor.getColumnIndexOrThrow(Names.ID))
						folderInfo.name = cursor.getString(cursor.getColumnIndexOrThrow(Names.FOLDER_NAME))
						folderInfo.created = cursor.getLong(cursor.getColumnIndexOrThrow(Names.FOLDER_CREATED))
						if (withCounts) {
							folderList.add(getFolderInfoWithCounts(folderInfo.id))
						} else {
							folderList.add(folderInfo)
						}
						cursor.moveToNext()
					}
				}
			}
		}
		return folderList
	}

	/**
	 * Returns the folder info.
	 *
	 * @param folderID Primary key of folder.
	 */
	fun getFolderInfo(folderID: Long): FolderInfo? {
		val qb = SQLiteQueryBuilder()
		qb.tables = Names.FOLDER
		qb.appendWhere(Names.ID + "=" + folderID)
		mOpenHelper.readableDatabase.use { db ->
			qb.query(db, null, null, null, null, null, null).use { cursor ->
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
		qb.appendWhere(Names.FOLDER_NAME + "=?")
		mOpenHelper.readableDatabase.use { db ->
			qb.query(db, null, null, arrayOf(folderName), null, null, null).use { cursor ->
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
	 * @param folderID Primary key of folder.
	 * @return folder info
	 */
	fun getFolderInfoWithCounts(folderID: Long): FolderInfo {
		val folder = FolderInfo(folderID, null)
		val q = "select f.${Names.ID} as ${Names.ID}, f.${Names.FOLDER_NAME} as ${Names.FOLDER_NAME}, " +
				"g.${Names.STATE} as ${Names.STATE}, count(g.${Names.STATE}) as ${Names.COUNT} " +
				"from ${Names.FOLDER} f left join ${Names.GAME} g on f.${Names.ID} = g.${Names.FOLDER_ID} " +
				"where f.${Names.ID} = $folderID " +
				"group by g.${Names.STATE}"
		mOpenHelper.readableDatabase.use { db ->
			db.rawQuery(q, null).use { cursor ->
				// selectionArgs: You may include ?s in where clause in the query, which will be replaced by the values from selectionArgs.
				// The values will be bound as Strings.
				while (cursor.moveToNext()) {
					val state = cursor.getInt(cursor.getColumnIndexOrThrow(Names.STATE))
					val count = cursor.getInt(cursor.getColumnIndexOrThrow(Names.COUNT))
					if (folder.name == null) {
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
		mOpenHelper.writableDatabase.use { db ->
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
	 * @param folderID Primary key of folder.
	 * @param name     New name for the folder.
	 */
	fun renameFolder(folderID: Long, name: String) {
		val existingFolder = getFolderInfo(name)
		if (existingFolder != null) {
			throw SQLException("Folder of this name already exists.")
		}
		val values = ContentValues()
		values.put(Names.FOLDER_NAME, name)
		mOpenHelper.writableDatabase.use { db ->
			db.update(Names.FOLDER, values, Names.ID + "=" + folderID, null)
		}
	}

	/**
	 * Deletes given folder.
	 *
	 * @param folderID Primary key of folder.
	 */
	fun deleteFolder(folderID: Long) {
		mOpenHelper.writableDatabase.use { db ->
			// delete all puzzles in folder we are going to delete
			db.delete(Names.GAME, Names.FOLDER_ID + "=" + folderID, null)
			// delete the folder
			db.delete(Names.FOLDER, Names.ID + "=" + folderID, null)
		}
	}

	/**
	 * Returns list of sudoku game objects
	 *
	 * @param folderID Primary key of folder.
	 */
	fun getSudokuGameList(folderID: Long, filter: SudokuListFilter?, sortOrder: String?): List<SudokuGame> {
		val qb = SQLiteQueryBuilder()
		qb.tables = Names.GAME
		//qb.setProjectionMap(sPlacesProjectionMap);
		qb.appendWhere(Names.FOLDER_ID + "=" + folderID)
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
		mOpenHelper.readableDatabase.use { db ->
			qb.query(db, null, null, null, null, null, sortOrder).use { cursor ->
				if (cursor.moveToFirst()) {
					val sudokuList: MutableList<SudokuGame> = LinkedList()
					while (!cursor.isAfterLast) {
						sudokuList.add(extractSudokuGameFromCursorRow(cursor))
						cursor.moveToNext()
					}
					return@getSudokuGameList sudokuList
				}
			}
		}
		return emptyList()
	}

	/**
	 * Returns sudoku game object.
	 *
	 * @param gameID Primary key of folder.
	 */
	fun getGame(gameID: Long): SudokuGame? {
		val qb = SQLiteQueryBuilder()
		qb.tables = Names.GAME
		qb.appendWhere(Names.ID + "=" + gameID)

		// Get the database and run the query
		mOpenHelper.readableDatabase.use { db ->
			qb.query(db, null, null, null, null, null, null).use { cursor ->
				if (cursor.moveToFirst()) {
					return@getGame extractSudokuGameFromCursorRow(cursor)
				}
			}
		}
		return null
	}

	private fun extractSudokuGameFromCursorRow(cursor: Cursor): SudokuGame {
		val game = SudokuGame()
		with(game) {
			id = cursor.getLong(cursor.getColumnIndexOrThrow(Names.ID))
			created = cursor.getLong(cursor.getColumnIndexOrThrow(Names.CREATED))
			cells = CellCollection.deserialize(cursor.getString(cursor.getColumnIndexOrThrow(Names.CELLS_DATA)))
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

	fun puzzleExists(gameToCheckCells: CellCollection): SudokuGame? {
		val qb = SQLiteQueryBuilder()
		qb.tables = Names.GAME
		val gameToCheckSerialized = gameToCheckCells.serialize(DATA_VERSION_ORIGINAL)

		// Get the database and run the query
		mOpenHelper.readableDatabase.use { db ->
			qb.query(db, null, null, null, null, null, null).use { cursor ->
				if (cursor.moveToFirst()) {
					while (!cursor.isAfterLast) {
						val existingGame = extractSudokuGameFromCursorRow(cursor)
						if (existingGame.cells.serialize(DATA_VERSION_ORIGINAL) == gameToCheckSerialized) {
							return@puzzleExists existingGame
						}
						cursor.moveToNext()
					}
				}

			}
		}
		return null
	}

	/**
	 * Inserts new puzzle into the database.
	 */
	fun insertPuzzle(newGame: SudokuGame): Long {
		mOpenHelper.writableDatabase.use { db ->
			val values = ContentValues()
			values.put(Names.CELLS_DATA, newGame.cells.serialize())
			values.put(Names.CREATED, newGame.created)
			values.put(Names.LAST_PLAYED, newGame.lastPlayed)
			values.put(Names.STATE, newGame.state)
			values.put(Names.TIME, newGame.time)
			values.put(Names.USER_NOTE, newGame.userNote)
			values.put(Names.FOLDER_ID, newGame.folderId)
			var commandStack = ""
			if (newGame.state == SudokuGame.GAME_STATE_PLAYING) {
				commandStack = newGame.commandStack.serialize()
			}
			values.put(Names.COMMAND_STACK, commandStack)
			val rowId = db.insert(Names.GAME, null, values)
			if (rowId > 0) {
				return@insertPuzzle rowId
			}
		}
		throw SQLException("Failed to insert sudoku.")
	}

	/**
	 * Updates sudoku game in the database.
	 */
	fun updatePuzzle(sudoku: SudokuGame) {
		val values = ContentValues()
		values.put(Names.CELLS_DATA, sudoku.cells.serialize())
		values.put(Names.LAST_PLAYED, sudoku.lastPlayed)
		values.put(Names.STATE, sudoku.state)
		values.put(Names.TIME, sudoku.time)
		values.put(Names.USER_NOTE, sudoku.userNote)
		var commandStack = ""
		if (sudoku.state == SudokuGame.GAME_STATE_PLAYING) {
			commandStack = sudoku.commandStack.serialize()
		}
		values.put(Names.COMMAND_STACK, commandStack)
		mOpenHelper.writableDatabase.use { db ->
			db.update(Names.GAME, values, Names.ID + "=" + sudoku.id, null)
		}
	}

	/**
	 * Deletes given sudoku from the database.
	 */
	fun deletePuzzle(sudokuID: Long) {
		mOpenHelper.writableDatabase.use { db ->
			db.delete(Names.GAME, Names.ID + "=" + sudokuID, null)
		}
	}

	override fun close() {
		mOpenHelper.close()
	}

	companion object {
		const val DATABASE_NAME = "opensudoku2"
	}
}
