package com.coida.data

/** Paquete de WhatsApp: los contactos de un toque escriben por ahí (D-15). */
const val WHATSAPP_PACKAGE = "com.whatsapp"

/**
 * Nombre de las apps que Coida conoce por su paquete. Sirve para pintar la
 * casilla apagada de una app que se ha desinstalado, que ya no tiene nombre en
 * Android: sin esto saldría «com.whatsapp».
 *
 * Antes eran las apps que Coida proponía instalar desde Ajustes; esa sección se
 * quitó (D-33) y solo queda el nombre.
 */
val KnownAppLabels: Map<String, String> = mapOf(
    WHATSAPP_PACKAGE to "WhatsApp",
    "com.sofascore.results" to "Sofascore",
)
