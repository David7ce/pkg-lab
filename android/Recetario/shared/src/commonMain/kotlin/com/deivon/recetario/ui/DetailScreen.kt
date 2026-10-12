package com.deivon.recetario.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.deivon.shared.ui.ConfirmDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deivon.recetario.logic.Quantities
import com.deivon.recetario.model.Recipe
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(app: AppState, screen: Screen.Detail) {
    val s = LocalStrings.current
    val recipe = app.recipe(screen.id) ?: return // deleted: navigation is updated right after
    val factor = screen.factor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(app) },
                actions = {
                    IconButton(onClick = { app.toggleFavorite(recipe.id) }) {
                        Icon(
                            if (recipe.favorite) AppIcons.Favorite else AppIcons.FavoriteOutline,
                            contentDescription = if (recipe.favorite) s.removeFavorite else s.addFavorite,
                            tint = if (recipe.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { app.edit(recipe.id) }) { Icon(AppIcons.Edit, contentDescription = s.edit) }
                    DetailMenu(app, recipe, screen)
                },
            )
        },
        floatingActionButton = {
            if (recipe.steps.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { app.startCooking(recipe.id, factor) },
                    icon = { Icon(AppIcons.Play, contentDescription = null) },
                    text = { Text(s.startCooking) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 96.dp),
            ) {
                if (recipe.imageFile != null) {
                    RecipeImage(
                        app,
                        recipe.imageFile,
                        Modifier.padding(top = 8.dp).fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)),
                        maxSize = 1600,
                        contentDescription = recipe.title,
                    )
                }
                Text(recipe.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
                StarRating(recipe.rating, Modifier.padding(top = 4.dp), starSize = 28.dp) { app.rate(recipe.id, it) }
                Meta(recipe, factor, s)
                if (recipe.tags.isNotEmpty()) Tags(recipe.tags)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(s.ingredients, Modifier.weight(1f), icon = AppIcons.Checklist)
                    if (recipe.ingredients.any { it.quantity != null }) {
                        IconTextButton(s.adjustQuantities, AppIcons.Tune, onClick = { screen.showAdjust = true }, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                if (factor != 1.0) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                s.scaledBy(Quantities.format(factor, s.decimalSeparator)),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            IconTextButton(s.originalQuantities, AppIcons.Reset, onClick = { screen.factor = 1.0 })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                val shown = Quantities.scale(recipe.ingredients, factor)
                if (shown.isEmpty()) Text(s.noIngredients, color = MaterialTheme.colorScheme.onSurfaceVariant)
                shown.forEach { IngredientRow(it, s) }

                SectionTitle(s.steps, icon = AppIcons.Play)
                if (recipe.steps.isEmpty()) Text(s.noSteps, color = MaterialTheme.colorScheme.onSurfaceVariant)
                recipe.steps.forEachIndexed { i, step -> StepRow(i + 1, step) }

                recipe.notes?.let { notes ->
                    SectionTitle(s.notes, icon = AppIcons.Notes)
                    Text(notes, style = MaterialTheme.typography.bodyLarge)
                }

                recipe.sourceUrl?.let { url ->
                    SectionTitle(s.source, icon = AppIcons.Link)
                    val platform = LocalPlatform.current
                    TextButton(onClick = { platform.openUrl(url) }) {
                        Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (screen.showAdjust) {
        AdjustQuantitiesDialog(
            baseServings = recipe.servings,
            current = factor,
            onApply = {
                screen.factor = it
                screen.showAdjust = false
            },
            onDismiss = { screen.showAdjust = false },
        )
    }

    if (screen.confirmDelete) {
        ConfirmDialog(
            title = s.deleteTitle,
            text = s.deleteBody(recipe.title),
            confirmLabel = s.delete,
            destructive = true,
            onConfirm = { screen.confirmDelete = false; app.delete(recipe.id) },
            onDismiss = { screen.confirmDelete = false },
        )
    }
}

/**
 * "Adjust quantity / yield": pick servings (when the recipe states them), a preset multiplier or any
 * other one (2, 1.5, 1,5, 1/2, ½…). Only numeric quantities change.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdjustQuantitiesDialog(baseServings: Int?, current: Double, onApply: (Double) -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    var factor by remember { mutableDoubleStateOf(current) }
    var custom by remember { mutableStateOf("") }
    val customValue = Quantities.parse(custom)?.takeIf { it <= MAX_FACTOR }
    val customError = custom.isNotBlank() && customValue == null

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(AppIcons.Tune, contentDescription = null) },
        title = { Text(s.adjustQuantities) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(s.adjustHint, style = MaterialTheme.typography.bodySmall)
                if (baseServings != null) {
                    val servings = baseServings * factor
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.People, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(s.servings, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { factor = (servings.roundToInt() - 1).coerceAtLeast(1).toDouble() / baseServings },
                            enabled = servings > 1,
                        ) { Icon(AppIcons.Minus, contentDescription = s.fewer) }
                        Text(Quantities.format(servings, s.decimalSeparator), style = MaterialTheme.typography.titleMedium)
                        IconButton(
                            onClick = { factor = (servings.roundToInt() + 1).toDouble() / baseServings },
                            enabled = servings < MAX_SERVINGS,
                        ) { Icon(AppIcons.Add, contentDescription = s.moreServings) }
                    }
                }
                Text(s.factor, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESETS.forEach { preset ->
                        FilterChip(
                            selected = abs(factor - preset) < 1e-9,
                            onClick = {
                                factor = preset
                                custom = ""
                            },
                            label = { Text("×" + Quantities.format(preset, s.decimalSeparator)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { v ->
                        custom = v
                        Quantities.parse(v)?.takeIf { it <= MAX_FACTOR }?.let { factor = it }
                    },
                    label = { Text(s.otherFactor) },
                    prefix = { Text("×") },
                    isError = customError,
                    supportingText = { if (customError) Text(s.factorInvalid) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(factor) }, enabled = !customError) {
                Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(s.apply)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

private val PRESETS = listOf(0.5, 1.0, 1.5, 2.0, 3.0)
private const val MAX_FACTOR = 100.0
private const val MAX_SERVINGS = 999

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Meta(recipe: Recipe, factor: Double, s: Strings) {
    val items = listOfNotNull(
        recipe.servings?.let { Triple(AppIcons.People, s.servings, Quantities.format(it * factor, s.decimalSeparator)) },
        recipe.prepMinutes?.let { Triple(AppIcons.Clock, s.prep, s.duration(it)) },
        recipe.cookMinutes?.let { Triple(AppIcons.Clock, s.cook, s.duration(it)) },
        recipe.totalMinutes?.takeIf { recipe.prepMinutes != null && recipe.cookMinutes != null }
            ?.let { Triple(AppIcons.Clock, s.total, s.duration(it)) },
    )
    if (items.isEmpty()) return
    FlowRow(
        Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (icon, label, value) -> MetaItem(icon, label, value) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(tags: List<String>) {
    FlowRow(
        Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Label, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun StepRow(number: Int, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(end = 12.dp),
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(4.dp))
}

/** Overflow menu. Individual export is PDF only; JSON stays for the whole library (Settings). */
@Composable
private fun DetailMenu(app: AppState, recipe: Recipe, screen: Screen.Detail) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    var open by remember { mutableStateOf(false) }
    fun close(action: () -> Unit): () -> Unit = {
        open = false
        action()
    }
    Box {
        IconButton(onClick = { open = true }) { Icon(AppIcons.More, contentDescription = s.more) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem(s.duplicate, AppIcons.Copy, close { app.duplicate(recipe.id) })
            if (app.transfer.canExportPdf) MenuItem(s.exportPdf, AppIcons.Pdf, close { app.transfer.exportRecipePdf(recipe, platform) })
            MenuItem(s.delete, AppIcons.Delete, close { screen.confirmDelete = true })
        }
    }
}
