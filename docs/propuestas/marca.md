# Propuesta: identidad visual de Doado

> Estado: **pendiente**. De la auditoría ya están aplicados los iconos (D-34 y
> D-35) y las cifras de contraste corregidas. El logo y la letra esperan a que
> decidan los dos creadores. Javier prefiere el logo «Botón» (B) y la letra
> Atkinson Hyperlegible Next para la marca y la app.
>
> Auditoría del 18/10/2026, hecha con el agente `.claude/agents/auditor-marca.md`
> y la skill `.claude/skills/brand-identity/`. Todo el material listo para
> aplicar está en `design/propuestas-marca/`. Los lienzos con las opciones, el
> sistema, los iconos y las animaciones están en un artefacto privado de Claude
> de Javier («Doado — identidad visual»). Para que Gonzalo lo vea, hay que
> compartirlo.

## Lo que encontró la auditoría

1. **El icono adaptativo lleva otro icono dentro.** La capa delantera es el icono
   entero (cuadrado marino incluido) con un inset del 17 %, y es el mismo fichero
   que el icono heredado y que `doado_icono.png`. No se nota porque los dos
   marinos separan a 1,02:1, pero aparece con otro fondo o con el paralaje. La D
   solo ocupa 40 × 44 dp de los 66 de la zona segura. **Sigue sin arreglar**: se
   arregla al aplicar cualquier logo de esta propuesta.
2. **Dos juegos de colores de marca.** El logo usa `#0B1F33` y `#29B6F6`, y el
   código usa `#0B1D2E` y `#29B6F0`.
3. **El visto desaparece en pequeño.** Mide el 4 % del alto de la D, así que a
   48 px queda en 1,2 px. La capa monocroma ni lo tiene.
4. **Cuatro tipografías.** Logo en Baloo 2 y Quicksand (sin convertir a trazos),
   app en la del sistema, pantallas de `design/` en IBM Plex Sans y presentación
   en Segoe UI.
5. **Cifras de contraste desfasadas.** Ya corregidas en `Color.kt`,
   `diseno.md`, `CLAUDE.md` y el README.

## Logo

| | Idea | A favor | En contra |
| --- | --- | --- | --- |
| **A. La D de siempre** | La D con el visto, afinada: visto en hueco, el doble de grueso | Continuidad | Un visto es un recurso muy visto |
| **B. Botón** (preferida) | D con un hueco redondo: el círculo del asistente y las «o» de «Doado» | La más legible a 16 px; sobria | Pierde el «hecho» del visto |
| **C. Diálogo** | La D como bocadillo, con el visto | Cuenta el asistente de voz | Se confunde con las apps de mensajes |

**Construcción de B** (rejilla de 64): la D mide 48 × 52 desde (8, 6), con una
recta de 22, medio círculo de radio 26 y las esquinas de la recta redondeadas
con radio 5. El hueco es un círculo de radio 10,5 con centro en (31,5; 32), un
hueco de verdad (`evenOdd`). En el icono adaptativo, la capa delantera es solo la
D, de 48 dp de alto, 2,45 dp a la derecha del centro (donde cae su peso), sin
inset ni cuadrado. La capa monocroma es la misma figura.

**Cómo se aplica B**: copiar `design/propuestas-marca/android/drawable/*` a
`res/drawable/`, `android/mipmap/anydpi-v26/ic_launcher.xml` a
`res/mipmap-anydpi-v26/`, y cada `android/mipmap/<densidad>/ic_launcher.png` a
`res/mipmap-<densidad>/`. Después, borrar `mipmap-xxxhdpi/ic_launcher_foreground.png`
y sustituir `doado_logo.*` y `doado_icono.png` por los de la carpeta.

## Letra

**Atkinson Hyperlegible Next**, del Braille Institute, pensada para lectores con
poca vista y con letras que no se confunden (I, l, 1; 0 y O). Licencia OFL.
Serviría para el logotipo (Bold, convertido a trazos) y para toda la app:
**SemiBold** en cuerpo y etiquetas y **Bold** en titulares. Sustituiría a D-17.

En `design/propuestas-marca/fuente/` hay cuatro grosores estáticos (Regular,
Medium, SemiBold y Bold), recortados al alfabeto latino, unos 150 KB en total, y
la licencia. Para aplicarla:

1. Copiar los `.ttf` a `res/font/`.
2. En `Type.kt`, declarar una `FontFamily` con los cuatro `Font(R.font.…)` y
   usarla en `doadoStyle` en lugar de `FontFamily.Default`.
3. Pasar el cuerpo de Medium a SemiBold.
4. Pasar el agente de accesibilidad con letra al 200 %: es más ancha que Roboto
   y puede partir antes «Mensajes» y los nombres.

## Movimiento de marca (para el logo B)

Para vídeos, redes y presentaciones. En la app solo entraría la Pulsación, como
splash de Android 12 y siguientes, y siempre sin rebotes (D-32).

| Nombre | Qué pasa | Duración | Para qué |
| --- | --- | --- | --- |
| Pulsación | La D se hunde un 6 % y el hueco se abre desde el centro | 0,9 s | Firma: cierre de cada vídeo, splash |
| Construcción | Se dibuja el contorno de la D, se rellena y se abre el hueco | 1,6 s | Entrada de vídeos y presentaciones |
| Escucha | Del hueco salen dos ondas que se desvanecen | 1,6 s en bucle | Vídeos del asistente de voz |
| Del botón al nombre | La D se aparta, sale «Doado» y las dos «o» crecen como el hueco | 2 s | Cabecera de LinkedIn, final con lema |
| Entrar por el botón | El hueco crece hasta llenar la pantalla y por él se entra en la app | 1,2 s | Transición a imágenes de la app |

Reglas:

- **Curvas**: las entradas, con salida suave (`cubic-bezier(0.2, 0, 0, 1)`); los
  bucles, con `ease-in-out`.
- **Rebote**: como mucho un 4 %, y solo en vídeo.
- **Colores**: solo los de marca.
- **Sin animaciones**: siempre hay una versión quieta para quien las tiene
  quitadas.
- **Formatos**: 1080 × 1080, 1080 × 1920 y 1920 × 1080, con la marca dentro del
  80 % central.
- **Entrega**: Lottie para web y redes; la Pulsación, como vector animado de
  Android.

## Pendiente, sea cual sea la elección

- Rehacer el isotipo de las ocho láminas de `design/presentacion/` y pasar la
  presentación de Segoe UI a una pila que exista en todas partes.
- Los colores sueltos de la presentación (`#3C5A70`, `#C9DCE8`…) y los estados
  que se distinguen solo por el color de una bolita (`EstadoLimites`).
