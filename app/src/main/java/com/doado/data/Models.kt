package com.doado.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Una app instalada en el móvil que el sistema considera "lanzable".
 * Es la materia prima de Ajustes → Aplicaciones, donde se elige cuáles salen
 * en la pantalla de inicio.
 */
data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
)

/**
 * Icono de una casilla. Las apps básicas usan los iconos de trazo de Doado, que
 * se tiñen con el color del tema; las que se añaden después (WhatsApp,
 * Sofascore...) llevan su icono de siempre, porque es por el que se reconocen.
 */
sealed interface AppIcon {
    data class Vector(@param:DrawableRes val res: Int) : AppIcon
    data class Bitmap(val image: ImageBitmap) : AppIcon
}

/**
 * Una casilla del grid de inicio.
 *
 * [packageName] es nulo cuando en este móvil no hay ninguna app capaz de atender
 * la acción (por ejemplo, un emulador sin app de cámara, o una app que se ha
 * desinstalado). En ese caso la casilla se sigue dibujando, pero desactivada y
 * avisando de por qué, en lugar de desaparecer: para alguien que ya se ha
 * aprendido dónde está cada botón, que un icono cambie de sitio es peor que un
 * icono apagado.
 */
data class FavoriteApp(
    val id: String,
    val label: String,
    val icon: AppIcon,
    val packageName: String?,
) {
    val isAvailable: Boolean get() = packageName != null
}

/** Qué hace un acceso directo de contacto al pulsarlo. */
enum class ContactAction {
    /** Llamada de teléfono directa (o el marcador, si no hay permiso). */
    CALL,

    /** Abre la conversación de WhatsApp con esa persona. */
    WHATSAPP,
}

/** Un contacto fijado arriba del todo en la pantalla de inicio. */
data class ContactShortcut(
    val id: String,
    val name: String,
    val number: String,
    val action: ContactAction,
    val onHome: Boolean,
)

/** Tamaño de letra elegido en Ajustes → Interfaz, sobre el que ya pida el sistema. */
enum class TextSize(val scale: Float) {
    NORMAL(1f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
}

enum class ThemeMode { DARK, LIGHT }

/** Qué hace el asistente cuando la persona mayor le pide algo (D-27). */
enum class AssistantMode {
    /** Lo hace él, siempre tras preguntar y recibir un «Sí» (D-26). */
    ACT,

    /** Explica los pasos y marca en el inicio el botón que hay que pulsar. */
    GUIDE,
}

/**
 * Ajustes del asistente de voz (CU-15). De fábrica viene apagado: hace falta que
 * el familiar lo encienda, lea a dónde va la voz y dé permiso al micrófono.
 */
data class AssistantSettings(
    val enabled: Boolean = false,
    val mode: AssistantMode = AssistantMode.GUIDE,
)

/** Lo que se puede personalizar de la interfaz. */
data class InterfaceSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val textSize: TextSize = TextSize.NORMAL,
    val columns: Int = 2,
    val showClock: Boolean = true,
)
