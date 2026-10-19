# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 19/10/2026 · Javier Brozos**

## Dónde estamos

- Launcher completo: inicio y siete pantallas de ajustes tras el PIN, con límite
  de intentos (RNF-SEG-03) y recuperación del PIN (D-23).
- Asistente de voz en pruebas (CU-04, CU-05, CU-15; D-25 a D-31, D-37): Groq, siempre
  confirma, actúa o guía, y conversa. Groq acierta 5 de 5 órdenes y 3 de 3
  correcciones desde el PC; en el emulador se vio el bucle entero.
- Interfaz renovada (D-32): grupos en ajustes, platos de icono, selector
  segmentado, «Volver» escrito, fundido y vibración al tocar. Vista en el emulador
  en los dos temas y auditada en tres rondas por el agente de accesibilidad.

## Sin probar

- **El asistente en el móvil con voz real**: los dos modos, aclarar, la casilla
  marcada, la conversación y las listas de `VoiceReplies`. En el emulador ya se
  vio el bucle entero con ruido (grabar, Groq, «no te he entendido», reabrir).
- La petición del micrófono desde Ajustes, los umbrales de `VoiceTuning` con una
  voz mayor, y TalkBack con la interfaz nueva.

## Siguiente

1. Probar con voz real, también con TalkBack. Cambiar la clave de Groq, que quedó
   expuesta en una sesión de trabajo.
2. CON-06: decidir con Gonzalo sobre `docs/propuestas/con-06-clave-asistente.md`
   (intermediario con suscripción). Sin resolverlo no se publica.
3. Auditoría de accesibilidad del 13/10, por decidir: etiqueta «Hablar» bajo el
   círculo (afecta a D-28) e icono de Ajustes a 6,71:1 en claro. La rejilla en
   mirilla ya no aplica (D-38).
4. Al 200 % y dos columnas, «Mensajes» y «Contactos» se parten, ahora por sílabas
   con guion («Men-sajes»). Se leen, pero siguen en dos líneas.
5. Sacar el `PackageManager` del hilo principal (riesgo de ANR en Play).
6. Ver en el emulador los iconos nuevos y sin plato (D-34, D-35), en los dos
   temas y con una casilla señalada por el asistente.
7. Ver el icono nuevo (D-36) en el emulador con varias máscaras y como icono
   temático. Decidir con Gonzalo la letra Atkinson Hyperlegible Next
   (`docs/propuestas/marca.md`), pendiente.

Deuda menor: sin pruebas automáticas, solo español, isotipo sin usar (D-28) y
capturas viejas.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 19/10 | Javier | Fusión con lo de Gonzalo del 05/10 en `main`: su respuesta de recuperación derivada (D-41, era su D-24) convive con el límite de intentos, y su `DoadoTextField` sustituye a los campos de Material en las tres pantallas de recuperación |
| 19/10 | Javier | Skill `doado-brand` en el proyecto: la marca (logo, colores, iconos, letra, tono y movimiento), con los SVG de la marca y `tokens.json`; enlazada en `CLAUDE.md` y en `recursos.md` |
| 19/10 | Javier | Contactos con dos interruptores, llamada y WhatsApp, que se pueden encender a la vez (D-40). Fuera el subtítulo de Ajustes; notas de Aplicaciones y Modo administrador más genéricas, sin nombrar apps. TalkBack lee cada interruptor con el nombre de la persona |
| 19/10 | Javier | El botón de Ajustes del inicio pasa a engranaje (D-39); los deslizadores, ahora `ic_sliders`, quedan para la fila Interfaz |
| 19/10 | Javier | Inicio en una sola rejilla (D-38, ERS 0.8): contactos como casillas tras «Tus contactos» y sin límite; círculo del asistente flotando encima, con aro; reloj y fecha más pequeños. Con la auditoría de accesibilidad: filas enteras sobre el círculo, Inicio vuelve arriba, nombres hasta 4 líneas, vibración en el círculo y fecha a 7:1. Visto en el emulador en claro con 5 contactos |
