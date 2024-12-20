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

                Log.d("FolderPicker", "Selected: $it")

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )

                Log.d("FolderPicker", "Children: $childrenUri")

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
                    var _songFiles = mutableListOf<SongFile>()

                    while(cursor.moveToNext()) {
                        if(cursor.getString(2).slice(0..4) != "audio") { continue } // of course
                        // google cant design a good API and of course it has 6 year old bugs in
                        // it, so mimetype filtering has to be done manually

                        _songFiles.add(SongFile(
                            cursor.getString(0),
                            DocumentsContract.buildDocumentUriUsingTree(
                                treeUri,
                                cursor.getString(1)
                            ),
                            cursor.getString(2))
                        )

                        Log.d("MusicFilePicker", "Music File: ${_songFiles.last().filename}, " +
                                "ID: ${_songFiles.last().uri}," +
                                "Type: ${_songFiles.last().mimeType}"
                        )
                    }

                    songFileList.value = _songFiles
                }
            }
        }
    }

    fun launch() {
        launcher.launch(null)
    }
}