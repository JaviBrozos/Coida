# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 18/10/2026 · Javier Brozos**

## Dónde estamos

- Launcher completo: inicio y siete pantallas de ajustes tras el PIN, con límite
  de intentos (RNF-SEG-03) y recuperación del PIN (D-23).
- Asistente de voz en pruebas (CU-04, CU-05, CU-15; D-25 a D-31): Groq, siempre
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
   círculo (afecta a D-28), icono de Ajustes a 6,71:1 en claro, rejilla en mirilla
   con 4 contactos y aviso. Nombres de contacto al 200 %: mejor sin la inicial,
   pero con dos por fila siguen justos.
4. Al 200 % y dos columnas, «Mensajes» y «Contactos» se parten, ahora por sílabas
   con guion («Men-sajes»). Se leen, pero siguen en dos líneas.
5. Auditoría de seguridad del 30/09: la respuesta de recuperación en claro.
6. Sacar el `PackageManager` del hilo principal (riesgo de ANR en Play).

Deuda menor: sin pruebas automáticas, solo español, isotipo sin usar (D-28),
capturas viejas y contrastes desfasados en `Color.kt`.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 18/10 | Javier | Fuera las apps «Recomendadas» de Ajustes → Aplicaciones (D-33, CU-13 retirado, ERS 0.7): las instaladas salen en «Otras apps» |
| 18/10 | Javier | Interfaz más profesional (D-32) con la skill `apple-design` en el proyecto y sus límites en `ui-compose.md`; el PIN pinta la cuarta cifra y comprueba fuera del hilo de la pantalla; teclado del PIN más pequeño y centrado; nombres partidos por sílabas |
| 13/10 | Javier | Propuesta de CON-06 para hablar con Gonzalo: intermediario con suscripción, costes y obligaciones (`docs/propuestas/`) |
| 13/10 | Javier | Asistente en modo conversación (D-31): micrófono que se reabre al acabar de hablar, «sí/no/siguiente/gracias» reconocidos en el móvil, memoria de seis turnos, tope de dos malentendidos, prompt que tutea |
