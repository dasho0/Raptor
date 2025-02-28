package com.example.raptor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.util.fastForEach
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * Handles the loading of music files in a given directory and its subdirectories as well as
 * launching a file picker instance
 */
class MusicFileLoader
@Inject constructor( @ApplicationContext private val context: Context) {
    /**
     * Dataclass representing a song file
     *
     * @param filename Name of the file
     * @param uri `Uri` corresponding to the file
     * @param mimeType The type of the file
     */
    data class SongFile(val filename: String, val uri: Uri, val mimeType: String)

    /**
     * Observable list of `SongFile` currently loaded
     */
    var songFileList = MutableStateFlow<List<SongFile>>(emptyList())
        private set

    private lateinit var launcher : ManagedActivityResultLauncher<Uri?, Uri?>

    private fun traverseDirs(treeUri: Uri): List<SongFile> {
        val _songFiles = mutableListOf<SongFile>()

        fun visit(uri: Uri) {
            val root = DocumentFile.fromTreeUri(context, uri)
            Log.d(javaClass.simpleName, "Visiting dir: ${root?.name}")
            val childDirs = root?.listFiles()?.filter { it.isDirectory }

            childDirs?.fastForEach { visit(it.uri) }

            val songFiles = root?.listFiles()
                ?.filter {
                    it.type?.slice(0..4) == "audio"
                }
                ?.map {
                    SongFile(
                        filename = it.name.toString(),
                        uri = it.uri,
                        mimeType = it.type.toString()
                    )
                }

            Log.d(javaClass.simpleName, "Visited dir ${root?.name}, songs: ${songFiles?.map { it
                .filename 
            }}")

            _songFiles.addAll(songFiles?: emptyList())

        }

        visit(treeUri)

        return _songFiles
    }

    /**
     * \@Composable function that prepares the file picker launcher
     *
     * Must be called before `launch()`
     */
    @Composable
    fun PrepareFilePicker() {
        val contentResolver = context.contentResolver

        launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { treeUri: Uri? ->
            treeUri?.let {
                val permissions = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(treeUri, permissions)

                Log.d(javaClass.simpleName, "Selected: $it")
                songFileList.value = traverseDirs(treeUri)
            }
        }
    }

    /**
     * Launches the file picker
     */
    fun launch() {
        launcher.launch(null)
    }
}