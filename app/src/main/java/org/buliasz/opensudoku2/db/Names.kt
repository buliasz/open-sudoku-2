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

import android.provider.BaseColumns

object Names : BaseColumns {
    const val FOLDER = "folder"
    const val GAME = "game"
    const val ID = BaseColumns._ID
    const val FOLDER_ID = "folder_id"
    const val CREATED = "created"
    const val STATE = "state"
    const val TIME = "time"
    const val LAST_PLAYED = "last_played"
    const val CELLS_DATA = "cells_data"
    const val USER_NOTE = "user_note"
    const val COMMAND_STACK = "command_stack"
    const val FOLDER_NAME = "folder_name"
    const val FOLDER_CREATED = "folder_created"
    const val COUNT = "count"
}
