package com.deivon.recetario.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deivon.recetario.RecetarioApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = (application as RecetarioApplication).database
        setContent { RecetarioApp(db.recipeQueries.selectAllRecipes().executeAsList().size.toLong()) }
    }
}
