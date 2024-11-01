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
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

//TODO: make this a singleton somehow

// this class is probably temporary. The intention is for it to scan the folder (maybe
// recursively sometime) and then prepare the data to feed it to some other database class.
class MusicFileLoader(val context: Context) {
    data class SongFile(val filename: String, val uri: Uri, val mimeType: String)

    private val _songFileList = MutableStateFlow<List<SongFile>>(emptyList())
    val songFileList: StateFlow<List<SongFile>> get() = _songFileList
    private var hasPickedFiles = CompletableDeferred<Unit>()

    private lateinit var launcher : ManagedActivityResultLauncher<Uri?, Uri?>

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

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )

                contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val files = mutableListOf<SongFile>()
                    while(cursor.moveToNext()) {
                        if(cursor.getString(2).slice(0..4) != "audio") continue

                        files.add(SongFile(
                            cursor.getString(0),
                            DocumentsContract.buildDocumentUriUsingTree(
                                treeUri,
                                cursor.getString(1)
                            ),
                            cursor.getString(2))
                        )
                    }
                    _songFileList.value = files
                    hasPickedFiles.complete(Unit)
                }
            }
        }
    }

    fun launch() {
        launcher.launch(null)
    }

    suspend fun getSongFiles(): List<SongFile> {
        hasPickedFiles.await()
        return _songFileList.value
    }
}