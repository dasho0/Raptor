package com.example.raptor

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.util.fastJoinToString
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jetbrains.annotations.NotNull
import java.io.File
import javax.inject.Inject

/**
 * Handles the extraction of album images from app storage and provides images from
 * app storage to the rest of the program
 */
class ImageManager @Inject constructor(@ApplicationContext private val context: Context) {
    // TODO: Probably should check whether an image file doesn't exist and then write to it - but
    //  then what if an album image changes?

    // TODO: should pass a MetadataRetriever as a param to not create a million of them
    /**
     * Extracts the image directly from some audio file and copies it into app storage
     *
     * @param uri Image URI
     * @param artistNames List of names of the composers of the file
     * @param albumName Name of the album of the file
     *
     * @return URI of the file created in app storage or `null`
     */
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

    /**
     * Retrieves a image bitmap in local storage
     *
     * @param uri URI of the image file from local storage
     * @return ImageBitmap from storage or blank bitmap
     */
    fun getBitmapFromAppStorage(uri: Uri?): ImageBitmap {
        Log.d(javaClass.simpleName, "Collecting bitmap with uri: $uri")
        if(uri != null) {
            context.contentResolver.openInputStream(Uri.parse(uri.toString())).use {
                return BitmapFactory.decodeStream(it)
                    .asImageBitmap()
            }
        } else {
            return ImageBitmap(1,1,)
        }
    }
}