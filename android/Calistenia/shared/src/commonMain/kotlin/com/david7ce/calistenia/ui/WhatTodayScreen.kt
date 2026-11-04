package com.david7ce.calistenia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.david7ce.calistenia.model.Muscle

/**
 * "What are you training today?": the body map, a chip per muscle (the way in for anyone who cannot
 * aim at a drawing) and "All" to clear. The selection lives in [AppState.muscleFilter] and only filters
 * the exercise library for this session.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatTodayScreen(app: AppState) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            strings.muscleMapHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            MuscleMap(
                selected = app.muscleFilter,
                onToggle = app::toggleMuscle,
                modifier = Modifier.padding(16.dp),
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FilterChip(
                selected = app.muscleFilter.isEmpty(),
                onClick = app::clearMuscles,
                label = { Text(strings.all) },
            )
            Muscle.entries.forEach { muscle ->
                FilterChip(
                    selected = muscle in app.muscleFilter,
                    onClick = { app.toggleMuscle(muscle) },
                    label = { Text(strings.muscleLabel(muscle)) },
                )
            }
        }
        Button(onClick = app::showFilteredExercises, modifier = Modifier.fillMaxWidth()) {
            Text(strings.showExercises)
        }
    }
}
