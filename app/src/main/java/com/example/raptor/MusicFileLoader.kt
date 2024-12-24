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

// this class is probably temporary. The intention is for it to scan the folder (maybe
// recursively sometime) and then prepare the data to feed it to some other database class.
class MusicFileLoader
@Inject constructor( @ApplicationContext private val context: Context) {
    data class SongFile(val filename: String, val uri: Uri, val mimeType: String)
    var songFileList = MutableStateFlow<List<SongFile>>(emptyList())
        private set

    private lateinit var launcher : ManagedActivityResultLauncher<Uri?, Uri?>

    private fun traverseDirs(treeUri: Uri): List<SongFile> {
        val _songFiles = mutableListOf<SongFile>()

        fun visit(uri: Uri) {
            val root = DocumentFile.fromTreeUri(context, uri)
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

            Log.d(javaClass.simpleName, "Visited dir $root, songs: $songFiles")

            _songFiles.addAll(songFiles?: emptyList())

        }
        visit(treeUri)

        return _songFiles
    }

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

    fun launch() {
        launcher.launch(null)
    }
}
