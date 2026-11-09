# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 30/10/2026 · Javier Brozos**

## Dónde estamos

- Launcher completo: inicio y ocho pantallas de ajustes tras el PIN, con límite
  de intentos (RNF-SEG-03) y recuperación del PIN (D-23).
- Probado el 28/10 con dos personas mayores. Salieron D-54 a D-59: casillas
  cuadradas de 1 a 4 por fila, un tamaño de letra menos, fotos de la agenda,
  «Contactos favoritos», asistente sin «¿Llamo a…?» y casillas que coloca el familiar.
- Asistente de voz en pruebas (CU-04, CU-05, CU-15; D-25, D-37, D-56, D-58).
- Casilla «Emergencias 112» (CU-17, D-51), apagada de fábrica.
- Web en `web/` (D-52, D-60, D-61), al día con la 1.4. El vídeo de marca (`design/video/`,
  D-53) aún enseña el asistente con «¿Llamo a María?» y casillas estiradas.

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
- La web en un móvil real, en Safari y en Firefox, y con TalkBack y VoiceOver.
- Una foto de fondo real de cámara (girada) y el selector de documentos de
  Android 9 o antes. El vídeo (D-53), escuchado con altavoces.

## Siguiente

1. Instalar la 1.4 en los dos móviles de la prueba y repetirla.
2. Cambiar la clave de Groq, que quedó expuesta en una sesión de trabajo.
3. CON-06 con Gonzalo. Sin resolverlo no se publica. Para Google Play, contar
   también el permiso de contactos en la política de privacidad (D-57).
4. Poner al día el vídeo con D-54 a D-59. La web se publica copiándola a
   `JaviBrozos/doado-web`; Cloudflare la sirve en `doadoapp.com`. Decidir el nombre: hay
   otra app Doado, francesa y de salud de la espalda, y falta mirar la marca. De la
   web, decidir lista de espera y si habrá precio.
5. Auditoría del 13/10, por decidir: etiqueta «Hablar» bajo el círculo (afecta a
   D-28) e icono de Ajustes a 6,71:1 en claro.
6. Ver el icono nuevo (D-36) con varias máscaras y pasar el texto del logo a Inter
   Bold en trazos (D-50).

Deuda menor: sin pruebas automáticas, solo español, isotipo sin usar (D-28) y
capturas viejas.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 30/10 | Javier | Web otra vez continua (D-61): el menú salta a cada sección, «Inicio» también en escritorio, mockup con la app encajada, hero anclado con tres pantallas al deslizar, vuelven las animaciones del principio y textos en neutro. En la barra, solo el isotipo y en el centro; el móvil del hero sigue el deslizamiento con inercia. Publicada en `doadoapp.com` |
| 30/10 | Javier | Web rehecha (D-60): siete secciones con cabecera de desplegables y menú de tres rayas, móvil en 3D en el hero, móviles anclados, entrada del logo, capturas nuevas de la 1.4 en un marco que no las recorta, pestaña «Doado» con icono y textos al día (D-54 a D-59). Fuera el panel de personalización, la pausa, la cinta, el comando de `adb` y toda mención al precio. Revisada con 16 tamaños de ventana y móvil en Chrome. Publicada en `doadoapp.com` |
| 29/10 | Javier | Web publicada en Cloudflare Pages desde `doado-web`, en `doadoapp.com` Repaso móvil de `web/` de 320 a 941 px, en apaisado, con letra al 200 % y con colores forzados: medidas en las capturas, anclas que ya no quedan cortas, nombres que no se salen a 3 por fila, comparador que no se mueve al desplazar, pantallas del móvil que abren arriba, `theme-color` y contraste con colores forzados. Lo publicado aún es la versión anterior |
| 28/10 | Javier | Asistente: el micrófono se abre antes. El pitido suena ya grabando y se tira lo que coincide con él, y su generador se crea una vez. Mientras escucha, en vez de la barra de volumen, el micrófono en grande late con la voz y suelta anillos, en degradado de la marca. Visto en el emulador, con la voz del ordenador por los altavoces |
| 28/10 | Javier | Lo que salió de la prueba con dos personas mayores (D-54 a D-59, ERS 0.11). Casillas siempre cuadradas, de 1 a 4 por fila y ofrecidas en cualquier pantalla: era lo que en el Samsung solo dejaba poner dos. El nombre se encoge sin partir palabras, con la misma letra en toda la rejilla, y lo apagado lleva contorno a trazos. Letra con un nivel más pequeño («Normal», 0,85), migrando la clave para que nadie cambie de tamaño. «Contactos favoritos» y la foto de la agenda (`READ_CONTACTS`, solo para los guardados). Asistente: dice «Llamo a María» y llama, con «Cancelar»; solo vuelve a escuchar para pedir que le repitan; final de frase medido contra la voz (1,2 s, tope de 10 s); de fábrica, «Lo hace». Pantalla nueva «Colocar las casillas», con arrastre y botones. Arreglos de la auditoría de accesibilidad. Probado en el emulador a 411 y 320 dp |
