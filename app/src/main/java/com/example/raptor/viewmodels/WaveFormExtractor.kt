package com.example.raptor.viewmodels

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

suspend fun extractWaveformDataFromUri(context: Context, uri: Uri): List<Float> = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    val rawPeaks = mutableListOf<Double>()

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
        if (audioTrackIndex < 0) return@withContext emptyList<Float>()

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList<Float>()

        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        // Increase or remove this limit if you want more frames
        val targetFrameCount = 500
        val bufferInfo = MediaCodec.BufferInfo()
        var framesDecoded = 0
        var inputEOS = false
        var outputEOS = false

        while (!outputEOS /* remove if you want full file */ && framesDecoded < targetFrameCount) {
            if (!inputEOS) {
                val inputBufferIndex = decoder.dequeueInputBuffer(10_000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEOS = true
                        } else {
                            decoder.queueInputBuffer(
                                inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }
            }
            val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                outputBufferIndex >= 0 -> {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        outputBuffer.clear()

                        // Peak amplitude approach
                        val shorts = ShortArray(chunk.size / 2)
                        for (i in shorts.indices) {
                            val lo = chunk[i * 2].toInt() and 0xFF
                            val hi = chunk[i * 2 + 1].toInt()
                            shorts[i] = ((hi shl 8) or lo).toShort()
                        }
                        var peak = 0.0
                        for (sample in shorts) {
                            val absVal = abs(sample.toInt())
                            if (absVal > peak) peak = absVal.toDouble()
                        }
                        rawPeaks.add(peak)
                        framesDecoded++
                    }
                    decoder.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputEOS = true
                    }
                }
            }
        }
        decoder.stop()
        decoder.release()
        extractor.release()

        while (rawPeaks.size < targetFrameCount) {
            rawPeaks.add(0.0)
        }

        // Normalize by file's max peak, then apply optional zoom factor (e.g., 1.2f)
        val maxPeak = rawPeaks.maxOrNull() ?: 1.0
        val zoom = 1.0f
        rawPeaks.map {
            val scaled = (it / maxPeak) * zoom
            scaled.coerceIn(0.0, 1.0).toFloat()
        }
    } catch (e: Exception) {
        extractor.release()
        emptyList<Float>()
    }
}


