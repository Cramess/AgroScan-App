package com.tecsup.agroscan.ui.utils

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Efecto de vidrio esmerilado en vivo (glassmorphism).
 *
 * Cómo funciona: la zona que queda "detrás" del vidrio ([fuenteVidrio]) se dibuja en una GraphicsLayer.
 * Cada [PanelVidrio] vuelve a dibujar esa capa recortada a su forma, desplazada a su posición, y le aplica
 * un desenfoque por GPU (RenderEffect). En Android la capa se dibuja por referencia (RenderNode), así que el
 * desenfoque sigue al contenido en tiempo real (scroll, cámara, mapa) sin recomponer nada.
 *
 * Requisitos: el desenfoque necesita Android 12 (API 31). En versiones anteriores, o si el usuario lo
 * desactiva, el panel cae a un fondo casi opaco. Un SurfaceView no se puede capturar: por eso la vista
 * previa de la cámara usa el modo COMPATIBLE (TextureView).
 */

/** true si el usuario tiene activado el efecto esmerilado (se cambia desde el perfil). */
val LocalVidrio = compositionLocalOf { true }

/** El desenfoque por GPU (RenderEffect) existe desde Android 12. */
val vidrioSoportado: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Contenido capturado que se verá desenfocado detrás de los paneles de vidrio. */
@Stable
class FondoVidrio internal constructor(internal val capa: GraphicsLayer) {
    internal var posicionEnRaiz by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberFondoVidrio(): FondoVidrio {
    val capa = rememberGraphicsLayer()
    return remember(capa) { FondoVidrio(capa) }
}

/**
 * Marca este elemento como el fondo que se desenfoca. Los [PanelVidrio] que lo usan deben estar
 * fuera de él (encima, como hermanos), nunca dentro: un panel no puede desenfocarse a sí mismo.
 */
@Composable
fun Modifier.fuenteVidrio(fondo: FondoVidrio): Modifier {
    if (!LocalVidrio.current || !vidrioSoportado) return this
    return this
        .onGloballyPositioned { fondo.posicionEnRaiz = it.positionInRoot() }
        .drawWithContent {
            fondo.capa.record { this@drawWithContent.drawContent() }
            drawLayer(fondo.capa)
        }
}

/**
 * Panel de vidrio esmerilado: muestra desenfocado lo que hay detrás ([fondo]) con un tinte encima.
 * Si [fondo] es null, el efecto está desactivado o el equipo es anterior a Android 12, usa un fondo sólido.
 */
@Composable
fun PanelVidrio(
    fondo: FondoVidrio?,
    modifier: Modifier = Modifier,
    forma: Shape = RoundedCornerShape(24.dp),
    tinte: Color = MaterialTheme.colorScheme.surface,
    opacidadTinte: Float = 0.55f,
    opacidadSinVidrio: Float = 0.94f,   // opacidad del tinte cuando no hay desenfoque
    desenfoque: Dp = 22.dp,
    alineacion: Alignment = Alignment.TopStart,
    contenido: @Composable BoxScope.() -> Unit
) {
    val activo = fondo != null && LocalVidrio.current && vidrioSoportado
    // Posición del panel en la ventana; solo se lee al dibujar, así moverla no recompone
    val posicion = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clip(forma)
            .then(if (activo) Modifier.onGloballyPositioned { posicion.value = it.positionInRoot() } else Modifier),
        contentAlignment = alineacion
    ) {
        if (activo) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val radio = desenfoque.toPx()
                        renderEffect = BlurEffect(radio, radio, TileMode.Clamp)
                        clip = true
                    }
                    .drawBehind {
                        val desplazamiento = posicion.value - fondo!!.posicionEnRaiz
                        translate(-desplazamiento.x, -desplazamiento.y) { drawLayer(fondo.capa) }
                    }
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(tinte.copy(alpha = if (activo) opacidadTinte else opacidadSinVidrio))
                // Borde claro muy fino: el "canto" del vidrio
                .border(0.8.dp, Color.White.copy(alpha = if (activo) 0.22f else 0.08f), forma)
        )
        contenido()
    }
}
