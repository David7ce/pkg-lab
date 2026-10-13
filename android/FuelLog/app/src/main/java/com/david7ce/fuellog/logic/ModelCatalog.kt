package com.david7ce.fuellog.logic

import java.text.Normalizer

enum class CatalogFuel(val key: String) { Petrol("petrol"), Diesel("diesel"), Hybrid("hybrid") }

/** A catalogue entry: brand, model, usual fuel and reference consumption in tenths of L/100 km. */
data class CatalogModel(val brand: String, val model: String, val fuel: CatalogFuel, val referenceDl100: Int) {
    val name: String get() = "$brand $model"
}

/**
 * Brand/model list shipped in assets/models_es.csv. Plain CSV so it can be extended without code:
 * `brand,model,fuel,l100`, `#` comments, one header line. Malformed lines are ignored.
 */
object ModelCatalog {

    fun parse(csv: String): List<CatalogModel> = csv.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("brand,") }
        .mapNotNull { line ->
            val f = line.split(',').map { it.trim() }
            if (f.size != 4 || f[0].isEmpty() || f[1].isEmpty()) return@mapNotNull null
            val fuel = CatalogFuel.entries.firstOrNull { it.key == f[2] } ?: return@mapNotNull null
            val dl = Decimals.parse(f[3], 1)?.toInt()?.takeIf { it in 10..300 } ?: return@mapNotNull null
            CatalogModel(f[0], f[1], fuel, dl)
        }
        .sortedWith(compareBy({ it.brand.lowercase() }, { it.model.lowercase() }))
        .toList()

    /** Case- and accent-insensitive match on "brand model", every word must appear ("seat leon" finds "Seat León"). */
    fun search(models: List<CatalogModel>, query: String): List<CatalogModel> {
        val words = fold(query).split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return models
        return models.filter { m -> fold(m.name).let { name -> words.all { it in name } } }
    }

    private fun fold(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
}
