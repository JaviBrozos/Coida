package com.doado.assistant.cloud

import com.doado.BuildConfig

/**
 * Proveedor del asistente, tal como llega de `local.properties` a través de
 * BuildConfig (D-25). Cambiar de proveedor o de modelo es cambiar esos valores,
 * no este código.
 *
 * La clave va dentro del APK y se puede extraer: vale para probar, no para
 * publicar (CON-06 en la ERS).
 */
data class AssistantConfig(
    val baseUrl: String,
    val speechModel: String,
    val languageModel: String,
    /** Vacío si el modelo no razona o no admite el parámetro. */
    val reasoningEffort: String,
    val apiKey: String,
) {
    /** Sin clave no hay asistente: el botón no aparece y los ajustes lo explican. */
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && baseUrl.isNotBlank() &&
            speechModel.isNotBlank() && languageModel.isNotBlank()

    /** Nunca enseña la clave: este objeto puede acabar en un log. */
    override fun toString(): String =
        "AssistantConfig(baseUrl=$baseUrl, speechModel=$speechModel, " +
            "languageModel=$languageModel, apiKey=${if (apiKey.isBlank()) "falta" else "***"})"

    companion object {
        fun fromBuildConfig() = AssistantConfig(
            baseUrl = BuildConfig.AI_BASE_URL.trimEnd('/'),
            speechModel = BuildConfig.AI_STT_MODEL,
            languageModel = BuildConfig.AI_LLM_MODEL,
            reasoningEffort = BuildConfig.AI_REASONING_EFFORT,
            apiKey = BuildConfig.AI_API_KEY,
        )
    }
}
