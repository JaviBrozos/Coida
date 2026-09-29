# Sistema visual

> **Fuente única de**: paleta, tipografía, medidas, iconografía y componentes
> compartidos. Enlaza desde otros documentos; no copies las cifras. Quien manda
> de verdad es `core/ui/`: si esto no cuadra con el código, **gana el código**.
>
> Última revisión: 29/09/2026.

El principio está en la ERS como requisitos medibles (RNF-ACC-01 a RNF-ACC-07).
Aquí está el cómo.

## Paleta

Dos colores de marca (marino y cian) y una escala de superficies construida
alrededor. Definidos en `core/ui/Color.kt`.

| Token | Hex | Para qué | Contraste |
| --- | --- | --- | --- |
| `DoadoNavy` | `#0B1D2E` | Marca. Texto y bordes en el tema claro | 16,8:1 sobre blanco |
| `DoadoCyan` | `#29B6F0` | Acento: iconos, estado activo, botón principal | 7,3:1 sobre tinta · **2,2:1 sobre blanco** |
| `DoadoCyanDeep` | `#10618F` | Texto secundario en el tema claro | 4,6:1 sobre blanco |
| `DoadoCyanLight` | `#6CCFFA` | Reservado para oscuro | 9,5:1 sobre negro |
| `DoadoInk` | `#0A1622` | Fondo del tema oscuro | — |
| `DoadoSurface` | `#122A3E` | Casilla y fila elevadas | 13:1 con el texto |
| `DoadoSurfaceHigh` | `#17384F` | Pulsada, y cuadrito tras un icono | — |
| `DoadoLine` | `#3D7AA6` | Contorno de 2 dp | 3,3:1 con la casilla |
| `DoadoText` | `#F2F7FB` | Texto principal en oscuro | 16:1 sobre tinta |
| `DoadoTextMuted` | `#9FC4DC` | Texto secundario en oscuro | 9,9:1 sobre tinta |
| `DoadoMist` / `DoadoMistDim` | `#E9F3FA` / `#DCEBF5` | Superficies suaves del tema claro | — |
| `DoadoSteel` | `#7A93A6` | Contenido apagado (app no disponible) | 5,7:1 sobre tinta |

**La regla que explica la paleta**: el cian de marca no vale para texto ni iconos
sobre fondo claro. Por eso en el tema claro manda el marino, y el cian solo pinta
superficies grandes.

## Roles de Material 3

Las pantallas nunca escriben un color: piden un rol. Así el mismo composable
funciona en los dos temas. Definido en `core/ui/Theme.kt`.

| Rol | Para qué | Oscuro (de fábrica) | Claro |
| --- | --- | --- | --- |
| `primary` | Iconos de acento, botón principal, estado activo | Cian | Marino |
| `onPrimary` | Lo que va encima de `primary` | Tinta | Blanco |
| `background` / `onBackground` | Fondo de pantalla y su texto | Tinta / Texto | Blanco / Marino |
| `surfaceContainer` | Fondo de casillas y filas | `DoadoSurface` | Blanco |
| `surfaceContainerHigh` | Pulsada, cuadrito de icono | `DoadoSurfaceHigh` | Mist |
| `onSurface` | Texto sobre una casilla | Texto | Marino |
| `onSurfaceVariant` | Texto secundario | TextMuted | CyanDeep |
| `outline` | Contorno | `DoadoLine` | Marino |
| `outlineVariant` | Contenido apagado | Steel | Steel |

`surfaceTint` es transparente en los dos: el diseño es plano y **separa por
borde, no por sombra ni por elevación**.

## Tipografía

`core/ui/Type.kt`: la escala de Material multiplicada por ~1,5, con más grosor
(el trazo fino se pierde a tamaño grande) e interletrado a 0 (el negativo de
Material apelmaza el texto cuando se amplía). Todo en `sp`.

| Estilo | Tamaño | Dónde se usa |
| --- | --- | --- |
| `displayMedium` | 52 sp | El reloj del inicio, la cuenta atrás del modo admin |
| `displaySmall` | 44 sp | El nombre «Doado» cuando el reloj está apagado |
| `headlineSmall` | 30 sp | Las cifras del teclado del PIN |
| `titleLarge` | 30 sp | Título de una pantalla de ajustes |
| `titleSmall` | 22 sp | Texto de botón, nombre de un contacto |
| `bodyMedium` | 22 sp | La fecha, textos de cuerpo |
| `bodySmall` | 20 sp | Subtítulos y notas |
| `labelLarge` | 24 sp | **La etiqueta de una casilla** y los títulos de fila |
| `labelMedium` | 20 sp | Rótulos de grupo y opciones |
| `labelSmall` | 18 sp | El «No disponible» de una casilla apagada. **Suelo: nada por debajo** |

Hay estilos declarados que hoy no usa nadie (`displayLarge`, `headlineLarge`,
`headlineMedium`, `titleMedium`, `bodyLarge`). Están para cuando hagan falta; no
los borres sin mirar.

## Medidas

`core/ui/DoadoDimens`. Nada de números sueltos en un composable.

| Token | Valor | Qué es |
| --- | --- | --- |
| `MinTouchTarget` | 48 dp | Suelo innegociable de cualquier pulsable |
| `ComfortableTouchTarget` | 72 dp | Lo normal para un botón suelto |
| `TileMinHeight` | 168 dp | Alto mínimo de una casilla (en el inicio suelen salir ~260) |
| `TileIconSize` | 72 dp | Icono dentro de la casilla |
| `TileCornerRadius` | 22 dp | Radio de casilla |
| `CardCornerRadius` | 20 dp | Radio de fila, tarjeta y botón |
| `ControlCornerRadius` | 14 dp | Radio de control pequeño |
| `BorderWidth` | 2 dp | Grosor de contorno |
| `RowHeight` | 88 dp | Alto de fila de ajustes |
| `ScreenPadding` | 20 dp | Margen de pantalla |
| `GridSpacing` | 16 dp | Separación de la rejilla |

**Deuda conocida**: `TileIconSize` y `MinTouchTarget` están declarados y no se
usan —`AppTile` pone 60/80 dp a pelo— y hay medidas de layout sueltas en
`HomeScreen`. Si tocas esas zonas, pásalas al token.

## Iconografía

- **De trazo**, dibujados sobre rejilla de 24 con grosor 1,7 y puntas redondas.
  Rellenos escalados a 100 px se ven hinchados; el trazo aguanta cualquier
  tamaño y se recolorea con el tint.
- Se tiñen con un rol del tema, nunca con un color escrito.
- **Nunca emoji ni dingbats.**
- El **isotipo** tiene dos versiones porque el cian no llega a 3:1 sobre blanco:
  `ic_doado_mark` (D cian, visto marino) para fondo oscuro y
  `ic_doado_mark_light` (D marina, visto blanco) para fondo claro. Es marca, no
  información: siempre `contentDescription = null`.

## Componentes compartidos

`core/ui/Components.kt`. **Antes de crear un composable nuevo, mira si ya está.**
Todos siguen la misma receta: superficie plana con contorno de 2 dp que pasa al
color de acento mientras se pulsa, y el área pulsable es la pieza entera.

| Componente | Cuándo |
| --- | --- |
| `DoadoCard` | Base de todo: casilla, fila, botón, tarjeta. `onClick = null` si no es pulsable |
| `SettingsRow` | Fila que lleva a otra pantalla (icono, título, subtítulo, chevron) |
| `SwitchRow` | Fila con interruptor. **Toda la fila** es el control, no la bolita |
| `ChoiceGroup` | Opciones excluyentes en fila (tamaño de letra, tema…) |
| `PrimaryButton` | Acción principal, rellena de acento. Una por pantalla |
| `OutlineButton` | Acción secundaria, solo contorno |
| `ScreenHeader` | Cabecera de ajustes: volver + título + subtítulo |
| `SectionLabel` | Rótulo de un grupo de filas |
| `IconBadge` | Cuadrito con icono de acento |
| `NoteCard` | Nota explicativa, no pulsable |
| `SettingsColumn` | Columna de pantalla de ajustes con el espaciado estándar |
| `DoadoSwitchVisual` | Solo el dibujo del interruptor; la semántica la pone la fila |

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
