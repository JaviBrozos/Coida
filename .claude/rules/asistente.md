---
paths:
  - "app/src/main/java/com/doado/assistant/**"
  - "app/src/main/java/com/doado/settings/AssistantSettingsScreen.kt"
---

# Asistente de voz

El asistente oye a una persona mayor y le hace caso, así que un error aquí no es
un fallo de interfaz: es una llamada a quien no era. Las decisiones están en
`docs/decisiones.md` (D-25 a D-31) y los casos de uso en la ERS (CU-04, CU-05 y
CU-15).

## Lo que no se negocia

1. **Nunca actúa sin confirmación** (D-26, D-31): toda acción pasa por el estado
   `Confirm` y por un «Sí» pulsado o dicho. La pregunta la redacta el código, no
   el modelo. **El sí dicho lo decide `VoiceReplies`, nunca la IA**, y solo
   cuenta si todas sus palabras son de sí o de relleno. Si ensanchas sus listas,
   que sea con palabras que no puedan llevar otra orden dentro.
2. **Lista blanca**: solo llama o escribe a contactos guardados en Doado y solo
   abre casillas del inicio. El número sale de `ContactShortcutsStore`, **nunca de
   lo que diga el modelo**. Un id que no está en `ActionCatalog` es «no te he
   entendido».
3. **Los números de teléfono no salen del móvil.** Al proveedor le llegan el audio,
   la transcripción, los nombres y unos ids cortos (`c1`, `a1`).
4. **El proveedor solo existe en `assistant/cloud/`.** Fuera de ahí no se nombra a
   Groq ni a ningún modelo. URL, modelos y clave van en `local.properties` y llegan
   por `BuildConfig`; en los textos de la interfaz se habla de «un servicio
   externo», para que cambiar de proveedor no obligue a reescribirlos.
5. **La clave nunca entra en git ni en un log.** `AssistantConfig.toString()` no la
   enseña; no la imprimas tú.
6. **Red y audio, fuera del hilo principal**, con tiempo máximo y cancelables:
   salir de Doado cancela la escucha y la petición (`onStop`).
7. **El audio no se guarda** en disco ni se reenvía: vive en memoria hasta que se
   transcribe. La conversación (como mucho seis turnos) también vive solo en
   memoria, y `cancel()` la borra.

## Hablarle a una persona mayor

- **No se corta a quien habla despacio** (RNF-USA-03). Los tiempos viven en
  `VoiceTuning.kt`; no los bajes sin haber probado con una voz mayor de verdad.
- **Lo que se oye, se ve**: toda frase que dice el móvil sale también escrita, en
  grande, con un botón «Repetir». Con TalkBack encendido, el móvil no habla por
  encima: lee TalkBack.
- **Mientras escucha, no habla**: la voz del móvil se grabaría con la orden. El
  micrófono se reabre solo cuando `Speaker` avisa de que ha terminado, y un
  pitido marca el momento de hablar. Con TalkBack, la espera crece con lo que
  acaba de leer (`VoiceTuning.SCREEN_READER_*`).
- **Un silencio no es un error**: si en la conversación no contesta, se apaga el
  micrófono y se queda la pantalla con «Hablar otra vez».
- **Nada caduca solo**: con el asistente abierto la pantalla no se apaga, y la
  casilla señalada se queda hasta que se toca algo o se pulsa Inicio.
- **Todo error dice qué hacer ahora** («Pulsa y prueba otra vez»), tuteando y sin
  códigos HTTP, nombres de modelo ni la palabra «error».
- **Ante la duda, pregunta**: entre dos contactos, un botón grande por cada uno.
  Nunca adivines.

## Dónde va cada cosa

| Qué | Dónde |
| --- | --- |
| Prompt del modelo y formato de respuesta | `Prompts.kt`, en español. Ningún prompt fuera de ahí |
| Tiempos y umbrales de la voz | `VoiceTuning.kt` |
| Validar lo que devuelve el modelo | `ActionCatalog.kt` |
| Respuestas cortas sin IA («sí», «siguiente», «gracias») | `VoiceReplies.kt` |
| Pasos fijos al guiar hacia algo de Doado | `GuideSteps.kt` |
| Proveedor y formato HTTP | `cloud/` |
| Ejecutar la acción (Intents) | `LauncherActivity`, nunca el asistente |

## Si cambias el proveedor o el modelo

Cambia `local.properties`, no el código. Si el nuevo proveedor no habla el
formato de OpenAI, escribe otra clase en `cloud/` que implemente `SpeechToText` e
`IntentInterpreter`, y no toques nada fuera de esa carpeta. Anótalo en
`docs/decisiones.md`.

## Antes de dar por hecho un cambio aquí

Además del repaso de `accesibilidad.md`: prueba en el emulador con el micrófono
del ordenador, en los dos modos, una frase con pausas largas, una orden que no
pueda hacer, y otra en modo avión.
