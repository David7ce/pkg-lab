package com.deivon.recetario.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deivon.recetario.logic.LibraryQuery
import com.deivon.recetario.logic.SortOrder
import com.deivon.recetario.logic.Text as TextUtil
import com.deivon.recetario.model.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(app: AppState) {
    val s = LocalStrings.current
    val query = app.query
    val visible = remember(app.recipes, query) { query.apply(app.recipes) }
    val allTags = remember(app.recipes) { LibraryQuery.allTags(app.recipes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.appName) },
                actions = {
                    SortMenu(app)
                    LibraryMenu(app)
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = app::newRecipe,
                icon = { Icon(AppIcons.Add, contentDescription = null) },
                text = { Text(s.addRecipe) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (app.recipes.isNotEmpty()) {
                OutlinedTextField(
                    value = query.text,
                    onValueChange = { app.query = query.copy(text = it) },
                    placeholder = { Text(s.searchHint) },
                    leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.text.isNotEmpty()) {
                            IconButton(onClick = { app.query = query.copy(text = "") }) {
                                Icon(AppIcons.Close, contentDescription = s.clearSearch)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (allTags.isNotEmpty()) TagFilters(allTags, query) { app.query = it }

            when {
                !app.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                app.recipes.isEmpty() -> MessageState(s.emptyTitle, s.emptyBody) {
                    Button(onClick = app::newRecipe) { Text(s.addRecipe) }
                    OutlinedButton(onClick = app.transfer::startUrlImport) { Text(s.importFromUrl) }
                }
                visible.isEmpty() -> MessageState(s.noMatches, null) {
                    TextButton(onClick = { app.query = query.copy(text = "", tags = emptySet()) }) { Text(s.clearFilters) }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visible, key = { it.id }) { recipe -> RecipeCard(app, recipe) }
                }
            }
        }
    }
}

@Composable
private fun TagFilters(tags: List<String>, query: LibraryQuery, onChange: (LibraryQuery) -> Unit) {
    val selected = query.tags.map(TextUtil::fold).toSet()
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val isOn = TextUtil.fold(tag) in selected
            FilterChip(
                selected = isOn,
                onClick = {
                    val next = if (isOn) query.tags.filterNot { TextUtil.fold(it) == TextUtil.fold(tag) }.toSet() else query.tags + tag
                    onChange(query.copy(tags = next))
                },
                label = { Text(tag) },
                leadingIcon = if (isOn) {
                    { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun RecipeCard(app: AppState, recipe: Recipe) {
    val s = LocalStrings.current
    Card(onClick = { app.open(recipe.id) }) {
        Column {
            RecipeImage(app, recipe.imageFile, Modifier.fillMaxWidth().aspectRatio(4f / 3f), maxSize = 480)
            Column(Modifier.padding(12.dp)) {
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                recipe.totalMinutes?.let { minutes ->
                    Spacer(Modifier.padding(top = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            AppIcons.Clock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            s.duration(minutes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(app: AppState) {
    val s = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(AppIcons.Sort, contentDescription = s.sort) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(SortOrder.Modified to s.sortByModified, SortOrder.Name to s.sortByName).forEach { (order, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        app.query = app.query.copy(sort = order)
                        open = false
                    },
                    leadingIcon = {
                        if (app.query.sort == order) Icon(AppIcons.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun LibraryMenu(app: AppState) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    val t = app.transfer
    var open by remember { mutableStateOf(false) }
    fun close(action: () -> Unit): () -> Unit = {
        open = false
        action()
    }
    Box {
        IconButton(onClick = { open = true }) { Icon(AppIcons.More, contentDescription = s.more) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem(s.importFromUrl, close { t.startUrlImport() })
            MenuItem(s.importFile, close { t.importFile(platform) })
            MenuItem(s.exportLibrary, close { t.exportLibrary(platform) })
            MenuItem(s.backup, close { t.backup(platform) })
            if (t.canExportPdf) MenuItem(s.cookbookPdf, close { t.cookbook(platform) })
            MenuItem(s.settings, close { app.openSettings() })
        }
    }
}

@Composable
internal fun MenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = onClick)
}
