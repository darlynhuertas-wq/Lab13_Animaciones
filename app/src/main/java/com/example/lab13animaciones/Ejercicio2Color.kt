package com.example.lab13animaciones

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Ejercicio2Color() {
    var esAzul by remember { mutableStateOf(true) }
    var usarSpring by remember { mutableStateOf(false) }

    // 3. Especificación de animación: tween (tiempo fijo) o spring (efecto resorte)
    val especificacion: AnimationSpec<Color> =
        if (usarSpring) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        } else {
            tween(durationMillis = 1000)
        }

    // 2. animateColorAsState anima el cambio entre azul y verde
    val colorFondo by animateColorAsState(
        targetValue = if (esAzul) Color(0xFF1E88E5) else Color(0xFF43A047),
        animationSpec = especificacion,
        label = "colorFondo"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ejercicio 2: animateColorAsState", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorFondo),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (esAzul) "Azul" else "Verde",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Botón que cambia el color
        Button(onClick = { esAzul = !esAzul }) {
            Text("Cambiar color")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("tween")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = usarSpring, onCheckedChange = { usarSpring = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("spring")
        }
        Text(
            if (usarSpring) "Usando spring (resorte, más lento)" else "Usando tween (1000 ms)",
            fontSize = 14.sp
        )
    }
}
