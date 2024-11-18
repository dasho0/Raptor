package com.example.raptor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow

class MusicFileLoader(private val context: Context) {
    data class SongFile(val filename: String, val uri: Uri, val mimeType: String)
    var songFileList = MutableStateFlow<List<SongFile>>(emptyList())
        private set

    private var launcher: ManagedActivityResultLauncher<Uri?, Uri?>? = null

    @Composable
    fun PrepareFilePicker() {
        // This ensures Compose lifecycle correctly initializes the launcher
        val localLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { treeUri: Uri? ->
            treeUri?.let {
                handleFolderSelection(it)
            }
        }

        DisposableEffect(Unit) {
            launcher = localLauncher
            onDispose {
                launcher = null
            }
        }
    }

    private fun handleFolderSelection(treeUri: Uri) {
        val contentResolver = context.contentResolver
        val permissions = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(treeUri, permissions)

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )

        val _songFiles = mutableListOf<SongFile>()

        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (!cursor.getString(2).startsWith("audio")) continue
                _songFiles.add(
                    SongFile(
                        filename = cursor.getString(0),
                        uri = DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            cursor.getString(1)
                        ),
                        mimeType = cursor.getString(2)
                    )
                )
            }
        }

        songFileList.value = _songFiles
        Log.d("MusicFileLoader", "Loaded ${_songFiles.size} songs from $treeUri")
    }

    fun launch() {
        launcher?.launch(null) ?: Log.e("MusicFileLoader", "Launcher not initialized")
    }
}
