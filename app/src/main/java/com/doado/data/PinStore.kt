package com.doado.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import androidx.core.content.edit
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cómo va el límite de intentos de la puerta de ajustes (RNF-SEG-03).
 *
 * @property failures fallos seguidos, sumando PIN y respuesta de recuperación.
 * @property remainingMs cuánto falta para poder volver a probar; 0 si se puede ya.
 */
data class PinLock(val failures: Int, val remainingMs: Long) {
    val locked: Boolean get() = remainingMs > 0L
}

/**
 * El PIN que protege los ajustes (RNF-SEG-01 de la ERS).
 *
 * Vive en su propio fichero de preferencias y no en [SettingsStore] por una
 * razón concreta: este es el único que se queda **fuera de la copia de
 * seguridad**. Así el móvil nuevo recupera el tema, las casillas y los
 * contactos, pero el PIN no viaja a la nube.
 *
 * No se guarda en claro: se deriva con PBKDF2 y una sal aleatoria distinta en
 * cada móvil. Conviene ser honestos sobre qué protege eso y qué no. Cuatro
 * cifras son diez mil combinaciones, así que no frena a quien pueda probarlas
 * todas con calma; lo que evita es que el PIN se lea de un vistazo abriendo el
 * fichero de preferencias, o que acabe en una copia de seguridad ajena.
 *
 * Lo que sí frena probarlas todas es el **límite de intentos** (D-24): a partir
 * del quinto fallo, la puerta se bloquea un tiempo que crece con cada fallo, como
 * en un móvil. El contador es el mismo para el PIN y para la respuesta de
 * recuperación, para que no se pueda esquivar alternando entre ellos. Solo se
 * bloquea la puerta de ajustes: el inicio no se entera.
 */
class PinStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrarPinEnClaro()
    }

    /** false mientras no se haya creado ninguno: la primera entrada lo pide. */
    fun hasPin(): Boolean = prefs.getString(KEY_PIN, null) != null

    /**
     * Comprueba el PIN y **cuenta el intento**. Con la puerta bloqueada devuelve
     * false sin mirar nada, aunque la pantalla no debería ni dejar escribir: así
     * el límite no depende de que la interfaz lo respete.
     */
    fun check(pin: String): Boolean {
        if (lockState().locked) return false
        val ok = matchesStoredPin(pin)
        recordAttempt(ok)
        return ok
    }

    fun save(pin: String) {
        val sal = ByteArray(BYTES_DE_SAL).also { SecureRandom().nextBytes(it) }
        prefs.edit {
            putString(KEY_PIN, codificar(sal) + SEPARADOR + derivar(pin, sal))
            // Un PIN recién puesto empieza sin fallos a cuestas.
            putInt(KEY_FAILURES, 0)
            putLong(KEY_LOCK_DURATION, 0L)
        }
    }

    fun lockState(): PinLock = PinLock(
        failures = prefs.getInt(KEY_FAILURES, 0),
        remainingMs = remainingLockMs(),
    )

    fun hasRecoveryQuestion(): Boolean =
        prefs.getString(KEY_RECOVERY_QUESTION, null) != null

    fun recoveryQuestion(): String =
        prefs.getString(KEY_RECOVERY_QUESTION, "") ?: ""

    /**
     * Guarda la pregunta en claro y la respuesta normalizada (sin mayúsculas ni
     * espacios extremos) también en claro, dentro de doado_security que ya está
     * fuera de la copia de seguridad. El objetivo de la pregunta no es seguridad
     * criptográfica sino un segundo factor de conveniencia para el familiar que
     * olvida el PIN; la alternativa sin respuesta es borrar los datos de la app.
     */
    fun saveRecoveryQuestion(question: String, answer: String) {
        prefs.edit {
            putString(KEY_RECOVERY_QUESTION, question.trim())
            putString(KEY_RECOVERY_ANSWER, answer.trim().lowercase())
        }
    }

    /** Como [check]: cuenta el intento en el mismo contador que el PIN. */
    fun checkRecoveryAnswer(answer: String): Boolean {
        if (lockState().locked) return false
        val guardada = prefs.getString(KEY_RECOVERY_ANSWER, null) ?: return false
        val ok = constantTimeEquals(answer.trim().lowercase(), guardada)
        recordAttempt(ok)
        return ok
    }

    private fun matchesStoredPin(pin: String): Boolean {
        val guardado = prefs.getString(KEY_PIN, null) ?: return false
        val partes = guardado.split(SEPARADOR)
        if (partes.size != 2) return false
        val sal = decodificar(partes[0]) ?: return false
        return constantTimeEquals(derivar(pin, sal), partes[1])
    }

    /**
     * Un acierto pone el contador a cero. Un fallo suma uno y, si toca según
     * [lockDurationMs], arranca el bloqueo.
     *
     * Se escribe con commit y no con apply: si alguien cierra la app a la fuerza
     * justo después de fallar, el fallo tiene que quedar contado.
     */
    private fun recordAttempt(ok: Boolean) {
        if (ok) {
            prefs.edit(commit = true) {
                putInt(KEY_FAILURES, 0)
                putLong(KEY_LOCK_DURATION, 0L)
            }
            return
        }
        val failures = prefs.getInt(KEY_FAILURES, 0) + 1
        val lock = lockDurationMs(failures)
        prefs.edit(commit = true) {
            putInt(KEY_FAILURES, failures)
            if (lock > 0L) {
                putLong(KEY_LOCK_DURATION, lock)
                putLong(KEY_LOCK_UNTIL, SystemClock.elapsedRealtime() + lock)
                putInt(KEY_LOCK_BOOT, bootCount())
            }
        }
    }

    /**
     * El bloqueo se mide con el reloj monotónico, que no cambia al tocar la hora
     * del móvil. Ese reloj vuelve a cero al reiniciar, así que se guarda también
     * el número de arranque: si no coincide, el móvil se ha reiniciado y el
     * bloqueo en curso vuelve a empezar entero. Ni cambiar la hora ni reiniciar
     * lo acortan.
     */
    private fun remainingLockMs(): Long {
        val duration = prefs.getLong(KEY_LOCK_DURATION, 0L)
        if (duration <= 0L) return 0L

        val now = SystemClock.elapsedRealtime()
        val boot = bootCount()
        val remaining = prefs.getLong(KEY_LOCK_UNTIL, 0L) - now

        // Reinicio detectado por el número de arranque, o por la cuenta: si falta
        // más de lo que dura el bloqueo, el reloj monotónico ha vuelto a empezar.
        // Esto segundo cubre los móviles que no rellenan BOOT_COUNT; sin ello, el
        // bloqueo se quedaría congelado tanto tiempo como llevara el móvil
        // encendido antes de reiniciarse.
        if (prefs.getInt(KEY_LOCK_BOOT, -1) != boot || remaining > duration) {
            // apply y no commit: esto puede ocurrir al pintar la pantalla, y si se
            // perdiera, la próxima lectura lo volvería a anclar igual.
            prefs.edit {
                putLong(KEY_LOCK_UNTIL, now + duration)
                putInt(KEY_LOCK_BOOT, boot)
            }
            return duration
        }

        if (remaining <= 0L) {
            // Cumplido: se olvida la duración para que un reinicio posterior no
            // lo resucite. Los fallos siguen contando para el próximo escalón.
            prefs.edit { putLong(KEY_LOCK_DURATION, 0L) }
            return 0L
        }
        return remaining
    }

    private fun bootCount(): Int =
        Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, 0)

    /** Compara sin cortar en la primera diferencia, para no dar pistas por el tiempo. */
    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

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

    private fun derivar(pin: String, sal: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), sal, ITERACIONES, BITS_DE_CLAVE)
        return try {
            codificar(SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded)
        } catch (e: GeneralSecurityException) {
            // Ningún Android sin PBKDF2 llega a minSdk 24, así que esto no debería
            // pasar; si pasara, es mejor romper aquí que guardar el PIN en claro.
            throw IllegalStateException("No se puede derivar el PIN", e)
        } finally {
            spec.clearPassword()
        }
    }

    private fun codificar(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decodificar(texto: String): ByteArray? =
        runCatching { Base64.decode(texto, Base64.NO_WRAP) }.getOrNull()

    companion object {
        /** A partir de cuántos fallos se le ofrece a la persona mayor volver al inicio. */
        const val FAILURES_BEFORE_EXIT_NOTE = 4

        /**
         * Escala del bloqueo, parecida a la de Android o iPhone (D-24): los cuatro
         * primeros fallos no bloquean; del quinto en adelante, cada vez más.
         */
        fun lockDurationMs(failures: Int): Long = when {
            failures < 5 -> 0L
            failures == 5 -> 30_000L
            failures == 6 -> 60_000L
            failures == 7 -> 5 * 60_000L
            failures == 8 -> 15 * 60_000L
            else -> 60 * 60_000L
        }

        /** A propósito, fuera de backup_rules.xml y de data_extraction_rules.xml. */
        private const val PREFS_NAME = "doado_security"
        private const val KEY_PIN = "settings_pin"

        // De dónde se rescata el PIN de las versiones anteriores.
        private const val PREFS_ANTIGUAS = "doado_settings"
        private const val KEY_ANTIGUA = "settings_pin"

        private const val KEY_RECOVERY_QUESTION = "recovery_question"
        private const val KEY_RECOVERY_ANSWER = "recovery_answer"

        // Límite de intentos (RNF-SEG-03).
        private const val KEY_FAILURES = "failed_attempts"
        private const val KEY_LOCK_UNTIL = "lock_until_elapsed"
        private const val KEY_LOCK_BOOT = "lock_boot_count"
        private const val KEY_LOCK_DURATION = "lock_duration"

        private const val SEPARADOR = "$"
        private const val ALGORITMO = "PBKDF2WithHmacSHA1"
        private const val ITERACIONES = 60_000
        private const val BITS_DE_CLAVE = 256
        private const val BYTES_DE_SAL = 16
    }
}
