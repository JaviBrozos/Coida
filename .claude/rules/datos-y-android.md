---
paths:
  - "app/src/main/java/com/doado/data/**/*.kt"
  - "app/src/main/AndroidManifest.xml"
  - "gradle/libs.versions.toml"
  - "**/build.gradle.kts"
---

# Datos y Android

## Preferencias

- **Una clave ya publicada no se renombra ni cambia de tipo.** Si hace falta otra
  cosa, añade una clave nueva y migra leyendo la antigua.
- **Toda lectura lleva valor por defecto explícito y seguro.** Ante datos
  ausentes o corruptos, la app arranca con la configuración básica; nunca peta.
  Los enums se leen tolerando un valor desconocido.
- **Preferencia nueva ⇒ añádela a `res/xml/backup_rules.xml` y a
  `res/xml/data_extraction_rules.xml`**, o se pierde al cambiar de móvil.
- El orden de las casillas es información, no adorno: se guarda como lista
  ordenada, y lo que se añade va **al final**.
- La tabla de claves y valores por defecto está en `docs/arquitectura.md`.

## Visibilidad de paquetes

- **Favorito nuevo ⇒ declara su intención en `<queries>`.** Desde Android 11, sin
  eso el `PackageManager` no lo ve y la casilla sale vacía.
- **Nunca `QUERY_ALL_PACKAGES`.** Si parece que hace falta, propón primero un
  `<queries>` acotado.

## El manifiesto es delicado

- **No toques el intent-filter de inicio (`HOME` + `DEFAULT`), el `launchMode`
  ni `excludeFromRecents` sin avisar**: un error ahí deja el móvil sin pantalla
  de inicio. El comportamiento esperado de INICIO, ATRÁS y recientes está en
  `docs/arquitectura.md`.
- **No añadas permisos por iniciativa propia.** Hoy el único peligroso es
  `CALL_PHONE`, y se pide al añadir un contacto, con el familiar delante.

## Intents

- **Todo intent hacia fuera se lanza con reserva**: usa `startFirst(...)`, que
  prueba las alternativas en orden, captura `ActivityNotFoundException` y
  `SecurityException` y avisa en lenguaje llano si ninguna funciona.
- `FLAG_ACTIVITY_NEW_TASK` siempre: el lanzador abre la app en su propia tarea.
- Copia defensiva de las plantillas de `DefaultFavorites`: son compartidas y no
  deben salir con flags añadidos.

## Hilo principal

`PackageManager` hace IPC: **no lo llames desde una recomposición**. Hoy se
resuelve en `onCreate` y en cada `onResume`, y ya es trabajo de más (está
anotado como deuda en `docs/estado.md`). No añadas más consultas por ahí.

## Gradle

- **Las versiones se declaran solo en `gradle/libs.versions.toml`**, nunca a mano
  en un `build.gradle.kts`.
- Este proyecto usa **AGP 9**: el plugin de Android ya trae Kotlin integrado, así
  que no se aplica `org.jetbrains.kotlin.android`. Lee el catálogo antes de
  proponer sintaxis de Gradle de memoria: AGP 9 no se parece a AGP 8.
- **Dependencia nueva ⇒ pídela antes.** Hasta hoy el proyecto va con Compose y
  poco más, y esa es la gracia.
