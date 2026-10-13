package com.david7ce.sleepcycles.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.david7ce.sleepcycles.logic.splitBold

private fun bolded(text: String): AnnotatedString = buildAnnotatedString {
    splitBold(text).forEach { (part, bold) ->
        if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) } else append(part)
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.howItWorks) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                s.howItWorksParagraphs.forEach { Text(bolded(it), style = MaterialTheme.typography.bodyMedium) }
                Text(
                    s.howItWorksNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.gotIt) } },
    )
}
