package com.doado.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * El PIN que protege los ajustes (RNF-SEG-01 de la ERS).
 *
 * Vive en su propio fichero de preferencias y no en [SettingsStore] por una
 * razón concreta: este es el único que se queda **fuera de la copia de
 * seguridad**. Así el móvil nuevo recupera el tema, las casillas y los
 * contactos, pero el PIN no viaja a la nube.
 *
 * Aquí vive también la respuesta a la pregunta de recuperación (CU-16), con el
 * mismo tratamiento.
 *
 * No se guarda en claro: se deriva con PBKDF2 y una sal aleatoria distinta en
 * cada móvil. Conviene ser honestos sobre qué protege eso y qué no. Cuatro
 * cifras son diez mil combinaciones, así que no frena a quien pueda probarlas
 * todas con calma; lo que evita es que el PIN se lea de un vistazo abriendo el
 * fichero de preferencias, o que acabe en una copia de seguridad ajena.
 */
class PinStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrarPinEnClaro()
        migratePlainRecoveryAnswer()
    }

    /** false mientras no se haya creado ninguno: la primera entrada lo pide. */
    fun hasPin(): Boolean = prefs.getString(KEY_PIN, null) != null

    fun check(pin: String): Boolean {
        val guardado = prefs.getString(KEY_PIN, null) ?: return false
        return matchesStored(pin, guardado)
    }

    fun save(pin: String) {
        prefs.edit { putString(KEY_PIN, deriveWithNewSalt(pin)) }
    }

    fun hasRecoveryQuestion(): Boolean =
        prefs.getString(KEY_RECOVERY_QUESTION, null) != null

    fun recoveryQuestion(): String =
        prefs.getString(KEY_RECOVERY_QUESTION, "") ?: ""

    /**
     * La pregunta va en claro, porque hay que enseñarla. La respuesta se guarda
     * derivada, igual que el PIN: para comprobarla basta con derivar lo que
     * escriba el familiar y comparar, así que no hay motivo para dejarla legible.
     *
     * Se normaliza antes (minúsculas, sin espacios extremos) para que no falle
     * por cómo se escriba.
     */
    fun saveRecoveryQuestion(question: String, answer: String) {
        prefs.edit {
            putString(KEY_RECOVERY_QUESTION, question.trim())
            putString(KEY_RECOVERY_ANSWER, deriveWithNewSalt(normalizeAnswer(answer)))
            remove(KEY_PLAIN_RECOVERY_ANSWER)
        }
    }

    fun checkRecoveryAnswer(answer: String): Boolean {
        val guardada = prefs.getString(KEY_RECOVERY_ANSWER, null) ?: return false
        return matchesStored(normalizeAnswer(answer), guardada)
    }

    private fun normalizeAnswer(answer: String): String = answer.trim().lowercase()

    /**
     * Las primeras versiones guardaban el PIN en claro dentro de los ajustes, que
     * sí se respaldan. Al arrancar se traslada aquí, derivado, y se borra de allí.
     */
    private fun migrarPinEnClaro() {
        val ajustes =
            appContext.getSharedPreferences(PREFS_ANTIGUAS, Context.MODE_PRIVATE)
        val enClaro = ajustes.getString(KEY_ANTIGUA, null) ?: return
        if (!hasPin()) save(enClaro)
        ajustes.edit { remove(KEY_ANTIGUA) }
    }

    /**
     * La primera versión de la pregunta de recuperación guardaba la respuesta en
     * claro (ya normalizada). Al arrancar se deriva y se borra la legible, de modo
     * que la respuesta que el familiar puso sigue valiendo.
     */
    private fun migratePlainRecoveryAnswer() {
        val enClaro = prefs.getString(KEY_PLAIN_RECOVERY_ANSWER, null) ?: return
        prefs.edit {
            if (!prefs.contains(KEY_RECOVERY_ANSWER)) {
                putString(KEY_RECOVERY_ANSWER, deriveWithNewSalt(enClaro))
            }
            remove(KEY_PLAIN_RECOVERY_ANSWER)
        }
    }

    /** Deriva [secret] con una sal recién generada y lo devuelve como `sal$derivado`. */
    private fun deriveWithNewSalt(secret: String): String {
        val sal = ByteArray(BYTES_DE_SAL).also { SecureRandom().nextBytes(it) }
        return codificar(sal) + SEPARADOR + derivar(secret, sal)
    }

    /** true si [secret], derivado con la sal de [stored], da lo mismo que hay guardado. */
    private fun matchesStored(secret: String, stored: String): Boolean {
        val partes = stored.split(SEPARADOR)
        if (partes.size != 2) return false
        val sal = decodificar(partes[0]) ?: return false
        return derivar(secret, sal) == partes[1]
    }

    private fun derivar(pin: String, sal: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), sal, ITERACIONES, BITS_DE_CLAVE)
        return try {
            codificar(SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded)
        } catch (e: GeneralSecurityException) {
            // Ningún Android sin PBKDF2 llega a minSdk 24, así que esto no debería
            // pasar; si pasara, es mejor romper aquí que guardar el secreto en claro.
            throw IllegalStateException("No se puede derivar el PIN ni la respuesta", e)
        } finally {
            spec.clearPassword()
        }
    }

    private fun codificar(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decodificar(texto: String): ByteArray? =
        runCatching { Base64.decode(texto, Base64.NO_WRAP) }.getOrNull()

    private companion object {
        /** A propósito, fuera de backup_rules.xml y de data_extraction_rules.xml. */
        const val PREFS_NAME = "doado_security"
        const val KEY_PIN = "settings_pin"

        // De dónde se rescata el PIN de las versiones anteriores.
        const val PREFS_ANTIGUAS = "doado_settings"
        const val KEY_ANTIGUA = "settings_pin"

        const val KEY_RECOVERY_QUESTION = "recovery_question"
        const val KEY_RECOVERY_ANSWER = "recovery_answer_derived"

        // Donde la primera versión dejaba la respuesta en claro.
        const val KEY_PLAIN_RECOVERY_ANSWER = "recovery_answer"

        const val SEPARADOR = "$"
        const val ALGORITMO = "PBKDF2WithHmacSHA1"
        const val ITERACIONES = 60_000
        const val BITS_DE_CLAVE = 256
        const val BYTES_DE_SAL = 16
    }
}
