package com.example.raptor.viewmodels

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

suspend fun extractWaveformDataFromUri(context: Context, uri: Uri): List<Float> = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    val result = mutableListOf<Float>()
    try {
        extractor.setDataSource(context, uri, null)
        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                audioTrackIndex = i
                break
            }
        }
        if (audioTrackIndex < 0) {
            Log.e("AudioWaveformExtractor", "No audio track found in file.")
            return@withContext emptyList<Float>()
        }

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME)!!

        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        val inputBuffers = decoder.inputBuffers
        val bufferInfo = MediaCodec.BufferInfo()

        val targetFrameCount = 500
        var framesDecoded = 0
        var done = false

        while (!done && framesDecoded < targetFrameCount) {
            val inputBufferIndex = decoder.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    decoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    done = true
                } else {
                    val presentationTimeUs = extractor.sampleTime
                    decoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        sampleSize,
                        presentationTimeUs,
                        0
                    )
                    extractor.advance()
                }
            }

            when (val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {}
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                else -> {
                    if (outputBufferIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex) ?: continue
                        val outData = ByteArray(bufferInfo.size)
                        outputBuffer.get(outData)
                        outputBuffer.clear()

                        val shorts = ShortArray(outData.size / 2)
                        for (i in shorts.indices) {
                            shorts[i] = ((outData[i*2+1].toInt() shl 8) or
                                    (outData[i*2].toInt() and 0xFF)).toShort()
                        }

                        var sum = 0.0
                        for (sample in shorts) {
                            sum += (sample * sample).toDouble()
                        }
                        val rms = sqrt(sum / shorts.size)
                        val amplitude = (rms / 32767.0).toFloat().coerceIn(0f,1f)

                        result.add(amplitude)
                        framesDecoded++

                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
            }
        }

        decoder.stop()
        decoder.release()
        extractor.release()

        // Pad the result if fewer than targetFrameCount frames were decoded
        if (result.size < targetFrameCount) {
            while (result.size < targetFrameCount) {
                result.add(0f)
            }
        }

        return@withContext result
    } catch (e: Exception) {
        Log.e("AudioWaveformExtractor", "Error extracting waveform: ${e.message}")
        extractor.release()
        return@withContext emptyList<Float>()
    }
}