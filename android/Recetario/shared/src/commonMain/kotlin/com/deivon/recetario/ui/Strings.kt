package com.deivon.recetario.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.deivon.recetario.logic.ImportFailure
import com.deivon.recetario.logic.NoteLabels
import com.deivon.recetario.logic.Problem
import com.deivon.recetario.logic.RecipePage
import com.deivon.recetario.platform.WebException

/**
 * UI text. English is the base; each language overrides it (Spanish here, the others in
 * Translations.kt). Plain Kotlin instead of a resources library: no code generation, identical on
 * every platform, and a missing translation falls back to English instead of crashing.
 */
open class Strings {
    open val decimalSeparator = '.'
    open val appName = "Recetario"

    // Library
    open val searchHint = "Search recipes or ingredients"
    open val clearSearch = "Clear search"
    open val addRecipe = "Add recipe"
    open val sort = "Sort"
    open val sortByName = "Name"
    open val sortByModified = "Recently edited"
    open val sortByRating = "Rating"
    open val filters = "Filters"
    open val favoritesOnly = "Show only favourites"
    open val minRating = "Minimum rating"
    open val anyRating = "Any"
    open val emptyTitle = "No recipes yet"
    open val emptyBody = "Add your first recipe, or import one from a web page or a backup."
    open val noMatches = "No recipes match"
    open val clearFilters = "Clear filters"
    open val addManually = "Add manually"
    open val importFromUrl = "Import from URL (web scraper)"
    open val importFile = "Import file (Recetario, RecipeSage, My RecipeBox)"
    open val exportLibrary = "Export all (JSON)"
    open val backup = "Back up with photos"
    open val cookbookPdf = "Cookbook (PDF)"
    open val settings = "Settings"
    open val loading = "Loading…"
    open val more = "More options"
    open val back = "Back"

    // Detail
    open val edit = "Edit"
    open val delete = "Delete"
    open val duplicate = "Duplicate"
    open val exportPdf = "Export PDF"
    open val startCooking = "Start cooking"
    open val servings = "Servings"
    open val servingsUnknown = "Servings not set: quantities are shown as written."
    open val fewer = "Fewer servings"
    open val moreServings = "More servings"
    open val prep = "Prep"
    open val cook = "Cook"
    open val total = "Total"
    open val ingredients = "Ingredients"
    open val steps = "Steps"
    open val source = "Source"
    open val noIngredients = "No ingredients."
    open val noSteps = "No steps."
    open val deleteTitle = "Delete recipe?"
    open fun deleteBody(title: String) = "“$title” and its photo will be deleted. This can't be undone."
    open val cancel = "Cancel"
    open fun copyTitle(title: String) = "$title (copy)"
    open fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }
    open val addFavorite = "Add to favourites"
    open val removeFavorite = "Remove from favourites"
    open val rating = "Rating"
    open fun rateAs(stars: Int) = "Rate $stars of 5"
    open val adjustQuantities = "Adjust quantities"
    open val adjustHint = "Every numeric quantity is multiplied; text such as “salt to taste” stays as written."
    open val factor = "Multiplier"
    open val otherFactor = "Other multiplier"
    open val factorInvalid = "Enter a number greater than 0, such as 1.5"
    open val originalQuantities = "Original quantities"
    open val apply = "Apply"
    open fun scaledBy(factor: String) = "Quantities ×$factor"
    open val deleted = "Recipe deleted"
    open val duplicated = "Copy created"

    // Editor
    open val newRecipe = "New recipe"
    open val editRecipe = "Edit recipe"
    open val reviewImport = "Review import"
    open val save = "Save"
    open val title = "Title"
    open val addPhoto = "Add photo"
    open val changePhoto = "Change photo"
    open val removePhoto = "Remove photo"
    open val prepMinutes = "Prep (min)"
    open val cookMinutes = "Cook (min)"
    open val tags = "Tags"
    open val tagsHint = "Separated by commas"
    open val sourceUrl = "Source URL"
    open val notes = "Notes"
    open val quantity = "Qty"
    open val unit = "Unit"
    open val ingredient = "Ingredient"
    open val note = "Note"
    open val addIngredient = "Add ingredient"
    open val addStep = "Add step"
    open fun stepNumber(n: Int) = "Step $n"
    open val moveUp = "Move up"
    open val moveDown = "Move down"
    open val remove = "Remove"
    open val discardTitle = "Discard changes?"
    open val discard = "Discard"
    open val keepEditing = "Keep editing"
    open fun importedFrom(source: String) = "Imported from $source. Check it: nothing is saved until you tap Save."
    open val fixErrors = "Some fields need attention."
    open val saved = "Recipe saved"
    open val imageUnreadable = "That file couldn't be read as an image."

    open fun problem(p: Problem): String = when (p) {
        Problem.TitleMissing -> "The title is required."
        Problem.TitleTooLong -> "The title is too long."
        Problem.ServingsInvalid -> "Servings must be a whole number from 1 to 999."
        Problem.PrepTimeInvalid -> "Prep time must be whole minutes."
        Problem.CookTimeInvalid -> "Cook time must be whole minutes."
        is Problem.IngredientNameMissing -> "Ingredient ${p.index + 1} needs a name."
        is Problem.QuantityInvalid -> "Ingredient ${p.index + 1}: the quantity must be a number such as 2, 1.5 or 1/2."
        is Problem.TagTooLong -> "The tag “${p.tag.take(20)}…” is too long."
        Problem.TooManyTags -> "Too many tags."
        Problem.SourceUrlInvalid -> "The source must be a web address starting with http:// or https://."
        Problem.IdInvalid -> "The recipe id is not valid."
        Problem.ImageNameInvalid -> "The image reference is not valid."
        Problem.StepEmpty -> "A step is empty."
        Problem.TooLarge -> "The recipe is too large."
        Problem.RatingInvalid -> "The rating must be from 1 to 5 stars."
    }

    open fun warning(w: RecipePage.Warning, totalMinutes: Int?): String = when (w) {
        RecipePage.Warning.NoTitle -> "The page gives no title."
        RecipePage.Warning.NoIngredients -> "The page lists no ingredients."
        RecipePage.Warning.NoSteps -> "The page lists no steps."
        RecipePage.Warning.OnlyTotalTime -> "The page only gives a total time (${duration(totalMinutes ?: 0)}); split it into prep and cook if you like."
    }

    // Cooking mode
    open fun stepOf(n: Int, total: Int) = "Step $n of $total"
    open val previous = "Previous"
    open val next = "Next"
    open val finish = "Finish"
    open val close = "Close"

    // Import / export
    open val urlLabel = "Page address"
    open val importAction = "Import"
    open val reading = "Reading the page…"
    open val importTitle = "Import recipes"
    open fun importSummary(added: Int, identical: Int, conflicts: Int): String = buildString {
        append("$added new")
        if (identical > 0) append(", $identical already in your library (skipped)")
        if (conflicts > 0) append(", $conflicts with the same id but different content")
        append('.')
    }
    open val conflictQuestion = "What should happen with the recipes that differ?"
    open fun importFrom(source: String) = "Import from $source"
    open fun skipped(n: Int) = "$n entr${if (n == 1) "y" else "ies"} could not be read as a recipe and will be left out."
    open fun photosToDownload(n: Int) = "$n photo(s) will be downloaded (needs a connection)."
    open fun downloadingPhotos(done: Int, total: Int) = "Downloading photos… $done of $total"
    open fun photosFailed(n: Int) = "$n photo(s) could not be downloaded or read; those recipes have no photo."
    open val inactiveTime = "Resting time"
    open val nutrition = "Nutrition"
    open val cookware = "Cookware"
    open val video = "Video"
    fun noteLabels() = NoteLabels(prep, cook, total, inactiveTime, nutrition, cookware, video, ::duration)
    open val keepMine = "Keep mine"
    open val importAsCopies = "Import as copies"
    open val replaceMine = "Replace mine"
    open fun imported(inserted: Int, replaced: Int) =
        if (replaced == 0) "Imported $inserted recipe(s)" else "Imported $inserted, replaced $replaced"
    open val nothingToImport = "Everything in this file is already in your library."
    open val fileSaved = "File saved"
    open val fileNotSaved = "The file couldn't be saved."
    open val fileNotRead = "The file couldn't be read."
    open fun backupMissingImages(n: Int) = "Backup saved. $n photo(s) were missing on this device and were left out."
    open val noRecipesToExport = "There are no recipes to export."
    open fun unexpected(detail: String?) = "Something went wrong" + (detail?.let { ": $it" } ?: ".")

    open fun web(e: WebException): String = when (e.kind) {
        WebException.Kind.InvalidUrl -> "That is not a web address (http:// or https://)."
        WebException.Kind.Network -> "The page couldn't be reached. Check the connection and try again."
        WebException.Kind.Http -> "The site refused the request (HTTP ${e.httpStatus}). Some sites block apps."
        WebException.Kind.TooLarge -> "The page is too large."
    }
    open val noRecipeOnPage = "No recipe data found on that page (it has no schema.org Recipe). You can still add it by hand."

    open fun failure(f: ImportFailure): String = when (f) {
        ImportFailure.NotRecognized -> "This file is not supported (Recetario, RecipeSage or My RecipeBox)."
        is ImportFailure.UnsupportedVersion -> "This file was made by a newer version (format ${f.version}). Update the app to import it."
        is ImportFailure.Malformed -> "The file is damaged (${f.detail})."
        is ImportFailure.DuplicateIds -> "The file contains the same recipe id more than once; nothing was imported."
        is ImportFailure.InvalidRecipe -> "“${f.title.take(40)}” is not valid: ${f.problems.firstOrNull()?.let { problem(it) }.orEmpty()}"
        is ImportFailure.MissingImage -> "The backup is incomplete: a photo is missing."
        is ImportFailure.UnsafeEntry -> "The backup contains an unexpected file; nothing was imported."
        ImportFailure.TooLarge -> "The file is too large."
        ImportFailure.Empty -> "The file contains no recipes."
    }

    // Settings
    open val appearance = "Appearance"
    open val language = "Language"
    open val languageSystem = "Device language"
    open val data = "Data"
    open val about = "About"
    open val theme = "Theme"
    open val themeSystem = "System"
    open val themeLight = "Light"
    open val themeDark = "Dark"
    open val aboutData = "Your recipes and photos are stored only on this device. Use “Back up with photos” to keep a copy."

    // PDF
    open fun pdfServings(n: Int) = "$n servings"
    open fun pdfCount(n: Int) = "$n recipes"
}

object EsStrings : Strings() {
    override val decimalSeparator = ','

    override val searchHint = "Buscar recetas o ingredientes"
    override val clearSearch = "Borrar búsqueda"
    override val addRecipe = "Añadir receta"
    override val sort = "Ordenar"
    override val sortByName = "Nombre"
    override val sortByModified = "Editadas recientemente"
    override val sortByRating = "Valoración"
    override val filters = "Filtros"
    override val favoritesOnly = "Mostrar solo favoritos"
    override val minRating = "Valoración mínima"
    override val anyRating = "Cualquiera"
    override val emptyTitle = "Aún no hay recetas"
    override val emptyBody = "Añade tu primera receta o impórtala desde una página web o una copia de seguridad."
    override val noMatches = "Ninguna receta coincide"
    override val clearFilters = "Quitar filtros"
    override val addManually = "Añadir manualmente"
    override val importFromUrl = "Importar desde URL (web scraper)"
    override val importFile = "Importar archivo (Recetario, RecipeSage, My RecipeBox)"
    override val exportLibrary = "Exportar todo (JSON)"
    override val backup = "Copia de seguridad con fotos"
    override val cookbookPdf = "Recetario (PDF)"
    override val settings = "Ajustes"
    override val loading = "Cargando…"
    override val more = "Más opciones"
    override val back = "Atrás"

    override val edit = "Editar"
    override val delete = "Eliminar"
    override val duplicate = "Duplicar"
    override val exportPdf = "Exportar PDF"
    override val startCooking = "Empezar a cocinar"
    override val servings = "Raciones"
    override val servingsUnknown = "Sin raciones indicadas: las cantidades se muestran tal cual."
    override val fewer = "Menos raciones"
    override val moreServings = "Más raciones"
    override val prep = "Preparación"
    override val cook = "Cocción"
    override val total = "Total"
    override val ingredients = "Ingredientes"
    override val steps = "Pasos"
    override val source = "Fuente"
    override val noIngredients = "Sin ingredientes."
    override val noSteps = "Sin pasos."
    override val deleteTitle = "¿Eliminar la receta?"
    override fun deleteBody(title: String) = "Se eliminarán «$title» y su foto. No se puede deshacer."
    override val cancel = "Cancelar"
    override fun copyTitle(title: String) = "$title (copia)"
    override val addFavorite = "Añadir a favoritos"
    override val removeFavorite = "Quitar de favoritos"
    override val rating = "Valoración"
    override fun rateAs(stars: Int) = "Valorar con $stars de 5"
    override val adjustQuantities = "Ajustar cantidades"
    override val adjustHint = "Se multiplican todas las cantidades numéricas; los textos como «sal al gusto» no cambian."
    override val factor = "Multiplicador"
    override val otherFactor = "Otro multiplicador"
    override val factorInvalid = "Introduce un número mayor que 0, como 1,5"
    override val originalQuantities = "Cantidades originales"
    override val apply = "Aplicar"
    override fun scaledBy(factor: String) = "Cantidades ×$factor"
    override val deleted = "Receta eliminada"
    override val duplicated = "Copia creada"

    override val newRecipe = "Nueva receta"
    override val editRecipe = "Editar receta"
    override val reviewImport = "Revisar importación"
    override val save = "Guardar"
    override val title = "Título"
    override val addPhoto = "Añadir foto"
    override val changePhoto = "Cambiar foto"
    override val removePhoto = "Quitar foto"
    override val prepMinutes = "Preparación (min)"
    override val cookMinutes = "Cocción (min)"
    override val tags = "Etiquetas"
    override val tagsHint = "Separadas por comas"
    override val sourceUrl = "URL de origen"
    override val notes = "Notas"
    override val quantity = "Cant."
    override val unit = "Unidad"
    override val ingredient = "Ingrediente"
    override val note = "Nota"
    override val addIngredient = "Añadir ingrediente"
    override val addStep = "Añadir paso"
    override fun stepNumber(n: Int) = "Paso $n"
    override val moveUp = "Subir"
    override val moveDown = "Bajar"
    override val remove = "Quitar"
    override val discardTitle = "¿Descartar los cambios?"
    override val discard = "Descartar"
    override val keepEditing = "Seguir editando"
    override fun importedFrom(source: String) = "Importada de $source. Revísala: no se guarda nada hasta que pulses Guardar."
    override val fixErrors = "Hay campos que revisar."
    override val saved = "Receta guardada"
    override val imageUnreadable = "Ese archivo no se pudo leer como imagen."

    override fun problem(p: Problem): String = when (p) {
        Problem.TitleMissing -> "El título es obligatorio."
        Problem.TitleTooLong -> "El título es demasiado largo."
        Problem.ServingsInvalid -> "Las raciones deben ser un número entero de 1 a 999."
        Problem.PrepTimeInvalid -> "La preparación debe ser un número entero de minutos."
        Problem.CookTimeInvalid -> "La cocción debe ser un número entero de minutos."
        is Problem.IngredientNameMissing -> "El ingrediente ${p.index + 1} necesita un nombre."
        is Problem.QuantityInvalid -> "Ingrediente ${p.index + 1}: la cantidad debe ser un número como 2, 1,5 o 1/2."
        is Problem.TagTooLong -> "La etiqueta «${p.tag.take(20)}…» es demasiado larga."
        Problem.TooManyTags -> "Demasiadas etiquetas."
        Problem.SourceUrlInvalid -> "La fuente debe ser una dirección web que empiece por http:// o https://."
        Problem.IdInvalid -> "El identificador de la receta no es válido."
        Problem.ImageNameInvalid -> "La referencia a la imagen no es válida."
        Problem.StepEmpty -> "Hay un paso vacío."
        Problem.TooLarge -> "La receta es demasiado grande."
        Problem.RatingInvalid -> "La valoración debe ser de 1 a 5 estrellas."
    }

    override fun warning(w: RecipePage.Warning, totalMinutes: Int?): String = when (w) {
        RecipePage.Warning.NoTitle -> "La página no indica título."
        RecipePage.Warning.NoIngredients -> "La página no incluye ingredientes."
        RecipePage.Warning.NoSteps -> "La página no incluye pasos."
        RecipePage.Warning.OnlyTotalTime -> "La página solo indica el tiempo total (${duration(totalMinutes ?: 0)}); repártelo entre preparación y cocción si quieres."
    }

    override fun stepOf(n: Int, total: Int) = "Paso $n de $total"
    override val previous = "Anterior"
    override val next = "Siguiente"
    override val finish = "Terminar"
    override val close = "Cerrar"

    override val urlLabel = "Dirección de la página"
    override val importAction = "Importar"
    override val reading = "Leyendo la página…"
    override val importTitle = "Importar recetas"
    override fun importSummary(added: Int, identical: Int, conflicts: Int): String = buildString {
        append("$added nuevas")
        if (identical > 0) append(", $identical ya están en tu recetario (se omiten)")
        if (conflicts > 0) append(", $conflicts con el mismo identificador pero contenido distinto")
        append('.')
    }
    override val conflictQuestion = "¿Qué hacemos con las recetas que difieren?"
    override fun importFrom(source: String) = "Importar desde $source"
    override fun skipped(n: Int) = "$n elemento(s) no se pudieron leer como receta y se omitirán."
    override fun photosToDownload(n: Int) = "Se descargarán $n foto(s) (hace falta conexión)."
    override fun downloadingPhotos(done: Int, total: Int) = "Descargando fotos… $done de $total"
    override fun photosFailed(n: Int) = "$n foto(s) no se pudieron descargar o leer; esas recetas quedan sin foto."
    override val inactiveTime = "Reposo"
    override val nutrition = "Nutrición"
    override val cookware = "Utensilios"
    override val video = "Vídeo"
    override val keepMine = "Conservar las mías"
    override val importAsCopies = "Importar como copias"
    override val replaceMine = "Reemplazar las mías"
    override fun imported(inserted: Int, replaced: Int) =
        if (replaced == 0) "Importadas $inserted receta(s)" else "Importadas $inserted, reemplazadas $replaced"
    override val nothingToImport = "Todo lo de este archivo ya está en tu recetario."
    override val fileSaved = "Archivo guardado"
    override val fileNotSaved = "No se pudo guardar el archivo."
    override val fileNotRead = "No se pudo leer el archivo."
    override fun backupMissingImages(n: Int) = "Copia guardada. Faltaban $n foto(s) en este dispositivo y no se incluyeron."
    override val noRecipesToExport = "No hay recetas que exportar."
    override fun unexpected(detail: String?) = "Algo salió mal" + (detail?.let { ": $it" } ?: ".")

    override fun web(e: WebException): String = when (e.kind) {
        WebException.Kind.InvalidUrl -> "Eso no es una dirección web (http:// o https://)."
        WebException.Kind.Network -> "No se pudo acceder a la página. Revisa la conexión e inténtalo de nuevo."
        WebException.Kind.Http -> "El sitio rechazó la petición (HTTP ${e.httpStatus}). Algunos sitios bloquean las apps."
        WebException.Kind.TooLarge -> "La página es demasiado grande."
    }
    override val noRecipeOnPage = "No hay datos de receta en esa página (no tiene schema.org Recipe). Puedes añadirla a mano."

    override fun failure(f: ImportFailure): String = when (f) {
        ImportFailure.NotRecognized -> "Este archivo no es compatible (Recetario, RecipeSage o My RecipeBox)."
        is ImportFailure.UnsupportedVersion -> "El archivo es de una versión más nueva (formato ${f.version}). Actualiza la app para importarlo."
        is ImportFailure.Malformed -> "El archivo está dañado (${f.detail})."
        is ImportFailure.DuplicateIds -> "El archivo repite el identificador de una receta; no se importó nada."
        is ImportFailure.InvalidRecipe -> "«${f.title.take(40)}» no es válida: ${f.problems.firstOrNull()?.let { problem(it) }.orEmpty()}"
        is ImportFailure.MissingImage -> "La copia está incompleta: falta una foto."
        is ImportFailure.UnsafeEntry -> "La copia contiene un archivo inesperado; no se importó nada."
        ImportFailure.TooLarge -> "El archivo es demasiado grande."
        ImportFailure.Empty -> "El archivo no contiene recetas."
    }

    override val appearance = "Apariencia"
    override val language = "Idioma"
    override val languageSystem = "Idioma del dispositivo"
    override val data = "Datos"
    override val about = "Acerca de"
    override val theme = "Tema"
    override val themeSystem = "Sistema"
    override val themeLight = "Claro"
    override val themeDark = "Oscuro"
    override val aboutData = "Tus recetas y fotos se guardan solo en este dispositivo. Usa «Copia de seguridad con fotos» para conservar una copia."

    override fun pdfServings(n: Int) = "$n raciones"
    override fun pdfCount(n: Int) = "$n recetas"
}

val LocalStrings = staticCompositionLocalOf<Strings> { Strings() }
