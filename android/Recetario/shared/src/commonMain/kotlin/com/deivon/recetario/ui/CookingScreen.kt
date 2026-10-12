package com.david7ce.recetario.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.recetario.logic.Quantities

/** One step at a time, large text, previous/next, ingredients on demand. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingScreen(app: AppState, screen: Screen.Cooking) {
    val s = LocalStrings.current
    val recipe = app.recipe(screen.id) ?: return
    val total = recipe.steps.size
    val step = screen.step.coerceIn(0, (total - 1).coerceAtLeast(0))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(app, AppIcons.Close) },
                actions = {
                    IconButton(onClick = { screen.showIngredients = true }) {
                        Icon(AppIcons.Checklist, contentDescription = s.ingredients)
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (total == 0) {
                MessageState(s.noSteps, null)
            } else Column(Modifier.widthIn(max = 720.dp).fillMaxSize().padding(16.dp)) {
                LinearProgressIndicator(progress = { (step + 1f) / total }, modifier = Modifier.fillMaxWidth())
                Text(
                    s.stepOf(step + 1, total),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(recipe.steps[step], style = MaterialTheme.typography.headlineSmall)
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { screen.step = step - 1 },
                        enabled = step > 0,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Icon(AppIcons.Previous, contentDescription = null)
                        Text(s.previous)
                    }
                    Button(
                        onClick = { if (step < total - 1) screen.step = step + 1 else app.back() },
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    ) {
                        Text(if (step < total - 1) s.next else s.finish)
                        Icon(if (step < total - 1) AppIcons.Next else AppIcons.Check, contentDescription = null)
                    }
                }
            }
        }
    }

    if (screen.showIngredients) {
        ModalBottomSheet(onDismissRequest = { screen.showIngredients = false }) {
            val shown = Quantities.scale(recipe.ingredients, screen.factor)
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
                SectionTitle(s.ingredients, icon = AppIcons.Checklist)
                val details = listOfNotNull(
                    recipe.servings?.let { "${s.servings}: ${Quantities.format(it * screen.factor, s.decimalSeparator)}" },
                    s.scaledBy(Quantities.format(screen.factor, s.decimalSeparator)).takeIf { screen.factor != 1.0 },
                )
                if (details.isNotEmpty()) {
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (shown.isEmpty()) Text(s.noIngredients)
                shown.forEach { IngredientRow(it, s) }
            }
        }
    }
}
