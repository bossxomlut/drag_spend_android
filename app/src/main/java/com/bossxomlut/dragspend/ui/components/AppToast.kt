package com.bossxomlut.dragspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ToastType {
    ERROR,
    SUCCESS,
    INFO,
}

@Composable
fun AppToast(
    message: String?,
    onDismiss: () -> Unit,
    type: ToastType = ToastType.ERROR,
    modifier: Modifier = Modifier,
) {
    val swipeScope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut(),
        modifier = modifier,
    ) {
        if (message != null) {
            val swipeOffset = remember(message) { Animatable(0f) }

            LaunchedEffect(message) {
                delay(3500)
                onDismiss()
            }

            val (accentColor, containerColor, icon) = when (type) {
                ToastType.ERROR -> Triple(
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.errorContainer,
                    "⚠️",
                )
                ToastType.SUCCESS -> Triple(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    "✅",
                )
                ToastType.INFO -> Triple(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primaryContainer,
                    "ℹ️",
                )
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .padding(horizontal = 16.dp)
                    .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                    .pointerInput(message) {
                        val swipeThreshold = size.width * 0.35f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                swipeScope.launch {
                                    if (abs(swipeOffset.value) > swipeThreshold) {
                                        onDismiss()
                                    } else {
                                        swipeOffset.animateTo(0f)
                                    }
                                }
                            },
                            onDragCancel = {
                                swipeScope.launch { swipeOffset.animateTo(0f) }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                swipeScope.launch {
                                    swipeOffset.snapTo(swipeOffset.value + dragAmount)
                                }
                            },
                        )
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accentColor),
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
