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
