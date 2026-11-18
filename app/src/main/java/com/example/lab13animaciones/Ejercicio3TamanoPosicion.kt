package com.example.lab13animaciones

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Ejercicio3TamanoPosicion() {
    var movido by remember { mutableStateOf(false) }
    var ordenA by remember { mutableStateOf(true) }

    // 2. animateDpAsState para el tamaño y para la posición (offset)
    val tamano by animateDpAsState(
        targetValue = if (movido) 150.dp else 80.dp,
        animationSpec = tween(durationMillis = 700),
        label = "tamano"
    )
    val desplazamientoX by animateDpAsState(
        targetValue = if (movido) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 700),
        label = "desplazamientoX"
    )
    val desplazamientoY by animateDpAsState(
        targetValue = if (movido) 200.dp else 0.dp,
        animationSpec = tween(durationMillis = 700),
        label = "desplazamientoY"
    )

    // 3. Mismo cuadro con los modificadores en distinto orden
    val modificadorCuadro =
        if (ordenA) {
            // Orden A: offset -> size -> background (se mueve todo el cuadro)
            Modifier
                .offset(x = desplazamientoX, y = desplazamientoY)
                .size(tamano)
                .background(Color(0xFFE53935))
        } else {
            // Orden B: size -> background -> offset (el fondo se queda, solo se mueve el contenido)
            Modifier
                .size(tamano)
                .background(Color(0xFFE53935))
                .offset(x = desplazamientoX, y = desplazamientoY)
        }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ejercicio 3: Tamaño y posición", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Botón que mueve y cambia de tamaño el cuadro
        Button(onClick = { movido = !movido }) {
            Text(if (movido) "Regresar" else "Mover y agrandar")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Orden B")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = ordenA, onCheckedChange = { ordenA = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("Orden A")
        }
        Text(
            if (ordenA) "A: offset → size → background" else "B: size → background → offset",
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Área donde se mueve el cuadro
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFEEEEEE))
        ) {
            Box(modifier = modificadorCuadro, contentAlignment = Alignment.Center) {
                Text(
                    if (ordenA) "A" else "B",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
