package com.david7ce.sleepcycles.logic

/**
 * Splits text marked with **double asterisks** into runs, each flagged bold or not.
 * An unmatched marker is kept as plain text so a typo in a translation never hides words.
 */
fun splitBold(text: String): List<Pair<String, Boolean>> {
    val parts = text.split("**")
    // An even number of parts means an odd number of markers: the last marker has no partner.
    if (parts.size % 2 == 0) return listOf(text to false)
    return parts.mapIndexedNotNull { index, part -> if (part.isEmpty()) null else part to (index % 2 == 1) }
}
