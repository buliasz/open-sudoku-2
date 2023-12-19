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

package org.buliasz.opensudoku2.gui.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.radiobutton.MaterialRadioButton
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.Cell
import org.buliasz.opensudoku2.gui.SudokuBoardView
import org.buliasz.opensudoku2.gui.fragments.ThemePreferenceDialogFragment.ThemeAdapter.ViewHolder
import org.buliasz.opensudoku2.utils.ThemeUtils

/**
 * Dialog fragment that displays a list of themes and a sudoku board to
 * preview the theme.
 *
 * Necessary because although an AlertDialog allows you to display a list of
 * items and specify a custom view, you cannot do both of these things at
 * the same time.
 *
 * This class is a reimplementation of [ListPreferenceDialogFragmentCompat]
 * but without calling setSingleChoiceItems() on the AlertDialog.Builder.
 *
 * This allows the class to set its own view which includes the list view
 * as well as other views.
 *
 * This also means this class has to manage the list adapter and recycler
 * view as well, instead of allowing the AlertDialog to do it.
 */
class ThemePreferenceDialogFragment : ListPreferenceDialogFragmentCompat() {
    private var mBoard: SudokuBoardView? = null
    var mClickedDialogEntryIndex = 0
    private var mEntries: Array<CharSequence?>? = null
    private var mEntryValues: Array<CharSequence>? = null
    private var mAdapter: ThemeAdapter? = null
    private val mOnItemClickListener = View.OnClickListener { v ->
        val viewHolder = v.tag as ViewHolder
        val prevSelectedPosition = mClickedDialogEntryIndex
        mClickedDialogEntryIndex = viewHolder.adapterPosition
        mAdapter!!.notifyItemChanged(prevSelectedPosition)
        mAdapter!!.notifyItemChanged(mClickedDialogEntryIndex)
        applyThemePreview(mEntryValues!![mClickedDialogEntryIndex] as String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = listPreference
            check(!(preference.entries == null || preference.entryValues == null)) {
                "ListPreference requires an entries array and an entryValues array."
            }
            val themeCode = preference.value
            mClickedDialogEntryIndex = preference.findIndexOfValue(themeCode)
            mEntries = preference.entries
            mEntryValues = preference.entryValues
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
    }

    private val listPreference: ListPreference
        get() = preference as ListPreference

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val inflater = LayoutInflater.from(context)
        val preferenceView = inflater.inflate(R.layout.preference_dialog_sudoku_board_theme, null)
        mBoard = preferenceView.findViewById(R.id.board_view)
        val recyclerView = preferenceView.findViewById<RecyclerView>(R.id.theme_list)
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        layoutManager.scrollToPosition(mClickedDialogEntryIndex)
        mAdapter = ThemeAdapter(mEntries)
        recyclerView.adapter = mAdapter
        mAdapter!!.setOnItemClickListener(mOnItemClickListener)
        prepareSudokuPreviewView("${mEntryValues!![mClickedDialogEntryIndex]}")
        builder.setView(preferenceView)
        builder.setTitle("")
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            val value = "${mEntryValues!![mClickedDialogEntryIndex]}"
            val preference = listPreference
            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

    internal inner class ThemeAdapter(private val mEntries: Array<CharSequence?>?) : RecyclerView.Adapter<ViewHolder?>() {
        private var mOnItemClickListener: View.OnClickListener? = null

        /** Drawable for icon that indicates this theme enforces dark mode  */
        private val mDarkModeIcon: Drawable? =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_dark_mode, requireContext().theme)

        internal inner class ViewHolder(itemView: MaterialRadioButton) : RecyclerView.ViewHolder(itemView) {
            val radioButton: MaterialRadioButton

            init {
                itemView.tag = this
                itemView.setOnClickListener(mOnItemClickListener)
                radioButton = itemView.findViewById(android.R.id.text1)
            }
        }

        fun setOnItemClickListener(itemClickListener: View.OnClickListener?) {
            mOnItemClickListener = itemClickListener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(
                R.layout.preference_dialog_theme_listitem,
                parent, false
            ) as MaterialRadioButton
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val button = holder.radioButton
            button.text = mEntries!![position]
            button.isChecked = position == mClickedDialogEntryIndex
            if (ThemeUtils.isDarkTheme(mEntryValues!![position] as String)) {
                button.setCompoundDrawablesWithIntrinsicBounds(null, null, mDarkModeIcon, null)
            } else {
                button.setCompoundDrawables(null, null, null, null)
            }
        }

        override fun getItemCount(): Int = mEntries?.size ?: 0
    }

    private fun prepareSudokuPreviewView(initialTheme: String) {
        mBoard!!.setOnCellSelectedListener { cell: Cell? ->
            mBoard!!.setHighlightedValue(cell?.value ?: 0)
        }
        ThemeUtils.prepareSudokuPreviewView(mBoard!!)
        applyThemePreview(initialTheme)
    }

    private fun applyThemePreview(theme: String) {
        ThemeUtils.applyThemeToSudokuBoardViewFromContext(theme, mBoard!!, requireContext())
    }

    companion object {
        var TAG = "ThemePreferenceDialogFragment"
        private const val SAVE_STATE_INDEX = "ThemePreferenceDialogFragment.index"
        private const val SAVE_STATE_ENTRIES = "ThemePreferenceDialogFragment.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "ThemePreferenceDialogFragment.entryValues"
        fun newInstance(key: String?): ThemePreferenceDialogFragment {
            val fragment = ThemePreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}
