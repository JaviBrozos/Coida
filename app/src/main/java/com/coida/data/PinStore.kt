package com.coida.data

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
 * Un PIN ya derivado por [PinStore.prepare], a falta de que [PinStore.check] lo
 * compare y cuente el intento. [derived] es null si no hay PIN guardado.
 */
class PreparedPin internal constructor(
    internal val derived: String?,
    /** El PIN, solo si está guardado con las vueltas de antes y hay que pasarlo a las nuevas. */
    internal val pinToUpgrade: String?,
)

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
        migratePlainRecoveryAnswer()
    }

    /** false mientras no se haya creado ninguno: la primera entrada lo pide. */
    fun hasPin(): Boolean = prefs.getString(KEY_PIN, null) != null

    /**
     * Deriva [pin] con la sal guardada, que es lo caro, sin compararlo ni contar
     * nada. La pantalla lo lanza a la vez que deja ver el cuarto punto, para no
     * sumar las dos esperas. Lo que devuelve no abre nada por sí solo: hace falta
     * [check], que es donde se mira el bloqueo y se cuenta el intento.
     */
    fun prepare(pin: String): PreparedPin {
        val guardado = prefs.getString(KEY_PIN, null)?.let(::parseStored)
            ?: return PreparedPin(derived = null, pinToUpgrade = null)
        return PreparedPin(
            derived = derivar(pin, guardado.salt, guardado.iterations),
            pinToUpgrade = pin.takeIf { guardado.iterations != ITERACIONES_PIN },
        )
    }

    /**
     * Comprueba el PIN y **cuenta el intento**. Con la puerta bloqueada devuelve
     * false sin mirar nada, aunque la pantalla no debería ni dejar escribir: así
     * el límite no depende de que la interfaz lo respete.
     *
     * Un PIN guardado con las vueltas de antes (D-49) se vuelve a guardar con las
     * de ahora al acertarlo: es el único momento en que se tiene el PIN en la mano.
     */
    fun check(prepared: PreparedPin): Boolean {
        if (lockState().locked) return false
        val guardado = prefs.getString(KEY_PIN, null)?.let(::parseStored)
        val ok = prepared.derived != null && guardado != null &&
            constantTimeEquals(prepared.derived, guardado.hash)
        recordAttempt(ok)
        if (ok) prepared.pinToUpgrade?.let(::save)
        return ok
    }

    fun save(pin: String) {
        prefs.edit {
            putString(KEY_PIN, deriveWithNewSalt(pin, ITERACIONES_PIN))
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
            putString(KEY_RECOVERY_ANSWER, deriveWithNewSalt(normalizeAnswer(answer), ITERACIONES_RESPUESTA))
            remove(KEY_PLAIN_RECOVERY_ANSWER)
        }
    }

    /** Como [check]: cuenta el intento en el mismo contador que el PIN. */
    fun checkRecoveryAnswer(answer: String): Boolean {
        if (lockState().locked) return false
        val guardada = prefs.getString(KEY_RECOVERY_ANSWER, null) ?: return false
        val ok = matchesStored(normalizeAnswer(answer), guardada)
        recordAttempt(ok)
        return ok
    }

    private fun normalizeAnswer(answer: String): String = answer.trim().lowercase()

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

    /**
     * La primera versión de la pregunta de recuperación guardaba la respuesta en
     * claro (ya normalizada). Al arrancar se deriva y se borra la legible, de modo
     * que la respuesta que el familiar puso sigue valiendo.
     */
    private fun migratePlainRecoveryAnswer() {
        val enClaro = prefs.getString(KEY_PLAIN_RECOVERY_ANSWER, null) ?: return
        prefs.edit {
            if (!prefs.contains(KEY_RECOVERY_ANSWER)) {
                putString(KEY_RECOVERY_ANSWER, deriveWithNewSalt(enClaro, ITERACIONES_RESPUESTA))
            }
            remove(KEY_PLAIN_RECOVERY_ANSWER)
        }
    }

    /**
     * Deriva [secret] con una sal recién generada y lo devuelve como
     * `vueltas$sal$derivado`: las vueltas van con él para poder cambiarlas sin
     * dejar inservible lo ya guardado (D-49).
     */
    private fun deriveWithNewSalt(secret: String, iterations: Int): String {
        val sal = ByteArray(BYTES_DE_SAL).also { SecureRandom().nextBytes(it) }
        return iterations.toString() + SEPARADOR + codificar(sal) + SEPARADOR + derivar(secret, sal, iterations)
    }

    /** true si [secret], derivado como lo que hay en [stored], da lo mismo. */
    private fun matchesStored(secret: String, stored: String): Boolean {
        val guardado = parseStored(stored) ?: return false
        return constantTimeEquals(derivar(secret, guardado.salt, guardado.iterations), guardado.hash)
    }

    /** Un secreto guardado, ya separado en sus partes. */
    private class StoredSecret(val iterations: Int, val salt: ByteArray, val hash: String)

    /**
     * `vueltas$sal$derivado`, o `sal$derivado` como se guardaba antes de D-49,
     * que siempre llevaba [ITERACIONES_ANTIGUAS]. null si no se entiende.
     */
    private fun parseStored(stored: String): StoredSecret? {
        val partes = stored.split(SEPARADOR)
        return when (partes.size) {
            2 -> decodificar(partes[0])?.let { StoredSecret(ITERACIONES_ANTIGUAS, it, partes[1]) }
            3 -> {
                val vueltas = partes[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
                decodificar(partes[1])?.let { StoredSecret(vueltas, it, partes[2]) }
            }
            else -> null
        }
    }

    private fun derivar(pin: String, sal: ByteArray, iterations: Int): String {
        val spec = PBEKeySpec(pin.toCharArray(), sal, iterations, BITS_DE_CLAVE)
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
        private const val KEY_RECOVERY_ANSWER = "recovery_answer_derived"

        // Donde la primera versión dejaba la respuesta en claro.
        private const val KEY_PLAIN_RECOVERY_ANSWER = "recovery_answer"

        // Límite de intentos (RNF-SEG-03).
        private const val KEY_FAILURES = "failed_attempts"
        private const val KEY_LOCK_UNTIL = "lock_until_elapsed"
        private const val KEY_LOCK_BOOT = "lock_boot_count"
        private const val KEY_LOCK_DURATION = "lock_duration"

        private const val SEPARADOR = "$"
        private const val ALGORITMO = "PBKDF2WithHmacSHA1"

        /**
         * Vueltas de PBKDF2 del PIN (D-49). Con cuatro cifras, quien copie el
         * fichero prueba las diez mil igual de rápido con 10 000 que con 60 000; lo
         * que frena es el límite de intentos. Con 60 000, el PIN tardaba casi un
         * segundo en abrir en un móvil lento.
         */
        private const val ITERACIONES_PIN = 10_000

        /** La respuesta es texto libre: ahí las vueltas sí cuentan, y se escribe poco. */
        private const val ITERACIONES_RESPUESTA = 60_000

        /** Las que llevaba todo lo guardado como `sal$derivado`, antes de D-49. */
        private const val ITERACIONES_ANTIGUAS = 60_000
        private const val BITS_DE_CLAVE = 256
        private const val BYTES_DE_SAL = 16
    }
}
