# La norma que no se salta

**Toda UI nueva prioriza la accesibilidad sobre la estética.** Si una propuesta
visual choca con una de estas normas, gana la norma, y lo dices en el resumen en
vez de aplicarla en silencio.

Los requisitos con su criterio de aceptación están en la ERS (RNF-ACC-01 a
RNF-ACC-07). Los valores concretos —paleta, escala, medidas— en `docs/diseno.md`.

## Normas

- **No inventes colores.** Usa los roles de `MaterialTheme` y los tokens de
  `core/ui/Color.kt`. Si de verdad hace falta uno nuevo, propónlo con su ratio de
  contraste calculado **antes** de usarlo.
- **El cian de marca nunca para texto ni iconos sobre fondo claro**: se queda en
  2,32:1. Ahí manda el marino.
- **Contraste**: 4,5:1 como mínimo; 7:1 es lo que se busca para texto y bordes.
  Se mide contra el fondo real sobre el que cae el texto, no contra el de la
  pantalla.
- **Nada pulsable por debajo de 48 dp**, y en el inicio bastante más: las
  casillas pasan de 168 dp. Se mide la pieza entera, no el icono.
- **Ningún texto por debajo de 18 sp**, y siempre desde
  `MaterialTheme.typography`, nunca un `.sp` a pelo en un composable.
- **Separa por borde, nunca por sombra ni por elevación sola.**
- **Prohibido el color dinámico** de Material You y cualquier tema que dependa
  del fondo de pantalla del usuario.
- **Las casillas no se mueven**: una app que no está se pinta apagada, en su
  sitio, con su aviso. Nunca reordenes ni compactes la rejilla.
- **TalkBack**: todo lo pulsable lleva etiqueta en español y en lenguaje llano;
  los iconos que van acompañados de texto son decorativos
  (`contentDescription = null`) para no oírlo dos veces.
- **Nada que desaparezca solo**, salvo la cuenta atrás del modo administrador.

## Cómo compruebas que cumple

1. Calcula el contraste del texto contra su fondo real.
2. Mide el área pulsable entera.
3. Pon el sistema al 200 % de tamaño de letra y mira que la pantalla siga
   usándose.
4. Míralo en gris: si algo solo se distingue por color, no vale.

Si cambias un token de color o de medida, actualiza `docs/diseno.md` en el mismo
commit.
