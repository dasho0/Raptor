package com.example.raptor

import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

//TODO: make this a singleton somehow

// this class is probably temporary. The intention is for it to scan the folder (maybe
// recursively sometime) and then prepare the data to feed it to some other database class.
class FilePicker() {
    private lateinit var launcher : ManagedActivityResultLauncher<Uri?, Uri?>
    @Composable fun PrepareFilePicker() {
        val context = LocalContext.current
        val contentResolver = context.contentResolver

        launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { treeUri: Uri? ->
            treeUri?.let {
                Log.d("FolderPicker", "Selected: $it")

                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )

                Log.d("FolderPicker", "Children: $childrenUri")

                val selection = "${DocumentsContract.Document.COLUMN_MIME_TYPE} LIKE 'audio/%'"
                contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    selection,
                    null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val displayName = cursor.getString(0)
                        val documentId = cursor.getString(1)
                        val mimeType = cursor.getString(2)

                        Log.d("MusicFilePicker", "Music File: $displayName, Type: $mimeType")
                        // Process the music file as needed
                    }
                }
            }
        }
    }

    fun launch() {
        launcher.launch(null)
    }
}
