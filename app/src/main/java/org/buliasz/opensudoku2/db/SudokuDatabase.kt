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
import android.database.sqlite.SQLiteStatement
import android.util.Log
import org.buliasz.opensudoku2.game.CellCollection
import org.buliasz.opensudoku2.game.FolderInfo
import org.buliasz.opensudoku2.game.SudokuGame
import org.buliasz.opensudoku2.game.command.CommandStack
import org.buliasz.opensudoku2.gui.SudokuListFilter
import org.buliasz.opensudoku2.gui.SudokuListSorter
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
 *
 * @author romario, Kotlin version by buliasz
 */
class SudokuDatabase(context: Context) {
    private val mOpenHelper: DatabaseHelper = DatabaseHelper(context)
    private var mInsertSudokuStatement: SQLiteStatement? = null

    val folderList: Cursor
        /**
         * Returns list of puzzle folders.
         *
         * @return
         */
        get() {
            val qb = SQLiteQueryBuilder()
            qb.tables = Names.FOLDER
            val db = mOpenHelper.readableDatabase
            return qb.query(db, null, null, null, null, null, "${Names.FOLDER_CREATED} ASC")
        }

    /**
     * Returns the folder info.
     *
     * @param folderID Primary key of folder.
     * @return
     */
    fun getFolderInfo(folderID: Long): FolderInfo? {
        val qb = SQLiteQueryBuilder()
        qb.tables = Names.FOLDER
        qb.appendWhere(Names.ID + "=" + folderID)
        mOpenHelper.readableDatabase.use { db ->
            qb.query(db, null, null, null, null, null, null).use { c ->
                return@getFolderInfo if (c.moveToFirst()) {
                    val id = c.getLong(c.getColumnIndexOrThrow(Names.ID))
                    val name = c.getString(c.getColumnIndexOrThrow(Names.FOLDER_NAME))
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
     * Returns the full folder info - this includes count of games in particular states.
     *
     * @param folderID Primary key of folder.
     * @return folder info
     */
    fun getFolderInfoFull(folderID: Long): FolderInfo? {
        var folder: FolderInfo? = null
        val q = "select f.${Names.ID} as ${Names.ID}, f.${Names.FOLDER_NAME} as ${Names.FOLDER_NAME}, " +
                "g.${Names.STATE} as ${Names.STATE}, count(g.${Names.STATE}) as ${Names.COUNT} " +
                "from ${Names.FOLDER} f left join ${Names.GAME} g on f.${Names.ID} = g.${Names.FOLDER_ID} " +
                "where f.${Names.ID} = $folderID " +
                "group by g.${Names.STATE}"
        val db: SQLiteDatabase = mOpenHelper.readableDatabase
        db.rawQuery(q, null).use { c ->
            // selectionArgs: You may include ?s in where clause in the query, which will be replaced by the values from selectionArgs.
            // The values will be bound as Strings.
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(Names.ID))
                val name = c.getString(c.getColumnIndexOrThrow(Names.FOLDER_NAME))
                val state = c.getInt(c.getColumnIndexOrThrow(Names.STATE))
                val count = c.getInt(c.getColumnIndexOrThrow(Names.COUNT))
                if (folder == null) {
                    folder = FolderInfo(id, name)
                }
                folder!!.puzzleCount += count
                if (state == SudokuGame.GAME_STATE_COMPLETED) {
                    folder!!.solvedCount += count
                } else if (state == SudokuGame.GAME_STATE_PLAYING) {
                    folder!!.playingCount += count
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
    fun insertFolder(name: String?, created: Long?): FolderInfo {
        val values = ContentValues()
        values.put(Names.FOLDER_CREATED, created)
        values.put(Names.FOLDER_NAME, name)
        val rowId: Long
        val db = mOpenHelper.writableDatabase
        rowId = db.insert(Names.FOLDER, Names.ID, values)
        if (rowId > 0) {
            val fi = FolderInfo()
            fi.id = rowId
            fi.name = name
            return fi
        }
        throw SQLException("Failed to insert folder '$name'.")
    }

    /**
     * Updates folder's information.
     *
     * @param folderID Primary key of folder.
     * @param name     New name for the folder.
     */
    fun updateFolder(folderID: Long, name: String?) {
        val values = ContentValues()
        values.put(Names.FOLDER_NAME, name)
        val db: SQLiteDatabase = mOpenHelper.writableDatabase
        db.update(Names.FOLDER, values, Names.ID + "=" + folderID, null)
    }

    /**
     * Deletes given folder.
     *
     * @param folderID Primary key of folder.
     */
    fun deleteFolder(folderID: Long) {

        // TODO: should run in transaction
        val db = mOpenHelper.writableDatabase
        // delete all puzzles in folder we are going to delete
        db.delete(Names.GAME, Names.FOLDER_ID + "=" + folderID, null)
        // delete the folder
        db.delete(Names.FOLDER, Names.ID + "=" + folderID, null)
    }

    /**
     * Returns list of puzzles in the given folder.
     *
     * @param folderID Primary key of folder.
     * @return
     */
    fun getSudokuList(folderID: Long, filter: SudokuListFilter?, sorter: SudokuListSorter): Cursor {
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
        val db = mOpenHelper.readableDatabase
        return qb.query(db, null, null, null, null, null, sorter.sortOrder)
    }

    /**
     * Returns list of sudoku game objects
     *
     * @param folderID Primary key of folder.
     */
    fun getAllSudokuByFolder(folderID: Long, sorter: SudokuListSorter): List<SudokuGame> {
        val cursor = getSudokuList(folderID, null, sorter)
        if (cursor.moveToFirst()) {
            val sudokuList: MutableList<SudokuGame> = LinkedList()
            while (!cursor.isAfterLast) {
                sudokuList.add(extractSudokuGameFromCursorRow(cursor))
                cursor.moveToNext()
            }
            return sudokuList
        }
        return emptyList()
    }

    /**
     * Returns sudoku game object.
     *
     * @param gameID Primary key of folder.
     */
    fun getSudoku(gameID: Long): SudokuGame? {
        val qb = SQLiteQueryBuilder()
        qb.tables = Names.GAME
        qb.appendWhere(Names.ID + "=" + gameID)

        // Get the database and run the query
        var s: SudokuGame? = null
        mOpenHelper.readableDatabase.use { db ->
            qb.query(db, null, null, null, null, null, null).use { c ->
                if (c.moveToFirst()) {
                    s = extractSudokuGameFromCursorRow(c)
                }
            }
        }
        return s
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
        }
        if (game.state == SudokuGame.GAME_STATE_PLAYING) {
            val commandStack = cursor.getString(cursor.getColumnIndexOrThrow(Names.COMMAND_STACK))
            if (commandStack != null && commandStack != "") {
                game.commandStack = CommandStack.deserialize(commandStack, game.cells)
            }
        }
        return game
    }

    /**
     * Inserts new puzzle into the database.
     *
     * @param folderID Primary key of the folder in which puzzle should be saved.
     */
    fun insertSudoku(folderID: Long, game: SudokuGame): Long {
        val db = mOpenHelper.writableDatabase
        val values = ContentValues()
        values.put(Names.CELLS_DATA, game.cells.serialize())
        values.put(Names.CREATED, game.created)
        values.put(Names.LAST_PLAYED, game.lastPlayed)
        values.put(Names.STATE, game.state)
        values.put(Names.TIME, game.time)
        values.put(Names.USER_NOTE, game.userNote)
        values.put(Names.FOLDER_ID, folderID)
        var commandStack = ""
        if (game.state == SudokuGame.GAME_STATE_PLAYING) {
            commandStack = game.commandStack.serialize()
        }
        values.put(Names.COMMAND_STACK, commandStack)
        val rowId = db.insert(Names.GAME, Names.FOLDER_NAME, values)
        if (rowId > 0) {
            return rowId
        }
        throw SQLException("Failed to insert sudoku.")
    }

    @Throws(SudokuInvalidFormatException::class)
    fun importSudoku(folderID: Long, importParams: SudokuImportParams) {
        if (!CellCollection.isValid(importParams.cellsData)) {
            Log.d(this.javaClass.simpleName, "data=${importParams.cellsData}")
            throw SudokuInvalidFormatException(importParams.cellsData)
        }
        if (mInsertSudokuStatement == null) {
            val db = mOpenHelper.writableDatabase
            mInsertSudokuStatement = db.compileStatement(
                "insert into ${Names.GAME} (${Names.FOLDER_ID}, ${Names.CREATED}, ${Names.STATE}, ${Names.TIME}, " +
                        "${Names.LAST_PLAYED}, ${Names.CELLS_DATA}, ${Names.USER_NOTE}, ${Names.COMMAND_STACK}) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?)"
            )
        }
        with(mInsertSudokuStatement!!) {
            bindLong(1, folderID)
            bindLong(2, importParams.created)
            bindLong(3, importParams.state)
            bindLong(4, importParams.time)
            bindLong(5, importParams.lastPlayed)
            bindString(6, importParams.cellsData)
            bindString(7, importParams.userNote)
            bindString(8, importParams.commandStack)

            if (executeInsert() < 0) {
                throw SQLException("Failed to insert OpenSudoku2 row")
            }
        }
    }

    /**
     * Returns List of sudokus to export.
     *
     * @param folderID Id of folder to export, -1 if all folders will be exported.
     * @return
     */
    fun exportFolder(folderID: Long): Cursor {
        var query = "select f.${Names.ID} as ${Names.FOLDER_ID}, f.${Names.FOLDER_NAME}, f.${Names.FOLDER_CREATED}, " +
                "g.${Names.CREATED}, g.${Names.STATE}, g.${Names.TIME}, g.${Names.LAST_PLAYED}, g.${Names.CELLS_DATA}, " +
                "g.${Names.USER_NOTE}, g.${Names.COMMAND_STACK}" +
                " from ${Names.FOLDER} f left outer join ${Names.GAME} g on f.${Names.ID} = g.${Names.FOLDER_ID}"
        val db = mOpenHelper.readableDatabase
        if (folderID != -1L) {
            query += " where f.${Names.ID} = ?"
        }
        return db.rawQuery(query, if (folderID != -1L) arrayOf("$folderID") else null)
    }

    /**
     * Updates sudoku game in the database.
     *
     * @param sudoku
     */
    fun updateSudoku(sudoku: SudokuGame) {
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
        val db = mOpenHelper.writableDatabase
        db.update(Names.GAME, values, Names.ID + "=" + sudoku.id, null)
    }

    /**
     * Deletes given sudoku from the database.
     *
     * @param sudokuID
     */
    fun deleteSudoku(sudokuID: Long) {
        val db = mOpenHelper.writableDatabase
        db.delete(Names.GAME, Names.ID + "=" + sudokuID, null)
    }

    fun close() {
        if (mInsertSudokuStatement != null) {
            mInsertSudokuStatement!!.close()
        }
        mOpenHelper.close()
    }

    fun beginTransaction() {
        mOpenHelper.writableDatabase.beginTransaction()
    }

    fun setTransactionSuccessful() {
        mOpenHelper.writableDatabase.setTransactionSuccessful()
    }

    fun endTransaction() {
        mOpenHelper.writableDatabase.endTransaction()
    }

    companion object {
        const val DATABASE_NAME = "opensudoku2"
    }
}
