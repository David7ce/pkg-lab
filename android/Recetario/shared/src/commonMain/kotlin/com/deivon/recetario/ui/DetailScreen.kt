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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deivon.recetario.logic.Quantities
import com.deivon.recetario.model.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(app: AppState, screen: Screen.Detail) {
    val s = LocalStrings.current
    val recipe = app.recipe(screen.id) ?: return // deleted: navigation is updated right after
    val servings = screen.servings ?: recipe.servings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(app) },
                actions = {
                    IconButton(onClick = { app.edit(recipe.id) }) { Icon(AppIcons.Edit, contentDescription = s.edit) }
                    DetailMenu(app, recipe, screen)
                },
            )
        },
        floatingActionButton = {
            if (recipe.steps.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { app.startCooking(recipe.id, servings) },
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
                Meta(recipe, s)
                if (recipe.tags.isNotEmpty()) Tags(recipe.tags)

                SectionTitle(s.ingredients)
                ServingsControl(recipe, servings, s) { screen.servings = it }
                val shown = if (recipe.servings != null && servings != null) {
                    Quantities.scale(recipe.ingredients, recipe.servings, servings)
                } else {
                    recipe.ingredients
                }
                if (shown.isEmpty()) Text(s.noIngredients, color = MaterialTheme.colorScheme.onSurfaceVariant)
                shown.forEach { IngredientRow(it, s) }

                SectionTitle(s.steps)
                if (recipe.steps.isEmpty()) Text(s.noSteps, color = MaterialTheme.colorScheme.onSurfaceVariant)
                recipe.steps.forEachIndexed { i, step -> StepRow(i + 1, step) }

                recipe.sourceUrl?.let { url ->
                    SectionTitle(s.source)
                    val platform = LocalPlatform.current
                    TextButton(onClick = { platform.openUrl(url) }) {
                        Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }

    if (screen.confirmDelete) {
        AlertDialog(
            onDismissRequest = { screen.confirmDelete = false },
            title = { Text(s.deleteTitle) },
            text = { Text(s.deleteBody(recipe.title)) },
            confirmButton = {
                TextButton(onClick = {
                    screen.confirmDelete = false
                    app.delete(recipe.id)
                }) { Text(s.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { screen.confirmDelete = false }) { Text(s.cancel) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Meta(recipe: Recipe, s: Strings) {
    val items = listOfNotNull(
        recipe.prepMinutes?.let { s.prep to s.duration(it) },
        recipe.cookMinutes?.let { s.cook to s.duration(it) },
        recipe.totalMinutes?.takeIf { recipe.prepMinutes != null && recipe.cookMinutes != null }?.let { s.total to s.duration(it) },
    )
    if (items.isEmpty()) return
    FlowRow(
        Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (label, value) -> MetaItem(AppIcons.Clock, label, value) }
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
                Text(it, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun ServingsControl(recipe: Recipe, servings: Int?, s: Strings, onChange: (Int) -> Unit) {
    if (recipe.servings == null || servings == null) {
        Text(
            s.servingsUnknown,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(s.servings, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
        IconButton(onClick = { onChange(servings - 1) }, enabled = servings > 1) {
            Icon(AppIcons.Minus, contentDescription = s.fewer)
        }
        Text("$servings", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onChange(servings + 1) }, enabled = servings < 999) {
            Icon(AppIcons.Add, contentDescription = s.moreServings)
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
            MenuItem(s.duplicate, close { app.duplicate(recipe.id) })
            MenuItem(s.exportJson, close { app.transfer.exportRecipe(recipe, platform) })
            if (app.transfer.canExportPdf) MenuItem(s.exportPdf, close { app.transfer.exportRecipePdf(recipe, platform) })
            MenuItem(s.delete, close { screen.confirmDelete = true })
        }
    }
}
