package io.github.sandroisu.threetimesaday.core.ui

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@Composable
internal fun WheelTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(WHEEL_ROW_HEIGHT),
        ) {}
        Row(verticalAlignment = Alignment.CenterVertically) {
            WheelColumn(valueCount = HOURS_IN_DAY, selectedValue = hour, onValueChange = onHourChange)
            Text(":", style = MaterialTheme.typography.headlineSmall)
            WheelColumn(valueCount = MINUTES_IN_HOUR, selectedValue = minute, onValueChange = onMinuteChange)
        }
    }
}

@Composable
private fun WheelColumn(
    valueCount: Int,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
) {
    // A long list that starts in the middle makes the wheel feel endless, like on iOS.
    val totalItemCount = valueCount * WHEEL_LOOP_COUNT
    val middleStart = valueCount * (WHEEL_LOOP_COUNT / 2)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = middleStart + selectedValue - VISIBLE_ROWS_AROUND_CENTER,
    )
    val centeredIndex by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { item -> abs(item.offset + item.size / 2 - viewportCenter) }
                ?.index
                ?: (listState.firstVisibleItemIndex + VISIBLE_ROWS_AROUND_CENTER)
        }
    }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(listState, valueCount) {
        snapshotFlow { centeredIndex % valueCount }
            .distinctUntilChanged()
            .collect { value -> currentOnValueChange(value) }
    }
    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(WHEEL_COLUMN_WIDTH)
            .height(WHEEL_ROW_HEIGHT * (VISIBLE_ROWS_AROUND_CENTER * 2 + 1)),
    ) {
        items(totalItemCount) { index ->
            val value = index % valueCount
            val distance = abs(index - centeredIndex)
            val isCentered = distance == 0
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(WHEEL_ROW_HEIGHT)
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (isCentered) 1f else 1f / (distance + 1) },
            ) {
                Text(
                    text = value.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (isCentered) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

private const val HOURS_IN_DAY = 24
private const val MINUTES_IN_HOUR = 60
private const val WHEEL_LOOP_COUNT = 1000
private const val VISIBLE_ROWS_AROUND_CENTER = 2
private val WHEEL_ROW_HEIGHT = 44.dp
private val WHEEL_COLUMN_WIDTH = 72.dp
