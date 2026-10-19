package com.doado.assistant

/**
 * Todo lo que decide cuándo escuchar y cuándo dar por acabada una frase (RNF-USA-03).
 *
 * Los asistentes generalistas cortan a las personas mayores entre 2 y 5 veces más
 * que a las jóvenes, porque hacen pausas largas a mitad de frase (D-25). Por eso
 * aquí los tiempos son generosos y siempre hay un botón «Ya he terminado».
 *
 * Son valores de partida: se ajustan probando con voces mayores de verdad, y solo
 * aquí. No los bajes para que «vaya más rápido».
 */
object VoiceTuning {

    /** Muestreo que espera Whisper. Más no mejora el reconocimiento y pesa el doble. */
    const val SAMPLE_RATE = 16_000

    /** Trozo de audio que se analiza cada vez, en ms. */
    const val FRAME_MS = 100

    /** Cuánto se espera a que empiece a hablar. Hay quien tarda en arrancar. */
    const val WAIT_FOR_SPEECH_MS = 10_000L

    /** Silencio seguido que da la frase por terminada, una vez ha hablado. */
    const val END_OF_SPEECH_SILENCE_MS = 2_500L

    /** Tope de una orden. Lo que pase de aquí no es una orden, es una conversación. */
    const val MAX_RECORDING_MS = 30_000L

    /** Voz mínima para considerar que ha dicho algo; menos es un golpe o un carraspeo. */
    const val MIN_SPEECH_MS = 400L

    /**
     * Volumen (RMS sobre 32 767) por debajo del cual nunca se considera voz. Bajo a
     * propósito: hay voces muy flojas, el móvil puede estar en la mesa y ya no hay
     * ganancia automática que las suba (D-37).
     */
    const val MIN_SPEECH_RMS = 150.0

    /**
     * Cuántas veces tiene que superar la voz al ruido de fondo para empezar a
     * contar (unos 8 dB). Con 3 y la ganancia automática encendida, una voz floja
     * no llegaba y la frase se cortaba a medias.
     */
    const val SPEECH_START_FACTOR = 2.5

    /**
     * Una vez empezada, basta con esto para seguir contando voz (unos 4 dB): el
     * final de las palabras, más flojo, no cuenta como silencio.
     */
    const val SPEECH_CONTINUE_FACTOR = 1.6

    /** Lo que sube la medida del ruido en cada trozo sin voz: alcanza a una tele en unos 2 s. */
    const val NOISE_RISE = 0.05

    /** Lo mismo durante la voz, mucho más despacio, para no tomar la voz por ruido. */
    const val NOISE_RISE_IN_SPEECH = 0.01

    /** Audio que se envía antes de la primera voz: el arranque de una palabra es flojo. */
    const val PADDING_BEFORE_SPEECH_MS = 500L

    /** Y después de la última: una sílaba final que se apaga. */
    const val PADDING_AFTER_SPEECH_MS = 800L

    /** Volumen que deja vacía la barra de «te estoy oyendo». Solo es para pintar. */
    const val LEVEL_EMPTY_RMS = 60.0

    /** Volumen que la llena. Solo es para pintar. */
    const val LEVEL_FULL_RMS = 3_000.0

    /**
     * Con TalkBack, cuánto se espera como mínimo antes de grabar: lo que tarda en
     * anunciar «Te escucho». Si no, su voz se grabaría con la orden y además subiría
     * el umbral de ruido. En la conversación, la espera crece con lo que acaba de
     * leer ([SCREEN_READER_MS_PER_CHAR]), hasta [SCREEN_READER_MAX_GRACE_MS].
     */
    const val SCREEN_READER_GRACE_MS = 2_500L
    const val SCREEN_READER_MS_PER_CHAR = 70L
    const val SCREEN_READER_MAX_GRACE_MS = 10_000L

    /**
     * Pitido corto de «habla ahora» antes de cada grabación. En la conversación es
     * lo que avisa de que el micrófono se ha vuelto a abrir solo.
     */
    const val READY_BEEP_MS = 150

    /** Velocidad de la voz del móvil. 1 es la normal; algo más pausada se entiende mejor. */
    const val SPEECH_RATE = 0.85f
}
