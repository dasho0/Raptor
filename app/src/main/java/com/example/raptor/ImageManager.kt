package com.example.raptor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.util.fastJoinToString
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

class ImageManager @Inject constructor(@ApplicationContext private val context: Context) {
    // TODO: Probably should check whether an image file doesn't exist and then write to it - but
    //  then what if an album image changes?
    fun extractAlbumimage(
        uri: Uri?,
        artistNames: List<String>,
        albumName: String
    ): Uri? {
        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(context, uri)
        val pictureBytes = retriever.embeddedPicture

        val bitmapFile = File(context.filesDir,
                artistNames.fastJoinToString(";") + ":$albumName"
        )

        pictureBytes?.let {
            bitmapFile.writeBytes(pictureBytes)
        }

        retriever.release()

        return bitmapFile.toUri()
    }

    fun collectBitmapFromUri(uri: Uri?): ImageBitmap {
        Log.d(javaClass.simpleName, "Collecting bitmap with uri: $uri")
        if(uri != null) {
            try {
                context.contentResolver.openInputStream(Uri.parse(uri.toString())).use {
                    return BitmapFactory.decodeStream(it)
                        .asImageBitmap()
                }
            } catch(e: FileNotFoundException) {
                Log.e(javaClass.simpleName, "Can't parse thumbnail at: $uri")
                return ImageBitmap(1,1)
            }
        } else {
            return ImageBitmap(1,1,)
        }
    }
}