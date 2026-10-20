# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 20/10/2026 · Javier Brozos**

## Dónde estamos

- Launcher completo: inicio y siete pantallas de ajustes tras el PIN, con límite
  de intentos (RNF-SEG-03) y recuperación del PIN (D-23).
- Asistente de voz en pruebas (CU-04, CU-05, CU-15; D-25 a D-31, D-37, D-42).
- Inicio personalizable (D-45 a D-48): una, dos o tres casillas por fila,
  contactos aparte, foto de fondo y aviso si Doado no es la pantalla de inicio.
- Letra: Inter en prueba, en lugar de Atkinson (D-44); falta decidirlo con Gonzalo.

## Sin probar

- **El asistente con voz real**: los dos modos, aclarar, la casilla marcada, la
  conversación, el «no» seco (D-42) y TalkBack con la interfaz nueva.
- TalkBack con tres por fila, las pestañas y el aviso de inicio. Visto al 100 % y
  al 200 %, en claro y en un móvil de 411 dp; falta uno de 360 dp y el tema oscuro.
- La guía del asistente con tres por fila y con los contactos aparte.
- Una foto real de cámara (girada) y el selector de documentos de Android 9 o antes.

## Siguiente

1. Decidir con Gonzalo la letra: Inter (prueba de hoy) o Atkinson (D-44). Si se
   queda Inter, quitar Atkinson de `res/font/` y corregir la marca.
2. Probar con voz real, también con TalkBack. Cambiar la clave de Groq, que quedó
   expuesta en una sesión de trabajo.
3. CON-06: decidir con Gonzalo sobre `docs/propuestas/con-06-clave-asistente.md`.
   Sin resolverlo no se publica.
4. Auditoría del 13/10, por decidir: etiqueta «Hablar» bajo el círculo (afecta a
   D-28) e icono de Ajustes a 6,71:1 en claro.
5. Ver el icono nuevo (D-36) con varias máscaras; pasar el texto del logo a trazos.

Deuda menor: sin pruebas automáticas, solo español, isotipo sin usar (D-28) y
capturas viejas.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 20/10 | Javier | Pestaña «Contactos» (cabe con la letra grande). Los avisos de pantalla de inicio llevan una equis: al cerrarlos explican cómo ponerlo más adelante; el del inicio se queda cerrado y el de Ajustes vuelve en cada visita. Por la auditoría: nombres sin tope de líneas, I y l distintas en Inter (`ss04`), solo dos grosores de Inter, y desde el inicio en Android 9 o antes el botón va a Ajustes, tras el PIN |
| 20/10 | Javier | Inicio: tres por fila casi cuadradas (D-45), contactos en una segunda pantalla con pestañas (D-46), foto de fondo con franjas lisas (D-47) y aviso con botón si Doado no es la pantalla de inicio, en el inicio y en Ajustes (D-48). PIN: de ~0,9 s a ~0,12 s, con 10 000 vueltas y derivando durante la pausa (D-49). Interruptores «Llamadas» y «WhatsApp»; «Elegir pantalla de inicio». Letra Inter en prueba; titulares a 1,3 de interlineado, cifras de ancho fijo y guiones en español. ERS 0.9. Visto en el emulador |
| 20/10 | Javier | Fusión en `main` de las ramas de Gonzalo `letra-atkinson` y `package-manager-fondo` |
| 20/10 | Gonzalo | Letra Atkinson Hyperlegible Next en la app (D-44, sustituye a D-17): cuatro grosores en `res/font/`, cuerpo en SemiBold |
| 20/10 | Gonzalo | Las consultas al `PackageManager` de `reload()` pasan a segundo plano (D-43): fuera el riesgo de ANR |
| 20/10 | Gonzalo | Un «no» seco tras «¿Llamo a …?» se descartaba por corto: al contestar basta con 200 ms de voz (D-42) |
| 20/10 | Javier | Versión 1.2 (`versionCode` 2): primera APK numerada. Las APK van a Descargas, no a git: llevan la clave del asistente |
