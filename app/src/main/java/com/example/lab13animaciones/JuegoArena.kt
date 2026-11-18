package com.example.lab13animaciones

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.random.Random

// ---------- Modelo del juego ----------

enum class Bando { Jugador, Enemigo }

enum class EstadoJuego { Inicio, Jugando, Victoria, Derrota, Empate }

data class Carta(
    val nombre: String,
    val emoji: String,
    val costo: Int,
    val vida: Float,
    val danio: Float,      // daño por segundo
    val velocidad: Float,  // fracción de la arena por segundo
    val alcance: Float     // distancia de ataque (fracción de la arena)
)

data class Tropa(
    val id: Int,
    val carta: Carta,
    val bando: Bando,
    val carril: Int,       // 0 = izquierda, 1 = derecha
    val y: Float,          // 0 = arriba (enemigo), 1 = abajo (jugador)
    val vida: Float
)

val MAZO = listOf(
    Carta("Espadachín", "⚔️", 3, 320f, 60f, 0.07f, 0.05f),
    Carta("Arquera", "🏹", 3, 180f, 45f, 0.07f, 0.16f),
    Carta("Coloso", "🛡️", 5, 900f, 35f, 0.045f, 0.05f),
    Carta("Lobo", "🐺", 2, 130f, 50f, 0.13f, 0.05f)
)

const val VIDA_PRINCESA = 1000f
const val VIDA_REY = 1600f
const val DANIO_TORRE = 30f

// Torres: índice 0 = izquierda, 1 = rey, 2 = derecha
fun posicionYTorre(bando: Bando, indice: Int): Float =
    if (bando == Bando.Enemigo) {
        if (indice == 1) 0.06f else 0.15f
    } else {
        if (indice == 1) 0.94f else 0.85f
    }

fun posicionXTorre(indice: Int): Float = when (indice) {
    0 -> 0.22f
    1 -> 0.5f
    else -> 0.78f
}

// ---------- Pantalla del juego ----------

@Composable
fun JuegoArena() {
    var estado by remember { mutableStateOf(EstadoJuego.Inicio) }
    var tropas by remember { mutableStateOf(listOf<Tropa>()) }
    var torresEnemigo by remember { mutableStateOf(listOf(VIDA_PRINCESA, VIDA_REY, VIDA_PRINCESA)) }
    var torresJugador by remember { mutableStateOf(listOf(VIDA_PRINCESA, VIDA_REY, VIDA_PRINCESA)) }
    var elixir by remember { mutableFloatStateOf(5f) }
    var tiempo by remember { mutableFloatStateOf(120f) }
    var seleccion by remember { mutableStateOf<Int?>(null) }
    var siguienteId by remember { mutableIntStateOf(0) }
    var esperaEnemigo by remember { mutableFloatStateOf(4f) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    val coronasJugador = if (torresEnemigo[1] <= 0f) 3 else torresEnemigo.count { it <= 0f }
    val coronasEnemigo = if (torresJugador[1] <= 0f) 3 else torresJugador.count { it <= 0f }
    val dobleElixir = tiempo <= 30f

    fun reiniciar() {
        tropas = emptyList()
        torresEnemigo = listOf(VIDA_PRINCESA, VIDA_REY, VIDA_PRINCESA)
        torresJugador = listOf(VIDA_PRINCESA, VIDA_REY, VIDA_PRINCESA)
        elixir = 5f
        tiempo = 120f
        seleccion = null
        esperaEnemigo = 4f
        estado = EstadoJuego.Jugando
    }

    fun desplegar(fx: Float, fy: Float) {
        if (estado != EstadoJuego.Jugando) return
        val indice = seleccion
        if (indice == null) {
            mensaje = "Primero elige una carta"
            return
        }
        if (fy < 0.5f) {
            mensaje = "Solo puedes desplegar en tu lado"
            return
        }
        val carta = MAZO[indice]
        if (elixir < carta.costo) {
            mensaje = "¡Elixir insuficiente!"
            return
        }
        elixir -= carta.costo
        val carril = if (fx < 0.5f) 0 else 1
        tropas = tropas + Tropa(siguienteId, carta, Bando.Jugador, carril, fy.coerceIn(0.55f, 0.8f), carta.vida)
        siguienteId++
        seleccion = null
    }

    // El mensaje se oculta solo después de 1.2 s
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            kotlinx.coroutines.delay(1200)
            mensaje = null
        }
    }

    // ---------- Bucle del juego (se ejecuta en cada fotograma) ----------
    LaunchedEffect(estado) {
        if (estado != EstadoJuego.Jugando) return@LaunchedEffect
        var ultimo = withFrameNanos { it }
        while (true) {
            val ahora = withFrameNanos { it }
            val dt = ((ahora - ultimo) / 1_000_000_000f).coerceAtMost(0.05f)
            ultimo = ahora

            // Tiempo y elixir
            tiempo -= dt
            val velocidadElixir = if (tiempo <= 30f) 2f else 1f
            elixir = (elixir + dt / 1.4f * velocidadElixir).coerceAtMost(10f)

            // El enemigo despliega tropas cada cierto tiempo
            esperaEnemigo -= dt
            if (esperaEnemigo <= 0f) {
                val carta = MAZO.random()
                tropas = tropas + Tropa(siguienteId, carta, Bando.Enemigo, Random.nextInt(2), 0.2f, carta.vida)
                siguienteId++
                esperaEnemigo = Random.nextFloat() * 3f + 3.5f
            }

            val actuales = tropas
            val torresE = torresEnemigo
            val torresJ = torresJugador
            val danioTropas = mutableMapOf<Int, Float>()
            val danioTorresE = FloatArray(3)
            val danioTorresJ = FloatArray(3)

            // Las torres disparan a la tropa rival más cercana
            for (i in 0..2) {
                if (torresE[i] > 0f) {
                    val yT = posicionYTorre(Bando.Enemigo, i)
                    val objetivo = actuales
                        .filter { it.bando == Bando.Jugador && (i == 1 || it.carril == i / 2) && abs(it.y - yT) <= 0.25f }
                        .minByOrNull { abs(it.y - yT) }
                    if (objetivo != null) {
                        danioTropas[objetivo.id] = (danioTropas[objetivo.id] ?: 0f) + DANIO_TORRE * dt
                    }
                }
                if (torresJ[i] > 0f) {
                    val yT = posicionYTorre(Bando.Jugador, i)
                    val objetivo = actuales
                        .filter { it.bando == Bando.Enemigo && (i == 1 || it.carril == i / 2) && abs(it.y - yT) <= 0.25f }
                        .minByOrNull { abs(it.y - yT) }
                    if (objetivo != null) {
                        danioTropas[objetivo.id] = (danioTropas[objetivo.id] ?: 0f) + DANIO_TORRE * dt
                    }
                }
            }

            // Cada tropa pelea, ataca una torre o avanza
            val movidas = actuales.map { t ->
                val direccion = if (t.bando == Bando.Jugador) -1f else 1f
                val rival = actuales
                    .filter { it.bando != t.bando && it.carril == t.carril }
                    .minByOrNull { abs(it.y - t.y) }

                if (rival != null && abs(rival.y - t.y) <= t.carta.alcance + 0.03f) {
                    danioTropas[rival.id] = (danioTropas[rival.id] ?: 0f) + t.carta.danio * dt
                    t
                } else {
                    val bandoRival = if (t.bando == Bando.Jugador) Bando.Enemigo else Bando.Jugador
                    val torresRival = if (t.bando == Bando.Jugador) torresE else torresJ
                    val indiceTorre = if (torresRival[t.carril * 2] > 0f) t.carril * 2 else 1
                    val yTorre = posicionYTorre(bandoRival, indiceTorre)
                    if (abs(yTorre - t.y) <= t.carta.alcance + 0.05f) {
                        if (t.bando == Bando.Jugador) {
                            danioTorresE[indiceTorre] += t.carta.danio * dt
                        } else {
                            danioTorresJ[indiceTorre] += t.carta.danio * dt
                        }
                        t
                    } else {
                        t.copy(y = t.y + direccion * t.carta.velocidad * dt)
                    }
                }
            }

            // Aplicar daño y quitar tropas derrotadas
            tropas = movidas
                .map { t -> t.copy(vida = t.vida - (danioTropas[t.id] ?: 0f)) }
                .filter { it.vida > 0f }
            torresEnemigo = torresE.mapIndexed { i, v -> (v - danioTorresE[i]).coerceAtLeast(0f) }
            torresJugador = torresJ.mapIndexed { i, v -> (v - danioTorresJ[i]).coerceAtLeast(0f) }

            // ¿Terminó la partida?
            val cJ = if (torresEnemigo[1] <= 0f) 3 else torresEnemigo.count { it <= 0f }
            val cE = if (torresJugador[1] <= 0f) 3 else torresJugador.count { it <= 0f }
            if (torresEnemigo[1] <= 0f) {
                estado = EstadoJuego.Victoria
                break
            } else if (torresJugador[1] <= 0f) {
                estado = EstadoJuego.Derrota
                break
            } else if (tiempo <= 0f) {
                estado = when {
                    cJ > cE -> EstadoJuego.Victoria
                    cJ < cE -> EstadoJuego.Derrota
                    else -> EstadoJuego.Empate
                }
                break
            }
        }
    }

    // Animación infinita: el río brilla
    val brillo = rememberInfiniteTransition(label = "brillo")
    val colorRio by brillo.animateColor(
        initialValue = Color(0xFF29B6F6),
        targetValue = Color(0xFF81D4FA),
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "colorRio"
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // ---------- Barra superior: coronas y tiempo ----------
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🔵 👑 $coronasJugador", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            val colorTiempo by animateColorAsState(
                targetValue = if (dobleElixir) Color(0xFFE53935) else Color(0xFF212121),
                label = "colorTiempo"
            )
            val segundos = tiempo.toInt().coerceAtLeast(0)
            AnimatedContent(
                targetState = segundos,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "tiempo"
            ) { s ->
                Text(
                    "%d:%02d".format(s / 60, s % 60),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTiempo
                )
            }

            Text("$coronasEnemigo 👑 🔴", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // ---------- Arena ----------
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF7CB342))
                .pointerInput(Unit) {
                    detectTapGestures { posicion ->
                        desplegar(posicion.x / size.width, posicion.y / size.height)
                    }
                }
        ) {
            val ancho = maxWidth
            val alto = maxHeight

            // Mitad del jugador un poco más clara
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(alto * 0.5f)
                    .offset(y = alto * 0.5f)
                    .background(Color(0xFF8BC34A))
            )

            // Río y puentes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(alto * 0.06f)
                    .offset(y = alto * 0.47f)
                    .background(colorRio)
            )
            for (fx in listOf(0.22f, 0.78f)) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = alto * 0.08f)
                        .offset(x = ancho * fx - 22.dp, y = alto * 0.46f)
                        .background(Color(0xFF8D6E63))
                )
            }

            // Torres
            for (i in 0..2) {
                TorreVista(
                    vida = torresEnemigo[i],
                    vidaMax = if (i == 1) VIDA_REY else VIDA_PRINCESA,
                    esRey = i == 1,
                    esJugador = false,
                    modifier = Modifier.offset(
                        x = ancho * posicionXTorre(i) - 32.dp,
                        y = alto * posicionYTorre(Bando.Enemigo, i) - 30.dp
                    )
                )
                TorreVista(
                    vida = torresJugador[i],
                    vidaMax = if (i == 1) VIDA_REY else VIDA_PRINCESA,
                    esRey = i == 1,
                    esJugador = true,
                    modifier = Modifier.offset(
                        x = ancho * posicionXTorre(i) - 32.dp,
                        y = alto * posicionYTorre(Bando.Jugador, i) - 30.dp
                    )
                )
            }

            // Tropas
            tropas.forEach { t ->
                key(t.id) {
                    val fx = if (t.carril == 0) 0.22f else 0.78f
                    TropaVista(
                        tropa = t,
                        modifier = Modifier.offset(
                            x = ancho * fx - 20.dp,
                            y = alto * t.y - 20.dp
                        )
                    )
                }
            }

            // Mensaje de aviso (elixir insuficiente, etc.)
            AnimatedVisibility(
                visible = mensaje != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    mensaje ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            // Aviso de doble elixir
            AnimatedVisibility(
                visible = dobleElixir && estado == EstadoJuego.Jugando,
                enter = scaleIn() + fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            ) {
                Text(
                    "⚡ x2 Elixir",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF8E24AA))
                        .padding(6.dp)
                )
            }

            // Pantalla de inicio / fin de partida
            AnimatedVisibility(
                visible = estado != EstadoJuego.Jugando,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xAA000000)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = estado,
                        transitionSpec = { (fadeIn(tween(500)) + scaleIn()) togetherWith fadeOut(tween(300)) },
                        label = "pantalla"
                    ) { e ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val titulo = when (e) {
                                EstadoJuego.Inicio -> "⚔️ Arena de Torres"
                                EstadoJuego.Victoria -> "🏆 ¡Victoria!"
                                EstadoJuego.Derrota -> "💀 Derrota"
                                EstadoJuego.Empate -> "🤝 Empate"
                                EstadoJuego.Jugando -> ""
                            }
                            Text(titulo, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (e == EstadoJuego.Inicio) {
                                Text(
                                    "Elige una carta y toca tu lado de la arena",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text("Coronas: $coronasJugador - $coronasEnemigo", color = Color.White, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { reiniciar() }) {
                                Text(if (e == EstadoJuego.Inicio) "¡Jugar!" else "Jugar de nuevo")
                            }
                        }
                    }
                }
            }
        }

        // ---------- Barra de elixir ----------
        val colorElixir by animateColorAsState(
            targetValue = if (elixir >= 10f) Color(0xFFE040FB) else Color(0xFFAB47BC),
            label = "colorElixir"
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("💧 ${elixir.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF4A148C))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((elixir / 10f).coerceIn(0f, 1f))
                        .background(colorElixir)
                )
            }
        }

        // ---------- Cartas ----------
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MAZO.forEachIndexed { i, carta ->
                val seleccionada = seleccion == i
                val alcanza = elixir >= carta.costo
                val elevacion by animateDpAsState(
                    targetValue = if (seleccionada) (-12).dp else 0.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "elevacion"
                )
                val colorBorde by animateColorAsState(
                    targetValue = if (seleccionada) Color(0xFFFFD600) else Color(0xFF283593),
                    label = "colorBorde"
                )
                val opacidad by animateFloatAsState(
                    targetValue = if (alcanza) 1f else 0.45f,
                    label = "opacidad"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = elevacion)
                        .alpha(opacidad)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF3949AB))
                        .border(3.dp, colorBorde, RoundedCornerShape(10.dp))
                        .clickable { seleccion = if (seleccionada) null else i }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(carta.emoji, fontSize = 24.sp)
                    Text(carta.nombre, fontSize = 10.sp, color = Color.White, maxLines = 1)
                    Text("💧${carta.costo}", fontSize = 12.sp, color = Color(0xFFE1BEE7), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------- Torre con barra de vida animada ----------
@Composable
fun TorreVista(vida: Float, vidaMax: Float, esRey: Boolean, esJugador: Boolean, modifier: Modifier = Modifier) {
    val porcentaje by animateFloatAsState(
        targetValue = (vida / vidaMax).coerceIn(0f, 1f),
        label = "vidaTorre"
    )
    val colorVida by animateColorAsState(
        targetValue = when {
            porcentaje > 0.6f -> Color(0xFF43A047)
            porcentaje > 0.3f -> Color(0xFFFFB300)
            else -> Color(0xFFE53935)
        },
        label = "colorVidaTorre"
    )
    // El rey "respira" con una animación infinita
    val latido = rememberInfiniteTransition(label = "latido")
    val escala by latido.animateFloat(
        initialValue = 1f,
        targetValue = if (esRey) 1.07f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "escala"
    )

    Column(modifier = modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = vida > 0f,
            exit = scaleOut(tween(600)) + fadeOut(tween(600))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF424242))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(porcentaje)
                            .background(colorVida)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(if (esRey) 52.dp else 44.dp)
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (esJugador) Color(0xFF1E88E5) else Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (esRey) "👑" else "🏰", fontSize = 22.sp)
                }
            }
        }
        if (vida <= 0f) {
            Text("💥", fontSize = 26.sp)
        }
    }
}

// ---------- Tropa con aparición animada ----------
@Composable
fun TropaVista(tropa: Tropa, modifier: Modifier = Modifier) {
    val aparicion = remember { MutableTransitionState(false).apply { targetState = true } }
    val porcentaje = (tropa.vida / tropa.carta.vida).coerceIn(0f, 1f)

    AnimatedVisibility(
        visibleState = aparicion,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (tropa.bando == Bando.Jugador) Color(0xFF1565C0) else Color(0xFFC62828))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(tropa.carta.emoji, fontSize = 20.sp)
            }
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(4.dp)
                    .background(Color(0xFF424242))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(porcentaje)
                        .background(Color(0xFF76FF03))
                )
            }
        }
    }
}
