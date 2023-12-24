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
package org.buliasz.opensudoku2.game.command

import java.util.StringTokenizer

/**
 * Generic interface for command in application.
 */
abstract class AbstractCommand {
	protected open fun deserialize(data: StringTokenizer) {}
	open fun serialize(data: StringBuilder) {
		val cmdLongName = commandClass
		for (cmdDef in commands) {
			if (cmdDef.longName == cmdLongName) {
				data.append(cmdDef.shortName).append("|")
				return
			}
		}
		throw IllegalArgumentException("Unknown command class '$cmdLongName'.")
	}

	private val commandClass: String
		get() = javaClass.simpleName

	/**
	 * Executes the command.
	 */
	abstract fun execute()

	/**
	 * Undo this command.
	 */
	abstract fun undo()
	private interface CommandCreatorFunction {
		fun create(): AbstractCommand
	}

	private class CommandDef(var longName: String, var shortName: String, var mCreator: CommandCreatorFunction) {
		fun create(): AbstractCommand = mCreator.create()
	}

	companion object {
		private val commands = arrayOf(
			CommandDef(ClearAllNotesCommand::class.java.simpleName, "c1",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = ClearAllNotesCommand()
				}),
			CommandDef(EditCellCornerNoteCommand::class.java.simpleName, "c2",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = EditCellCornerNoteCommand()
				}),
			CommandDef(FillInNotesCommand::class.java.simpleName, "c3",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = FillInNotesCommand()
				}),
			CommandDef(SetCellValueCommand::class.java.simpleName, "c4",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = SetCellValueCommand()
				}),
			CommandDef(CheckpointCommand::class.java.simpleName, "c5",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = CheckpointCommand()
				}),
			CommandDef(SetCellValueAndRemoveNotesCommand::class.java.simpleName, "c6",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = SetCellValueAndRemoveNotesCommand()
				}),
			CommandDef(FillInNotesWithAllValuesCommand::class.java.simpleName, "c7",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = FillInNotesWithAllValuesCommand()
				}),
			CommandDef(EditCellCenterNoteCommand::class.java.simpleName, "c8",
				object : CommandCreatorFunction {
					override fun create(): AbstractCommand = EditCellCenterNoteCommand()
				})
		)

		fun deserialize(data: StringTokenizer): AbstractCommand {
			val cmdShortName = data.nextToken()
			for (cmdDef in commands) {
				if (cmdDef.shortName == cmdShortName) {
					val cmd = cmdDef.create()
					cmd.deserialize(data)
					return cmd
				}
			}
			throw IllegalArgumentException("Unknown command class '$cmdShortName'.")
		}
	}
}
