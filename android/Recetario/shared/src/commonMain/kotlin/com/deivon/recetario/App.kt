package com.deivon.recetario

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun RecetarioApp(recipeCount: Long) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) { Text("Recetario · $recipeCount") }
        }
    }
}

fun greeting(): String = "Recetario"
