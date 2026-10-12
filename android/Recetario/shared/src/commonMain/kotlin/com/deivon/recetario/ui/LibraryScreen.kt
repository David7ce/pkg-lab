package com.david7ce.recetario.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david7ce.recetario.logic.LibraryQuery
import com.david7ce.recetario.logic.SortOrder
import com.david7ce.recetario.logic.UpdateInfo
import com.david7ce.recetario.model.Recipe
import com.david7ce.recetario.logic.Text as TextUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(app: AppState) {
    val s = LocalStrings.current
    val query = app.query
    val visible = remember(app.recipes, query) { query.apply(app.recipes) }
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.appName) },
                actions = {
                    IconButton(onClick = app::openSettings) { Icon(AppIcons.Settings, contentDescription = s.settings) }
                },
            )
        },
        floatingActionButton = { AddMenuButton(app) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            app.updateInfo?.let { UpdateBanner(it, app) }
            if (app.recipes.isNotEmpty()) SearchRow(app, query) { showFilters = true }

            when {
                !app.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                app.recipes.isEmpty() -> MessageState(s.emptyTitle, s.emptyBody) {
                    Button(onClick = app::newRecipe) {
                        Icon(AppIcons.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.addManually)
                    }
                    OutlinedButton(onClick = app::newFromUrl) {
                        Icon(AppIcons.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.importFromUrl)
                    }
                }
                visible.isEmpty() -> MessageState(s.noMatches, null) {
                    IconTextButton(s.clearFilters, AppIcons.Reset, onClick = { app.query = query.cleared() })
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

    if (showFilters) FilterSheet(app) { showFilters = false }
}

@Composable
private fun UpdateBanner(info: UpdateInfo, app: AppState) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(s.updateAvailable(info.version), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { platform.openUrl(info.url) }) { Text(s.viewUpdate) }
            IconButton(onClick = app::dismissUpdate) { Icon(AppIcons.Close, contentDescription = s.close) }
        }
    }
}

/** Search field, filter panel button (with a count of active filters) and the favourites toggle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchRow(app: AppState, query: LibraryQuery, openFilters: () -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query.text,
            onValueChange = { app.query = query.copy(text = it) },
            placeholder = { Text(s.searchHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(AppIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (query.text.isNotEmpty()) {
                    IconButton(onClick = { app.query = query.copy(text = "") }) {
                        Icon(AppIcons.Close, contentDescription = s.clearSearch)
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = openFilters) {
            BadgedBox(badge = { if (query.panelFilterCount > 0) Badge { Text("${query.panelFilterCount}") } }) {
                Icon(AppIcons.Filter, contentDescription = s.filters)
            }
        }
        IconToggleButton(
            checked = query.favoritesOnly,
            onCheckedChange = { app.query = query.copy(favoritesOnly = it) },
        ) {
            Icon(
                if (query.favoritesOnly) AppIcons.Favorite else AppIcons.FavoriteOutline,
                contentDescription = s.favoritesOnly,
                tint = if (query.favoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "Add recipe" opens a two-option menu: by hand, or from a web page. */
@Composable
private fun AddMenuButton(app: AppState) {
    val s = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    Box {
        ExtendedFloatingActionButton(
            onClick = { open = true },
            icon = { Icon(AppIcons.Add, contentDescription = null) },
            text = { Text(s.addRecipe) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem(s.addManually, AppIcons.Edit) {
                open = false
                app.newRecipe()
            }
            MenuItem(s.importFromUrl, AppIcons.Link) {
                open = false
                app.newFromUrl()
            }
        }
    }
}

/** Sort order, minimum rating and tags: everything that narrows the grid besides search and favourites. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(app: AppState, onClose: () -> Unit) {
    val s = LocalStrings.current
    val query = app.query
    val allTags = remember(app.recipes) { LibraryQuery.allTags(app.recipes) }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Filter, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(s.filters, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (query.isFiltering) IconTextButton(s.clearFilters, AppIcons.Reset, onClick = { app.query = query.cleared() })
            }

            SectionTitle(s.sort, icon = AppIcons.Sort)
            Column(Modifier.selectableGroup()) {
                listOf(
                    Triple(SortOrder.Modified, s.sortByModified, AppIcons.Clock),
                    Triple(SortOrder.Name, s.sortByName, AppIcons.Sort),
                    Triple(SortOrder.Rating, s.sortByRating, AppIcons.Star),
                ).forEach { (order, label, icon) ->
                    OptionRow(label, icon, selected = query.sort == order) { app.query = query.copy(sort = order) }
                }
            }

            SectionTitle(s.minRating, icon = AppIcons.Star)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..5).forEach { stars ->
                    FilterChip(
                        selected = query.minRating == stars,
                        onClick = { app.query = query.copy(minRating = stars) },
                        label = { Text(if (stars == 0) s.anyRating else "$stars+") },
                        leadingIcon = if (stars > 0) {
                            { Icon(AppIcons.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                    )
                }
            }

            if (allTags.isNotEmpty()) {
                SectionTitle(s.tags, icon = AppIcons.Label)
                val selected = query.tags.map(TextUtil::fold).toSet()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTags.forEach { tag ->
                        val isOn = TextUtil.fold(tag) in selected
                        FilterChip(
                            selected = isOn,
                            onClick = {
                                val next = if (isOn) {
                                    query.tags.filterNot { TextUtil.fold(it) == TextUtil.fold(tag) }.toSet()
                                } else {
                                    query.tags + tag
                                }
                                app.query = query.copy(tags = next)
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
        }
    }
}

/** A selectable row: radio button, icon and label, the whole row as the touch target. */
@Composable
internal fun OptionRow(label: String, icon: ImageVector?, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(label)
    }
}

@Composable
private fun RecipeCard(app: AppState, recipe: Recipe) {
    val s = LocalStrings.current
    Card(onClick = { app.open(recipe.id) }) {
        Column {
            Box {
                RecipeImage(app, recipe.imageFile, Modifier.fillMaxWidth().aspectRatio(4f / 3f), maxSize = 480)
                if (recipe.favorite) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    ) {
                        Icon(
                            AppIcons.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(4.dp).size(18.dp),
                        )
                    }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (recipe.rating != null) StarRating(recipe.rating, Modifier.padding(top = 4.dp), starSize = 14.dp)
                recipe.totalMinutes?.let { minutes ->
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
