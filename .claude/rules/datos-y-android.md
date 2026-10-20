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
- **No añadas permisos por iniciativa propia.** Hoy hay tres: `CALL_PHONE` y
  `RECORD_AUDIO`, que son peligrosos y se piden con el familiar delante (al añadir
  un contacto y al encender el asistente), e `INTERNET`, solo para el asistente.
  La persona mayor nunca ve un diálogo de permisos.
- **Solo HTTPS** (`usesCleartextTraffic="false"`).
- **`TextToSpeech` necesita su `<intent>` de `TTS_SERVICE` en `<queries>`**:
  desde Android 11, sin él no encuentra el motor de voz y el asistente se queda
  mudo sin dar error.

## Intents

- **Todo intent hacia fuera se lanza con reserva**: usa `startFirst(...)`, que
  prueba las alternativas en orden, captura `ActivityNotFoundException` y
  `SecurityException` y avisa en lenguaje llano si ninguna funciona.
- `FLAG_ACTIVITY_NEW_TASK` siempre: el lanzador abre la app en su propia tarea.
- Copia defensiva de las plantillas de `DefaultFavorites`: son compartidas y no
  deben salir con flags añadidos.

## Hilo principal

`PackageManager` hace IPC: **no lo llames desde una recomposición ni en el hilo
principal**. Las consultas de `onCreate` y `onResume` van por `loadApps()` en
`LauncherActivity`, en `Dispatchers.IO` (D-43). Una consulta nueva va ahí dentro,
no suelta. Solo se queda en el hilo principal la que dispara un toque (abrir una
casilla), que es una sola.

## Secretos

- **Las claves van en `local.properties`** (que está en `.gitignore`) y llegan al
  código por `BuildConfig`. Nunca en un `.kt`, en `strings.xml` ni en un log.
- Hoy la clave del asistente viaja dentro del APK y se puede extraer: vale para
  probar, no para publicar (CON-06 en la ERS).

## Gradle

- **Las versiones se declaran solo en `gradle/libs.versions.toml`**, nunca a mano
  en un `build.gradle.kts`.
- Este proyecto usa **AGP 9**: el plugin de Android ya trae Kotlin integrado, así
  que no se aplica `org.jetbrains.kotlin.android`. Lee el catálogo antes de
  proponer sintaxis de Gradle de memoria: AGP 9 no se parece a AGP 8.
- **Dependencia nueva ⇒ pídela antes.** Hasta hoy el proyecto va con Compose y
  poco más, y esa es la gracia.
