# Propuesta: identidad visual de Doado

> Estado: **logo decidido, letra pendiente.** El logo sigue siendo la D con
> visto, con las esquinas redondeadas y un visto nuevo (D-36). Ya están
> aplicados también los iconos (D-34 y D-35) y las cifras de contraste
> corregidas. Queda por decidir la letra: Javier propone Atkinson Hyperlegible
> Next para la marca y la app.
>
> Auditoría del 18/10/2026, hecha con el agente `.claude/agents/auditor-marca.md`
> y la skill `.claude/skills/brand-identity/`. La fuente lista para aplicar está en
> `design/propuestas-marca/fuente/`. Los lienzos con las opciones, el
> sistema, los iconos y las animaciones están en un artefacto privado de Claude
> de Javier («Doado — identidad visual»). Para que Gonzalo lo vea, hay que
> compartirlo.

## Lo que encontró la auditoría

1. **El icono adaptativo lleva otro icono dentro.** La capa delantera es el icono
   entero (cuadrado marino incluido) con un inset del 17 %, y es el mismo fichero
   que el icono heredado y que `doado_icono.png`. No se nota porque los dos
   marinos separan a 1,02:1, pero aparece con otro fondo o con el paralaje. La D
   solo ocupa 40 × 44 dp de los 66 de la zona segura. **Arreglado en D-36.**
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
| **A. La D de siempre** (elegida, D-36, con esquinas redondeadas y visto nuevo) | La D con el visto, afinada: visto en hueco, el doble de grueso | Continuidad | Un visto es un recurso muy visto |
| **B. Botón** (descartada) | D con un hueco redondo: el círculo del asistente y las «o» de «Doado» | La más legible a 16 px; sobria | Pierde el «hecho» del visto |
| **C. Diálogo** (descartada) | La D como bocadillo, con el visto | Cuenta el asistente de voz | Se confunde con las apps de mensajes |

La geometría del logo elegido está en `docs/diseno.md`, «Marca».

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

## Movimiento de marca

Las cinco animaciones del lienzo se diseñaron para el logo B, que se descartó.
Hay que rehacerlas para la D con visto, con las mismas reglas:

- **Curvas**: entradas con salida suave (`cubic-bezier(0.2, 0, 0, 1)`).
- **Rebote**: como mucho un 4 %, y solo en vídeo; en la app, ninguno (D-32).
- **Colores**: solo los de marca.
- **Sin animaciones**: siempre una versión quieta.
- **Formatos**: 1080 × 1080, 1080 × 1920 y 1920 × 1080.

El punto de partida natural es que el visto se dibuje, como al confirmar una
orden del asistente.

## Pendiente, sea cual sea la elección

- Rehacer el isotipo de las ocho láminas de `design/presentacion/` y pasar la
  presentación de Segoe UI a una pila que exista en todas partes.
- Los colores sueltos de la presentación (`#3C5A70`, `#C9DCE8`…) y los estados
  que se distinguen solo por el color de una bolita (`EstadoLimites`).
