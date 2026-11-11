package com.tecsup.agroscan.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** true si el usuario tiene activadas las animaciones (se cambia desde el perfil). */
val LocalAnimaciones = compositionLocalOf { true }

/** Curva suave de salida usada en toda la app para que los movimientos se sientan consistentes. */
val CurvaSuave = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Duración estándar o salto inmediato, según la preferencia de animaciones. */
@Composable
fun <T> especAnimacion(duracionMs: Int = 350, retrasoMs: Int = 0): AnimationSpec<T> =
    if (LocalAnimaciones.current) tween(duracionMs, retrasoMs, CurvaSuave) else snap()

/**
 * Encoge levemente el elemento mientras se mantiene pulsado.
 * Un solo bucle de gestos (no se reinicia en cada toque) y la escala se lee solo al dibujar,
 * así pulsar no recompone el botón.
 */
fun Modifier.bounceClick() = composed {
    if (!LocalAnimaciones.current) return@composed this
    val escala = remember { Animatable(1f) }
    val alcance = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = escala.value
            scaleY = escala.value
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                alcance.launch { escala.animateTo(0.95f, spring(stiffness = Spring.StiffnessMedium)) }
                waitForUpOrCancellation()
                alcance.launch { escala.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)) }
            }
        }
}

/** Claves de los elementos que ya hicieron su entrada; ver [aparicion]. */
@Composable
fun rememberApariciones(): MutableSet<Any> = remember { mutableSetOf() }

/**
 * Entrada escalonada: el elemento aparece con un fundido y sube unos píxeles.
 * [indice] retrasa cada elemento de una lista para que lleguen uno tras otro.
 * En listas perezosas pase [clave] y [vistos]: así un elemento que vuelve a la pantalla al hacer
 * scroll no repite la animación (antes cada fila reaparecía con retraso y el scroll se veía a saltos).
 */
fun Modifier.aparicion(indice: Int = 0, clave: Any? = null, vistos: MutableSet<Any>? = null) = composed {
    if (!LocalAnimaciones.current) return@composed this
    val yaVisto = clave != null && vistos != null && clave in vistos
    val progreso = remember { Animatable(if (yaVisto) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (clave != null) vistos?.add(clave)
        if (progreso.value >= 1f) return@LaunchedEffect
        delay(indice.coerceAtMost(8) * 55L)
        progreso.animateTo(1f, tween(480, easing = CurvaSuave))
    }
    this.graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * 28.dp.toPx()
        val escala = 0.97f + 0.03f * progreso.value
        scaleX = escala
        scaleY = escala
    }
}

/** Número que cuenta hasta su valor al aparecer o cambiar. */
@Composable
fun NumeroAnimado(
    valor: Int,
    color: Color,
    tamano: TextUnit,
    peso: FontWeight = FontWeight.Bold,
    estilo: TextStyle = LocalTextStyle.current
) {
    val animaciones = LocalAnimaciones.current
    var objetivo by remember { mutableIntStateOf(if (animaciones) 0 else valor) }
    LaunchedEffect(valor) { objetivo = valor }
    val mostrado by animateIntAsState(objetivo, especAnimacion(900), label = "numeroAnimado")
    Text(mostrado.toString(), color = color, fontSize = tamano, fontWeight = peso, style = estilo)
}
