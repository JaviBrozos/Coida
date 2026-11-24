# Sistema visual

> **Fuente única de**: paleta, tipografía, medidas, iconografía y componentes
> compartidos. Enlaza desde otros documentos; no copies las cifras. Quien manda
> de verdad es `core/ui/`: si esto no cuadra con el código, **gana el código**.
>
> Última revisión: 11/11/2026.

El principio está en la ERS como requisitos medibles (RNF-ACC-01 a RNF-ACC-07).
Aquí está el cómo.

## Paleta

Dos colores de marca (marino y cian) y una escala de superficies construida
alrededor. Definidos en `core/ui/Color.kt`.

| Token | Hex | Para qué | Contraste |
| --- | --- | --- | --- |
| `CoidaNavy` | `#0B1D2E` | Marca. Texto y bordes en el tema claro | 17,08:1 sobre blanco |
| `CoidaCyan` | `#29B6F0` | Acento: iconos, estado activo, botón principal | 7,86:1 sobre tinta · **2,32:1 sobre blanco** |
| `CoidaCyanDeep` | `#10618F` | Texto secundario en el tema claro | 6,71:1 sobre blanco |
| `CoidaCyanLight` | `#6CCFFA` | Reservado para oscuro | 10,38:1 sobre tinta |
| `CoidaInk` | `#0A1622` | Fondo del tema oscuro | — |
| `CoidaSurface` | `#122A3E` | Casilla y fila elevadas | 13:1 con el texto |
| `CoidaSurfaceHigh` | `#17384F` | Pulsada, y cuadrito tras un icono | — |
| `CoidaLine` | `#3D7AA6` | Contorno de 2 dp y raya entre filas | 3,18:1 con la casilla · 3,94:1 sobre tinta |
| `CoidaText` | `#F2F7FB` | Texto principal en oscuro | 16:1 sobre tinta |
| `CoidaTextMuted` | `#9FC4DC` | Texto secundario en oscuro | 9,9:1 sobre tinta |
| `CoidaMist` / `CoidaMistDim` | `#E9F3FA` / `#DCEBF5` | Superficies suaves del tema claro | — |
| `CoidaSteelDeep` | `#54697A` | Contenido apagado **en claro** | 5,71:1 sobre blanco · 5,07:1 sobre Mist |
| `CoidaSteelLight` | `#A3B8C6` | Contenido apagado **en oscuro** | 7,17:1 sobre la casilla · 8,89:1 sobre tinta |

**La regla que explica la paleta**: el cian de marca no vale para texto ni iconos
sobre fondo claro. Por eso en el tema claro manda el marino, y el cian solo pinta
superficies grandes.

**Cómo se leen los contrastes de esta tabla.** Se miden contra el fondo **real**
sobre el que cae el texto, que casi siempre es la casilla y no el fondo de
pantalla. La distinción no es académica: el gris de apagado figuró aquí durante
meses como «5,7:1 sobre tinta», que era cierto y era el número equivocado —ese
texto cae sobre la casilla, donde daba 4,59:1 en oscuro y 3,21:1 en claro—. Un
token que sale en los dos temas necesita **una medida por tema**, no una.

## Roles de Material 3

Las pantallas nunca escriben un color: piden un rol. Así el mismo composable
funciona en los dos temas. Definido en `core/ui/Theme.kt`.

| Rol | Para qué | Oscuro (de fábrica) | Claro |
| --- | --- | --- | --- |
| `primary` | Iconos de acento, botón principal, estado activo | Cian | Marino |
| `onPrimary` | Lo que va encima de `primary` | Tinta | Blanco |
| `background` / `onBackground` | Fondo de pantalla y su texto | Tinta / Texto | Blanco / Marino |
| `surfaceContainer` | Fondo de casillas y filas | `CoidaSurface` | Blanco |
| `surfaceContainerHigh` | Pulsada, y relleno de las notas | `CoidaSurfaceHigh` | Mist |
| `surfaceContainerLowest` | Plato de las iniciales y del micrófono del asistente (cian 7,86:1, marino 15,18:1) | Tinta | Mist |
| `onSurface` | Texto sobre una casilla | Texto | Marino |
| `onSurfaceVariant` | Texto secundario | TextMuted | CyanDeep |
| `outline` | Contorno | `CoidaLine` | Marino |
| `outlineVariant` | Contenido apagado | `CoidaSteelLight` | `CoidaSteelDeep` |

`surfaceTint` es transparente en los dos: el diseño es plano y **separa por
borde, no por sombra ni por elevación**.

## Tipografía

`core/ui/Type.kt`: **Inter** (D-50). Dentro del APK en `res/font/`, solo los dos
grosores que usa la escala (SemiBold y Bold), y con el juego `ss04` (la I con
remates y la l con cola, que en Inter son dos palos casi iguales). La escala es
la de Material multiplicada por ~1,5, con más grosor (el trazo fino se pierde a
tamaño grande): **SemiBold** en cuerpo y etiquetas y **Bold** en titulares.
Interletrado a 0 (el negativo de Material apelmaza el texto cuando se amplía).
Todo en `sp`. Un grosor nuevo en la escala necesita su fichero en `res/font/`.
Titulares y `display` con 1,3 veces su tamaño de interlineado como mínimo, y el
cuerpo de 22 sp con 32 (1,45). El reloj, las cuentas atrás y
las teclas del PIN llevan cifras de ancho fijo (`TabularFigures`): si no, la hora
baila de lado a cada minuto.

| Estilo | Tamaño | Dónde se usa |
| --- | --- | --- |
| `displayMedium` | 52 sp | La cuenta atrás del modo admin |
| `headlineLarge` | 40 sp | El reloj del inicio (D-38) |
| `headlineMedium` | 34 sp | El nombre «Coida» cuando el reloj está apagado |
| `headlineSmall` | 30 sp | Las cifras del teclado del PIN |
| `titleLarge` | 30 sp | Título de una pantalla de ajustes |
| `titleSmall` | 22 sp | Texto de botón, nombre de un contacto |
| `bodyMedium` | 22 sp | Textos de cuerpo |
| `bodySmall` | 20 sp | Subtítulos, notas y la fecha del inicio |
| `labelLarge` | 24 sp | **La etiqueta de una casilla** y los títulos de fila |
| `labelMedium` | 20 sp | Rótulos de grupo, opciones y pestañas del inicio |
| `labelSmall` | 18 sp | El «No disponible» de una casilla apagada |

Estos tamaños son los de «Grande». La persona elige en Interfaz entre cuatro
niveles: «Normal» (0,85), «Grande» (1), «Muy grande» (1,15) y «Enorme» (1,3), que se
multiplican por la escala de Android, con la escala total entre 0,85 y 2 (D-55).
Con «Normal», `labelSmall` queda en unos 15 sp: el suelo de 18 sp ya no es
absoluto.

**Texto que no se parte** (D-54, `core/ui/FitText.kt`): en las casillas, las
pestañas del inicio y la frase del asistente, la letra baja de tamaño hasta que
cada palabra cabe en su línea, sin bajar de unos 8 dp en pantalla. Nunca se parte
una palabra ni se ponen guiones. En la rejilla, todas las casillas de un apartado
llevan la misma letra: la del nombre que menos cabe. En las apps no cuenta el «No
disponible», para que desinstalar una no encoja todo el inicio.

Hay estilos declarados que hoy no usa nadie (`displayLarge`, `bodyLarge`). Están
para cuando hagan falta; no los borres sin mirar.

## Medidas

`core/ui/CoidaDimens`. Nada de números sueltos en un composable.

| Token | Valor | Qué es |
| --- | --- | --- |
| `MinTouchTarget` | 48 dp | Suelo innegociable de cualquier pulsable |
| `ComfortableTouchTarget` | 72 dp | Lo normal para un botón suelto |
| `TileIconRatio` · `TileIconMin` / `TileIconMax` | 0,4 · 24 / 96 dp | Icono de la casilla cuadrada, sin plato (D-35), en proporción al lado (D-54) |
| `TilePaddingRatio` · `TilePaddingMin` / `TilePaddingMax` | 0,08 · 4 / 16 dp | Margen interior de la casilla cuadrada; entre icono y nombre, la mitad |
| `TileCornerRatio` · `TileCornerRadius` | 0,18 · 28 dp | Radio de casilla y de contacto: proporcional al lado, como mucho 28 dp |
| `TileRowHeight` / `TileIconSizeCompact` | 112 / 54 dp | Casilla de una por fila, en horizontal y de alto fijo, y su icono |
| `MissedBadgeMin` / `MissedBadgeMax` | 20 / 44 dp | Bola de llamadas perdidas de «Llamadas» (D-66): 0,6 del icono, entre los dos |
| `AvatarSize` | 52 dp | Foto o inicial de un contacto en Ajustes → Contactos |
| `CardCornerRadius` | 20 dp | Radio de fila, tarjeta y botón |
| `ControlCornerRadius` | 14 dp | Radio de control pequeño |
| `BorderWidth` | 2 dp | Grosor de contorno |
| `DividerWidth` | 2 dp | Raya entre las filas de un grupo de ajustes |
| `HighlightBorderWidth` | 4 dp | Contorno de la casilla o el contacto que señala el asistente al guiar (que además va relleno de acento) |
| `AssistantButtonSize` | 96 dp | Círculo del asistente en el inicio |
| `AssistantRingWidth` | 8 dp | Aro del color del fondo alrededor del círculo, que lo separa de las casillas que pasan por debajo |
| `CornerButtonSize` | 64 dp | Botón de Ajustes en la esquina del inicio |
| `BackButtonHeight` | 60 dp | Alto del botón «Volver» de las cabeceras |
| `PinKeyHeight` / `PinPadMaxWidth` | 64 / 320 dp | Tecla del PIN (alto mínimo) y ancho máximo del teclado, centrado |
| `AssistantBadgeSize` | 64 dp | Círculo hundido con el micrófono en la pantalla del asistente (no pulsable) |
| `ListeningPulseHeight` | 200 dp | Sitio del micrófono que late mientras el asistente escucha (`ListeningPulse`) |
| `LargeTextScale` | 1,3 | Escala de letra a partir de la que las opciones de un `ChoiceGroup` van en columna |
| `RowHeight` | 88 dp | Alto de fila de ajustes |
| `ScreenPadding` | 20 dp | Margen de pantalla |
| `GridSpacing` / `GridSpacingDense` | 16 / 10 dp | Separación de la rejilla; la segunda, con cuatro por fila |

**Deuda conocida**: `MinTouchTarget` está declarado y no se usa, y hay medidas de
layout sueltas en `HomeScreen` y en los componentes. Si tocas esas zonas, pásalas
al token.

## Marca

Una C con un visto que asoma por su boca (D-62; sustituye a la D de D-36). Tres
reglas: **el visto no toca la C**, o se lee como una «e»; **los colores son los de
`Color.kt`**; y el icono de la app lleva **solo la C**, sin un segundo cuadrado.

- **Construcción** (rejilla de 64), de trazo y no de relleno: la C es un anillo de
  radio 20 con centro en (35,5; 32), trazo de 9 y la boca abierta a la derecha
  (±60°), con puntas redondas. El visto pasa por (31,5; 33), (38,5; 40) y
  (52,5; 25), con trazo de 6,5 y puntas y codo redondos. Entre la punta del visto
  y el remate de arriba de la C quedan 4,7 unidades, y entre el codo del visto y el
  interior del anillo, 3,71: es el hueco más estrecho, y por eso la marca no baja de
  24 px de alto.
- **Icono adaptativo**: fondo `ic_launcher_background` (marino) y delante
  `drawable/ic_launcher_foreground.xml`, sin inset. La figura se escala a 0,98 (48 dp
  de alto en el lienzo de 108), centrada en el lienzo (la caja de la figura cae
  1,35 dp a la derecha por su propia forma, lo que equilibra la masa del anillo), y queda dentro de la zona segura de 66 (26 dp del centro como
  mucho). La capa monocroma es la misma figura.
  Para API 24-25, PNG de mdpi a xxxhdpi (48 a 192 px), y `publishing/icono-512.png`
  para Google Play. El cian sobre el marino da 7,36:1.
- **Logotipo** (`coida_logo.svg` y los de `.claude/skills/coida-brand/assets/`): la
  C y «Coida» en Inter Bold pasada a trazos (D-68). La C mide 1,25 veces la altura
  de la mayúscula, centrada con ella, y el hueco hasta la palabra es 0,27 del ancho
  de la C, medido en la punta del visto. Hay una versión marina para fondo claro y
  otra con la C en cian y el texto en `#F2F7FB` para fondo oscuro.
- **Piezas para fuera** (D-68): la cabecera del README (`docs/img/cabecera.png`),
  el gráfico destacado de Play y la imagen de LinkedIn (en `publishing/`). Llevan
  fondo tinta o marino, el lema en `#F2F7FB` y el texto secundario en `#9FC4DC`
  (9,91:1 sobre tinta).

## Iconografía

- **De trazo**, dibujados sobre rejilla de 24 con grosor 1,7 y puntas redondas.
  Mismo lenguaje en todos (D-34): zona viva de 20, radio 3 en los contenedores grandes
  (2,5 en los pequeños) y el círculo como motivo donde tiene sentido: objetivo de
  la cámara, cabeza, tiradores de los ajustes, cerradura, centro del engranaje y
  la cuarta casilla de «Aplicaciones». «Volver» es una flecha con asta, no un
  ángulo suelto.
  Rellenos escalados a 100 px se ven hinchados; el trazo aguanta cualquier
  tamaño y se recolorea con el tint.
- Se tiñen con un rol del tema, nunca con un color escrito. El de las casillas es
  `primary`, salvo la sirena de «Emergencias 112», que va en `error` (D-51).
- **Nunca emoji ni dingbats.**
- El **isotipo** tiene dos versiones porque el cian no llega a 3:1 sobre blanco:
  `ic_coida_mark` (C cian) para fondo oscuro y `ic_coida_mark_light` (C marina)
  para fondo claro. Es marca, no información:
  siempre `contentDescription = null`. Desde D-28 ya no sale en el inicio, donde
  está ahora el botón de Ajustes; los ficheros se conservan. Geometría en «Marca».
- **`ic_assistant` es un micrófono.** Se llama así, y no `ic_mic`, para que
  cambiarlo sea sustituir el fichero sin tocar código.

## El inicio

- **Ajustes** es un círculo de 64 dp arriba a la derecha, solo con un engranaje (D-39), contorno
  y tono apagado (`onSurfaceVariant`). Es del familiar: lejos del pulgar. Redondo
  como el del asistente: lo que flota fuera de la rejilla no parece una casilla.
- **Casillas**: el icono va directamente sobre la casilla, sin plato (D-35): solo
  su silueta dentro del contorno. La de una app que no está lleva el contorno **a
  trazos** en `outlineVariant`: con cuatro por fila el «No disponible» es pequeño,
  y en gris el color solo no la distinguía. Vale igual para los iconos de trazo y para los
  de las apps. En la casilla que señala el asistente, el icono va en `onPrimary`.
- **Contactos**: casillas como las de las apps, en la misma rejilla y tras el
  rótulo «Contactos favoritos» (D-38). En el sitio del icono va la foto de la
  agenda recortada en círculo y con el contorno de siempre (D-57) o, si no hay,
  la inicial en su círculo. Debajo, el nombre y la acción escrita. Sin límite de
  número.
  Si el familiar los pone aparte (D-46), van en una segunda pantalla del inicio,
  con dos pestañas arriba, «Aplicaciones» y «Contactos favoritos»: la elegida en
  `primary`, la otra con contorno. Las dos miden 72 dp de alto y llevan la misma
  letra, en una línea.
- **Emergencias 112** (D-51): una casilla como las demás, con la sirena
  (`ic_emergency`) en `error` y el nombre en `onSurface`. Se distingue por el
  icono y el texto, no solo por el color. Apagada de fábrica.
- **Llamadas perdidas** (D-66, solo en debug y familia): en la casilla «Llamadas»,
  una bola `error` con la cifra en `onError` (6,54:1 en claro, 7,72:1 en oscuro;
  contra la casilla, 6,54:1 y 8,67:1) y un aro `surfaceContainer`, arriba a la
  derecha del icono. Mide 0,6 del icono, entre `MissedBadgeMin` y `MissedBadgeMax`;
  desde 10, «9+». Debajo del nombre pone «2 perdidas». En oscuro, el rojo claro: el
  oscuro se queda a 1,6:1 contra la casilla.
- **Casillas por fila**: de una a cuatro, siempre ofrecidas (D-54). Con dos o
  más, **cuadradas**, todas iguales y sin estirarse nunca: el icono, los márgenes
  y el radio van en proporción al lado, y el nombre (`labelLarge`) baja lo que
  haga falta para caber entero. Con una por fila, filas de 112 dp con el icono a
  la izquierda.
- **Orden**: el que elija el familiar en «Colocar las casillas» (D-59). La casilla
  que se arrastra va rellena de `primary`, como la señalada.
- **Aviso de pantalla de inicio** (D-48): si Coida no lo es, `NotHomeNotice` arriba,
  con un `PrimaryButton` que lo pone y una equis redonda de 64 dp (`ic_close`).
- **Foto de fondo** (D-47): detrás de todo y decorativa, dentro del margen seguro
  (la barra de estado sigue lisa). Lo que no es casilla (reloj, rótulo «Contactos
  favoritos») va sobre una franja `background` con contorno, así que el
  contraste es el de siempre.
- **El asistente** es un círculo de 96 dp abajo en el centro, relleno `primary`
  con el icono en `onPrimary` (7,86:1 en oscuro, 17,08:1 en claro). Al pulsarlo
  pasa a `surfaceContainerHigh` con el icono en `onSurface` (11,35:1 y 15,18:1).
  Flota encima de la rejilla, que se ve pasar por sus lados (D-38), con un aro de
  8 dp del color del fondo. La rejilla acaba con un margen de su alto: cualquier
  casilla se puede subir hasta dejarla entera a la vista.
- **Mientras escucha**, la pantalla del asistente enseña arriba, en el centro, ese
  mismo círculo de 96 dp (`ListeningPulse`). Lleva un degradado de `primary` a
  `secondary` (en claro, del marino al cian profundo; en oscuro, del cian al azul
  claro), con el micrófono en `onPrimary` a más de 6,7:1. Crece hasta un 22 % con
  la voz y suelta anillos de `primary` que se abren y se apagan en 1,4 s: seguidos
  al hablar y uno tenue cada 1,6 s en silencio. Sin rebotes. Con «Quitar
  animaciones», sin anillos. Sustituye a la barra de volumen.
- **Señalar sin mover**: al guiar, la casilla o el contacto se **rellena de
  `primary`** con el contenido en `onPrimary` y el contorno a
  `HighlightBorderWidth`, y TalkBack lo anuncia («El asistente te lo señala»). Solo
  el borde no bastaba: en claro el contorno normal ya es del mismo marino que el
  acento, y en gris no se distinguía.

## Componentes compartidos

`core/ui/Components.kt`. **Antes de crear un composable nuevo, mira si ya está.**
Todos siguen la misma receta: superficie plana con contorno de 2 dp que pasa al
color de acento mientras se pulsa, el área pulsable es la pieza entera, y un
toque que cuenta da una vibración corta de teclado (la del sistema: si está
apagada, no suena). Las pantallas cambian con un fundido de 180 ms (D-32).

| Componente | Cuándo |
| --- | --- |
| `CoidaCard` | Base de todo: casilla, fila, botón, tarjeta. `onClick = null` si no es pulsable. `borderWidth` solo cambia para señalar algo; `dashed`, contorno a trazos para lo que no está |
| `SettingsGroup` + `GroupPosition` | Filas de un mismo apartado en un bloque, con una raya entre ellas. Cada fila recibe su posición (`GroupPosition.of`) |
| `SettingsRow` | Fila que lleva a otra pantalla (icono, título, subtítulo, chevron) |
| `SwitchRow` | Fila con interruptor. **Toda la fila** es el control, no la bolita |
| `ChoiceGroup` | Opciones excluyentes como selector segmentado; en columna con tres opciones o más, o con letra por encima de 1,3 |
| `PrimaryButton` | Acción principal, rellena de acento. Una por pantalla |
| `OutlineButton` | Acción secundaria, solo contorno |
| `ScreenHeader` | Cabecera de ajustes: «Volver» escrito, y debajo título y subtítulo a todo lo ancho |
| `SectionLabel` | Rótulo de un grupo de filas |
| `IconBadge` | Icono de acento, sin plato salvo el del asistente |
| `InitialBadge` | La inicial de un nombre sobre un plato; decorativa para TalkBack |
| `ContactAvatar` | La foto de un contacto en círculo con contorno, o su inicial si no hay (D-57) |
| `FitText` · `rememberFitScale` · `rememberCommonFitScale` | Texto que baja de tamaño para caber sin partir palabras (D-54); el último, el tamaño común de varios |
| `NoteCard` | Nota explicativa, no pulsable: relleno y contorno |
| `NotHomeNotice` | Aviso de que Coida no es la pantalla de inicio (D-48), con su botón y una equis que lo cambia por cómo ponerlo más adelante |
| `CoidaTextField` | Campo para escribir, con contorno de 2 dp que pasa a acento al escribir y a error si falla |
| `SettingsColumn` | Columna de pantalla de ajustes con el espaciado estándar |
| `CoidaSwitchVisual` | Solo el dibujo del interruptor; la semántica la pone la fila |
| `AssistantButton` | El círculo del asistente. Vive en `assistant/ui/`, no en `core/ui/`: solo se usa en el inicio |

## El lienzo de `design/`

Las pantallas se diseñaron antes de escribirlas, en `design/*.dc.html`, y la
presentación del proyecto está en `design/presentacion/`. Sirve para ver la
intención: proporciones, jerarquía y textos.

**Si el lienzo y el código no cuadran, manda el código.** El lienzo no se
actualiza con cada cambio; no lo uses como referencia de valores.

## Cómo comprobar que algo cumple

1. **Contraste**: calcula el ratio del texto contra el fondo real sobre el que
   cae (no contra el fondo de la pantalla). 4,5:1 es el mínimo; 7:1 lo que se
   busca para texto y bordes.
2. **Tamaño táctil**: mide la pieza entera pulsable, no el icono.
3. **Letra grande**: pon el móvil al 200 % de tamaño de fuente y mira que la
   pantalla siga siendo usable.
4. **En gris**: si algo solo se distingue por color, no vale.
