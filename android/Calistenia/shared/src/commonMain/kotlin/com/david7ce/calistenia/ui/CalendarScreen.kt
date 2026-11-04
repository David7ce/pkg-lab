package com.david7ce.calistenia.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.logic.CalendarData
import com.david7ce.calistenia.logic.DayCell
import com.david7ce.calistenia.logic.MonthRef
import com.david7ce.calistenia.logic.dayNumber
import com.david7ce.calistenia.logic.monthCells
import com.david7ce.calistenia.model.PlannedWorkout
import com.david7ce.shared.ui.SharedIcons

/**
 * Month grid, Monday first. Read-only: it shows plans and finished sessions; editing plans comes later.
 * Everything is derived from the same sessions History lists, so the two can never disagree.
 */
@Composable
fun CalendarScreen(app: AppState) {
    val strings = LocalStrings.current
    val data = remember(app.plans, app.history, app.active) { app.calendar }
    val today = app.today()
    val month = app.shownMonth()

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { app.showMonth(-1) }) { Icon(SharedIcons.Previous, strings.previousMonth) }
            Text(
                "${strings.monthNames[month.month - 1]} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { app.showMonth(1) }) { Icon(SharedIcons.Next, strings.nextMonth) }
            TextButton(onClick = app::showToday) { Text(strings.todayLabel) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            strings.weekdaysShort.forEach { name ->
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        monthCells(month).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).padding(2.dp)) {
                        if (day != null) DayCellView(day, data.cell(day, today)) { app.openCalendarDay(day) }
                    }
                }
            }
        }
        Legend()
    }

    app.calendarDay?.let { day -> DaySheet(app, data, day) }
}

@Composable
private fun DayCellView(day: String, cell: DayCell, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val border = if (cell.today) {
        Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
    } else {
        Modifier
    }
    Column(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).then(border).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            dayNumber(day).toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (cell.today) FontWeight.Bold else FontWeight.Normal,
        )
        Box(Modifier.padding(top = 3.dp).size(14.dp), contentAlignment = Alignment.Center) {
            CellMark(cell)
        }
    }
}

/** empty: nothing; outline dot: planned; filled: done; ring + filled: both; pulsing: in progress. */
@Composable
private fun CellMark(cell: DayCell) {
    if (!cell.planned && !cell.done && !cell.inProgress) return
    val color = MaterialTheme.colorScheme.primary
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
    )
    val alpha = if (cell.inProgress) pulse else 1f
    Canvas(Modifier.size(14.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val filled = cell.done || cell.inProgress
        if (filled) {
            val radius = size.minDimension * if (cell.planned) 0.26f else 0.34f
            drawCircle(color, radius = radius, center = center, alpha = alpha)
        }
        if (cell.planned) {
            val radius = if (filled) size.minDimension / 2 - 1.dp.toPx() else size.minDimension * 0.3f
            drawCircle(color, radius = radius, center = center, style = Stroke(1.5.dp.toPx()))
        }
    }
}

@Composable
private fun Legend() {
    val strings = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(DayCell("", planned = true, done = false, inProgress = false, today = false), strings.plannedLabel)
        LegendItem(DayCell("", planned = false, done = true, inProgress = false, today = false), strings.sessionsDoneLabel)
        LegendItem(DayCell("", planned = false, done = false, inProgress = true, today = false), strings.inProgressLabel)
    }
}

@Composable
private fun LegendItem(cell: DayCell, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) { CellMark(cell) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** What a day holds: its plan and the sessions that started on it. Read-only. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySheet(app: AppState, data: CalendarData, day: String) {
    val strings = LocalStrings.current
    val plan = data.plan(day)
    val sessions = data.sessions(day)
    val ref = MonthRef.of(day)
    ModalBottomSheet(onDismissRequest = app::closeCalendarDay) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (ref != null) "${dayNumber(day)} ${strings.monthNames[ref.month - 1]} ${ref.year}" else day,
                style = MaterialTheme.typography.titleLarge,
            )
            if (plan == null && sessions.isEmpty()) {
                Text(strings.nothingThisDay, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            plan?.let { PlanCard(it) }
            sessions.forEach { session ->
                Card(
                    onClick = {
                        app.closeCalendarDay()
                        app.openSession(session.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(session.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (session.isFinished) {
                                "${app.formatTime(session.startedAt)} · ${strings.setsCount(session.totalSets)} · " +
                                    strings.exercisesCount(session.exercises.size)
                            } else {
                                "${app.formatTime(session.startedAt)} · ${strings.inProgressLabel}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: PlannedWorkout) {
    val strings = LocalStrings.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(strings.plannedLabel, style = MaterialTheme.typography.labelLarge, color = muted)
            Text(
                plan.routineName.ifBlank { strings.plannedAnything },
                style = MaterialTheme.typography.titleMedium,
                color = if (plan.routineDeleted) muted else MaterialTheme.colorScheme.onSurface,
            )
            if (plan.routineDeleted) {
                Text(strings.routineDeletedNote, style = MaterialTheme.typography.bodySmall, color = muted)
            }
            if (plan.note.isNotBlank()) {
                Text(plan.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
