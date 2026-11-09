# Cómo se trabaja en Coida

## Idioma y estilo

- **Todo lo visible va en español de España**: interfaz, textos de error,
  comentarios, mensajes de commit y documentación. Los identificadores de código
  (clases, funciones, variables) van en inglés; no mezcles los dos dentro de un
  mismo nombre.
- Los comentarios explican **el porqué**, no el qué. Un comentario que repite lo
  que dice la línea siguiente sobra; uno que explica una decisión rara, no.
- Lleva KDoc en las clases y en las decisiones que sorprendan a quien lea el
  fichero dentro de seis meses.
- Nada de emoji, ni en el código ni en la interfaz ni en los commits.

## Compilar y probar

```bash
./gradlew assembleDebug     # compila
./gradlew installDebug      # instala en el dispositivo o emulador conectado
./gradlew lint              # análisis estático: se espera 0 errores
```

En esta máquina, `JAVA_HOME` es el JDK de Android Studio
(`C:\Program Files\Android\Android Studio\jbr`) y `adb` está en
`%LOCALAPPDATA%\Android\Sdk\platform-tools`.

**No hay pruebas automáticas** (`app/src` solo tiene `main`). No inventes
comandos de test ni suites que no existen: se verifica compilando y probándolo a
mano.

Para verlo como pantalla de inicio de verdad:

```bash
adb shell cmd package set-home-activity com.coida.launcher/.LauncherActivity
```

Si vas a tocar el manifiesto, ten a mano cómo devolver el mando a otro launcher
(`CLAUDE.md`, sección «Si se rompe el launcher»): un launcher roto deja el móvil
sin pantalla de inicio.

## Cuándo una pantalla está «hecha»

1. Compila y `lint` no da errores.
2. Cumple los criterios del caso de uso correspondiente de la ERS, y los citas
   por su ID (CU-xx) al contar lo que hiciste.
3. Pasa el repaso de `.claude/rules/accesibilidad.md`.
4. La has visto funcionando en el emulador, no solo compilando.
5. La documentación que tu cambio invalida está corregida.

## Documentación: se corrige o se borra

Si tu cambio deja en falso una frase de `docs/`, `README.md` o `CLAUDE.md`,
**arréglala en el mismo commit**. Si no te da tiempo, bórrala: vacío es mejor que
falso.

- ¿Decisión que descarta una alternativa razonable? Entrada nueva en
  `docs/decisiones.md`, con fecha. Las viejas no se reescriben.
- **En cada cambio**, fila nueva en el registro de `docs/estado.md` con fecha y
  autor, sin pasar de unas 50 líneas (detalle en `CLAUDE.md`, «Obligatorio en
  cada cambio»).
- Casos de uso y requisitos **solo** en `docs/ERS.md`. No los dupliques.

## Git

- Mensajes en español, en imperativo y en una línea («Añade…», «Arregla…»,
  «Documenta…»), con el cuerpo explicando el porqué si hace falta.
- **No hagas commit ni push sin que te lo pidan.** Cuando te lo pidan, mira antes
  qué entra (`git status`) y no metas `local.properties`, APKs ni claves.
- Código y documentación, en commits separados.
