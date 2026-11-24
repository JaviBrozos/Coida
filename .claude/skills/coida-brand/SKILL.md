---
name: coida-brand
description: Identidad visual de Coida (logo, logotipo, icono de la app, colores, iconos, letra, tono y movimiento) y cómo aplicarla. Úsala siempre que se cree o cambie algo que lleve la marca o que se vea, dentro o fuera de la app (pantallas, iconos, recursos de res/, presentación, web, README, Google Play, redes, vídeos, el post de LinkedIn), aunque no se diga «marca».
---

# Marca de Coida

Coida (en gallego, «cuida») es una pantalla de inicio de Android para personas
mayores. La marca tiene que transmitir **calma, claridad y confianza**, y nunca
puede ganarle a la accesibilidad: es el invariante número uno del proyecto.

Esta skill resume la marca y dice cómo aplicarla. **Las cifras mandan en su
fuente**:

- los colores, en `core/ui/Color.kt`;
- la geometría del icono, en `res/drawable/ic_coida_mark.xml`;
- las medidas, en `docs/diseno.md`;
- el porqué, en `docs/decisiones.md` (D-02, D-10, D-11, D-34, D-35, D-50, D-62
  y D-68).

Si algo de aquí no cuadra con ellas, gana la fuente y se corrige esta skill en el
mismo commit. Antes del 09/11/2026 la app se llamaba Doado y su marca era una D
con un visto (D-36): eso ya no se usa en ninguna pieza nueva.

## Antes de tocar nada

1. Lee `docs/diseno.md` («Paleta», «Marca» e «Iconografía») y, para UI,
   `.claude/rules/ui-compose.md` y `.claude/rules/recursos.md`.
2. Para piezas nuevas, usa los ficheros de `assets/` de esta skill o los de `res/`.
   No redibujes la marca a mano.
3. Al terminar, pasa el agente de accesibilidad (`.claude/agents/accesibilidad.md`)
   si hay UI, o el auditor de marca (`.claude/agents/auditor-marca.md`) si es una
   pieza de marca.

## Logo

Una **C de trazo con un visto que asoma por su boca** (D-62): la C dice el nombre
y el visto dice «hecho, ya está», que es lo que busca la persona mayor.

- **Construcción** (rejilla de 64):
  - Anillo de radio 20 con centro en (35,5; 32), trazo de 9 y puntas redondas. La
    boca se abre a la derecha, entre −60° y +60°:
    `M45.5,14.68 A20,20 0 1 0 45.5,49.32`.
  - Visto por (31,5; 33), (38,5; 40) y (52,5; 25), con trazo de 6,5 y puntas y
    codo redondos: `M31.5,33 L38.5,40 L52.5,25`.
  - El visto **no toca el anillo**: si lo toca, se lee como una «e».
  - La figura ocupa de (11; 7,5) a (55,75; 56,5). Su centro óptico va un poco a la
    derecha del geométrico, por el peso del visto.
- **Es trazo, no relleno**: en SVG, `fill="none"` y `stroke`, como en
  `ic_coida_mark.xml`.
- **Versiones** (`assets/`):

  | Fichero | Para qué |
  | --- | --- |
  | `coida-marca-marino.svg` | Sobre fondo claro |
  | `coida-marca-cian.svg` | Sobre marino o tinta |
  | `coida-marca-blanca.svg` | Sobre una foto oscura o a una tinta |
  | `coida-icono.svg` | Icono cuadrado de 512 px: marino con la C en cian |
  | `coida-logotipo-marino.svg` | La C y «Coida», sobre fondo claro |
  | `coida-logotipo-claro.svg` | La C en cian y «Coida» en texto claro, sobre oscuro |

  En Android, `ic_coida_mark` (cian, para fondo oscuro), `ic_coida_mark_light`
  (marino, para fondo claro) y el icono adaptativo. En la raíz del repositorio
  están `coida_logo.svg`/`.png` y `coida_icono.png`.
- **Icono de la app**: fondo marino (`ic_launcher_background`) y delante solo la C
  (`drawable/ic_launcher_foreground.xml`), dentro de la zona segura de 66 dp. La
  capa monocroma es la misma figura. Si cambias la marca, regenera también los PNG
  de `mipmap-*dpi` y `publishing/icono-512.png`.
- **Logotipo**: la C y la palabra «Coida» en **Inter Bold pasada a trazos**, que
  no depende de tener la letra instalada.
  - La C mide 1,25 veces la altura de la mayúscula y va centrada con ella.
  - El hueco entre la C y la palabra es 0,27 del ancho de la C, medido en la punta
    del visto: por la boca abierta, a media altura se ve más ancho.
  - «Coida» no tiene I ni l, así que `ss04` no cambia nada en el logotipo.
- **Zona de respeto**: alrededor de la C, como mínimo la mitad de su ancho.
  **Tamaño mínimo**: 24 px de alto, el isotipo y el logotipo. El hueco más
  estrecho, entre el codo del visto y el interior del anillo, mide 3,71 unidades de
  64: por debajo de 24 px se empasta y se lee como una «e». Para un favicon, el
  isotipo solo, sin el cuadrado.
- **Nunca**:
  - la C en cian sobre blanco (2,32:1);
  - estirarla, girarla o ponerle sombras, degradados o contornos;
  - el visto pegado al anillo;
  - otro dibujo dentro del icono de la app;
  - la D de Doado ni los colores antiguos del logo (`#0B1F33`, `#29B6F6`).

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
- **El círculo como motivo** donde tiene sentido, igual que en la C: objetivo,
  cabeza, tiradores, cerradura y centro del engranaje.
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
- **Fuera de la app** (presentación, web e imágenes): Inter con `ss04` donde se
  pueda incrustar (los `.ttf` de la app están en `res/font/`, y está en Google
  Fonts). Si no se puede, `Roboto, system-ui, sans-serif`. Nada de Segoe UI ni de
  letras redondeadas o infantiles (D-10, «no infantiliza»).

## Tono

- **Trato**: se tutea, con frases cortas y verbos claros.
- **Palabras del proyecto**: «casilla», «inicio», «familiar», «modo
  administrador» (`.claude/rules/producto.md`).
- **Con la persona mayor**: nunca jerga técnica ni un error que asuste.
- **Lema**: «Una app para tus mayores».
- **Cómo se presenta**: como lo que es, sin prometer más de lo que hace. La
  versión pública no tiene asistente de voz (D-63, D-67): no se enseña ni se
  promete en ninguna pieza de la tienda, la web o las redes.
- **Fuera, ante empresas o la universidad**: dos estudiantes de Ingeniería
  Informática (Javier, en la UPM; Gonzalo, en la UDC) que lo han probado con
  personas mayores de verdad. Con datos que se puedan comprobar en el repositorio,
  sin inflar.

## Piezas hechas

Sacadas de un SVG o un HTML con Edge sin ventana, con la letra de `res/font/`:

| Pieza | Fichero | Medida |
| --- | --- | --- |
| Cabecera del README y *social preview* de GitHub | `docs/img/cabecera.png` | 1280 × 640 |
| Gráfico destacado de Play (24 bits, sin alfa) | `publishing/grafico-destacado.png` | 1024 × 500 |
| Imagen del post de LinkedIn | `publishing/linkedin.png` | 1200 × 627 |
| Icono de Play (32 bits) | `publishing/icono-512.png` | 512 × 512 |
| Capturas de la release, con datos inventados | `docs/capturas/1-…8-*.png` | 1080 × 2400 |

Composición común: fondo tinta o marino, el logotipo claro, el lema en Bold
`#F2F7FB` y el texto secundario en SemiBold `#9FC4DC` (9,91:1 sobre tinta). Los
móviles van con un borde `#3D7AA6`, sin sombra.

## Movimiento

- **En la app**: corto y tranquilo. 120 ms al pulsar y un fundido de 180 ms al
  cambiar de pantalla (D-32). Sin rebotes ni muelles. Con «Quitar animaciones», un
  corte.
- **Marketing** (vídeo y redes): el vídeo de `design/video/` es todavía de Doado,
  con la D. Para la C, la idea de partida es que el anillo se cierre y el visto se
  dibuje, como al terminar algo.
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
- [ ] Ni rastro de la D ni del nombre Doado, ni del asistente en lo público.
- [ ] Si cambió la marca: `docs/diseno.md`, `docs/decisiones.md`,
      `docs/estado.md` y esta skill, en el mismo commit.
