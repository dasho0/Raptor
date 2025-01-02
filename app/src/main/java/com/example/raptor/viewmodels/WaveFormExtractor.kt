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
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList<Float>()

        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        var inputEOS = false
        var outputEOS = false

        val bufferInfo = MediaCodec.BufferInfo()
        val targetFrameCount = 500
        var framesDecoded = 0

        val inputBuffers = decoder.inputBuffers

        while (!outputEOS && framesDecoded < targetFrameCount) {
            if (!inputEOS) {
                val inputBufferIndex = decoder.dequeueInputBuffer(10_000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = inputBuffers[inputBufferIndex]
                    inputBuffer.clear()

                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputEOS = true
                        Log.d("AudioWaveformExtractor", "Input EOS reached")
                    } else {
                        val presentationTimeUs = extractor.sampleTime
                        decoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            sampleSize,
                            presentationTimeUs,
                            0
                        )
                        // Advance to the next sample
                        extractor.advance()
                    }
                }
            }
            val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)

            when {
                outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = decoder.outputFormat
                    Log.d("AudioWaveformExtractor","New format: $newFormat")
                }
                outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                }
                outputBufferIndex >= 0 -> {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val outData = ByteArray(bufferInfo.size)
                        outputBuffer.get(outData)
                        outputBuffer.clear()

                        val shorts = ShortArray(outData.size / 2)
                        for (i in shorts.indices) {
                            val lo = outData[i * 2].toInt() and 0xFF
                            val hi = outData[i * 2 + 1].toInt()
                            shorts[i] = ((hi shl 8) or lo).toShort()
                        }

                        var sum = 0.0
                        for (sample in shorts) {
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / shorts.size)
                        val amplitude = (rms / 32767.0).toFloat().coerceIn(0f,1f)
                        result.add(amplitude)
                        framesDecoded++
                    }

                    decoder.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputEOS = true
                        Log.d("AudioWaveformExtractor", "Output EOS reached")
                    }
                }
            }
        }

        decoder.stop()
        decoder.release()
        extractor.release()

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