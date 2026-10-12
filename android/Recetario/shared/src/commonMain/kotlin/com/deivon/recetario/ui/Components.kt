package com.deivon.recetario.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.deivon.recetario.logic.Quantities
import com.deivon.recetario.model.Ingredient

/** A recipe photo from the image folder, or from not-yet-saved bytes; a neutral placeholder otherwise. */
@Composable
fun RecipeImage(
    app: AppState,
    file: String?,
    modifier: Modifier = Modifier,
    pending: ByteArray? = null,
    maxSize: Int = 800,
    contentDescription: String? = null,
) {
    val bitmap by produceState<ImageBitmap?>(null, file, pending, maxSize) {
        value = when {
            pending != null -> app.decode(pending, maxSize)
            file != null -> app.image(file, maxSize)
            else -> null
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(b, contentDescription, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(
                AppIcons.Dish,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
fun BackButton(app: AppState, icon: ImageVector = AppIcons.Back) {
    IconButton(onClick = app::back) { Icon(icon, contentDescription = LocalStrings.current.back) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

/** Empty, no-results and similar states: a short title, an explanation and optional actions. */
@Composable
fun MessageState(title: String, body: String?, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(AppIcons.Dish, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (body != null) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            actions()
        }
    }
}

@Composable
fun MetaItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** "200 g flour, sifted" with the amount in bold and the note muted. */
@Composable
fun ingredientText(ing: Ingredient, s: Strings): AnnotatedString {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    return buildAnnotatedString {
        val amount = listOfNotNull(ing.quantity?.let { Quantities.format(it, s.decimalSeparator) }, ing.unit).joinToString(" ")
        if (amount.isNotEmpty()) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(amount) }
            append(' ')
        }
        append(ing.name)
        ing.note?.let {
            withStyle(SpanStyle(color = muted, fontStyle = FontStyle.Italic)) {
                append(", ")
                append(it)
            }
        }
    }
}

@Composable
fun IngredientRow(ing: Ingredient, s: Strings, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(18.dp))
        Text(ingredientText(ing, s), style = MaterialTheme.typography.bodyLarge)
    }
}
