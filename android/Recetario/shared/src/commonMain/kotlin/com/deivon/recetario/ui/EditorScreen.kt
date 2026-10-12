package com.deivon.recetario.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deivon.recetario.logic.ImageChange
import com.deivon.recetario.logic.IngredientDraft
import com.deivon.recetario.logic.Problem
import com.deivon.recetario.logic.moved

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(app: AppState, state: EditorState) {
    val s = LocalStrings.current
    val draft = state.draft
    val problems = state.problems

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            state.importedFrom != null -> s.reviewImport
                            state.isNew -> s.newRecipe
                            else -> s.editRecipe
                        },
                    )
                },
                navigationIcon = { BackButton(app, AppIcons.Close) },
                actions = {
                    TextButton(onClick = { app.save(state) }, enabled = !state.saving) { Text(s.save) }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.importedFrom?.let { ImportBanner(s.importedFrom(it), state.importNotes) }
                if (problems.isNotEmpty()) ProblemList(problems, s)

                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { v -> state.update { it.copy(title = v) } },
                    label = { Text(s.title) },
                    isError = problems.any { it == Problem.TitleMissing || it == Problem.TitleTooLong },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                PhotoField(app, state)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(s.servings, draft.servings, problems.contains(Problem.ServingsInvalid), Modifier.weight(1f)) { v ->
                        state.update { it.copy(servings = v) }
                    }
                    NumberField(s.prepMinutes, draft.prepMinutes, problems.contains(Problem.PrepTimeInvalid), Modifier.weight(1f)) { v ->
                        state.update { it.copy(prepMinutes = v) }
                    }
                    NumberField(s.cookMinutes, draft.cookMinutes, problems.contains(Problem.CookTimeInvalid), Modifier.weight(1f)) { v ->
                        state.update { it.copy(cookMinutes = v) }
                    }
                }

                OutlinedTextField(
                    value = draft.tags,
                    onValueChange = { v -> state.update { it.copy(tags = v) } },
                    label = { Text(s.tags) },
                    supportingText = { Text(s.tagsHint) },
                    isError = problems.any { it is Problem.TagTooLong || it == Problem.TooManyTags },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionTitle(s.ingredients)
                draft.ingredients.forEachIndexed { index, row ->
                    IngredientEditor(
                        row = row,
                        index = index,
                        last = draft.ingredients.lastIndex,
                        problems = problems,
                        s = s,
                        onChange = { new -> state.update { d -> d.copy(ingredients = d.ingredients.map { if (it.key == row.key) new else it }) } },
                        onMove = { by -> state.update { d -> d.copy(ingredients = d.ingredients.moved(index, by)) } },
                        onRemove = { state.update { d -> d.copy(ingredients = d.ingredients.filterNot { it.key == row.key }) } },
                    )
                }
                OutlinedButton(onClick = { state.update { it.withIngredient() } }) {
                    Icon(AppIcons.Add, contentDescription = null)
                    Text(s.addIngredient, Modifier.padding(start = 8.dp))
                }

                SectionTitle(s.steps)
                draft.steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = step.text,
                            onValueChange = { v ->
                                state.update { d -> d.copy(steps = d.steps.map { if (it.key == step.key) it.copy(text = v) else it }) }
                            },
                            label = { Text(s.stepNumber(index + 1)) },
                            minLines = 2,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.weight(1f),
                        )
                        RowActions(
                            s = s,
                            canUp = index > 0,
                            canDown = index < draft.steps.lastIndex,
                            onMove = { by -> state.update { d -> d.copy(steps = d.steps.moved(index, by)) } },
                            onRemove = { state.update { d -> d.copy(steps = d.steps.filterNot { it.key == step.key }) } },
                        )
                    }
                }
                OutlinedButton(onClick = { state.update { it.withStep() } }) {
                    Icon(AppIcons.Add, contentDescription = null)
                    Text(s.addStep, Modifier.padding(start = 8.dp))
                }

                OutlinedTextField(
                    value = draft.sourceUrl,
                    onValueChange = { v -> state.update { it.copy(sourceUrl = v) } },
                    label = { Text(s.sourceUrl) },
                    isError = problems.contains(Problem.SourceUrlInvalid),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
    }

    if (state.confirmDiscard) {
        AlertDialog(
            onDismissRequest = { state.confirmDiscard = false },
            title = { Text(s.discardTitle) },
            confirmButton = { TextButton(onClick = { app.discard(state) }) { Text(s.discard) } },
            dismissButton = { TextButton(onClick = { state.confirmDiscard = false }) { Text(s.keepEditing) } },
        )
    }
}

@Composable
private fun ImportBanner(text: String, notes: List<String>) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ProblemList(problems: List<Problem>, s: Strings) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            problems.forEach {
                Text(s.problem(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun PhotoField(app: AppState, state: EditorState) {
    val s = LocalStrings.current
    val platform = LocalPlatform.current
    val draft = state.draft
    val pending = (draft.image as? ImageChange.Replace)?.bytes
    val hasImage = pending != null || draft.shownImageFile != null
    if (hasImage) {
        RecipeImage(
            app,
            draft.shownImageFile,
            Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)),
            pending = pending,
            maxSize = 1200,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { app.pickImage(state, platform) }) {
            Icon(AppIcons.Photo, contentDescription = null)
            Text(if (hasImage) s.changePhoto else s.addPhoto, Modifier.padding(start = 8.dp))
        }
        if (hasImage) {
            TextButton(onClick = {
                state.update { it.copy(image = if (it.existingImage != null) ImageChange.Remove else ImageChange.Keep) }
            }) { Text(s.removePhoto) }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, isError: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(5)) },
        label = { Text(label, maxLines = 1) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

@Composable
private fun IngredientEditor(
    row: IngredientDraft,
    index: Int,
    last: Int,
    problems: List<Problem>,
    s: Strings,
    onChange: (IngredientDraft) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.quantity,
                    onValueChange = { onChange(row.copy(quantity = it)) },
                    label = { Text(s.quantity, maxLines = 1) },
                    isError = problems.contains(Problem.QuantityInvalid(index)),
                    singleLine = true,
                    modifier = Modifier.weight(0.9f),
                )
                OutlinedTextField(
                    value = row.unit,
                    onValueChange = { onChange(row.copy(unit = it)) },
                    label = { Text(s.unit, maxLines = 1) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = row.name,
                onValueChange = { onChange(row.copy(name = it)) },
                label = { Text(s.ingredient) },
                isError = problems.contains(Problem.IngredientNameMissing(index)),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.note,
                    onValueChange = { onChange(row.copy(note = it)) },
                    label = { Text(s.note) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                RowActions(s, canUp = index > 0, canDown = index < last, onMove = onMove, onRemove = onRemove, vertical = false)
            }
        }
    }
}

@Composable
private fun RowActions(
    s: Strings,
    canUp: Boolean,
    canDown: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    vertical: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        IconButton(onClick = { onMove(-1) }, enabled = canUp) { Icon(AppIcons.Up, contentDescription = s.moveUp) }
        IconButton(onClick = { onMove(1) }, enabled = canDown) { Icon(AppIcons.Down, contentDescription = s.moveDown) }
        IconButton(onClick = onRemove) { Icon(AppIcons.Delete, contentDescription = s.remove) }
    }
    if (vertical) Column { content() } else Row { content() }
}
