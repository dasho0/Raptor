package com.example.raptor.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme

@Composable
fun WaveformSeekBar(
    waveform: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    onProgressChanged: (Float) -> Unit
) {
    var internalProgress by remember { mutableStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }

    // Sync internal progress with external updates if not dragging
    LaunchedEffect(progress) {
        if (!isDragging) {
            internalProgress = progress
        }
    }

    // Retrieve theme colors outside Canvas
    val filledColor = MaterialTheme.colorScheme.primary
    val unfilledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val progressLineColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .pointerInput(waveform) {
                detectTapGestures(
                    onTap = { offset ->
                        val width = size.width
                        val newProgress = (offset.x / width).coerceIn(0f,1f)
                        internalProgress = newProgress
                        onProgressChanged(internalProgress)
                    }
                )
            }
            .pointerInput(waveform) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        val width = size.width
                        val newX = (internalProgress * width) + dragAmount.x
                        val clamped = min(max(newX, 0f), width.toFloat())
                        val newProg = clamped / width
                        internalProgress = newProg
                        onProgressChanged(internalProgress)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val frontX = internalProgress * width

            val filledPath = Path()
            val unfilledPath = Path()

            val count = waveform.size
            if (count > 0) {
                val step = width / count
                val lineMid = height / 2f

                for (i in waveform.indices) {
                    val x = i * step
                    if (x <= frontX) {
                        val lineHeight = (waveform[i].coerceIn(0f, 1f)) * height
                        val topY = lineMid - (lineHeight / 2f)
                        val bottomY = lineMid + (lineHeight / 2f)

                        filledPath.moveTo(x, topY)
                        filledPath.lineTo(x, bottomY)
                    } else {
                        unfilledPath.moveTo(x, lineMid)
                        unfilledPath.lineTo(x, lineMid)
                    }
                }
            }

            drawPath(
                path = filledPath,
                color = filledColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx()
                )
            )

            drawPath(
                path = unfilledPath,
                color = unfilledColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx()
                )
            )

            drawLine(
                color = progressLineColor.copy(alpha = 0.5f),
                start = Offset(frontX, 0f),
                end = Offset(frontX, height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
