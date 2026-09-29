---
paths:
  - "app/src/main/java/com/doado/**/*.kt"
---

# UI en Compose

## Antes de escribir un composable

Mira si ya existe en `core/ui/Components.kt`: `DoadoCard`, `SettingsRow`,
`SwitchRow`, `ChoiceGroup`, `PrimaryButton`, `OutlineButton`, `ScreenHeader`,
`SectionLabel`, `IconBadge`, `NoteCard`, `SettingsColumn`, `DoadoSwitchVisual`.
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

## Escribir para una persona mayor

Los textos del inicio los lee ella; los de ajustes, el familiar.

- Frases cortas, tuteando, sin tecnicismos («intent», «caché», «paquete») y sin
  códigos de error.
- Un mensaje de error dice **qué hacer ahora**, no qué ha fallado por dentro.
- Nada de diálogos sin una salida grande y visible, ni gestos como única forma de
  hacer algo.

## Cosas de este launcher que se olvidan

- La rejilla es de **una o dos columnas**. No metas un tercer valor ni un cálculo
  automático por ancho de pantalla.
- Las casillas **conservan su posición** aunque la app no esté: apagadas, nunca
  reordenadas ni ocultas.
- `HOME` se pinta fuera del `Box` con `safeDrawingPadding()`; el resto de
  pantallas van dentro.
- Si cambias la jerarquía de pantallas (el `enum Screen` o `parentOf`),
  actualiza `docs/arquitectura.md` en el mismo commit.

## El lienzo de `design/`

Si la pantalla ya existe ahí, síguelo. Si te separas, dilo en el resumen y
explica por qué. Cuando el lienzo y el código no cuadran, **manda el código**.
