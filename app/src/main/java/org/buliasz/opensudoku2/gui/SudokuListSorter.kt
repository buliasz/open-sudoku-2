package org.buliasz.opensudoku2.gui

import org.buliasz.opensudoku2.db.Names

class SudokuListSorter @JvmOverloads constructor(sortType: Int = SORT_BY_CREATED, var isAscending: Boolean = false) {
    internal var sortType: Int = sortType
        set(value) {
            field = if (value in 0..<SORT_TYPE_OPTIONS_LENGTH) value else SORT_BY_CREATED
        }


    val sortOrder: String
        get() {
            val order = if (isAscending) " ASC" else " DESC"
            when (sortType) {
                SORT_BY_CREATED -> return Names.CREATED + order
                SORT_BY_TIME -> return Names.TIME + order
                SORT_BY_LAST_PLAYED -> return Names.LAST_PLAYED + order
            }
            return Names.CREATED + order
        }

    companion object {
        const val SORT_BY_CREATED = 0
        const val SORT_BY_TIME = 1
        const val SORT_BY_LAST_PLAYED = 2
        private const val SORT_TYPE_OPTIONS_LENGTH = 3
    }
}
