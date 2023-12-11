package org.buliasz.opensudoku2.gui.importing

import android.content.Context
import android.net.Uri
import org.buliasz.opensudoku2.db.SudokuInvalidFormatException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL

/**
 * Handles import of .sdm files (see http://sudocue.net/download.php).
 *
 * @author romario, Kotlin version by buliasz
 */
class SdmImportTask(private val mUri: Uri) : AbstractImportTask() {
    @Throws(SudokuInvalidFormatException::class)
    override fun processImport(context: Context) {
        importFolder(mUri.lastPathSegment)
        val isr: InputStreamReader
        try {
            isr = if (mUri.scheme == "content") {
                val contentResolver = context.contentResolver
                InputStreamReader(contentResolver.openInputStream(mUri))
            } else {
                val url = URL(mUri.toString())
                InputStreamReader(url.openStream())
            }
            BufferedReader(isr).use { br ->
                var s: String
                while (br.readLine().also { s = it } != null) {
                    if (s != "") {
                        if (s.contains(".")) {
                            s = s.replace(".", "0")
                        }
                        importGame(s)
                    }
                }
            }
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
