---
paths:
  - "app/src/main/java/com/doado/**/*.kt"
---

# UI en Compose

## Antes de escribir un composable

Mira si ya existe en `core/ui/Components.kt`: `DoadoCard`, `SettingsGroup`,
`SettingsRow`, `SwitchRow`, `ChoiceGroup`, `PrimaryButton`, `OutlineButton`,
`ScreenHeader`, `SectionLabel`, `IconBadge`, `InitialBadge`, `ContactAvatar`, `NoteCard`, `NotHomeNotice`, `FitText`, `DoadoTextField`,
`SettingsColumn`, `DoadoSwitchVisual`.
Casi toda pantalla de ajustes sale de ahí. La tabla de para qué sirve cada uno
está en `docs/diseno.md`.

## Reglas

- **Colores por rol de `MaterialTheme`**, nunca un `Color(0xFF…)` ni un token de
  `Color.kt` directamente en una pantalla. El mapa de roles está en
  `docs/diseno.md`.
- **Texto desde `MaterialTheme.typography`.** Ningún `fontSize` suelto.
- **Medidas desde `DoadoDimens`.** Si necesitas una que no está, añádela al
  objeto en vez de escribir el número en la pantalla.
- **Las pantallas no conocen Android**: nada de `Context`, `PackageManager`,
  `Intent` ni permisos dentro de un composable. Reciben datos ya resueltos y
  devuelven callbacks; la fontanería vive en `LauncherActivity`.
- **Semántica para TalkBack** en todo lo pulsable: etiqueta de acción con
  `onClick(label = …)`, y `contentDescription` propio cuando el elemento está
  deshabilitado y hay que explicar por qué.
- **El área pulsable es la pieza entera**, no un botoncito dentro. Una fila con
  interruptor se hace `toggleable` completa, con `Role.Switch`.
- **Previews con `DoadoTheme(darkTheme = true)`**, que es el tema de fábrica.
- **Filas de un mismo apartado, en un `SettingsGroup`**, cada una con su
  `GroupPosition`. En una `LazyColumn`, sin `spacedBy` general: el hueco se pone
  entre apartados, no entre filas.
- **La vibración del toque ya la dan los componentes** (`rememberClickFeedback`).
  No la añadas a mano, y nunca al apoyar el dedo: en una lista vibraría al
  arrastrar.

## Escribir para una persona mayor

Los textos del inicio los lee ella; los de ajustes, el familiar.

- Frases cortas, tuteando, sin tecnicismos («intent», «caché», «paquete») y sin
  códigos de error.
- Un mensaje de error dice **qué hacer ahora**, no qué ha fallado por dentro.
- Nada de diálogos sin una salida grande y visible, ni gestos como única forma de
  hacer algo.

## Cosas de este launcher que se olvidan

- La rejilla es de **una a cuatro columnas**, todas ofrecidas en cualquier pantalla
  (D-54). Con dos o más, las casillas son **cuadradas** y nunca se estiran; con una,
  filas. El nombre nunca se parte: baja de tamaño (`FitText`, y la letra común de
  `rememberTileLabelScale`). El inicio nunca cambia de columnas solo, ni por la
  letra ni por el ancho.
- Las casillas **conservan su posición** aunque la app no esté: apagadas (contorno
  a trazos), nunca reordenadas ni ocultas. Solo el familiar las cambia de sitio,
  en «Colocar las casillas» (D-59).
- `HOME` se pinta fuera del `Box` con `safeDrawingPadding()`; el resto de
  pantallas van dentro.
- En el inicio, Ajustes es el botón pequeño de arriba a la derecha y el asistente,
  el círculo flotante de abajo, que va encima de la rejilla (D-38). La rejilla
  acaba con un margen del alto del círculo: **toda casilla tiene que poder quedar
  entera a la vista**. Los contactos van en la misma rejilla, tras «Contactos favoritos».
  Si el familiar los pone aparte (D-46), van en una segunda pantalla del inicio
  con dos pestañas siempre a la vista: deslizar es un atajo, nunca la única forma.
- Para señalar una casilla (el asistente guiando): relleno `primary`, contenido
  `onPrimary`, `borderWidth = DoadoDimens.HighlightBorderWidth` y
  `stateDescription` para TalkBack. Solo el borde no basta: en claro el contorno ya
  es del color del acento. Nunca moverla, agrandarla ni hacerla parpadear.
- Con foto de fondo (D-47), todo texto del inicio que no vaya dentro de una
  casilla va sobre una franja lisa con contorno (`PhotoBand`): el contraste se
  mide contra ella, nunca contra la foto.
- Si cambias la jerarquía de pantallas (el `enum Screen` o `parentOf`),
  actualiza `docs/arquitectura.md` en el mismo commit.

## El lienzo de `design/`

Si la pantalla ya existe ahí, síguelo. Si te separas, dilo en el resumen y
explica por qué. Cuando el lienzo y el código no cuadran, **manda el código**.

## La skill `apple-design`

Está en `.claude/skills/apple-design/` y explica cómo diseña Apple: respuesta
inmediata, movimiento que se puede interrumpir y coherencia espacial. Está
pensada para la web y para el estilo de Apple. **Aquí mandan las normas de
Doado**: nada de translucidez ni sombras (D-04), nada de interletrado negativo
(D-03), nada de gestos de arrastre ni muelles en lo que usa la persona mayor
(deslizar entre las dos pantallas de D-46 es un atajo de sus pestañas, no la forma
de usarlas; arrastrar solo se permite en pantallas del familiar, y siempre con otra
forma de hacerlo, D-59), y el asistente dice siempre lo que va a hacer antes de
hacerlo (D-56). Qué se tomó de ella y qué no está en D-32.
