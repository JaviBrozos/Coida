---
paths:
  - "app/src/main/res/**"
---

# Recursos

## Textos

- **Todo texto visible vive en `res/values/strings.xml`**, con nombre
  descriptivo. Ningún literal suelto en un composable.
- En español de España, tuteando, en frases cortas y sin tecnicismos. Los textos
  del inicio los lee la persona mayor; los de ajustes, el familiar.
- Cantidades variables con `<plurals>`, no concatenando.
- No hay traducciones y no hay `values-xx`: si hiciera falta otro idioma, es una
  decisión de producto (RNF-IDI-01 en la ERS), no algo que se añade de paso.

## Colores

- Los colores de la app viven en `core/ui/Color.kt`, no en `res/values/colors.xml`.
- `colors.xml` es solo para lo que Android necesita antes de que Compose pinte:
  el fondo de ventana del tema de arranque y el fondo del icono adaptativo.

## Iconos

- **De trazo**: lienzo de 24×24, `strokeWidth` 1,7, puntas y uniones redondas,
  `fillColor="#00000000"` y `strokeColor="#FF000000"` para que el tint del tema
  los recoloree.
- Siluetas reconocibles a tamaño grande. Nada de iconografía fina, abstracta ni
  decorativa, y **nunca emoji**.
- El isotipo tiene dos versiones (`ic_doado_mark` y `ic_doado_mark_light`) porque
  el cian no llega a 3:1 sobre blanco. Usa la que toque según el fondo.

## Otros

- No añadas cualificadores de pantalla (`layout-sw600dp` y compañía) sin hablarlo:
  hoy la app tiene un solo diseño en vertical.
- El tema XML (`values/themes.xml` y `values-night/themes.xml`) solo fija el
  fondo de ventana y las barras transparentes del arranque. El color de verdad lo
  pone Compose.

Si tocas la paleta o la iconografía, actualiza `docs/diseno.md` en el mismo
commit.
