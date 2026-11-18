package com.example.lab13animaciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lab13animaciones.ui.theme.Lab13AnimacionesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab13AnimacionesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MenuEjercicios(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MenuEjercicios(modifier: Modifier = Modifier) {
    var ejercicio by rememberSaveable { mutableIntStateOf(1) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = ejercicio - 1) {
            for (numero in 1..4) {
                Tab(
                    selected = ejercicio == numero,
                    onClick = { ejercicio = numero },
                    text = { Text("Ej $numero") }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (ejercicio) {
                1 -> Ejercicio1Visibilidad()
                2 -> Ejercicio2Color()
                3 -> Ejercicio3TamanoPosicion()
                else -> Ejercicio4Contenido()
            }
        }
    }
}
