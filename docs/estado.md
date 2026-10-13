# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 13/10/2026 · Javier Brozos**

## Dónde estamos

- Launcher completo: inicio y siete pantallas de ajustes tras el PIN, con límite
  de intentos (RNF-SEG-03) y recuperación del PIN (D-23).
- Asistente de voz programado (CU-04, CU-05, CU-15 «En pruebas»; D-25 a D-31):
  Groq en la nube, siempre confirma, actúa o guía según el familiar. Ajustes pasa
  a un botón en la esquina y el asistente es un círculo fijo abajo (D-28).
- Visto en emulador: el círculo no tapa casillas (también al 200 %), escucha,
  «No te he oído» a los 10 s, «Probar otra vez» y «Cancelar» suelta el micrófono.
- Groq probado desde el PC con la clave real y el prompt de la app: Whisper
  transcribe bien una orden con muletillas y el modelo acierta 5 de 5 órdenes,
  y 3 de 3 correcciones con la conversación previa, en menos de 1,2 s cada una.
- El asistente conversa (D-31): reabre el micrófono solo tras hablar, y el «sí»
  vale dicho.

## Sin probar

- **El asistente en el móvil con voz real**: los dos modos, aclarar, la casilla
  marcada, la conversación y las listas de `VoiceReplies`. En el emulador ya se
  vio el bucle entero con ruido (grabar, Groq, «no te he entendido», reabrir).
- Ajustes → Asistente de voz y la petición del micrófono (PIN del emulador
  desconocido; se encendió por `adb`). Los umbrales de `VoiceTuning` con una voz
  mayor. TalkBack.

## Siguiente

1. Probar con voz real, también con TalkBack. Cambiar la clave de Groq, que quedó
   expuesta en una sesión de trabajo.
2. CON-06: proteger la clave antes de publicar. Para Play, declarar en
   «Seguridad de los datos» que el audio se comparte con un tercero.
3. Auditoría de accesibilidad del 13/10, por decidir: etiqueta «Hablar» bajo el
   círculo (afecta a D-28), icono de Ajustes a 6,71:1 en claro, nombres de
   contacto truncados al 200 % y rejilla en mirilla con 4 contactos y aviso.
4. Al 200 % y dos columnas, «Mensajes» y «Contactos» se parten (pese a D-22).
5. Auditoría de seguridad del 30/09: la respuesta de recuperación en claro.
6. Sacar el `PackageManager` del hilo principal (riesgo de ANR en Play).

Deuda menor: sin pruebas automáticas, solo español, apps recomendadas duplicadas,
isotipo sin usar (D-28), capturas viejas, contrastes desfasados en `Color.kt`.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 13/10 | Javier | Asistente en modo conversación (D-31): micrófono que se reabre al acabar de hablar, «sí/no/siguiente/gracias» reconocidos en el móvil, memoria de seis turnos, tope de dos malentendidos, prompt que tutea |
| 13/10 | Javier | Auditoría de accesibilidad del asistente aplicada: TalkBack no se graba, «Repetir» con TalkBack, una sola región viva, señalado con relleno, pantalla encendida |
| 13/10 | Javier | Asistente de voz: Groq (Whisper + gpt-oss), grabación tolerante a pausas, confirmación siempre, modo guiar, ajustes CU-15, Ajustes a la esquina. ERS 0.6, D-25 a D-30, norma `asistente.md` |
