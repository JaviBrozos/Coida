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
     * propósito: hay voces muy flojas y el móvil puede estar en la mesa.
     */
    const val MIN_SPEECH_RMS = 250.0

    /** La voz tiene que superar el ruido de fondo medido por este factor. */
    const val NOISE_FACTOR = 3.0

    /** Volumen que llena la barra de «te estoy oyendo». Solo es para pintar. */
    const val LEVEL_FULL_SCALE = 3_000.0

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
