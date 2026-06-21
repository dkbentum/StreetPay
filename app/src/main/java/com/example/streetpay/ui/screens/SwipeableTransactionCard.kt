package com.example.streetpay.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.streetpay.data.TransactionEntity
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeableTransactionCard(
    transaction: TransactionEntity,
    isSelected: Boolean,
    onMarkSeen: (TransactionEntity) -> Unit,
    onDoubleClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val swipeThreshold = screenWidthPx * 0.3f
    
    // Track how many times we've swiped
    var swipeCount by remember(transaction.id) { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch {
                        offsetX.snapTo(offsetX.value + delta)
                    }
                },
                onDragStopped = {
                    coroutineScope.launch {
                        // Check if we passed 30% threshold
                        if (kotlin.math.abs(offsetX.value) >= swipeThreshold) {
                            swipeCount++
                            if (swipeCount >= 2) {
                                onMarkSeen(transaction)
                                swipeCount = 0 // Reset after action
                            }
                        }
                        
                        // Always snap back to center
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = spring()
                        )
                    }
                }
            )
    ) {
        TransactionCard(
            transaction = transaction,
            isSelected = isSelected,
            onDoubleClick = onDoubleClick
        )
    }
}
