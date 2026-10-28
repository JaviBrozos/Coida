# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 28/10/2026 · Javier Brozos**

## Dónde estamos

- Launcher completo: inicio y ocho pantallas de ajustes tras el PIN, con límite
  de intentos (RNF-SEG-03) y recuperación del PIN (D-23).
- Probado el 28/10 con dos personas mayores. Salieron D-54 a D-59: casillas
  cuadradas de 1 a 4 por fila, un tamaño de letra menos, fotos de la agenda,
  «Contactos favoritos», asistente sin «¿Llamo a…?» y casillas que coloca el familiar.
- Asistente de voz en pruebas (CU-04, CU-05, CU-15; D-25, D-37, D-56, D-58).
- Casilla «Emergencias 112» (CU-17, D-51), apagada de fábrica.
- Web en `web/` (D-52) y vídeo de marca en `design/video/` (D-53). Aún enseñan el
  asistente con «¿Llamo a María?» y casillas estiradas.

## Sin probar

- **El asistente con voz real y ruido de casa** (D-56, D-58): que corte al acabar
  la frase, el «Cancelar» a tiempo, el «repítemelo» y TalkBack. En el emulador solo
  se ha visto el camino sin voz.
- **El Samsung del abuelo**: que salgan las cuatro opciones de casillas por fila.
  Simulado a 320 dp en el emulador, con 1 a 4 por fila.
- Las fotos de los contactos con contactos que tengan foto, y el permiso denegado.
- «Colocar las casillas» con TalkBack. El arrastre y los botones, probados.
- Lo último de las revisiones (contorno a trazos de lo apagado, pestañas en dos
  líneas, inicio con el móvil girado, barra de «Colocar» con letra grande): compila
  y pasa lint, pero no se ha visto, porque el emulador se cerró por falta de memoria.
- Con la misma persona en dos contactos de la agenda que comparten número, la foto
  puede salir de la otra (se busca por número).
- Una foto de fondo real de cámara (girada) y el selector de documentos de
  Android 9 o antes. El vídeo (D-53), escuchado con altavoces.

## Siguiente

1. Instalar la 1.4 en los dos móviles de la prueba y repetirla.
2. Cambiar la clave de Groq, que quedó expuesta en una sesión de trabajo.
3. CON-06 con Gonzalo. Sin resolverlo no se publica. Para Google Play, contar
   también el permiso de contactos en la política de privacidad (D-57).
4. Poner al día la web (copia a mano en `JaviBrozos/doado-web`), el vídeo y las
   capturas con D-54 a D-59. De la web, decidir lista de espera y hosting.
5. Auditoría del 13/10, por decidir: etiqueta «Hablar» bajo el círculo (afecta a
   D-28) e icono de Ajustes a 6,71:1 en claro.
6. Ver el icono nuevo (D-36) con varias máscaras y pasar el texto del logo a Inter
   Bold en trazos (D-50).

Deuda menor: sin pruebas automáticas, solo español, isotipo sin usar (D-28) y
capturas viejas.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 28/10 | Javier | Asistente: el micrófono se abre antes. El pitido suena ya grabando y se tira lo que coincide con él, y su generador se crea una vez. Mientras escucha, en vez de la barra de volumen, el micrófono en grande late con la voz y suelta anillos, en degradado de la marca. Visto en el emulador, con la voz del ordenador por los altavoces |
| 28/10 | Javier | Lo que salió de la prueba con dos personas mayores (D-54 a D-59, ERS 0.11). Casillas siempre cuadradas, de 1 a 4 por fila y ofrecidas en cualquier pantalla: era lo que en el Samsung solo dejaba poner dos. El nombre se encoge sin partir palabras, con la misma letra en toda la rejilla, y lo apagado lleva contorno a trazos. Letra con un nivel más pequeño («Normal», 0,85), migrando la clave para que nadie cambie de tamaño. «Contactos favoritos» y la foto de la agenda (`READ_CONTACTS`, solo para los guardados). Asistente: dice «Llamo a María» y llama, con «Cancelar»; solo vuelve a escuchar para pedir que le repitan; final de frase medido contra la voz (1,2 s, tope de 10 s); de fábrica, «Lo hace». Pantalla nueva «Colocar las casillas», con arrastre y botones. Arreglos de la auditoría de accesibilidad. Probado en el emulador a 411 y 320 dp |
| 22/10 | Javier | Vídeo: a media velocidad y con pausas de lectura (1 s más 1 s por cada 15 caracteres), 53,2 s en total; música rehecha para esa duración. Por la auditoría de marca: textos secundarios a 32–34 px, fundas abiertas a 200 px, botón «No» junto a «Sí, llama», casilla señalada sin aro, lo oído aparece por palabras sin cursor, «Sus contactos» y «Tu PIN», fecha bien (miércoles), escenario con borde opaco y sin sombra. Salida `out/doado-video.mp4`, 10 Mb/s |
| 22/10 | Javier | Vídeo de marca de 18 s en `design/video/` (D-53): 1920 × 1080 a 60 fps, H.264 y AAC, con el guion del encargo y el contenido de Doado (inicio, contactos, «A su medida», el asistente con «¿Llamo a María?», fundas de ajustes y la firma con su visto). Música y efectos propios a 120 BPM. Se renderiza en Chrome sin interfaz, sin instalar nada. Salida en `design/video/out/` |
