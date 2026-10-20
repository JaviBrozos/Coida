# Arquitectura

> **Fuente única de**: cómo está construido Doado por dentro. Si necesitas algo
> de aquí en otro sitio, enlaza; no copies. Los valores exactos (claves,
> constantes, tamaños) los manda el código: si esto no cuadra con él, **gana el
> código** y hay que corregir esta página.
>
> Última revisión: 13/10/2026.

## Las cuatro capas

```
Pantallas (Compose)      launcher/, settings/ y assistant/ui/. Solo pintan:
        ↓                reciben lo que hay que enseñar y avisan de los toques.
LauncherActivity         Estado, navegación, permisos y todo lo que sale fuera.
        ↓          ↘
data/          assistant/  Repositorio del PackageManager y cuatro almacenes ·
        ↓          ↓       el asistente (micrófono, red y voz).
Android                  Apps instaladas, preferencias, Intents, audio y red.
```

Las pantallas no tocan Android: no conocen `Context`, `PackageManager` ni
`Intent`. Eso permite previsualizarlas y cambiarlas sin arrancar nada.

## Navegación

No hay librería de navegación: son diez pantallas y planas.

- Un `enum Screen { HOME, PIN, SETTINGS, INTERFACE, CONTACTS, APPS, ADMIN,
  CHANGE_PIN, ASSISTANT, ASSISTANT_SETTINGS }` y un `when` en `CurrentScreen()`,
  dentro de `LauncherActivity`.
- `parentOf()` define la vuelta: `HOME`, `PIN` y `SETTINGS` vuelven a `HOME`;
  `ASSISTANT` vuelve a donde se abrió (el inicio, o sus ajustes con «Probar el
  asistente»); todo lo demás vuelve a `SETTINGS`.
- `BackHandler` está activo en todas menos en `HOME`. En `ASSISTANT`, además,
  cancela la escucha.

### Comportamiento de INICIO, ATRÁS y recientes

Esto es lo que distingue un launcher de una app normal, y es donde se rompen:

| Gesto | Qué tiene que pasar | Cómo se consigue |
| --- | --- | --- |
| **INICIO** desde otra app | Vuelve a Doado, y además a la pantalla de inicio, no donde se dejó | `launchMode="singleTask"` + `onNewIntent()` pone `screen = HOME` |
| **INICIO** estando ya en Doado | Vuelve al inicio desde cualquier pantalla de ajustes | Lo mismo: llega un `onNewIntent` |
| **ATRÁS** en el inicio | No hace nada: no se puede «salir» de la pantalla de inicio | El `BackHandler` está desactivado en `HOME` |
| **Recientes** | Doado no aparece como tarjeta | `excludeFromRecents="true"` |
| **Irse a otra app** estando en Ajustes | Al volver, los ajustes están cerrados y hay que repetir el PIN | `onStop()` devuelve a `HOME` (RNF-SEG-02) |
| **Irse a otra app** con el asistente abierto | Deja de escuchar y descarta la petición. La casilla señalada se queda: apagar la pantalla no debe borrarla | `onStop()` llama a `assistant.cancel()` |
| Asistente abierto y nadie toca | La pantalla no se apaga, para que el tiempo de apagado no cancele una pregunta | `FLAG_KEEP_SCREEN_ON` mientras `screen == ASSISTANT` |
| **INICIO** con una casilla señalada | Se quita la marca | `onNewIntent()` pone `highlightedId = null` |
| Volver de algo que abrió Doado | Sigues donde estabas: la tienda, el selector de contactos o los ajustes de Android no te echan | La bandera `salidaPropia` marca esa ida y vuelta |
| Girar la pantalla | No se recrea la Activity | `configChanges` incluye `orientation` y compañía |

`stateNotNeeded="true"` permite que el sistema mate el proceso sin guardar
estado: al volver, Doado arranca limpio en el inicio, que es lo correcto aquí.

## Dónde vive el estado

En la propia `LauncherActivity`, con `mutableStateOf`. No hay ViewModel porque no
hay nada asíncrono que sobreviva a la Activity, y `configChanges` evita las
recreaciones. Lo único asíncrono, el asistente, vive en `AssistantController`, una
clase normal con el `lifecycleScope` de la Activity (D-29).

`reload()` vuelve a leer preferencias, casillas, contactos y apps instaladas. Se
llama en `onCreate` y en **cada `onResume`**: así, si mientras tanto se instaló o
desinstaló una app, las casillas se actualizan solas al volver.

Dos cosas se aplican envolviendo toda la app:

- **Tamaño de letra**: `CompositionLocalProvider(LocalDensity ...)` multiplica el
  `fontScale` del sistema por el factor elegido (1 / 1,15 / 1,3).
- **Barras del sistema**: `enableEdgeToEdge()` se vuelve a aplicar con
  `SystemBarStyle.dark` o `.light` según el tema elegido, porque el tema lo
  decide el familiar y no el sistema; si no, los iconos de la barra saldrían
  blancos sobre blanco.

## Qué se guarda

Cuatro ficheros de `SharedPreferences`. **Una clave ya publicada no se renombra
ni cambia de tipo**: se añade otra y se migra leyendo la antigua.

### `doado_settings` — `SettingsStore`

| Clave | Tipo | Por defecto |
| --- | --- | --- |
| `theme` | String (`DARK` \| `LIGHT`) | `DARK` |
| `text_size` | String (`NORMAL` \| `LARGE` \| `EXTRA_LARGE`) | `NORMAL` |
| `columns` | Int, acotado a 1–2 | `2` |
| `show_clock` | Boolean | `true` |
| `admin_unlocked_until` | Long (epoch ms) | `0` |
| `assistant_enabled` | Boolean | `false` |
| `assistant_mode` | String (`ACT` \| `GUIDE`) | `GUIDE` |

Los enums se leen con un ayudante que devuelve el valor por defecto si el
guardado no existe: ante datos corruptos, la app arranca, no peta.

### `doado_contacts` — `ContactShortcutsStore`

Una sola clave, `contacts`, con un `JSONArray`. Cada elemento: `id`, `name`,
`number`, `action` (`CALL` \| `WHATSAPP`) y `onHome`. La lectura entera va en un
`runCatching`: si el JSON se corrompe, la lista queda vacía en vez de romper el
arranque. Sin límite de accesos directos en el inicio (D-38). Cada casilla es un elemento; la misma persona (el mismo número, comparando sus 9 últimas cifras) puede tener uno `CALL` y otro `WHATSAPP` (D-40), y Ajustes y el asistente los juntan por persona.

### `doado_favorites` — `FavoritesStore`

Una clave, `favorite_ids`, con los ids separados por comas. **El orden importa**:
es la posición de cada casilla, y por eso no se usa un `StringSet`. Dos formas de
id:

- Los favoritos de fábrica: `phone`, `messages`, `camera`, `contacts`.
- Cualquier otra app: `pkg:` + su paquete (`pkg:com.whatsapp`).

Al encender una casilla se añade **al final**: nada de lo que ya estaba se mueve.

### `doado_security` — `PinStore`

Una clave, `settings_pin`, con el PIN **derivado**, nunca en claro: PBKDF2
(`PBKDF2WithHmacSHA1`, 60 000 iteraciones, clave de 256 bits) sobre una sal
aleatoria de 16 bytes distinta en cada móvil, guardado como `sal$hash` en Base64.

Está en un fichero aparte por una razón concreta: **es el único que se queda
fuera de la copia de seguridad**. Así el móvil nuevo recupera tema, casillas y
contactos, pero el PIN no viaja y se vuelve a crear en la primera entrada.

Al arrancar, `PinStore` mira si queda un PIN en claro de las versiones anteriores
(estaba en `doado_settings`), lo guarda derivado aquí y lo borra de allí.

La pregunta de recuperación (CU-16, D-23) vive en el mismo fichero: la pregunta
en `recovery_question`, en claro porque hay que enseñarla, y la respuesta en
`recovery_answer_derived`, **derivada igual que el PIN** (D-41) tras pasarla a
minúsculas y sin espacios extremos para que no falle por cómo se escriba.
Responderla bien permite poner un PIN nuevo; no devuelve el antiguo.

El límite de intentos (RNF-SEG-03, D-24) usa cuatro claves más, todas aquí:

| Clave | Tipo | Qué guarda |
| --- | --- | --- |
| `failed_attempts` | Int | Fallos seguidos, sumando PIN y respuesta. Un acierto o un PIN nuevo lo ponen a 0 |
| `lock_duration` | Long | Cuánto dura el bloqueo en curso, en ms; 0 si no hay |
| `lock_until_elapsed` | Long | Cuándo acaba, en el reloj `SystemClock.elapsedRealtime()` |
| `lock_boot_count` | Int | `Settings.Global.BOOT_COUNT` al fijarlo |

El reloj monotónico no cambia al tocar la hora, pero vuelve a cero al reiniciar:
si el número de arranque no coincide, o si falta más de lo que dura el bloqueo,
el bloqueo vuelve a empezar entero. Se escribe con `commit` para que forzar el
cierre justo después de fallar no se salte el fallo.

Conviene ser honesto sobre el alcance: el hash solo evita que el PIN se lea
abriendo el fichero y que acabe en una copia ajena; lo que frena probar las diez
mil combinaciones desde la pantalla es el límite de intentos. Quien pueda leer el
fichero (una build de depuración con `run-as`, o un móvil con root) puede probarlas
todas fuera del móvil en poco tiempo.

### Copias de seguridad

`res/xml/backup_rules.xml` y `res/xml/data_extraction_rules.xml` declaran
`doado_favorites`, `doado_contacts` y `doado_settings` con una lista de
`<include>`. `doado_security` queda fuera **por no estar en la lista**: un
`<exclude>` explícito no vale, porque lint lo rechaza al no estar incluido.
**Preferencia nueva ⇒ añadirla a los dos ficheros**, y pensar en qué lado va.

## `InstalledAppsRepository`

Única puerta al `PackageManager`. Resuelve las casillas (`homeApps()`), dice si
un paquete está instalado, da la intención con la que abrir cada cosa y lista las
apps del móvil para la pantalla de Aplicaciones.

Detalles que conviene saber:

- Los iconos reales de las apps se cargan a 192 px y se convierten a `ImageBitmap`
  en cada `reload()`, sin caché. Se hace fuera del hilo principal, en
  `loadApps()` de `LauncherActivity` (D-43); el inicio no se pinta hasta la
  primera carga.
- `installedApps()` descarta launchers secundarios con `distinctBy { packageName }`.
- `InstalledApp.activityName` se rellena pero no se usa: se lanza siempre por
  `getLaunchIntentForPackage`.

## Intents: todo sale con reserva

`startFirst(vararg intents)` prueba las intenciones en orden, añade
`FLAG_ACTIVITY_NEW_TASK`, captura `ActivityNotFoundException` y `SecurityException`
y sigue con la siguiente. Si ninguna funciona, avisa con un mensaje en lenguaje
llano. **Ningún intent hacia fuera se lanza a pelo.**

| Acción | Intento principal | Reserva |
| --- | --- | --- |
| Abrir casilla | Plantilla del favorito, o `getLaunchIntentForPackage` | Aviso con el nombre de la app |
| Llamar a un contacto | `ACTION_CALL` si hay permiso | `ACTION_DIAL` con el número puesto |
| WhatsApp a un contacto | `ACTION_VIEW` sobre `https://wa.me/<E164>` con `setPackage` | Aviso |
| Instalar una app | `market://details?id=…` | La misma ficha en `play.google.com` |
| Abrir la tienda | Launcher de `com.android.vending` | `play.google.com/store/apps` |
| Ajustes de Android | `Settings.ACTION_SETTINGS` | — |
| Elegir pantalla de inicio | `Settings.ACTION_HOME_SETTINGS` | `Settings.ACTION_SETTINGS` |
| Sonido y pantalla | `Settings.ACTION_SOUND_SETTINGS` | — |
| Elegir contacto | `ACTION_PICK` sobre `Phone.CONTENT_URI` | Aviso |
| Acción confirmada del asistente | La misma que el toque equivalente: llamar, WhatsApp o abrir casilla. Puede llamar a un contacto cuyo acceso directo abre WhatsApp, y al revés | La misma |

El número se pasa a formato internacional con `PhoneNumberUtils.formatNumberToE164`
usando el país de la SIM, luego el de la red y luego el del idioma. Si no hay
país usable, se manda tal cual, quitando lo que no sea dígito.

## Permisos y manifiesto

- **`CALL_PHONE`** es peligroso y se pide **al añadir un contacto**, con el
  familiar delante, no en el inicio con la persona mayor. Si se deniega, las
  llamadas caen al marcador y no se vuelve a pedir.
- **`RECORD_AUDIO`** también es peligroso y se pide **al encender el asistente**
  en sus ajustes. Si se deniega, el interruptor se queda apagado con una nota. Si
  alguien lo quita después, el asistente lo dice en lenguaje llano en vez de
  pedirlo delante de la persona mayor.
- **`INTERNET`**, solo para el asistente, y solo HTTPS
  (`usesCleartextTraffic="false"`).
- `<uses-feature android:name="android.hardware.microphone" android:required="false" />`,
  igual que la telefonía.
- El `<intent>` de `TTS_SERVICE` en `<queries>`: sin él, desde Android 11,
  `TextToSpeech` no encuentra el motor de voz.
- `<uses-feature android:name="android.hardware.telephony" android:required="false" />`
  para no dejar fuera tablets en Google Play.
- **No hay `READ_CONTACTS`**: el selector de contactos concede permiso solo sobre
  la fila elegida. Tampoco `QUERY_ALL_PACKAGES`: el bloque `<queries>` declara
  únicamente lo que Doado necesita resolver.
- La actividad: `singleTask`, `excludeFromRecents`, `stateNotNeeded`, y el
  intent-filter con `HOME` + `DEFAULT` + `LAUNCHER`.

## Asistente de voz

Casos de uso CU-04, CU-05 y CU-15; decisiones D-25 a D-31; normas en
`.claude/rules/asistente.md`.

```
toque en el círculo → LauncherActivity.openAssistant() → AssistantController.start()
  VoiceRecorder      graba hasta 2,5 s de silencio, 30 s o «Ya he terminado»
  SpeechToText       Whisper, con los nombres como pista
  IntentInterpreter  modelo de lenguaje, respuesta en JSON (Prompts)
  ActionCatalog      valida contra la lista blanca; lo dudoso es «no entendido»
  → Confirm (modo «Lo hace») o Guide (modo «Explica cómo», pasos de GuideSteps)
  → Speaker termina de hablar → pitido → vuelve a escuchar con la pantalla igual
       VoiceReplies   «sí», «no», «siguiente», «repite», «gracias»: sin IA
       el resto       al modelo, con los últimos 6 turnos de la conversación
  → «Sí» (pulsado o dicho): onExecute → LauncherActivity lanza el Intent
  → «Entendido»: onFinish → inicio con la casilla marcada (highlightedId)
```

| Pieza | Qué hace |
| --- | --- |
| `AssistantController` | Máquina de estados (`AssistantState`) y la conversación: reabre el micrófono al acabar de hablar (`followUpListening`) y guarda en memoria los últimos turnos. Corre en el `lifecycleScope`; `onStop` lo cancela y olvida la conversación |
| `VoiceReplies` | Reconoce respuestas cortas sin IA. Solo cuentan si todas sus palabras son de esa respuesta o de relleno (D-31) |
| `audio/VoiceRecorder` | `AudioRecord` a 16 kHz, fuente `VOICE_RECOGNITION`, sin supresor de ruido ni ganancia (D-37). Decide qué es voz comparando con el ruido de fondo, con un umbral para empezar y otro más bajo para seguir, y solo envía el tramo con voz. El WAV vive en memoria |
| `audio/Speaker` | `TextToSpeech` en español a 0,85. Calla con TalkBack encendido, salvo en «Repetir». Avisa al acabar de hablar (`UtteranceProgressListener`), que es cuando se reabre el micrófono. Con TalkBack, el controlador espera lo que tarda en leer la pantalla (de 2,5 a 10 s) antes del pitido |
| `cloud/OpenAiCompatibleClient` | `/audio/transcriptions` y `/chat/completions` con `HttpURLConnection`. Conexión 10 s, lectura 20 s. Traduce cada fallo a un `AssistantFailure` |
| `ActionCatalog` | Da a cada contacto y casilla un id corto (`c1`, `a1`). Al modelo solo le llegan ids y nombres |
| `GuideSteps` | Pasos fijos para guiar hacia algo de Doado, con su posición en pantalla |
| `VoiceTuning`, `Prompts` | Tiempos de voz y prompt, cada uno en un único sitio |

### Configuración del proveedor

En `local.properties`, que no entra en git, y de ahí a `BuildConfig`:

| Clave | Por defecto |
| --- | --- |
| `doado.ai.baseUrl` | `https://api.groq.com/openai/v1` |
| `doado.ai.sttModel` | `whisper-large-v3` |
| `doado.ai.llmModel` | `openai/gpt-oss-120b` |
| `doado.ai.reasoningEffort` | `low` (vacío = no se envía) |
| `doado.ai.apiKey` | vacía: sin ella el asistente no se puede encender |

La clave viaja dentro del APK y se puede extraer. Vale para probar; antes de
publicar hay que resolver CON-06.
