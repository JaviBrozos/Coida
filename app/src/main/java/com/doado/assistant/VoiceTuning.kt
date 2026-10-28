package com.doado.assistant

/**
 * Todo lo que decide cuándo escuchar y cuándo dar por acabada una frase (RNF-USA-03).
 *
 * Los asistentes generalistas cortan a las personas mayores entre 2 y 5 veces más
 * que a las jóvenes, porque hacen pausas largas a mitad de frase (D-25). Por eso
 * siempre hay un botón «Ya he terminado».
 *
 * Pero en la prueba con dos personas mayores pasaba lo contrario (D-58): tras la
 * orden, el micrófono seguía abierto dos segundos y medio y se colaban la tele y
 * las voces de la casa, que Whisper transcribía y liaban al modelo. Ahora la frase
 * acaba antes, y el final se mide contra la propia voz: lo que suena bastante más
 * flojo que quien habla pegado al móvil ya no cuenta como voz.
 *
 * Son valores de partida: se ajustan probando con voces mayores de verdad, y solo
 * aquí. El registro `DoadoVoz` (en depuración) da las cifras de cada grabación.
 */
object VoiceTuning {

    /** Muestreo que espera Whisper. Más no mejora el reconocimiento y pesa el doble. */
    const val SAMPLE_RATE = 16_000

    /** Trozo de audio que se analiza cada vez, en ms. */
    const val FRAME_MS = 100

    /** Cuánto se espera a que empiece a hablar. Hay quien tarda en arrancar. */
    const val WAIT_FOR_SPEECH_MS = 10_000L

    /**
     * Silencio seguido que da la frase por terminada, una vez ha hablado. Era de
     * 2,5 s y dejaba entrar el ruido de después (D-58); con 1,2 s todavía cabe la
     * pausa de quien busca una palabra, y si se corta, el asistente pide repetirlo.
     */
    const val END_OF_SPEECH_SILENCE_MS = 1_200L

    /**
     * Tope de una orden, contado desde que empieza a hablar: una orden es una frase.
     * Con 30 s, un ruido seguido la alargaba sin fin.
     */
    const val MAX_RECORDING_MS = 10_000L

    /** Voz mínima para considerar que ha dicho algo; menos es un golpe o un carraspeo. */
    const val MIN_SPEECH_MS = 400L

    /**
     * Volumen (RMS sobre 32 767) por debajo del cual nunca se considera voz. Se
     * subió de 150 (D-58): con 150 contaba como voz una conversación al otro lado
     * de la habitación. Sigue sin haber ganancia automática (D-37).
     */
    const val MIN_SPEECH_RMS = 250.0

    /**
     * Cuántas veces tiene que superar la voz al ruido de fondo para empezar a
     * contar (unos 10 dB). Con 2,5 arrancaba con ruidos de la casa (D-58).
     */
    const val SPEECH_START_FACTOR = 3.2

    /**
     * Una vez empezada, basta con esto para seguir contando voz (unos 4 dB): el
     * final de las palabras, más flojo, no cuenta como silencio.
     */
    const val SPEECH_CONTINUE_FACTOR = 1.6

    /**
     * Además, para seguir contando voz hay que llegar a esta parte del nivel de la
     * propia voz (unos −12 dB). Quien da la orden habla cerca del móvil; la tele o
     * alguien de fondo suenan bastante más flojos y, en cuanto calla, cuentan como
     * silencio (D-58). Sin esto, el ruido de fondo alargaba la frase.
     */
    const val SPEECH_DROP_RATIO = 0.25

    /**
     * Lo que se acerca la medida del nivel de la voz a cada trozo con voz. Despacio,
     * y sin dejar que un trozo pese más de [SPEECH_LEVEL_MAX_JUMP] veces la medida:
     * un golpe no la dispara y deja de oír la voz de verdad.
     */
    const val SPEECH_LEVEL_RISE = 0.15
    const val SPEECH_LEVEL_MAX_JUMP = 4.0

    /** Lo que sube la medida del ruido en cada trozo sin voz: alcanza a una tele en unos 2 s. */
    const val NOISE_RISE = 0.05

    /** Lo mismo durante la voz, mucho más despacio, para no tomar la voz por ruido. */
    const val NOISE_RISE_IN_SPEECH = 0.01

    /**
     * Audio que se envía antes de la primera voz: el arranque de una palabra es
     * flojo. Más margen era más ruido de fondo para Whisper (D-58).
     */
    const val PADDING_BEFORE_SPEECH_MS = 300L

    /** Y después de la última: una sílaba final que se apaga. */
    const val PADDING_AFTER_SPEECH_MS = 300L

    /** Volumen que deja vacía la barra de «te estoy oyendo». Solo es para pintar. */
    const val LEVEL_EMPTY_RMS = 60.0

    /** Volumen que la llena. Solo es para pintar. */
    const val LEVEL_FULL_RMS = 3_000.0

    /**
     * Con TalkBack, cuánto se espera como mínimo antes de grabar: lo que tarda en
     * anunciar «Te escucho». Si no, su voz se grabaría con la orden y además subiría
     * el umbral de ruido. Al volver a escuchar, la espera crece con lo que acaba de
     * leer ([SCREEN_READER_MS_PER_CHAR]), hasta [SCREEN_READER_MAX_GRACE_MS].
     */
    const val SCREEN_READER_GRACE_MS = 2_500L
    const val SCREEN_READER_MS_PER_CHAR = 70L
    const val SCREEN_READER_MAX_GRACE_MS = 10_000L

    /**
     * Pitido corto de «habla ahora» antes de cada grabación. Al pedir que se lo
     * repitan, es lo que avisa de que el micrófono se ha vuelto a abrir solo.
     */
    const val READY_BEEP_MS = 150

    /**
     * Lo que se tira del principio de la grabación además del pitido: lo que tarda
     * en apagarse en la habitación. El pitido suena ya con el micrófono abierto.
     */
    const val READY_BEEP_TAIL_MS = 100L

    /**
     * En «Lo hace», lo que se espera tras decir «Llamo a María» antes de llamar
     * (D-56): un respiro para pulsar «Cancelar» si no es ella.
     */
    const val ACT_AFTER_SPEECH_MS = 700L

    /** Lo más que se espera a que acabe esa frase, por si el motor de voz no avisa. */
    const val ACT_MAX_WAIT_MS = 6_000L

    /**
     * Lo menos que pasa entre enseñar «Llamo a María» y llamar. Sin motor de voz la
     * frase «acaba» al instante, y llamaba a los 0,7 s, sin tiempo para cancelar.
     */
    const val ACT_MIN_WINDOW_MS = 2_500L

    /**
     * Con TalkBack, lo que se añade a lo que tarda en leer la frase: el tiempo de
     * llegar con el dedo a «Cancelar» y pulsarlo dos veces.
     */
    const val ACT_SCREEN_READER_EXTRA_MS = 6_000L

    /** Velocidad de la voz del móvil. 1 es la normal; algo más pausada se entiende mejor. */
    const val SPEECH_RATE = 0.85f
}
