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

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.game.FolderInfo

internal class FolderListRecyclerAdapter(
	private val mContext: Context,
	private var folders: List<FolderInfo>,
	private val onClickListener: (Long) -> Unit
) : RecyclerView.Adapter<FolderListRecyclerAdapter.ViewHolder?>() {

	var selectedFolderId: Long = 0

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.folder_list_item, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = folders.size

	@SuppressLint("NotifyDataSetChanged")
	fun updateFoldersList(newFolders: List<FolderInfo>) {
		folders = newFolders
		notifyDataSetChanged()
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val folder: FolderInfo = folders[position]
		holder.itemView.setOnClickListener { onClickListener(folder.id) }
		holder.itemView.setOnCreateContextMenuListener { menu, _, _ ->
			selectedFolderId = folder.id
			with(menu ?: return@setOnCreateContextMenuListener) {
				setHeaderTitle(folder.name)
				add(0, FolderListActivity.MENU_ITEM_EXPORT, 0, R.string.export_folder)
				add(0, FolderListActivity.MENU_ITEM_RENAME, 1, R.string.rename_folder)
				add(0, FolderListActivity.MENU_ITEM_DELETE, 2, R.string.delete_folder)
			}
		}

		holder.name.text = folder.name
		holder.detail.text = folder.getDetail(mContext)
	}

	internal class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val name: TextView = itemView.findViewById(R.id.name)
		val detail: TextView = itemView.findViewById(R.id.detail)
	}
}
