package com.david7ce.shared.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Multi-select for a list. Turned on with a long press; while something is selected,
 * tapping a row marks/unmarks it instead of opening it. [K] is each row's stable key.
 */
class Selection<K> {
    var keys by mutableStateOf<Set<K>>(emptySet())
        private set

    val active: Boolean get() = keys.isNotEmpty()

    operator fun contains(key: K): Boolean = key in keys

    fun toggle(key: K) { keys = if (key in keys) keys - key else keys + key }

    fun selectAll(all: Collection<K>) { keys = all.toSet() }

    fun clear() { keys = emptySet() }

    /** Quita de la selección lo que ya no existe (p. ej. el histórico se recortó). */
    fun retain(all: Collection<K>) {
        val existing = all.toSet()
        if (!existing.containsAll(keys)) keys = keys intersect existing
    }
}

@Composable
fun <K> rememberSelection(): Selection<K> = remember { Selection() }

/** Acciones de la barra superior en modo selección: seleccionar todo y borrar. */
@Composable
fun RowScope.SelectionActions(allSelected: Boolean, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    val strings = LocalSharedStrings.current
    IconButton(onClick = onSelectAll, enabled = !allSelected) {
        Icon(SharedIcons.SelectAll, strings.selectAll)
    }
    IconButton(onClick = onDelete) {
        Icon(SharedIcons.Delete, strings.delete)
    }
}
