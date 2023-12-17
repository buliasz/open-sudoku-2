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

package org.buliasz.opensudoku2.gui.importing

import android.content.Context
import android.net.Uri
import org.buliasz.opensudoku2.R
import org.buliasz.opensudoku2.db.Names
import org.buliasz.opensudoku2.db.SudokuImportParams
import org.buliasz.opensudoku2.db.SudokuInvalidFormatException
import org.buliasz.opensudoku2.gui.exporting.FileExportTask
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException

/**
 * Handles import of application/x-opensudoku2 or .opensudoku2 files.
 *
 * @author romario, Kotlin version by buliasz
 */
class OpenSudoku2ImportTask(private val mUri: Uri) : AbstractImportTask() {
    @Throws(SudokuInvalidFormatException::class)
    override fun processImport(context: Context) {
        try {
            val streamReader: InputStreamReader
            if (mUri.scheme == "content") {
                val contentResolver = context.contentResolver
                streamReader = InputStreamReader(contentResolver.openInputStream(mUri))
            } else {
                val juri: URI = URI(mUri.scheme, mUri.schemeSpecificPart, mUri.fragment)
                streamReader = InputStreamReader(juri.toURL().openStream())
            }
            try {
                importXml(context, streamReader)
            } finally {
                streamReader.close()
            }
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    @Throws(SudokuInvalidFormatException::class)
    private fun importXml(context: Context, `in`: Reader) {
        val inBR = BufferedReader(`in`)

        // parse xml
        val factory: XmlPullParserFactory
        val xpp: XmlPullParser
        try {
            factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            xpp = factory.newPullParser()
            xpp.setInput(inBR)
            var eventType = xpp.eventType
            var rootTag: String
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    rootTag = xpp.name
                    if (rootTag == "opensudoku2") {
                        when (xpp.getAttributeValue(null, "version")) {
                            FileExportTask.FILE_EXPORT_VERSION -> {
                                importStateV3(xpp)
                            }

                            else -> {
                                setError("Unknown version of data.")
                            }
                        }
                    } else {
                        setError(context.getString(R.string.invalid_format))
                        return
                    }
                }
                eventType = xpp.next()
            }
        } catch (e: XmlPullParserException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class, SudokuInvalidFormatException::class)
    private fun importStateV3(parser: XmlPullParser) {
        var eventType = parser.eventType
        var lastTag: String
        val importParams = SudokuImportParams()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                lastTag = parser.name
                if (lastTag == Names.FOLDER) {
                    val name = parser.getAttributeValue(null, Names.FOLDER_NAME)
                    val created = parser.getAttributeValue(null, Names.FOLDER_CREATED).toLong()
                    importFolder(name, created)
                } else if (lastTag == Names.GAME) {
                    importParams.clear()
                    importParams.created = parser.getAttributeValue(null, Names.CREATED).toLong()
                    importParams.state = parser.getAttributeValue(null, Names.STATE).toLong()
                    importParams.time = parser.getAttributeValue(null, Names.TIME).toLong()
                    importParams.lastPlayed = parser.getAttributeValue(null, Names.LAST_PLAYED).toLong()
                    importParams.cellsData = parser.getAttributeValue(null, Names.CELLS_DATA)
                    importParams.userNote = parser.getAttributeValue(null, Names.USER_NOTE)
                    importParams.commandStack = parser.getAttributeValue(null, Names.COMMAND_STACK)
                    importGame(importParams)
                }
            }
            eventType = parser.next()
        }
    }
}
