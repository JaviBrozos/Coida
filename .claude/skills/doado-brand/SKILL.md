---
name: doado-brand
description: Identidad visual de Doado (logo, icono de la app, colores, iconos, letra, tono y movimiento) y cómo aplicarla. Úsala siempre que se cree o cambie algo que lleve la marca o que se vea, dentro o fuera de la app (pantallas, iconos, recursos de res/, presentación, README, Google Play, redes, vídeos, el post de LinkedIn), aunque no se diga «marca».
---

# Marca de Doado

Doado (en gallego, «sencillo») es una pantalla de inicio de Android para
personas mayores. La marca tiene que transmitir **calma, claridad y confianza**,
y nunca puede ganarle a la accesibilidad: es el invariante número uno del
proyecto.

Esta skill resume la marca y dice cómo aplicarla. **Las cifras mandan en su
fuente**: los colores, en `core/ui/Color.kt`; la construcción y las medidas, en
`docs/diseno.md`; el porqué, en `docs/decisiones.md` (D-02, D-10, D-11, D-34 a
D-36). Si algo de aquí no cuadra con ellas, gana la fuente y se corrige esta
skill en el mismo commit.

## Antes de tocar nada

1. Lee `docs/diseno.md` («Paleta», «Marca» e «Iconografía») y, para UI,
   `.claude/rules/ui-compose.md` y `.claude/rules/recursos.md`.
2. Para piezas nuevas, usa los ficheros de `assets/` de esta skill o los de `res/`.
   No redibujes la marca a mano.
3. Al terminar, pasa el agente de accesibilidad (`.claude/agents/accesibilidad.md`)
   si hay UI, o el auditor de marca (`.claude/agents/auditor-marca.md`) si es una
   pieza de marca.

## Logo

Una **D con un visto dentro** (D-36): la D dice el nombre y el visto dice
«hecho», lo que hace el asistente tras el «Sí».

- **Construcción** (rejilla de 64): D de 48 × 52 desde (8, 6), con una recta de
  22, medio círculo de radio 26 y las esquinas de la recta de radio 6. El visto
  pasa por (18,8; 32,2), (25; 38,8) y (39,6; 24,6), con un trazo de 5 y puntas y
  codo redondos. El `pathData` exacto está en `assets/tokens.json`.
- **El visto es un hueco** (`evenOdd`): deja ver el fondo, nunca es una pintura
  encima.
- **Versiones** (`assets/`):

  | Fichero | Para qué |
  | --- | --- |
  | `doado-marca-marino.svg` | Sobre fondo claro |
  | `doado-marca-cian.svg` | Sobre marino o tinta |
  | `doado-marca-blanca.svg` | Sobre una foto oscura o a una tinta |
  | `doado-icono.svg` | Icono cuadrado de 512 px |

  En Android: `ic_doado_mark` (cian, para fondo oscuro), `ic_doado_mark_light`
  (marina, para fondo claro) y el icono adaptativo.
- **Icono de la app**: fondo marino (`ic_launcher_background`) y delante solo la D
  (`drawable/ic_launcher_foreground.xml`), sin inset ni un segundo cuadrado. La D
  mide 48 dp de alto en el lienzo de 108, 2,2 dp a la derecha del centro. La capa
  monocroma es la misma figura. Si cambias la marca, regenera también los PNG de
  `mipmap-*dpi`.
- **Zona de respeto**: alrededor de la D, como mínimo la mitad de su ancho.
  **Tamaño mínimo**: 16 px.
- **Nunca**:
  - la D en cian sobre blanco (2,32:1);
  - estirarla, girarla o ponerle sombras, degradados o contornos;
  - el visto pintado encima;
  - otro dibujo dentro del icono de la app;
  - los colores antiguos del logo (`#0B1F33`, `#29B6F6`).
- **Logotipo** (`doado_logo.svg`): la D y «Doado». El texto va hoy en Baloo 2 y
  Quicksand; falta pasarlo a Inter Bold en trazos (ver «Letra»).

## Color

Seis colores de marca y una escala de superficies. Nada de color dinámico (D-02).

| Color | Hex | Uso |
| --- | --- | --- |
| Marino | `#0B1D2E` | Marca; texto y bordes en claro (17,08:1 sobre blanco) |
| Cian | `#29B6F0` | Acento solo sobre oscuro (7,86:1 sobre tinta). **Nunca texto ni iconos sobre claro** |
| Cian profundo | `#10618F` | Texto secundario en claro (6,71:1) |
| Tinta | `#0A1622` | Fondo del tema oscuro, el de fábrica |
| Niebla | `#E9F3FA` | Superficie suave en claro |
| Texto claro | `#F2F7FB` | Texto en oscuro (16,92:1 sobre tinta) |

El resto (superficies, línea, apagados) y todos los contrastes están en
`assets/tokens.json` y en `docs/diseno.md`.

- **Contraste**: se mide contra el fondo real sobre el que cae el texto, que casi
  siempre es la casilla. Texto, como mínimo 4,5:1 (se apunta a 7:1); gráficos y
  bordes, 3:1.
- **En Compose**: las pantallas no escriben colores, piden roles del tema
  (`primary`, `onSurface`…).

## Iconos

De trazo y dibujados a medida (D-11, D-34). Están en `res/drawable/ic_*.xml`.

- **Rejilla y trazo**: rejilla de 24 con zona viva de 20; trazo de 1,7 con puntas
  y uniones redondas; `strokeColor="#FF000000"`, que el tinte del tema recolorea.
- **Radios**: 3 en los contenedores grandes y 2,5 en los pequeños.
- **El círculo como motivo** donde tiene sentido: objetivo, cabeza, tiradores,
  cerradura, centro del engranaje.
- **Sin plato** (D-35): solo la silueta, sobre la casilla o la fila. Igual con los
  iconos de las demás apps.
- **Siluetas reconocibles a 72 dp**. Nunca emoji ni iconos de librerías de
  terceros.
- **Nombres**: un icono nuevo se llama `ic_<qué hace>` y se dibuja con estas
  reglas antes de usarlo.

## Letra

- **Inter** (D-50), licencia OFL (`design/propuestas-marca/fuente/inter-OFL.txt`),
  siempre con el juego `ss04`: la I con remates y la l con cola, que sin él son
  dos palos casi iguales («lsabel»).
- **App**: en `res/font/`, con la escala de `Type.kt`. Va en `sp`, con
  interletrado 0, **SemiBold** en cuerpo y etiquetas y **Bold** en titulares, y
  nada por debajo de 18 sp. Solo esos dos grosores: un tercero necesita su
  fichero.
- **Logotipo**: Bold convertida a trazos. Pendiente de rehacer `doado_logo.svg`.
- **Fuera de la app** (presentación, web, imágenes): Inter con `ss04` donde se
  pueda incrustar (los `.ttf` de la app están en `res/font/`, y está en Google
  Fonts); si no, `Roboto, system-ui, sans-serif`. Nada de Segoe UI ni
  de letras redondeadas o infantiles (D-10, «no infantiliza»).

## Tono

- **Trato**: se tutea, con frases cortas y verbos claros.
- **Palabras del proyecto**: «casilla», «inicio», «familiar», «modo
  administrador» (`.claude/rules/producto.md`).
- **Con la persona mayor**: nunca jerga técnica ni un error que asuste.
- **Lema**: «Una app para tus mayores».
- **Cómo se presenta**: como lo que es, sin prometer más de lo que hace (por
  ejemplo, que impide salir a Android).

## Movimiento

- **En la app**: corto y tranquilo. 120 ms al pulsar, fundido de 180 ms al cambiar
  de pantalla (D-32). Sin rebotes ni muelles. Con «Quitar animaciones», un corte.
- **Marketing** (vídeo y redes): las animaciones de marca están por rehacer para
  la D con visto. La idea de partida es que el visto se dibuje, como al confirmar
  una orden.
- **Reglas comunes**:
  - curvas: entradas con `cubic-bezier(0.2, 0, 0, 1)`;
  - rebote: como mucho un 4 %, y solo en vídeo;
  - colores: solo los de marca;
  - siempre una versión quieta;
  - formatos: 1080 × 1080, 1080 × 1920 y 1920 × 1080.

## Al entregar una pieza

- [ ] La marca sale de `assets/` o de `res/`, con sus colores exactos.
- [ ] Ningún cian sobre fondo claro para texto o iconos.
- [ ] Contrastes medidos sobre el fondo real.
- [ ] Iconos nuevos con las reglas de la rejilla de 24, y sin plato.
- [ ] Letra Inter con `ss04`, o la pila de reserva fuera de la app.
- [ ] Si cambió la marca: `docs/diseno.md`, `docs/decisiones.md`,
      `docs/estado.md` y esta skill, en el mismo commit.
