package com.example.lab13animaciones

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// =====================================================================
//  MODELO DEL JUEGO
// =====================================================================

enum class Bando { Jugador, Enemigo }

enum class EstadoJuego { Inicio, Cuenta, Jugando, Victoria, Derrota, Empate }

enum class Rareza(val claro: Color, val oscuro: Color, val borde: Color) {
    Comun(Color(0xFF90A4AE), Color(0xFF37474F), Color(0xFFCFD8DC)),
    Rara(Color(0xFFFFB74D), Color(0xFFBF360C), Color(0xFFFFE0B2)),
    Epica(Color(0xFFCE93D8), Color(0xFF4A148C), Color(0xFFE1BEE7)),
    Legendaria(Color(0xFF80DEEA), Color(0xFF1A237E), Color(0xFFFFE082))
}

enum class Tipo { Espadachin, Arqueras, Coloso, Lobos, Lancero, Meteoro }

data class CartaInfo(
    val tipo: Tipo,
    val nombre: String,
    val costo: Int,
    val rareza: Rareza,
    val vida: Float = 0f,
    val danio: Float,
    val cadencia: Float = 1f,
    val velocidad: Float = 0f,
    val alcance: Float = 0f,
    val cantidad: Int = 1,
    val soloTorres: Boolean = false,
    val esHechizo: Boolean = false,
    val escala: Float = 1f
)

val CARTAS = listOf(
    CartaInfo(Tipo.Espadachin, "Espadachín", 3, Rareza.Comun, vida = 650f, danio = 80f, cadencia = 1.1f, velocidad = 0.065f, alcance = 0.05f),
    CartaInfo(Tipo.Arqueras, "Arqueras", 3, Rareza.Comun, vida = 220f, danio = 50f, cadencia = 1.0f, velocidad = 0.065f, alcance = 0.2f, cantidad = 2, escala = 0.85f),
    CartaInfo(Tipo.Coloso, "Coloso", 5, Rareza.Epica, vida = 2400f, danio = 130f, cadencia = 1.6f, velocidad = 0.04f, alcance = 0.05f, soloTorres = true, escala = 1.45f),
    CartaInfo(Tipo.Lobos, "Lobos", 2, Rareza.Rara, vida = 160f, danio = 45f, cadencia = 0.8f, velocidad = 0.12f, alcance = 0.04f, cantidad = 3, escala = 0.75f),
    CartaInfo(Tipo.Lancero, "Lancero", 4, Rareza.Rara, vida = 800f, danio = 115f, cadencia = 1.3f, velocidad = 0.06f, alcance = 0.09f, escala = 1.05f),
    CartaInfo(Tipo.Meteoro, "Meteoro", 4, Rareza.Legendaria, danio = 380f, alcance = 0.12f, esHechizo = true)
)

const val VIDA_PRINCESA = 1400f
const val VIDA_REY = 2400f
const val DURACION_PARTIDA = 120f

// Perspectiva: la parte de arriba de la arena (lejos) se ve más angosta
const val PERSPECTIVA = 0.78f

fun escalaEn(y: Float): Float = PERSPECTIVA + (1f - PERSPECTIVA) * y.coerceIn(0f, 1f)

fun rival(b: Bando) = if (b == Bando.Jugador) Bando.Enemigo else Bando.Jugador

fun colorEquipo(b: Bando) = if (b == Bando.Jugador) Color(0xFF2979FF) else Color(0xFFE53935)

fun aclarar(c: Color, f: Float) = lerp(c, Color.White, f)

fun oscurecer(c: Color, f: Float) = lerp(c, Color.Black, f)

fun fraccion(v: Float) = v - floor(v)

fun distancia(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = (ax - bx) * 0.62f
    val dy = ay - by
    return sqrt(dx * dx + dy * dy)
}

class Unidad(val id: Int, val info: CartaInfo, val bando: Bando, var x: Float, var y: Float, var z: Float) {
    var vida = info.vida
    var vidaMostrada = info.vida
    var recarga = 0.4f
    var atacando = false
    var golpe = 0f
    var edad = 0f
    var vz = 0f
    var enSuelo = false
    var destello = 0f
    var paso = 0f
    var mirandoIzquierda = false
    var dirX = 0f
    var dirY = 0f
}

class Torre(val bando: Bando, val indice: Int) {
    val esRey = indice == 1
    val vidaMax = if (esRey) VIDA_REY else VIDA_PRINCESA
    var vida = vidaMax
    var vidaMostrada = vidaMax
    var recarga = 0f
    var activa = !esRey
    var angulo = if (bando == Bando.Jugador) -PI.toFloat() / 2f else PI.toFloat() / 2f
    var retroceso = 0f
    var destello = 0f
    val x = when (indice) {
        0 -> 0.22f
        1 -> 0.5f
        else -> 0.78f
    }
    val y = if (bando == Bando.Enemigo) {
        if (esRey) 0.09f else 0.19f
    } else {
        if (esRey) 0.93f else 0.83f
    }
    val radio = if (esRey) 0.075f else 0.06f
    val viva: Boolean get() = vida > 0f
}

enum class TipoProyectil { Flecha, Bala, Meteoro }

class Proyectil(
    val tipo: TipoProyectil,
    val bando: Bando,
    val x0: Float,
    val y0: Float,
    var tx: Float,
    var ty: Float,
    val objetivo: Unidad?,
    val torre: Torre?,
    val danio: Float,
    val duracion: Float,
    val radio: Float = 0f,
    val altura: Float = 0.05f
) {
    var t = 0f
}

enum class TipoEfecto { Chispa, Numero, Explosion, Aparicion, Quemadura, Carta }

class Efecto(
    val tipo: TipoEfecto,
    val x: Float,
    val y: Float,
    val duracion: Float,
    val texto: String = "",
    val color: Color = Color.White,
    val carta: CartaInfo? = null
) {
    var edad = 0f
}

class Particula(
    var x: Float, var y: Float, var z: Float,
    var vx: Float, var vy: Float, var vz: Float,
    val color: Color, val tamano: Float, val vidaMax: Float, val gravedad: Float = 1.2f
) {
    var edad = 0f
}

class Caido(val unidad: Unidad) {
    var edad = 0f
}

class Mundo {
    val unidades = ArrayList<Unidad>()
    val torres = listOf(
        Torre(Bando.Enemigo, 0), Torre(Bando.Enemigo, 1), Torre(Bando.Enemigo, 2),
        Torre(Bando.Jugador, 0), Torre(Bando.Jugador, 1), Torre(Bando.Jugador, 2)
    )
    val proyectiles = ArrayList<Proyectil>()
    val efectos = ArrayList<Efecto>()
    val particulas = ArrayList<Particula>()
    val caidos = ArrayList<Caido>()
    var siguienteId = 0
    var reloj = 0f
    var tiempo = DURACION_PARTIDA
    var elixirEnemigo = 5f
    var cartaEnemigo = CARTAS.random()
    var esperaEnemigo = 3f
    var temblor = false

    fun rey(b: Bando) = torres.first { it.bando == b && it.esRey }

    fun coronasDe(b: Bando): Int {
        val r = rival(b)
        return if (!rey(r).viva) 3 else torres.count { it.bando == r && !it.viva }
    }

    fun desplegar(info: CartaInfo, bando: Bando, x: Float, y: Float) {
        efectos.add(Efecto(TipoEfecto.Carta, x, y, 0.3f, carta = info, color = colorEquipo(bando)))
        if (info.esHechizo) {
            proyectiles.add(
                Proyectil(
                    TipoProyectil.Meteoro, bando, x - 0.15f, y - 0.1f, x, y,
                    null, null, info.danio, 1.0f, radio = info.alcance
                )
            )
            return
        }
        for (i in 0 until info.cantidad) {
            val separacion = (i - (info.cantidad - 1) / 2f) * 0.08f
            unidades.add(
                Unidad(siguienteId++, info, bando, (x + separacion).coerceIn(0.06f, 0.94f), y, 0.3f + i * 0.04f)
            )
        }
    }

    private fun polvo(x: Float, y: Float, cantidad: Int, color: Color, fuerza: Float = 0.15f) {
        repeat(cantidad) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val v = fuerza * (0.4f + Random.nextFloat() * 0.6f)
            particulas.add(
                Particula(
                    x, y, 0.005f,
                    cos(ang) * v, sin(ang) * v * 0.6f, 0.1f + Random.nextFloat() * 0.25f,
                    color, 0.006f + Random.nextFloat() * 0.006f, 0.6f + Random.nextFloat() * 0.4f
                )
            )
        }
    }

    private fun herir(u: Unidad, danio: Float) {
        u.vida -= danio
        u.destello = 1f
        efectos.add(Efecto(TipoEfecto.Numero, u.x, u.y, 0.8f, "-${danio.toInt()}"))
    }

    private fun herirTorre(t: Torre, danio: Float) {
        if (!t.viva) return
        t.vida = (t.vida - danio).coerceAtLeast(0f)
        t.destello = 1f
        if (t.esRey) t.activa = true
        efectos.add(Efecto(TipoEfecto.Numero, t.x, t.y - 0.06f, 0.8f, "-${danio.toInt()}", Color(0xFFFFEB3B)))
        if (!t.viva) {
            temblor = true
            efectos.add(Efecto(TipoEfecto.Explosion, t.x, t.y, 1.0f))
            efectos.add(Efecto(TipoEfecto.Quemadura, t.x, t.y, 6f))
            // Escombros que salen volando
            repeat(18) {
                val ang = Random.nextFloat() * 2f * PI.toFloat()
                val v = 0.12f + Random.nextFloat() * 0.25f
                val gris = 0.35f + Random.nextFloat() * 0.35f
                particulas.add(
                    Particula(
                        t.x, t.y, 0.05f,
                        cos(ang) * v, sin(ang) * v * 0.6f, 0.4f + Random.nextFloat() * 0.5f,
                        Color(gris, gris, gris), 0.009f + Random.nextFloat() * 0.01f, 1.6f, gravedad = 2f
                    )
                )
            }
            polvo(t.x, t.y, 14, Color(0xFFBCAAA4), 0.25f)
            rey(t.bando).activa = true
        }
    }

    fun actualizar(dt: Float, multiplicadorElixir: Float) {
        reloj += dt
        tiempo -= dt
        jugarEnemigo(dt, multiplicadorElixir)

        for (u in unidades) actualizarUnidad(u, dt)
        for (t in torres) actualizarTorre(t, dt)

        val iterador = proyectiles.iterator()
        while (iterador.hasNext()) {
            val p = iterador.next()
            val obj = p.objetivo
            if (obj != null && obj.vida > 0f) {
                p.tx = obj.x
                p.ty = obj.y
            }
            p.t += dt / p.duracion
            if (p.t >= 1f) {
                impacto(p)
                iterador.remove()
            }
        }

        val iteradorU = unidades.iterator()
        while (iteradorU.hasNext()) {
            val u = iteradorU.next()
            if (u.vida <= 0f) {
                caidos.add(Caido(u))
                iteradorU.remove()
            }
        }
        animarEfectos(dt)
    }

    fun animarEfectos(dt: Float) {
        for (e in efectos) e.edad += dt
        efectos.removeAll { it.edad >= it.duracion }
        for (c in caidos) c.edad += dt
        caidos.removeAll { it.edad >= 0.7f }
        for (p in particulas) {
            p.edad += dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.vz -= p.gravedad * dt
            if (p.z < 0f) {
                p.z = 0f
                p.vz = -p.vz * 0.35f
                p.vx *= 0.6f
                p.vy *= 0.6f
            }
        }
        particulas.removeAll { it.edad >= it.vidaMax }
        for (u in unidades) {
            u.vidaMostrada += (u.vida - u.vidaMostrada) * min(1f, dt * 8f)
            u.destello = max(0f, u.destello - dt * 5f)
        }
        for (t in torres) {
            t.vidaMostrada += (t.vida - t.vidaMostrada) * min(1f, dt * 6f)
            t.destello = max(0f, t.destello - dt * 4f)
            t.retroceso = max(0f, t.retroceso - dt * 4f)
        }
    }

    private fun jugarEnemigo(dt: Float, multiplicadorElixir: Float) {
        elixirEnemigo = min(10f, elixirEnemigo + dt / 2.8f * multiplicadorElixir)
        esperaEnemigo -= dt
        if (esperaEnemigo > 0f || elixirEnemigo < cartaEnemigo.costo) return

        val carta = cartaEnemigo
        if (carta.esHechizo) {
            val objetivo = unidades.filter { it.bando == Bando.Jugador }.maxByOrNull { it.info.costo }
            if (objetivo == null) {
                cartaEnemigo = CARTAS.filter { !it.esHechizo }.random()
                return
            }
            desplegar(carta, Bando.Enemigo, objetivo.x, objetivo.y)
        } else {
            val invasor = unidades.firstOrNull { it.bando == Bando.Jugador && it.y < 0.5f }
            val x = invasor?.x ?: if (Random.nextBoolean()) 0.22f else 0.78f
            val y = if (invasor != null) 0.25f else 0.28f + Random.nextFloat() * 0.12f
            desplegar(carta, Bando.Enemigo, x, y)
        }
        elixirEnemigo -= carta.costo
        cartaEnemigo = CARTAS.random()
        esperaEnemigo = 1.5f + Random.nextFloat() * 2.5f
    }

    private fun torreObjetivo(u: Unidad): Torre? {
        val r = rival(u.bando)
        val indicePrincesa = if (u.x < 0.5f) 0 else 2
        val princesa = torres.first { it.bando == r && it.indice == indicePrincesa }
        if (princesa.viva) return princesa
        val rey = rey(r)
        return if (rey.viva) rey else null
    }

    private fun actualizarUnidad(u: Unidad, dt: Float) {
        u.edad += dt
        u.golpe = max(0f, u.golpe - dt * 3f)

        // Cae del cielo con un pequeño rebote
        if (!u.enSuelo) {
            u.vz -= 1.8f * dt
            u.z += u.vz * dt
            if (u.z <= 0f) {
                u.z = 0f
                if (u.vz < -0.25f) {
                    u.vz = -u.vz * 0.3f
                } else {
                    u.vz = 0f
                    u.enSuelo = true
                    efectos.add(Efecto(TipoEfecto.Aparicion, u.x, u.y, 0.5f, color = colorEquipo(u.bando)))
                    polvo(u.x, u.y, 6, Color(0xFFD7CCC8))
                }
            }
            return
        }

        u.recarga -= dt
        val r = rival(u.bando)

        var objetivoU: Unidad? = null
        if (!u.info.soloTorres) {
            objetivoU = unidades
                .filter { it.bando == r && it.enSuelo && it.vida > 0f && distancia(u.x, u.y, it.x, it.y) < 0.24f }
                .minByOrNull { distancia(u.x, u.y, it.x, it.y) }
        }
        val objetivoT: Torre? = if (objetivoU == null) torreObjetivo(u) else null
        val ox = objetivoU?.x ?: objetivoT?.x ?: return
        val oy = objetivoU?.y ?: objetivoT?.y ?: return
        val radioObjetivo = if (objetivoU != null) 0.025f else (objetivoT?.radio ?: 0.05f)

        if (distancia(u.x, u.y, ox, oy) <= u.info.alcance + radioObjetivo) {
            u.atacando = true
            u.mirandoIzquierda = ox < u.x - 0.01f
            val largoDir = sqrt((ox - u.x) * (ox - u.x) + (oy - u.y) * (oy - u.y)).coerceAtLeast(0.0001f)
            u.dirX = (ox - u.x) / largoDir
            u.dirY = (oy - u.y) / largoDir
            if (u.recarga <= 0f) {
                u.recarga = u.info.cadencia
                u.golpe = 1f
                if (u.info.alcance >= 0.12f) {
                    proyectiles.add(
                        Proyectil(TipoProyectil.Flecha, u.bando, u.x, u.y, ox, oy, objetivoU, objetivoT, u.info.danio, 0.4f, altura = 0.05f)
                    )
                } else {
                    if (objetivoU != null) herir(objetivoU, u.info.danio)
                    else objetivoT?.let { herirTorre(it, u.info.danio) }
                    efectos.add(Efecto(TipoEfecto.Chispa, (u.x + ox) / 2f, (u.y + oy) / 2f, 0.3f))
                }
            }
        } else {
            u.atacando = false
            var destinoX = ox
            var destinoY = oy
            val enMiLado = u.y > 0.5f
            val objetivoEnMiLado = oy > 0.5f
            if (enMiLado != objetivoEnMiLado) {
                val puenteX = if (u.x < 0.5f) 0.22f else 0.78f
                destinoX = puenteX
                destinoY = if (abs(u.x - puenteX) > 0.02f) {
                    if (enMiLado) 0.58f else 0.42f
                } else {
                    if (enMiLado) 0.40f else 0.60f
                }
            }
            val vx = destinoX - u.x
            val vy = destinoY - u.y
            val largo = sqrt(vx * vx + vy * vy)
            if (largo > 0.001f) {
                u.x += vx / largo * u.info.velocidad * dt
                u.y += vy / largo * u.info.velocidad * dt
                u.paso += dt * 14f
                if (abs(vx) > 0.01f) u.mirandoIzquierda = vx < 0f
            }
        }
    }

    private fun actualizarTorre(t: Torre, dt: Float) {
        if (!t.viva || !t.activa) return
        t.recarga -= dt
        val rango = if (t.esRey) 0.27f else 0.3f
        val objetivo = unidades
            .filter { it.bando != t.bando && it.enSuelo && it.vida > 0f && distancia(t.x, t.y, it.x, it.y) < rango }
            .minByOrNull { distancia(t.x, t.y, it.x, it.y) } ?: return
        t.angulo = atan2(objetivo.y - t.y, (objetivo.x - t.x) * 0.62f)
        if (t.recarga <= 0f) {
            t.recarga = if (t.esRey) 1f else 0.8f
            t.retroceso = 1f
            proyectiles.add(
                Proyectil(
                    if (t.esRey) TipoProyectil.Bala else TipoProyectil.Flecha,
                    t.bando, t.x, t.y - 0.02f, objetivo.x, objetivo.y,
                    objetivo, null, if (t.esRey) 90f else 70f, 0.35f,
                    altura = if (t.esRey) 0.04f else 0.07f
                )
            )
        }
    }

    private fun impacto(p: Proyectil) {
        if (p.tipo == TipoProyectil.Meteoro) {
            val r = rival(p.bando)
            unidades.filter { it.bando == r && distancia(p.tx, p.ty, it.x, it.y) <= p.radio }
                .forEach { herir(it, p.danio) }
            torres.filter { it.bando == r && it.viva && distancia(p.tx, p.ty, it.x, it.y) <= p.radio + it.radio }
                .forEach { herirTorre(it, p.danio * 0.35f) }
            efectos.add(Efecto(TipoEfecto.Explosion, p.tx, p.ty, 0.8f))
            efectos.add(Efecto(TipoEfecto.Quemadura, p.tx, p.ty, 4f))
            polvo(p.tx, p.ty, 16, Color(0xFFFF8A65), 0.3f)
            temblor = true
        } else {
            val obj = p.objetivo
            if (obj != null) {
                if (obj.vida > 0f) herir(obj, p.danio)
            } else {
                p.torre?.let { herirTorre(it, p.danio) }
            }
            if (p.tipo == TipoProyectil.Bala) polvo(p.tx, p.ty, 4, Color(0xFF9E9E9E), 0.1f)
        }
    }
}

// =====================================================================
//  DIBUJO 3D CON CANVAS (perspectiva, sombras y volumen)
// =====================================================================

// Convierte una posición de la arena (x, y) y una altura z a un punto de la pantalla
fun DrawScope.punto(x: Float, y: Float, z: Float = 0f): Offset {
    val e = escalaEn(y)
    return Offset((0.5f + (x - 0.5f) * e) * size.width, y * size.height - z * size.height)
}

fun DrawScope.cuadro(x1: Float, y1: Float, x2: Float, y2: Float, z: Float = 0f): Path {
    val a = punto(x1, y1, z)
    val b = punto(x2, y1, z)
    val c = punto(x2, y2, z)
    val d = punto(x1, y2, z)
    return Path().apply {
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        lineTo(d.x, d.y)
        close()
    }
}

// Círculo con luz arriba a la izquierda: parece una esfera
fun DrawScope.esfera(color: Color, radio: Float, centro: Offset) {
    drawCircle(
        Brush.radialGradient(
            listOf(aclarar(color, 0.5f), color, oscurecer(color, 0.5f)),
            center = Offset(centro.x - radio * 0.35f, centro.y - radio * 0.4f),
            radius = radio * 1.5f
        ),
        radio,
        centro
    )
}

fun DrawScope.ovalo3d(color: Color, esquina: Offset, tam: Size) {
    drawOval(
        Brush.radialGradient(
            listOf(aclarar(color, 0.45f), color, oscurecer(color, 0.5f)),
            center = Offset(esquina.x + tam.width * 0.35f, esquina.y + tam.height * 0.3f),
            radius = max(tam.width, tam.height)
        ),
        esquina,
        tam
    )
}

fun DrawScope.piernas(cx: Float, y: Float, r: Float, paso: Float, color: Color, abertura: Float = 0.28f, grosor: Float = 0.26f) {
    val l1 = max(0f, sin(paso)) * r * 0.22f
    val l2 = max(0f, -sin(paso)) * r * 0.22f
    drawLine(color, Offset(cx - r * abertura, y + r * 0.45f), Offset(cx - r * (abertura + 0.03f), y + r * 0.95f - l1), strokeWidth = r * grosor, cap = StrokeCap.Round)
    drawLine(color, Offset(cx + r * abertura, y + r * 0.45f), Offset(cx + r * (abertura + 0.03f), y + r * 0.95f - l2), strokeWidth = r * grosor, cap = StrokeCap.Round)
}

fun DrawScope.dibujarArbol(x: Float, y: Float, reloj: Float) {
    val e = escalaEn(y)
    val b = punto(x, y)
    val r = size.width * 0.055f * e
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(b.x - r, b.y - r * 0.25f), Size(r * 2f, r * 0.5f))
    drawRect(
        Brush.horizontalGradient(listOf(Color(0xFF3E2723), Color(0xFF795548), Color(0xFF3E2723)), startX = b.x - r * 0.15f, endX = b.x + r * 0.15f),
        Offset(b.x - r * 0.15f, b.y - r * 0.8f),
        Size(r * 0.3f, r * 0.8f)
    )
    val vaiven = sin(reloj * 1.5f + x * 7f + y * 13f) * r * 0.08f
    for (k in 0..2) {
        val baseY = b.y - r * 0.6f - k * r * 0.55f
        val ancho = r * (1.1f - k * 0.25f)
        val copa = Path().apply {
            moveTo(b.x - ancho, baseY)
            lineTo(b.x + vaiven * (k + 1), baseY - r * 0.95f)
            lineTo(b.x + ancho, baseY)
            close()
        }
        drawPath(
            copa,
            Brush.horizontalGradient(
                listOf(Color(0xFF1B5E20), Color(0xFF4CAF50), Color(0xFF1B5E20)),
                startX = b.x - ancho,
                endX = b.x + ancho
            )
        )
    }
}

fun DrawScope.dibujarFondo(reloj: Float, mostrarZona: Boolean) {
    val w = size.width
    val h = size.height

    // Bosque alrededor de la arena
    drawRect(Brush.verticalGradient(listOf(Color(0xFF14301A), Color(0xFF2E5D2E), Color(0xFF1B3A1B))))
    for (i in 0 until 7) {
        val yy = 0.02f + i * 0.075f
        dibujarArbol(-0.08f, yy, reloj)
        dibujarArbol(1.08f, yy, reloj)
    }

    // Borde de piedra del tablero (da grosor)
    val tablero = cuadro(0f, 0f, 1f, 1f)
    drawPath(tablero, Color(0xFF5D4037), style = Stroke(width = 10.dp.toPx()))
    drawPath(tablero, Color(0xFF8D6E63), style = Stroke(width = 4.dp.toPx()))
    drawPath(tablero, Color(0xFF66BB45))

    // Pasto en cuadros (con perspectiva)
    val columnas = 9
    val filas = 16
    for (f in 0 until filas) {
        for (c in 0 until columnas) {
            if ((f + c) % 2 == 0) continue
            drawPath(
                cuadro(c / columnas.toFloat(), f / filas.toFloat(), (c + 1) / columnas.toFloat(), (f + 1) / filas.toFloat()),
                Color(0xFF5BAF3D)
            )
        }
    }

    // Caminos de tierra
    val tierra = Color(0xFFD7B57A).copy(alpha = 0.65f)
    for (cx in listOf(0.22f, 0.78f)) drawPath(cuadro(cx - 0.06f, 0.2f, cx + 0.06f, 0.82f), tierra)
    drawPath(cuadro(0.22f, 0.165f, 0.78f, 0.215f), tierra)
    drawPath(cuadro(0.22f, 0.805f, 0.78f, 0.855f), tierra)

    // Matas de pasto que se mueven con el viento
    for (i in 0 until 50) {
        val gx = fraccion(i * 0.6180339f)
        val gy = fraccion(i * 0.3819660f + 0.13f)
        if (gy > 0.44f && gy < 0.56f) continue
        val base = punto(gx, gy)
        val largo = w * 0.013f * escalaEn(gy)
        val viento = sin(reloj * 2f + i) * largo * 0.3f
        val verde = Color(0xFF3E8E2A)
        drawLine(verde, base, Offset(base.x - largo * 0.4f + viento, base.y - largo), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(verde, base, Offset(base.x + viento, base.y - largo * 1.3f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(verde, base, Offset(base.x + largo * 0.4f + viento, base.y - largo), strokeWidth = 1.5f, cap = StrokeCap.Round)
    }

    // Río hundido: pared del fondo, agua con degradado y orilla
    drawPath(cuadro(0f, 0.462f, 1f, 0.48f), Color(0xFF5D4037))
    drawPath(
        cuadro(0f, 0.48f, 1f, 0.535f),
        Brush.verticalGradient(listOf(Color(0xFF0D47A1), Color(0xFF29B6F6)), startY = 0.48f * h, endY = 0.535f * h)
    )
    drawPath(cuadro(0f, 0.535f, 1f, 0.543f), Color(0xFF8D6E63))
    for (k in 0..2) {
        val ola = Path()
        val yo = 0.488f + k * 0.015f
        var x = 0f
        val inicio = punto(0f, yo)
        ola.moveTo(inicio.x, inicio.y)
        while (x <= 1f) {
            val p = punto(x, yo)
            ola.lineTo(p.x, p.y + sin(x * 14f + reloj * 3f + k) * h * 0.003f)
            x += 0.025f
        }
        drawPath(ola, Color.White.copy(alpha = 0.3f), style = Stroke(width = 1.5.dp.toPx()))
    }
    for (i in 0 until 14) {
        val bx = fraccion(i * 0.37f + reloj * 0.03f)
        val by = 0.485f + fraccion(i * 0.53f) * 0.045f
        val brillo = (sin(reloj * 4f + i * 1.3f) + 1f) / 2f
        drawCircle(Color.White.copy(alpha = brillo * 0.8f), 1.5.dp.toPx(), punto(bx, by))
    }

    // Puentes de madera en relieve
    for (bx in listOf(0.22f, 0.78f)) {
        val x1 = bx - 0.075f
        val x2 = bx + 0.075f
        val y1 = 0.455f
        val y2 = 0.55f
        val alto = 0.012f
        val frente = Path().apply {
            val a = punto(x1, y2, alto)
            val b = punto(x2, y2, alto)
            val c = punto(x2, y2, 0f)
            val d = punto(x1, y2, 0f)
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            lineTo(d.x, d.y)
            close()
        }
        drawPath(frente, Color(0xFF4E2C17))
        drawPath(cuadro(x1, y1, x2, y2, alto), Color(0xFF9C6B47))
        for (k in 1 until 7) {
            val yy = y1 + (y2 - y1) * k / 7f
            drawLine(Color(0xFF6D4429), punto(x1, yy, alto), punto(x2, yy, alto), strokeWidth = 1.5.dp.toPx())
        }
        for (lx in listOf(x1, x2)) {
            for (yy in listOf(y1, (y1 + y2) / 2f, y2)) {
                drawLine(Color(0xFF3E2316), punto(lx, yy, alto), punto(lx, yy, alto + 0.014f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            drawLine(Color(0xFF5D3A22), punto(lx, y1, alto + 0.014f), punto(lx, y2, alto + 0.014f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
    }

    // Zona de despliegue
    if (mostrarZona) {
        val alfa = 0.12f + 0.08f * sin(reloj * 5f)
        drawPath(cuadro(0f, 0.55f, 1f, 1f), Color.White.copy(alpha = alfa))
        drawLine(
            Color.White.copy(alpha = 0.7f),
            punto(0f, 0.55f),
            punto(1f, 0.55f),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), reloj * 30f)
        )
    }
}

fun DrawScope.dibujarNubes(reloj: Float) {
    val w = size.width
    val h = size.height
    for (i in 0 until 3) {
        val nx = fraccion(reloj * 0.012f + i * 0.4f) * 1.6f - 0.3f
        val ny = 0.15f + i * 0.3f
        val c = punto(nx, ny)
        val e = escalaEn(ny)
        drawOval(Color.Black.copy(alpha = 0.09f), Offset(c.x - w * 0.18f * e, c.y - h * 0.03f), Size(w * 0.36f * e, h * 0.06f))
        drawOval(Color.Black.copy(alpha = 0.07f), Offset(c.x - w * 0.05f * e, c.y - h * 0.05f), Size(w * 0.22f * e, h * 0.05f))
    }
    // Niebla en el fondo para dar profundidad
    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent), startY = 0f, endY = h * 0.22f))
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.18f)), startY = h * 0.85f, endY = h))
}

fun DrawScope.barraVida(centroX: Float, y: Float, ancho: Float, porcentaje: Float, color: Color, texto: String?, medidor: TextMeasurer) {
    val alto = 7.dp.toPx()
    drawRoundRect(Color(0xCC000000), Offset(centroX - ancho / 2f - 2f, y - 2f), Size(ancho + 4f, alto + 4f), CornerRadius(4.dp.toPx()))
    drawRoundRect(
        Brush.verticalGradient(listOf(aclarar(color, 0.35f), color, oscurecer(color, 0.25f)), startY = y, endY = y + alto),
        Offset(centroX - ancho / 2f, y),
        Size(ancho * porcentaje.coerceIn(0f, 1f), alto),
        CornerRadius(3.dp.toPx())
    )
    if (texto != null) {
        val t = medidor.measure(
            texto,
            TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, shadow = Shadow(Color.Black, Offset(1f, 1f), 2f))
        )
        drawText(t, topLeft = Offset(centroX - t.size.width / 2f, y - t.size.height))
    }
}

fun DrawScope.dibujarTorre(t: Torre, reloj: Float, medidor: TextMeasurer) {
    val w = size.width
    val h = size.height
    val e = escalaEn(t.y)
    val temblorX = sin(reloj * 90f) * t.destello * w * 0.004f
    val base = punto(t.x, t.y) + Offset(temblorX, 0f)
    val radio = w * (if (t.esRey) 0.09f else 0.07f) * e
    val alto = h * (if (t.esRey) 0.07f else 0.085f) * e
    val colorE = colorEquipo(t.bando)

    if (!t.viva) {
        // Ruinas: un muñón de la torre y piedras
        drawOval(Color.Black.copy(alpha = 0.3f), Offset(base.x - radio * 1.2f, base.y - radio * 0.3f), Size(radio * 2.4f, radio * 0.8f))
        val tocon = alto * 0.25f
        drawRect(
            Brush.horizontalGradient(listOf(Color(0xFF424242), Color(0xFF9E9E9E), Color(0xFF424242)), startX = base.x - radio, endX = base.x + radio),
            Offset(base.x - radio, base.y - tocon),
            Size(radio * 2f, tocon)
        )
        drawOval(Color(0xFF757575), Offset(base.x - radio, base.y - tocon - radio * 0.3f), Size(radio * 2f, radio * 0.6f))
        esfera(Color(0xFF9E9E9E), radio * 0.3f, Offset(base.x - radio * 1.1f, base.y + radio * 0.1f))
        esfera(Color(0xFF8D8D8D), radio * 0.24f, Offset(base.x + radio * 1.15f, base.y))
        esfera(Color(0xFFA5A5A5), radio * 0.2f, Offset(base.x + radio * 0.4f, base.y + radio * 0.3f))
        return
    }

    val tope = base.y - alto
    val piedra = Brush.horizontalGradient(
        listOf(Color(0xFF5A5A5A), Color(0xFFC8C8C8), Color(0xFF9E9E9E), Color(0xFF3F3F3F)),
        startX = base.x - radio,
        endX = base.x + radio
    )

    // Sombra, cuerpo cilíndrico y base curva
    drawOval(Color.Black.copy(alpha = 0.32f), Offset(base.x - radio * 1.4f, base.y - radio * 0.3f), Size(radio * 2.9f, radio * 0.9f))
    drawRect(piedra, Offset(base.x - radio, tope), Size(radio * 2f, alto))
    drawOval(piedra, Offset(base.x - radio, base.y - radio * 0.35f), Size(radio * 2f, radio * 0.7f))
    for (k in 1..3) {
        val yy = tope + alto * k / 4f
        drawArc(Color(0x40000000), 0f, 180f, false, Offset(base.x - radio, yy - radio * 0.35f), Size(radio * 2f, radio * 0.7f), style = Stroke(width = 1.dp.toPx()))
    }
    // Franja del equipo
    drawRect(
        Brush.horizontalGradient(listOf(oscurecer(colorE, 0.4f), aclarar(colorE, 0.2f), oscurecer(colorE, 0.5f)), startX = base.x - radio, endX = base.x + radio),
        Offset(base.x - radio, tope + alto * 0.3f),
        Size(radio * 2f, alto * 0.16f)
    )
    // Puerta
    drawRoundRect(Color(0xFF3E2723), Offset(base.x - radio * 0.25f, base.y - alto * 0.38f), Size(radio * 0.5f, alto * 0.38f + radio * 0.3f), CornerRadius(radio * 0.25f))
    if (t.destello > 0f) {
        drawRect(Color.White.copy(alpha = t.destello * 0.45f), Offset(base.x - radio, tope), Size(radio * 2f, alto))
    }

    // Plataforma superior con almenas (las de atrás primero, luego las de adelante)
    val rx = radio * 1.15f
    val ry = radio * 0.42f
    fun almena(angulo: Float) {
        val px = base.x + cos(angulo) * rx * 0.88f
        val py = tope + sin(angulo) * ry * 0.88f
        val aw = radio * 0.36f
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFFD6D6D6), Color(0xFF7A7A7A)), startY = py - aw, endY = py),
            Offset(px - aw / 2f, py - aw),
            Size(aw, aw)
        )
    }
    for (a in listOf(200f, 240f, 270f, 300f, 340f)) almena(a * PI.toFloat() / 180f)
    drawOval(
        Brush.radialGradient(listOf(Color(0xFFE6E6E6), Color(0xFF9A9A9A)), center = Offset(base.x - rx * 0.3f, tope - ry * 0.3f), radius = rx * 1.3f),
        Offset(base.x - rx, tope - ry),
        Size(rx * 2f, ry * 2f)
    )

    // Defensor en la torre (apunta al objetivo y retrocede al disparar)
    val cabeza = Offset(base.x, tope - radio * 0.55f)
    val dx = cos(t.angulo)
    val dy = sin(t.angulo) * 0.6f
    val largoArma = radio * (if (t.esRey) 1.0f else 0.85f) * (1f - t.retroceso * 0.25f)
    if (t.esRey) {
        // Cañón
        drawLine(Color(0xFF263238), cabeza, Offset(cabeza.x + dx * largoArma, cabeza.y + dy * largoArma), strokeWidth = radio * 0.32f, cap = StrokeCap.Round)
        if (t.retroceso > 0.6f) {
            drawCircle(Color(0xFFFFEB3B).copy(alpha = t.retroceso), radio * 0.25f, Offset(cabeza.x + dx * largoArma * 1.15f, cabeza.y + dy * largoArma * 1.15f))
        }
        esfera(colorE, radio * 0.42f, Offset(cabeza.x, cabeza.y + radio * 0.25f))
        esfera(Color(0xFFFFCC80), radio * 0.32f, cabeza)
        val corona = Path().apply {
            val cy = cabeza.y - radio * 0.22f
            moveTo(cabeza.x - radio * 0.3f, cy)
            lineTo(cabeza.x - radio * 0.3f, cy - radio * 0.3f)
            lineTo(cabeza.x - radio * 0.15f, cy - radio * 0.15f)
            lineTo(cabeza.x, cy - radio * 0.38f)
            lineTo(cabeza.x + radio * 0.15f, cy - radio * 0.15f)
            lineTo(cabeza.x + radio * 0.3f, cy - radio * 0.3f)
            lineTo(cabeza.x + radio * 0.3f, cy)
            close()
        }
        drawPath(
            corona,
            Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFB300)), startY = cabeza.y - radio * 0.6f, endY = cabeza.y - radio * 0.2f)
        )
        if (!t.activa) {
            val zzz = medidor.measure("z Z", TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            drawText(zzz, topLeft = Offset(cabeza.x + radio * 0.4f, cabeza.y - radio * 1.1f - sin(reloj * 2f) * 4f))
        }
    } else {
        // Ballesta y bandera
        val mastil = Offset(base.x + rx * 0.75f, tope)
        val topeMastil = mastil.y - radio * 1.5f
        drawLine(Color(0xFF4E342E), mastil, Offset(mastil.x, topeMastil), strokeWidth = 2.dp.toPx())
        val onda = sin(reloj * 6f + t.x * 10f) * radio * 0.08f
        val bandera = Path().apply {
            moveTo(mastil.x, topeMastil)
            lineTo(mastil.x + radio * 0.55f, topeMastil + radio * 0.08f + onda)
            lineTo(mastil.x + radio * 0.52f, topeMastil + radio * 0.36f + onda)
            lineTo(mastil.x, topeMastil + radio * 0.3f)
            close()
        }
        drawPath(bandera, Brush.horizontalGradient(listOf(colorE, aclarar(colorE, 0.3f), oscurecer(colorE, 0.2f)), startX = mastil.x, endX = mastil.x + radio * 0.55f))
        esfera(colorE, radio * 0.34f, Offset(cabeza.x, cabeza.y + radio * 0.22f))
        esfera(Color(0xFFFFCC80), radio * 0.26f, cabeza)
        drawArc(Color(0xFF8D6E63), 180f, 180f, true, Offset(cabeza.x - radio * 0.28f, cabeza.y - radio * 0.3f), Size(radio * 0.56f, radio * 0.5f))
        val punta = Offset(cabeza.x + dx * largoArma, cabeza.y + radio * 0.15f + dy * largoArma)
        drawLine(Color(0xFF5D4037), Offset(cabeza.x, cabeza.y + radio * 0.15f), punta, strokeWidth = radio * 0.14f, cap = StrokeCap.Round)
        drawLine(Color(0xFF795548), Offset(punta.x - dy * radio * 0.35f, punta.y + dx * radio * 0.35f), Offset(punta.x + dy * radio * 0.35f, punta.y - dx * radio * 0.35f), strokeWidth = radio * 0.1f, cap = StrokeCap.Round)
    }

    val porcentaje = t.vidaMostrada / t.vidaMax
    val colorVida = if (t.bando == Bando.Jugador) Color(0xFF42A5F5) else Color(0xFFEF5350)
    barraVida(base.x, tope - radio * 1.9f, radio * 2.2f, porcentaje, colorVida, t.vida.toInt().toString(), medidor)
}

// Personajes con volumen (se usan en la arena y en las cartas)
fun DrawScope.dibujarFigura(tipo: Tipo, colorE: Color, cx: Float, cy: Float, r: Float, fase: Float, golpe: Float, paso: Float) {
    val piel = Color(0xFFFFCC80)
    val y = cy - abs(sin(paso)) * r * 0.1f + sin(fase) * r * 0.04f

    when (tipo) {
        Tipo.Espadachin -> {
            piernas(cx, y, r, paso, Color(0xFF37474F))
            val manoX = cx + r * 0.65f
            val manoY = y + r * 0.1f
            rotate(degrees = -30f + golpe * 100f, pivot = Offset(manoX, manoY)) {
                drawLine(
                    Brush.horizontalGradient(listOf(Color(0xFF9E9E9E), Color.White, Color(0xFF9E9E9E)), startX = manoX - r * 0.1f, endX = manoX + r * 0.1f),
                    Offset(manoX, manoY), Offset(manoX, manoY - r * 1.35f),
                    strokeWidth = r * 0.18f, cap = StrokeCap.Round
                )
                drawLine(Color(0xFFFFB300), Offset(manoX - r * 0.25f, manoY - r * 0.15f), Offset(manoX + r * 0.25f, manoY - r * 0.15f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
            }
            esfera(colorE, r * 0.72f, Offset(cx, y + r * 0.12f))
            drawRect(Color(0xFF5D4037), Offset(cx - r * 0.6f, y + r * 0.25f), Size(r * 1.2f, r * 0.12f))
            esfera(piel, r * 0.42f, Offset(cx, y - r * 0.5f))
            drawArc(
                Brush.verticalGradient(listOf(Color.White, Color(0xFF78909C)), startY = y - r * 1.0f, endY = y - r * 0.5f),
                180f, 180f, true, Offset(cx - r * 0.48f, y - r * 0.98f), Size(r * 0.96f, r * 0.96f)
            )
            drawLine(Color(0xFF263238), Offset(cx - r * 0.25f, y - r * 0.52f), Offset(cx + r * 0.25f, y - r * 0.52f), strokeWidth = r * 0.07f)
            esfera(colorE, r * 0.15f, Offset(cx, y - r * 1.02f))
            esfera(Color(0xFF8D6E63), r * 0.34f, Offset(cx - r * 0.72f, y + r * 0.15f))
            esfera(colorE, r * 0.2f, Offset(cx - r * 0.72f, y + r * 0.15f))
        }

        Tipo.Arqueras -> {
            piernas(cx, y, r, paso, Color(0xFF4E342E), abertura = 0.22f)
            drawRect(Color(0xFF6D4C41), Offset(cx - r * 0.65f, y - r * 0.45f), Size(r * 0.22f, r * 0.75f))
            for (k in 0..2) {
                drawLine(Color.White, Offset(cx - r * 0.6f + k * r * 0.06f, y - r * 0.45f), Offset(cx - r * 0.6f + k * r * 0.06f, y - r * 0.65f), strokeWidth = r * 0.05f)
            }
            esfera(colorE, r * 0.6f, Offset(cx, y + r * 0.15f))
            esfera(piel, r * 0.42f, Offset(cx, y - r * 0.45f))
            drawArc(
                Brush.verticalGradient(listOf(Color(0xFFF06292), Color(0xFF880E4F)), startY = y - r * 0.95f, endY = y - r * 0.3f),
                160f, 220f, true, Offset(cx - r * 0.46f, y - r * 0.92f), Size(r * 0.92f, r * 0.92f)
            )
            esfera(Color(0xFFC2185B), r * 0.2f, Offset(cx - r * 0.45f, y - r * 0.15f))
            val arcoX = cx + r * 0.6f
            drawArc(
                Color(0xFF6D4C41), -90f, 180f, false,
                Offset(arcoX - r * 0.3f, y - r * 0.55f), Size(r * 0.6f, r * 1.2f),
                style = Stroke(width = r * 0.12f, cap = StrokeCap.Round)
            )
            val tension = golpe * r * 0.25f
            drawLine(Color.White, Offset(arcoX, y - r * 0.55f), Offset(arcoX - tension, y + r * 0.05f), strokeWidth = r * 0.05f)
            drawLine(Color.White, Offset(arcoX - tension, y + r * 0.05f), Offset(arcoX, y + r * 0.65f), strokeWidth = r * 0.05f)
        }

        Tipo.Coloso -> {
            piernas(cx, y + r * 0.1f, r, paso, Color(0xFF6D4C41), abertura = 0.38f, grosor = 0.4f)
            val puno = golpe * r * 0.4f
            esfera(Color(0xFF8D6E63), r * 0.36f, Offset(cx - r * 0.88f, y + r * 0.1f - puno))
            esfera(Color(0xFF8D6E63), r * 0.36f, Offset(cx + r * 0.88f, y + r * 0.1f - puno))
            ovalo3d(Color(0xFF8D6E63), Offset(cx - r * 0.78f, y - r * 0.62f), Size(r * 1.56f, r * 1.45f))
            drawLine(Color(0x66000000), Offset(cx - r * 0.3f, y - r * 0.3f), Offset(cx - r * 0.1f, y), strokeWidth = r * 0.05f)
            drawLine(Color(0x66000000), Offset(cx + r * 0.35f, y - r * 0.4f), Offset(cx + r * 0.2f, y - r * 0.1f), strokeWidth = r * 0.05f)
            drawCircle(Color(0xFF7CB342), r * 0.12f, Offset(cx + r * 0.4f, y - r * 0.45f))
            drawCircle(Color(0xFF7CB342), r * 0.08f, Offset(cx - r * 0.45f, y + r * 0.2f))
            drawRect(colorE, Offset(cx - r * 0.76f, y + r * 0.12f), Size(r * 1.52f, r * 0.22f))
            esfera(Color(0xFFA1887F), r * 0.37f, Offset(cx, y - r * 0.72f))
            val brillo = 0.6f + 0.4f * sin(fase * 3f)
            drawCircle(Color(0xFFFFEB3B).copy(alpha = brillo), r * 0.08f, Offset(cx - r * 0.13f, y - r * 0.74f))
            drawCircle(Color(0xFFFFEB3B).copy(alpha = brillo), r * 0.08f, Offset(cx + r * 0.13f, y - r * 0.74f))
        }

        Tipo.Lobos -> {
            val pata = sin(paso) * r * 0.12f
            for (k in 0..3) {
                val px = cx - r * 0.4f + k * r * 0.27f
                val mov = if (k % 2 == 0) pata else -pata
                drawLine(Color(0xFF616161), Offset(px, y + r * 0.2f), Offset(px + mov, y + r * 0.55f), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
            }
            val cola = sin(paso * 1.5f) * r * 0.15f
            drawLine(Color(0xFF757575), Offset(cx + r * 0.5f, y), Offset(cx + r * 0.85f, y - r * 0.25f + cola), strokeWidth = r * 0.18f, cap = StrokeCap.Round)
            ovalo3d(Color(0xFF9E9E9E), Offset(cx - r * 0.58f, y - r * 0.22f), Size(r * 1.16f, r * 0.8f))
            val cabezaY = y - r * 0.42f
            val cabezaX = cx - r * 0.35f
            val orejas = Path().apply {
                moveTo(cabezaX - r * 0.32f, cabezaY - r * 0.1f)
                lineTo(cabezaX - r * 0.22f, cabezaY - r * 0.55f)
                lineTo(cabezaX - r * 0.02f, cabezaY - r * 0.22f)
                close()
                moveTo(cabezaX + r * 0.32f, cabezaY - r * 0.1f)
                lineTo(cabezaX + r * 0.22f, cabezaY - r * 0.55f)
                lineTo(cabezaX + r * 0.02f, cabezaY - r * 0.22f)
                close()
            }
            drawPath(orejas, Color(0xFF616161))
            esfera(Color(0xFFBDBDBD), r * 0.38f, Offset(cabezaX, cabezaY))
            esfera(Color(0xFFE0E0E0), r * 0.17f, Offset(cabezaX - r * 0.08f, cabezaY + r * 0.15f))
            drawCircle(Color(0xFF212121), r * 0.07f, Offset(cabezaX - r * 0.14f, cabezaY + r * 0.2f))
            drawCircle(Color(0xFFFFEB3B), r * 0.07f, Offset(cabezaX - r * 0.14f, cabezaY - r * 0.08f))
            drawCircle(Color(0xFFFFEB3B), r * 0.07f, Offset(cabezaX + r * 0.12f, cabezaY - r * 0.08f))
            drawRect(colorE, Offset(cabezaX - r * 0.28f, cabezaY + r * 0.3f), Size(r * 0.56f, r * 0.12f))
        }

        Tipo.Lancero -> {
            piernas(cx, y, r, paso, Color(0xFF3E2723))
            val empuje = golpe * r * 0.55f
            drawLine(
                Brush.horizontalGradient(listOf(Color(0xFF4E342E), Color(0xFFA1887F), Color(0xFF4E342E)), startX = cx + r * 0.53f, endX = cx + r * 0.67f),
                Offset(cx + r * 0.6f, y + r * 0.6f - empuje),
                Offset(cx + r * 0.6f, y - r * 1.5f - empuje),
                strokeWidth = r * 0.14f,
                cap = StrokeCap.Round
            )
            val punta = Path().apply {
                moveTo(cx + r * 0.6f, y - r * 1.9f - empuje)
                lineTo(cx + r * 0.43f, y - r * 1.45f - empuje)
                lineTo(cx + r * 0.77f, y - r * 1.45f - empuje)
                close()
            }
            drawPath(punta, Brush.horizontalGradient(listOf(Color(0xFF9E9E9E), Color.White), startX = cx + r * 0.43f, endX = cx + r * 0.77f))
            esfera(colorE, r * 0.7f, Offset(cx, y + r * 0.15f))
            esfera(piel, r * 0.42f, Offset(cx, y - r * 0.5f))
            val casco = Path().apply {
                moveTo(cx - r * 0.5f, y - r * 0.55f)
                lineTo(cx, y - r * 1.2f)
                lineTo(cx + r * 0.5f, y - r * 0.55f)
                close()
            }
            drawPath(casco, Brush.horizontalGradient(listOf(Color(0xFFFF8F00), Color(0xFFFFE082), Color(0xFFE65100)), startX = cx - r * 0.5f, endX = cx + r * 0.5f))
        }

        Tipo.Meteoro -> {
            for (i in 1..5) {
                drawCircle(
                    Color(0xFFFF7043).copy(alpha = 0.55f - i * 0.1f),
                    r * (0.75f - i * 0.1f),
                    Offset(cx - r * 0.35f * i, y - r * 0.3f * i)
                )
            }
            drawCircle(Color(0xFFFFAB00).copy(alpha = 0.35f), r * 1.1f, Offset(cx, y))
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFFFDE7), Color(0xFFFFC107), Color(0xFFE64A19), Color(0xFF6D1B00)),
                    center = Offset(cx - r * 0.2f, y - r * 0.2f),
                    radius = r * 0.95f
                ),
                r * 0.8f,
                Offset(cx, y)
            )
        }
    }
}

fun DrawScope.dibujarUnidad(u: Unidad, reloj: Float) {
    val e = escalaEn(u.y)
    val r = size.width * 0.034f * u.info.escala * e
    val suelo = punto(u.x, u.y)
    val c = punto(u.x + u.dirX * u.golpe * 0.012f, u.y + u.dirY * u.golpe * 0.012f, u.z)

    // Sombra: se achica cuando la tropa está en el aire
    val s = (1f - u.z * 3f).coerceIn(0.3f, 1f)
    drawOval(
        Color.Black.copy(alpha = 0.32f * s),
        Offset(suelo.x - r * 0.85f * s, suelo.y + r * 0.85f - r * 0.24f * s),
        Size(r * 1.7f * s, r * 0.48f * s)
    )
    // Anillo del color del equipo bajo los pies
    drawOval(
        colorEquipo(u.bando).copy(alpha = 0.5f),
        Offset(suelo.x - r * 0.7f, suelo.y + r * 0.72f),
        Size(r * 1.4f, r * 0.36f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    val aparecer = min(1f, u.edad / 0.2f)
    scale(scaleX = (if (u.mirandoIzquierda) -1f else 1f) * aparecer, scaleY = aparecer, pivot = c) {
        dibujarFigura(u.info.tipo, colorEquipo(u.bando), c.x, c.y, r, reloj * 3f + u.id, u.golpe, u.paso)
    }
    if (u.destello > 0f) {
        drawCircle(Color.White.copy(alpha = u.destello * 0.5f), r * 0.85f, c)
    }
    if (u.vidaMostrada < u.info.vida) {
        val p = (u.vidaMostrada / u.info.vida).coerceIn(0f, 1f)
        val bw = r * 2f
        val bh = 4.dp.toPx()
        val by = c.y - r * 1.75f
        drawRect(Color(0xAA000000), Offset(c.x - bw / 2f, by), Size(bw, bh))
        drawRect(if (u.bando == Bando.Jugador) Color(0xFF42A5F5) else Color(0xFFEF5350), Offset(c.x - bw / 2f, by), Size(bw * p, bh))
    }
}

fun DrawScope.dibujarCaido(caido: Caido) {
    val u = caido.unidad
    val p = (caido.edad / 0.7f).coerceIn(0f, 1f)
    val e = escalaEn(u.y)
    val r = size.width * 0.034f * u.info.escala * e
    val c = punto(u.x, u.y)
    val giro = 90f * min(1f, p * 2.5f) * (if (u.mirandoIzquierda) -1f else 1f)
    rotate(degrees = giro, pivot = Offset(c.x, c.y + r * 0.9f)) {
        scale(1f - p * 0.9f, pivot = Offset(c.x, c.y + r * 0.9f)) {
            dibujarFigura(u.info.tipo, oscurecer(colorEquipo(u.bando), 0.3f), c.x, c.y, r, 0f, 0f, 0f)
        }
    }
    // Alma que sube
    drawCircle(Color.White.copy(alpha = (1f - p) * 0.6f), r * 0.35f, Offset(c.x, c.y - p * r * 3f))
}

fun DrawScope.puntoProyectil(p: Proyectil, t: Float): Offset {
    val x = p.x0 + (p.tx - p.x0) * t
    val y = p.y0 + (p.ty - p.y0) * t
    val z = sin(t * PI.toFloat()) * p.altura + 0.02f
    return punto(x, y, z)
}

fun DrawScope.dibujarProyectil(p: Proyectil) {
    val w = size.width
    val h = size.height
    if (p.tipo == TipoProyectil.Meteoro) {
        val e = escalaEn(p.ty)
        val destino = punto(p.tx, p.ty)
        val rx = p.radio / 0.62f * w * e
        val ry = p.radio * h
        drawOval(Color(0xFFFF3D00).copy(alpha = 0.12f + 0.25f * p.t), Offset(destino.x - rx, destino.y - ry), Size(rx * 2f, ry * 2f))
        drawOval(Color(0xFFFF6E40), Offset(destino.x - rx, destino.y - ry), Size(rx * 2f, ry * 2f), style = Stroke(width = 2.dp.toPx()))
        val sx = p.x0 + (p.tx - p.x0) * p.t
        val sy = p.y0 + (p.ty - p.y0) * p.t
        val z = (1f - p.t) * 0.45f
        val sombra = punto(sx, sy)
        drawOval(Color.Black.copy(alpha = 0.3f * p.t), Offset(sombra.x - w * 0.03f * p.t, sombra.y - h * 0.008f), Size(w * 0.06f * p.t, h * 0.016f))
        for (k in 1..6) {
            val tt = p.t - k * 0.035f
            if (tt > 0f) {
                val q = punto(p.x0 + (p.tx - p.x0) * tt, p.y0 + (p.ty - p.y0) * tt, (1f - tt) * 0.45f)
                drawCircle(Color(0xFFFF9100).copy(alpha = 0.4f - k * 0.05f), w * (0.03f - k * 0.003f), q)
            }
        }
        val pos = punto(sx, sy, z)
        dibujarFigura(Tipo.Meteoro, Color.Red, pos.x, pos.y, w * 0.05f * e, 0f, 0f, 0f)
    } else {
        val a = puntoProyectil(p, p.t)
        val b = puntoProyectil(p, min(1f, p.t + 0.03f))
        var ux = b.x - a.x
        var uy = b.y - a.y
        val largo = sqrt(ux * ux + uy * uy)
        if (largo < 0.01f) {
            ux = 0f
            uy = 1f
        } else {
            ux /= largo
            uy /= largo
        }
        val suelo = punto(p.x0 + (p.tx - p.x0) * p.t, p.y0 + (p.ty - p.y0) * p.t)
        drawOval(Color.Black.copy(alpha = 0.25f), Offset(suelo.x - 3.dp.toPx(), suelo.y - 1.dp.toPx()), Size(6.dp.toPx(), 2.dp.toPx()))
        if (p.tipo == TipoProyectil.Bala) {
            esfera(Color(0xFF424242), 3.5.dp.toPx(), a)
        } else {
            val l = 11.dp.toPx()
            val cola = Offset(a.x - ux * l, a.y - uy * l)
            drawLine(Color(0xFF6D4C41), cola, a, strokeWidth = 2.dp.toPx())
            val punta = Path().apply {
                moveTo(a.x + ux * 4.dp.toPx(), a.y + uy * 4.dp.toPx())
                lineTo(a.x - uy * 2.dp.toPx(), a.y + ux * 2.dp.toPx())
                lineTo(a.x + uy * 2.dp.toPx(), a.y - ux * 2.dp.toPx())
                close()
            }
            drawPath(punta, Color(0xFFCFD8DC))
            drawLine(Color.White, cola, Offset(cola.x - uy * 3.dp.toPx(), cola.y + ux * 3.dp.toPx()), strokeWidth = 1.5.dp.toPx())
            drawLine(Color.White, cola, Offset(cola.x + uy * 3.dp.toPx(), cola.y - ux * 3.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        }
    }
}

fun DrawScope.dibujarParticula(p: Particula) {
    val a = (1f - p.edad / p.vidaMax).coerceIn(0f, 1f)
    val tam = p.tamano * size.width * escalaEn(p.y)
    if (p.z > 0.003f) drawCircle(Color.Black.copy(alpha = 0.2f * a), tam * 0.8f, punto(p.x, p.y))
    drawCircle(p.color.copy(alpha = a), tam, punto(p.x, p.y, p.z))
}

fun DrawScope.dibujarQuemadura(e: Efecto) {
    val p = (e.edad / e.duracion).coerceIn(0f, 1f)
    val c = punto(e.x, e.y)
    val esc = escalaEn(e.y)
    val rx = size.width * 0.07f * esc
    drawOval(Color.Black.copy(alpha = 0.35f * (1f - p)), Offset(c.x - rx, c.y - rx * 0.35f), Size(rx * 2f, rx * 0.7f))
}

fun DrawScope.dibujarEfecto(e: Efecto, medidor: TextMeasurer) {
    val w = size.width
    val p = (e.edad / e.duracion).coerceIn(0f, 1f)
    val esc = escalaEn(e.y)

    when (e.tipo) {
        TipoEfecto.Chispa -> {
            val c = punto(e.x, e.y, 0.025f)
            val r = w * 0.035f * esc * (0.5f + p)
            for (i in 0 until 8) {
                val ang = i * PI.toFloat() / 4f
                drawLine(
                    Color(0xFFFFF59D).copy(alpha = 1f - p),
                    Offset(c.x + cos(ang) * r * 0.4f, c.y + sin(ang) * r * 0.4f),
                    Offset(c.x + cos(ang) * r, c.y + sin(ang) * r),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(Color.White.copy(alpha = 1f - p), r * 0.3f, c)
        }

        TipoEfecto.Numero -> {
            val c = punto(e.x, e.y, 0.05f + p * 0.05f)
            val escalaTexto = if (p < 0.15f) 1f + (0.15f - p) * 4f else 1f
            val texto = medidor.measure(
                e.texto,
                TextStyle(
                    color = e.color.copy(alpha = 1f - p * p),
                    fontSize = (12f * escalaTexto).sp,
                    fontWeight = FontWeight.ExtraBold,
                    shadow = Shadow(Color.Black.copy(alpha = 1f - p), Offset(1.5f, 1.5f), 2f)
                )
            )
            drawText(texto, topLeft = Offset(c.x - texto.size.width / 2f, c.y - texto.size.height))
        }

        TipoEfecto.Explosion -> {
            val c = punto(e.x, e.y)
            val rx = w * (0.05f + p * 0.18f) * esc
            drawOval(Color.White.copy(alpha = (1f - p) * 0.7f), Offset(c.x - rx, c.y - rx * 0.35f), Size(rx * 2f, rx * 0.7f), style = Stroke(width = 4.dp.toPx() * (1f - p)))
            val bola = punto(e.x, e.y, 0.03f + p * 0.04f)
            val rb = w * (0.04f + p * 0.09f) * esc
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFFFDE7).copy(alpha = 1f - p), Color(0xFFFFC107).copy(alpha = (1f - p) * 0.9f), Color(0xFFE64A19).copy(alpha = 0f)),
                    center = bola,
                    radius = rb
                ),
                rb,
                bola
            )
            drawCircle(Color(0xFF616161).copy(alpha = p * (1f - p) * 1.6f), rb * 0.8f, punto(e.x, e.y, 0.06f + p * 0.08f))
        }

        TipoEfecto.Aparicion -> {
            val c = punto(e.x, e.y)
            val rx = w * (0.03f + p * 0.07f) * esc
            drawOval(e.color.copy(alpha = 1f - p), Offset(c.x - rx, c.y - rx * 0.35f), Size(rx * 2f, rx * 0.7f), style = Stroke(width = 3.dp.toPx()))
        }

        TipoEfecto.Carta -> {
            val info = e.carta ?: return
            val inicio = Offset(0.5f, 1.05f)
            val x = inicio.x + (e.x - inicio.x) * p
            val y = inicio.y + (e.y - inicio.y) * p
            val c = punto(x, y, sin(p * PI.toFloat()) * 0.08f)
            val cw = w * 0.1f * (1f - p * 0.6f)
            val ch = cw * 1.28f
            rotate(degrees = p * 360f, pivot = c) {
                drawRoundRect(
                    Brush.verticalGradient(listOf(info.rareza.claro, info.rareza.oscuro), startY = c.y - ch / 2f, endY = c.y + ch / 2f),
                    Offset(c.x - cw / 2f, c.y - ch / 2f),
                    Size(cw, ch),
                    CornerRadius(cw * 0.12f)
                )
                drawRoundRect(info.rareza.borde, Offset(c.x - cw / 2f, c.y - ch / 2f), Size(cw, ch), CornerRadius(cw * 0.12f), style = Stroke(width = 2.dp.toPx()))
            }
        }

        TipoEfecto.Quemadura -> {}
    }
}

// =====================================================================
//  INTERFAZ (Compose)
// =====================================================================

private val sombraTexto = Shadow(Color.Black, Offset(2f, 2f), 3f)

@Composable
fun JuegoArena() {
    var estado by remember { mutableStateOf(EstadoJuego.Inicio) }
    var mundo by remember { mutableStateOf(Mundo()) }
    var frame by remember { mutableLongStateOf(0L) }
    var elixir by remember { mutableFloatStateOf(5f) }
    var elixirEntero by remember { mutableIntStateOf(5) }
    var segundos by remember { mutableIntStateOf(DURACION_PARTIDA.toInt()) }
    var coronasJ by remember { mutableIntStateOf(0) }
    var coronasE by remember { mutableIntStateOf(0) }
    var cuenta by remember { mutableIntStateOf(3) }
    var seleccion by remember { mutableStateOf<Int?>(null) }
    var mano by remember { mutableStateOf(listOf(0, 1, 2, 3)) }
    var cola by remember { mutableStateOf(listOf(4, 5)) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var temblores by remember { mutableIntStateOf(0) }
    val sacudida = remember { Animatable(0f) }
    val camara = remember { Animatable(1f) }
    val medidor = rememberTextMeasurer()

    fun reiniciar() {
        mundo = Mundo()
        elixir = 5f
        elixirEntero = 5
        segundos = DURACION_PARTIDA.toInt()
        coronasJ = 0
        coronasE = 0
        seleccion = null
        val baraja = CARTAS.indices.shuffled()
        mano = baraja.take(4)
        cola = baraja.drop(4)
        estado = EstadoJuego.Cuenta
    }

    fun jugar(fx: Float, fy: Float) {
        if (estado != EstadoJuego.Jugando) return
        val ranura = seleccion ?: run {
            mensaje = "Elige una carta primero"
            return
        }
        val info = CARTAS[mano[ranura]]
        if (!info.esHechizo && fy < 0.55f) {
            mensaje = "Solo puedes desplegar en tu lado"
            return
        }
        if (elixir < info.costo) {
            mensaje = "¡Elixir insuficiente!"
            return
        }
        elixir -= info.costo
        elixirEntero = elixir.toInt()
        mundo.desplegar(info, Bando.Jugador, fx.coerceIn(0.04f, 0.96f), if (info.esHechizo) fy else fy.coerceIn(0.58f, 0.9f))
        val usada = mano[ranura]
        mano = mano.toMutableList().also { it[ranura] = cola.first() }
        cola = cola.drop(1) + usada
        seleccion = null
    }

    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            delay(1300)
            mensaje = null
        }
    }

    // Temblor de pantalla con resorte
    LaunchedEffect(temblores) {
        if (temblores > 0) {
            sacudida.snapTo(18f)
            sacudida.animateTo(0f, spring(dampingRatio = 0.15f, stiffness = 900f))
        }
    }

    // Animación de fondo cuando no se está jugando
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            if (estado != EstadoJuego.Jugando) {
                mundo.reloj += 0.016f
                mundo.animarEfectos(0.016f)
                frame++
            }
        }
    }

    // Cuenta regresiva con acercamiento de cámara y bucle principal
    LaunchedEffect(estado) {
        when (estado) {
            EstadoJuego.Cuenta -> {
                launch {
                    camara.snapTo(1.4f)
                    camara.animateTo(1f, tween(2600, easing = FastOutSlowInEasing))
                }
                for (n in 3 downTo 1) {
                    cuenta = n
                    delay(750)
                }
                cuenta = 0
                delay(700)
                estado = EstadoJuego.Jugando
            }

            EstadoJuego.Jugando -> {
                var ultimo = withFrameNanos { it }
                while (true) {
                    val ahora = withFrameNanos { it }
                    val dt = ((ahora - ultimo) / 1_000_000_000f).coerceAtMost(0.05f)
                    ultimo = ahora

                    val m = mundo
                    val multiplicador = if (m.tiempo <= 60f) 2f else 1f
                    elixir = min(10f, elixir + dt / 2.8f * multiplicador)
                    if (elixir.toInt() != elixirEntero) elixirEntero = elixir.toInt()
                    m.actualizar(dt, multiplicador)
                    frame++

                    if (m.temblor) {
                        m.temblor = false
                        temblores++
                    }
                    val s = m.tiempo.toInt().coerceAtLeast(0)
                    if (s != segundos) segundos = s
                    val cJ = m.coronasDe(Bando.Jugador)
                    val cE = m.coronasDe(Bando.Enemigo)
                    if (cJ != coronasJ) coronasJ = cJ
                    if (cE != coronasE) coronasE = cE

                    if (!m.rey(Bando.Enemigo).viva) {
                        estado = EstadoJuego.Victoria
                        break
                    }
                    if (!m.rey(Bando.Jugador).viva) {
                        estado = EstadoJuego.Derrota
                        break
                    }
                    if (m.tiempo <= 0f) {
                        estado = when {
                            cJ > cE -> EstadoJuego.Victoria
                            cJ < cE -> EstadoJuego.Derrota
                            else -> EstadoJuego.Empate
                        }
                        break
                    }
                }
            }

            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF14301A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Marcador("CPU", coronasE, Color(0xFFC62828))
            Reloj(segundos, doble = segundos <= 60 && estado == EstadoJuego.Jugando)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .graphicsLayer {
                    translationX = sacudida.value
                    translationY = sacudida.value * 0.5f
                    scaleX = camara.value
                    scaleY = camara.value
                }
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val fy = pos.y / size.height
                        val fx = 0.5f + (pos.x / size.width - 0.5f) / escalaEn(fy)
                        jugar(fx, fy)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (frame < 0L) return@Canvas
                val m = mundo
                dibujarFondo(m.reloj, seleccion != null && estado == EstadoJuego.Jugando)
                m.efectos.filter { it.tipo == TipoEfecto.Quemadura }.forEach { dibujarQuemadura(it) }

                // Todo lo que tiene altura se dibuja de atrás hacia adelante
                val dibujables = ArrayList<Pair<Float, () -> Unit>>()
                m.torres.forEach { t -> dibujables.add(t.y to { dibujarTorre(t, m.reloj, medidor) }) }
                m.unidades.forEach { u -> dibujables.add(u.y to { dibujarUnidad(u, m.reloj) }) }
                m.caidos.forEach { c -> dibujables.add(c.unidad.y to { dibujarCaido(c) }) }
                dibujables.sortedBy { it.first }.forEach { it.second() }

                m.particulas.forEach { dibujarParticula(it) }
                m.proyectiles.forEach { dibujarProyectil(it) }
                m.efectos.filter { it.tipo != TipoEfecto.Quemadura }.forEach { dibujarEfecto(it, medidor) }
                dibujarNubes(m.reloj)
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = mensaje != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
            ) {
                Text(
                    mensaje ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xDD000000))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = estado == EstadoJuego.Cuenta,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                AnimatedContent(
                    targetState = cuenta,
                    transitionSpec = {
                        (scaleIn(initialScale = 2.5f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut())
                    },
                    label = "cuenta"
                ) { n ->
                    Text(
                        if (n > 0) "$n" else "¡A PELEAR!",
                        color = if (n > 0) Color.White else Color(0xFFFFD54F),
                        fontSize = if (n > 0) 80.sp else 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(shadow = sombraTexto)
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = estado != EstadoJuego.Jugando && estado != EstadoJuego.Cuenta,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 1.2f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xB3000000)),
                    contentAlignment = Alignment.Center
                ) {
                    if (estado == EstadoJuego.Victoria) Confeti()
                    AnimatedContent(
                        targetState = estado,
                        transitionSpec = { (fadeIn(tween(500)) + scaleIn()) togetherWith fadeOut(tween(250)) },
                        label = "pantalla"
                    ) { e ->
                        PantallaMenu(e, coronasJ, coronasE) { reiniciar() }
                    }
                }
            }
        }

        // Bandeja de cartas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF6D4C41), Color(0xFF3E2723))))
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Marcador("Tú", coronasJ, Color(0xFF1565C0))
                Spacer(modifier = Modifier.weight(1f))
                Text("Siguiente:", color = Color(0xFFFFE0B2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                key(cola.first()) {
                    CartaVista(
                        info = CARTAS[cola.first()],
                        seleccionada = false,
                        alcanza = true,
                        onClick = {},
                        mini = true,
                        modifier = Modifier.width(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                mano.forEachIndexed { ranura, indice ->
                    key(indice) {
                        CartaVista(
                            info = CARTAS[indice],
                            seleccionada = seleccion == ranura,
                            alcanza = elixirEntero >= CARTAS[indice].costo,
                            onClick = { seleccion = if (seleccion == ranura) null else ranura },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            BarraElixir(elixir = { elixir }, entero = elixirEntero)
        }
    }
}

@Composable
fun Confeti() {
    val anim = rememberInfiniteTransition(label = "confeti")
    val t by anim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "tiempoConfeti"
    )
    val colores = listOf(Color(0xFFFFD54F), Color(0xFFE53935), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFAB47BC))
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (i in 0 until 70) {
            val x = fraccion(i * 0.6180339f) * size.width + sin(t * 12.56f + i) * 14f
            val y = fraccion(t + fraccion(i * 0.37f)) * (size.height + 40f) - 20f
            rotate(degrees = t * 720f + i * 40f, pivot = Offset(x, y)) {
                drawRect(colores[i % colores.size], Offset(x - 5f, y - 2.5f), Size(10f, 5f))
            }
        }
    }
}

@Composable
fun Marcador(nombre: String, coronas: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(aclarar(color, 0.2f), oscurecer(color, 0.3f))))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedContent(
            targetState = coronas,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = Spring.DampingRatioHighBouncy), initialScale = 2.5f) + fadeIn()) togetherWith fadeOut()
            },
            label = "coronas"
        ) { c ->
            Text("👑 $c", color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun Reloj(segundos: Int, doble: Boolean) {
    val colorFondo by animateColorAsState(
        targetValue = if (doble) Color(0xFF8E24AA) else Color(0xFF37474F),
        animationSpec = tween(600),
        label = "fondoReloj"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorFondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text("⏱", fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        AnimatedContent(
            targetState = segundos,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
            label = "reloj"
        ) { s ->
            Text(
                "%d:%02d".format(s / 60, s % 60),
                color = if (s <= 10) Color(0xFFFF8A80) else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        if (doble) {
            Spacer(modifier = Modifier.width(6.dp))
            Text("x2", color = Color(0xFFFF80AB), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun PantallaMenu(estado: EstadoJuego, coronasJ: Int, coronasE: Int, onJugar: () -> Unit) {
    val pulso = rememberInfiniteTransition(label = "pulsoBoton")
    val escalaBoton by pulso.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "escalaBoton"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val (titulo, color) = when (estado) {
            EstadoJuego.Victoria -> "¡VICTORIA!" to Color(0xFFFFD54F)
            EstadoJuego.Derrota -> "DERROTA" to Color(0xFFEF5350)
            EstadoJuego.Empate -> "EMPATE" to Color(0xFFB0BEC5)
            else -> "ARENA DE TORRES" to Color.White
        }
        Text(titulo, color = color, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(shadow = sombraTexto))
        Spacer(modifier = Modifier.height(12.dp))

        if (estado == EstadoJuego.Inicio) {
            Text(
                "Elige una carta y toca tu lado de la arena.\nDestruye la torre del rey para ganar.",
                color = Color.White,
                fontSize = 14.sp
            )
        } else {
            Row {
                for (i in 0 until 3) {
                    val aparecer = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(300L + i * 250L)
                        aparecer.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    Text(
                        "👑",
                        fontSize = 44.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = aparecer.value
                            scaleY = aparecer.value
                            rotationZ = (1f - aparecer.value) * 180f
                            alpha = if (i < coronasJ) 1f else 0.25f
                        }
                    )
                }
            }
            Text("Tú $coronasJ  -  $coronasE CPU", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onJugar,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
            modifier = Modifier.graphicsLayer {
                scaleX = escalaBoton
                scaleY = escalaBoton
            }
        ) {
            Text(
                if (estado == EstadoJuego.Inicio) "¡BATALLA!" else "JUGAR DE NUEVO",
                color = Color(0xFF3E2723),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun CartaVista(
    info: CartaInfo,
    seleccionada: Boolean,
    alcanza: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mini: Boolean = false
) {
    val elevacion by animateDpAsState(
        targetValue = if (seleccionada) (-14).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "elevacionCarta"
    )
    val escala by animateFloatAsState(
        targetValue = if (seleccionada) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escalaCarta"
    )
    val colorBorde by animateColorAsState(
        targetValue = if (seleccionada) Color(0xFFFFEB3B) else info.rareza.borde,
        label = "bordeCarta"
    )
    val opacidad by animateFloatAsState(
        targetValue = if (alcanza) 1f else 0.5f,
        label = "opacidadCarta"
    )
    // Entrada de la carta con giro 3D
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrada.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val anim = rememberInfiniteTransition(label = "carta")
    val barrido by anim.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Restart),
        label = "barrido"
    )
    val respiracion by anim.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "respiracion"
    )

    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .offset(y = elevacion)
            .graphicsLayer {
                scaleX = escala * (0.6f + 0.4f * entrada.value)
                scaleY = escala * (0.6f + 0.4f * entrada.value)
                rotationY = (1f - entrada.value) * 90f
                rotationX = if (seleccionada) -8f else 0f
                cameraDistance = 10f * density
                alpha = opacidad
            }
            .clip(RoundedCornerShape(if (mini) 5.dp else 10.dp))
            .background(Brush.verticalGradient(listOf(info.rareza.claro, info.rareza.oscuro)))
            .border(if (seleccionada) 3.dp else 2.dp, colorBorde, RoundedCornerShape(if (mini) 5.dp else 10.dp))
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF81D4FA), Color(0xFF7CB342)), startY = size.height * 0.08f, endY = size.height * 0.72f),
                Offset(size.width * 0.08f, size.height * 0.08f),
                Size(size.width * 0.84f, size.height * 0.64f),
                CornerRadius(6.dp.toPx())
            )
            val r = size.width * 0.24f
            val cx = size.width / 2f
            val cy = size.height * 0.44f
            drawOval(Color.Black.copy(alpha = 0.3f), Offset(cx - r * 0.8f, cy + r * 0.75f), Size(r * 1.6f, r * 0.4f))
            val paso = if (seleccionada) respiracion * 2f else 0f
            dibujarFigura(info.tipo, Color(0xFF2979FF), cx, cy, r, respiracion, 0f, paso)
            val x = barrido * size.width
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.4f), Color.Transparent),
                    start = Offset(x - size.width * 0.3f, 0f),
                    end = Offset(x + size.width * 0.3f, size.height)
                )
            )
        }
        GotaElixir(
            costo = info.costo,
            tamano = if (mini) 14.dp else 22.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(2.dp)
        )
        if (!mini) {
            Text(
                info.nombre,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                style = TextStyle(shadow = Shadow(Color.Black, Offset(1f, 1f), 2f))
            )
        }
    }
}

@Composable
fun GotaElixir(costo: Int, modifier: Modifier = Modifier, tamano: Dp = 22.dp) {
    Box(modifier = modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val gota = Path().apply {
                moveTo(w / 2f, 0f)
                cubicTo(w * 0.95f, h * 0.45f, w * 0.95f, h, w / 2f, h)
                cubicTo(w * 0.05f, h, w * 0.05f, h * 0.45f, w / 2f, 0f)
                close()
            }
            drawPath(gota, Brush.radialGradient(listOf(Color(0xFFF8BBD0), Color(0xFFEC407A), Color(0xFF880E4F)), center = Offset(w * 0.35f, h * 0.45f), radius = w))
            drawPath(gota, Color.White, style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.6f), w * 0.08f, Offset(w * 0.36f, h * 0.45f))
        }
        Text(
            "$costo",
            color = Color.White,
            fontSize = (tamano.value * 0.48f).sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = tamano * 0.15f),
            style = TextStyle(shadow = Shadow(Color.Black, Offset(1f, 1f), 2f))
        )
    }
}

@Composable
fun BarraElixir(elixir: () -> Float, entero: Int) {
    val lleno = entero >= 10
    val colorRelleno by animateColorAsState(
        targetValue = if (lleno) Color(0xFFFF80AB) else Color(0xFFEC407A),
        label = "colorElixir"
    )
    val pulso = rememberInfiniteTransition(label = "pulsoElixir")
    val alfa by pulso.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "alfaElixir"
    )
    // La gota "salta" cada vez que ganas un punto de elixir
    val salto = remember { Animatable(1f) }
    LaunchedEffect(entero) {
        salto.snapTo(1.3f)
        salto.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        GotaElixir(
            costo = entero,
            tamano = 28.dp,
            modifier = Modifier.graphicsLayer {
                scaleX = salto.value
                scaleY = salto.value
            }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
        ) {
            val w = size.width
            val h = size.height
            drawRoundRect(Color(0xFF1A0E2E), cornerRadius = CornerRadius(h / 2f))
            val relleno = w * (elixir() / 10f).coerceIn(0f, 1f)
            drawRoundRect(
                Brush.verticalGradient(listOf(aclarar(colorRelleno, 0.3f), colorRelleno, Color(0xFF880E4F))),
                size = Size(relleno, h),
                cornerRadius = CornerRadius(h / 2f)
            )
            drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(4f, 2f), Size((relleno - 8f).coerceAtLeast(0f), h * 0.3f), CornerRadius(h / 4f))
            for (i in 1..9) {
                val x = w * i / 10f
                drawLine(Color.Black.copy(alpha = 0.35f), Offset(x, 2f), Offset(x, h - 2f), strokeWidth = 1.5f)
            }
            if (lleno) {
                drawRoundRect(Color.White.copy(alpha = alfa), cornerRadius = CornerRadius(h / 2f))
            }
        }
    }
}
