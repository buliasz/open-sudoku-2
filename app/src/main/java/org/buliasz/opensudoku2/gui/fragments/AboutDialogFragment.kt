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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.utils.AndroidUtils

class AboutDialogFragment(private val factory: LayoutInflater) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val aboutView = factory.inflate(R.layout.about, null)
        val versionLabel = aboutView.findViewById<TextView>(R.id.version_label)
        val versionName = AndroidUtils.getAppVersionName(requireContext())
        versionLabel.text = getString(R.string.version, versionName)

        val builder = AlertDialog.Builder(requireActivity())
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.app_name)
            .setView(aboutView)
            .setPositiveButton("OK", null)

        return builder.create()
    }
}
