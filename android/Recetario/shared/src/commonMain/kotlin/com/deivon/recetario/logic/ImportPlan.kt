package com.david7ce.recetario.logic

import com.david7ce.recetario.model.Recipe

/** What the user chooses when an imported recipe has the same id as a different local one. */
enum class ConflictPolicy { KeepMine, ImportAsCopies, ReplaceMine }

/**
 * Compares incoming recipes with the library, by id:
 * - [added]: id not in the library;
 * - [identical]: same id and same content (skipped, nothing to do);
 * - [conflicts]: same id, different content — never overwritten without an explicit [ConflictPolicy].
 */
class ImportPlan(
    val added: List<Recipe>,
    val identical: List<Recipe>,
    val conflicts: List<Recipe>,
) {
    val isEmpty: Boolean get() = added.isEmpty() && conflicts.isEmpty()

    /** The writes to perform. Copies get a fresh id; replacements keep the local creation date. */
    fun resolve(policy: ConflictPolicy, existing: Map<String, Recipe>, newId: () -> String, now: Long): Writes {
        val conflictWrites = when (policy) {
            ConflictPolicy.KeepMine -> Writes(emptyList(), emptyList())
            ConflictPolicy.ImportAsCopies -> Writes(conflicts.map { it.copy(id = newId(), updatedAt = now) }, emptyList())
            ConflictPolicy.ReplaceMine -> Writes(
                emptyList(),
                conflicts.map { it.copy(createdAt = existing[it.id]?.createdAt ?: it.createdAt, updatedAt = now) },
            )
        }
        return Writes(added + conflictWrites.insert, conflictWrites.replace)
    }

    /** [insert]: ids not in the library. [replace]: ids that are, whose rows (and image) are replaced. */
    class Writes(val insert: List<Recipe>, val replace: List<Recipe>) {
        val all: List<Recipe> get() = insert + replace
    }

    companion object {
        fun of(existing: Map<String, Recipe>, incoming: List<Recipe>): ImportPlan {
            val added = mutableListOf<Recipe>()
            val identical = mutableListOf<Recipe>()
            val conflicts = mutableListOf<Recipe>()
            for (r in incoming) {
                val mine = existing[r.id]
                when {
                    mine == null -> added += r
                    mine.sameContentAs(r) -> identical += r
                    else -> conflicts += r
                }
            }
            return ImportPlan(added, identical, conflicts)
        }
    }
}
