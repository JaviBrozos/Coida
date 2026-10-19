# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 19/10/2026 · Javier Brozos**

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
7. Ver en el emulador los iconos nuevos y sin plato (D-34, D-35), en los dos
   temas y con una casilla señalada por el asistente.
8. Ver el icono nuevo (D-36) en el emulador con varias máscaras y como icono
   temático. Decidir con Gonzalo la letra Atkinson Hyperlegible Next
   (`docs/propuestas/marca.md`), pendiente.

Deuda menor: sin pruebas automáticas, solo español, isotipo sin usar (D-28) y
capturas viejas.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 19/10 | Javier | Logo: la D con visto de siempre, con esquinas redondeadas y visto nuevo en hueco (D-36); icono adaptativo sin cuadrado dentro, monocroma con visto y PNG de mdpi a xxxhdpi. Fuera las marcas B y C de la propuesta |
| 19/10 | Javier | Auditoría de la identidad visual (agente `auditor-marca`, skill `brand-identity`): 19 iconos redibujados (D-34) y sin plato en casillas y filas (D-35); contrastes corregidos en `Color.kt` y la documentación; logo y letra nuevos, pendientes, en `docs/propuestas/marca.md` |
| 18/10 | Javier | Fuera las apps «Recomendadas» de Ajustes → Aplicaciones (D-33, CU-13 retirado, ERS 0.7): las instaladas salen en «Otras apps» |
