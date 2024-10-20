package com.example.raptor

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.provider.DocumentsContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.raptor.ui.theme.RaptorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaptorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StartScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun StartScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val folderPickerLauncher = rememberLauncherForActivityResult(
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray)
    ) {
        Button(
            onClick = {
                folderPickerLauncher.launch(null)
            },
            modifier = modifier
                .align(Alignment.Center)
        ) {
            Text("Button")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    RaptorTheme {
        StartScreen()
    }
}