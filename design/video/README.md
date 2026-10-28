# Vídeo de Doado

Pieza de 53,2 segundos a 1920 × 1080 y 60 fps, con música y efectos propios, para
presentar Doado. El guion está escrito en 18 s de escena y el vídeo lo reproduce a
media velocidad, con pausas donde hay texto que leer (al menos 1 s más 1 s por cada
15 caracteres). Sigue la estructura de un anuncio de producto musical (una idea
por segundo, un objeto protagonista que pasa de plano en plano y una barra fija
abajo), pero con el contenido, los textos y la marca de Doado.

El resultado está en `out/`:

| Fichero | Qué es |
| --- | --- |
| `doado-video.mp4` | El vídeo: H.264 High 4.2 a 10 Mb/s, BT.709, 60 fps exactos; AAC estéreo a 48 kHz |
| `doado-cartel.png` | La versión quieta (la marca la pide siempre): el último fotograma |
| `musica.wav` | La música y los efectos, sin comprimir, para montarla aparte |
| `hoja-final.png`, `densa_*.png` | Hojas de contacto del render, para revisar |
| `stats.json` | Datos del último render |

## Qué cuenta, segundo a segundo

| s | Plano |
| --- | --- |
| 0–1,2 | La D llega al centro y su visto se dibuja, como un hueco |
| 1,2–5,0 | Firma pequeña y «Una app / para tus mayores» |
| 5,0–6,8 | Sube el inicio de Doado, con una inclinación que se endereza |
| 6,8–10,0 | La cámara se acerca a «Llamar», «Mensajes» y «Cámara»: «Lo de cada día» |
| 10,0–13,6 | «Tus contactos»: cuatro filas que entran escalonadas |
| 13,6–18,4 | «A su medida»: una tira de inicios distintos (claro, oscuro, letra grande, 1, 2 o 3 casillas por fila, casilla señalada) |
| 18,4–21,4 | «Te escucho»: el asistente oye «Llama a María» (por palabras, no se escribe) y sale su casilla |
| 21,4–24,2 | La casilla de María crece hasta el centro |
| 24,2–28,2 | Se aparta a la izquierda: «¿Llamo a María?» y los botones «Sí, llama» y «No» |
| 28,2–30,4 | El escenario se llena de cian; la firma oscura dibuja su visto (el «hecho») |
| 30,4–33,4 | Vuelve lo oscuro y la casilla viene hacia la cámara, con desenfoque |
| 33,4–37,2 | «Que el móvil / vuelva a ser suyo» |
| 37,2–42,2 | «Tú lo configuras una vez»: tres fundas de ajustes se abren en seis |
| 42,2–45,2 | Cuatro casillas flotan con perspectiva y profundidad |
| 45,2–47,6 | Convergen, se pliegan detrás de la D y la D sale del mismo punto |
| 47,6–53,2 | Firma grande y «Una app para tus mayores. / Fácil, cada día.» |

La barra de abajo es el asistente de voz: en Doado el círculo del asistente está
siempre abajo, en el inicio.

## Cómo está hecho

Todo se pinta en un canvas y es **función del tiempo absoluto**: `await
window.seek(t)` deja en el lienzo el fotograma del instante `t`, sin temporizadores
ni estado que se acumule. Cualquier fotograma sale igual pidiéndolo en cualquier
orden.

| Fichero | Qué hace |
| --- | --- |
| `src/base.js` | Medidas, colores (los de `Color.kt`), curvas, letra, iconos, la D y el fondo |
| `src/piezas.js` | Casilla, inicio, barra del asistente, campo, botón, fundas… en dp, con `DoadoDimens` |
| `src/escenas.js` | **El guion**: los tiempos (`D.T`), los textos, cada escena y el mapa de tiempo (`WARP`: velocidad y pausas de lectura) |
| `src/gl.js` | Perspectiva de verdad con WebGL2, solo donde hace falta |
| `src/audio.js` | La música y los efectos, sintetizados con Web Audio |
| `src/acum.js` | Desenfoque de movimiento (media de subfotogramas) y paso a YUV BT.709 |
| `src/mp4.js` | El empaquetador MP4, escrito a mano |
| `src/render.js` | Lo que hace `render.html`: hojas, tramos, análisis del audio y el render final |
| `src/verificar.js` | Comprueba el MP4 ya hecho, descodificándolo |
| `src/iconos.js` | Los iconos de la app, copiados de `res/drawable` (generado) |
| `src/fuentes.js` | Inter con `ss04` horneado, en base64 (generado) |

### La letra

La marca pide Inter con `ss04` (D-50). El canvas no deja activar rasgos OpenType,
así que `tools/hornear_ss04.py` reescribe el `cmap` de una copia de la Inter de la
app para que la I y la l (y sus variantes con tilde) apunten a las alternativas de
`ss04`. Las copias están en `fonts/`, con su licencia OFL.

## Mirarlo y cambiarlo

Abre `index.html` con doble clic: es el mismo código, pintado en tiempo real, con
sonido, barra de tiempo y paso de fotograma en fotograma (flechas y espacio).

- **Textos**: en `src/escenas.js`, en cada escena.
- **Tiempos**: el objeto `D.T` al principio de `src/escenas.js`, en tiempo de
  escena. **Velocidad y pausas**: `WARP` justo debajo (`scale` y `holds`). El audio
  pasa por el mismo mapa, así que la música y los efectos se mueven con la imagen.
- **Música**: el arreglo, al final de `D.renderAudio` en `src/audio.js`.

Si cambias los iconos de la app o la fuente:

```bash
py tools/extraer_iconos.py ../../app/src/main/res/drawable src/iconos.js
py tools/hornear_ss04.py ../../app/src/main/res/font/inter_bold.ttf fonts/inter-bold-ss04.ttf
py tools/hornear_ss04.py ../../app/src/main/res/font/inter_semibold.ttf fonts/inter-semibold-ss04.ttf
py tools/empaquetar_fuentes.py
```

## Renderizar

Solo hace falta Chrome y Python (biblioteca estándar). Desde `design/video`, el
render del MP4 de `out/` (con la GPU y el codificador H.264 de la tarjeta, a
10 Mb/s; tarda unos tres minutos):

```bash
py tools/servidor.py --gpu --pagina render.html --salida out --consulta "modo=final&sub=5&obturador=0.5&hw=prefer-hardware&bitrate=10000000&nombre=doado-video.mp4"
```

Sin `--gpu` ni `hw=prefer-hardware` se pinta y codifica por software: sale igual
de diseño, pero el codificador de software se queda en unos 4 Mb/s y deja bloques
en las escenas oscuras. Cinco subfotogramas por fotograma en un obturador de medio
fotograma; el zoom lleva seis muestras más dentro de cada subfotograma. Para
revisar sin codificar:

```bash
py tools/servidor.py --pagina render.html --salida out/revision --consulta "modo=hojas&paso=0.25&lint=1"
py tools/servidor.py --pagina render.html --salida out/revision --consulta "modo=tramo&desde=8.4&hasta=9.8&sub=5"
py tools/servidor.py --pagina render.html --salida out/revision --consulta "modo=audio"
```

Con `lint=1` comprueba en todos los fotogramas que ningún texto pisa a otro.

## Comprobar el MP4

```bash
py tools/mp4info.py out/doado-video.mp4
py tools/servidor.py --pagina verificar.html --salida out/revision --consulta "mp4=out/doado-video.mp4"
```

El primero lee las cajas del fichero (duración, 3192 muestras igual de espaciadas,
perfil, color, lista de edición del audio). El segundo lo abre como un reproductor:
lo descodifica entero, cuenta los fotogramas y mide el audio.
