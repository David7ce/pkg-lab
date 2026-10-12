package com.david7ce.shared.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Confirmation before an action that can't be undone. [destructive] paints the confirm
 * button in the error color. There's always a cancel button, and it closes when tapped outside.
 */
@Composable
fun ConfirmDialog(
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = if (title != null) ({ Text(title) }) else null,
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ) else ButtonDefaults.textButtonColors()
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(LocalSharedStrings.current.cancel) } }
    )
}
