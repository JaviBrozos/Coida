# Coida

**Una app para tus mayores.**

Coida es un *launcher* (pantalla de inicio) de Android para personas mayores que
se pelean con el móvil: menos cosas en pantalla, más grandes y con más contraste.
El móvil lo usa una persona y lo configura otra, así que la app tiene dos caras:
una pantalla de inicio fija y unos ajustes detrás de un PIN.

El producto que se publica es el **launcher**. El **asistente de voz con IA** se
diseñó como la principal propuesta de valor y está programado, pero en las pruebas
a las personas mayores no les resultó útil: la versión pública (Google Play y la APK
de GitHub) sale **sin asistente**, que solo existe en la de depuración (D-63, D-67).
Hasta el 09/11/2026 se llamaba **Doado** (D-62). El repositorio es público (D-68).

## Obligatorio en cada cambio

Ningún cambio está terminado sin esto, lo haga una persona o Claude:

1. **Registra el cambio en [`docs/estado.md`](docs/estado.md)**: una fila nueva
   arriba del registro con fecha, autor y qué se hizo, y ajusta «Dónde estamos»,
   «Sin probar» y «Siguiente» si cambian. Actualiza la fecha de cabecera.
   - Autor: la persona que hace o pide el cambio (`git config user.name`).
   - El fichero no pasa de unas **50 líneas**: al añadir, borra las filas más
     antiguas del registro. El historial completo está en git.
2. **Corrige la documentación que el cambio deje en falso** (`docs/`, `README.md`,
   este fichero) en el mismo commit. Si no da tiempo, borra la línea: vacío es
   mejor que falso.
3. **Decisión que descarta una alternativa razonable** ⇒ entrada nueva en
   [`docs/decisiones.md`](docs/decisiones.md). Las antiguas no se reescriben.
4. **Casos de uso y requisitos solo en [`docs/ERS.md`](docs/ERS.md)**. Si cambia
   el estado de uno, actualiza la ERS y su historial de revisiones.

## Mapa del proyecto

| Necesitas… | Está en |
| --- | --- |
| Actores, casos de uso, requisitos, glosario y conflictos abiertos (**fuente única**) | [`docs/ERS.md`](docs/ERS.md) |
| Navegación, estado, persistencia, intents y manifiesto | [`docs/arquitectura.md`](docs/arquitectura.md) |
| Paleta, tipografía, medidas, iconos y componentes | [`docs/diseno.md`](docs/diseno.md) |
| Por qué algo es como es y qué se descartó | [`docs/decisiones.md`](docs/decisiones.md) |
| Qué hay hecho, qué falta y quién hizo qué | [`docs/estado.md`](docs/estado.md) |
| Propuestas en discusión, antes de decidirlas (hoy, CON-06) | [`docs/propuestas/`](docs/propuestas) |
| Presentación en diez láminas, con PDF | [`design/presentacion/`](design/presentacion) |
| Web de presentación, sin servidor (D-52, D-60) | [`web/`](web) |
| Vídeo de marca de 53 s, con su código (D-53) | [`design/video/`](design/video) |
| Publicar en Google Play: privacidad, permisos, ficha y capturas | [`publishing/`](publishing) |
| Vista para alguien de fuera | [`README.md`](README.md) |

Los valores exactos (colores, medidas, claves) los manda el código: si un
documento no cuadra, gana el código y se corrige el documento.

Las normas de trabajo están en `.claude/rules/` y se cargan solas. **Las de UI,
datos y recursos solo aparecen al abrir un fichero de esa zona**; si vas a crear
algo desde cero, ábrelas tú:

- UI o Compose → `.claude/rules/ui-compose.md`
- `data/`, manifiesto o Gradle → `.claude/rules/datos-y-android.md`
- `res/` → `.claude/rules/recursos.md`
- `assistant/` → `.claude/rules/asistente.md`

Para auditar accesibilidad hay un subagente: `.claude/agents/accesibilidad.md`.
Toda UI nueva pasa por él antes de darse por hecha.

La marca está en la skill `.claude/skills/coida-brand/`: logo, icono de la app,
colores, iconos, letra, tono y movimiento, con los ficheros de la marca en
`assets/`. Úsala en cualquier pieza que se vea, dentro o fuera de la app. Para
auditar la marca hay otro subagente,
`.claude/agents/auditor-marca.md`, que trabaja con la skill
`.claude/skills/brand-identity/`. La letra es Inter (D-50), en `res/font/`.

Hay una skill de diseño en el proyecto, `.claude/skills/apple-design/` (de Emil
Kowalski, licencia MIT): los principios de Apple para interfaces. Sirve de
inspiración, pero **mandan las normas de Coida**; qué se tomó y qué no, en D-32.

## Producto

### Quién usa Coida

| Actor | Qué hace | Dónde vive |
| --- | --- | --- |
| **ACT-01 Persona mayor** | Llama, escribe y hace fotos desde el inicio, siempre igual. No entra en los ajustes. | Pantalla de inicio |
| **ACT-02 Familiar o cuidador** | Configura el móvil: apps, contactos de un toque, letra y tema. | Ajustes, tras el PIN |
| **ACT-03 Asistente de IA** | Interpreta órdenes habladas y dice lo que va a hacer y lo hace, con opción a cancelar (D-56); o guía. | Círculo fijo del inicio |

El **modo administrador** no es un actor: es un estado de 15 minutos en el que el
familiar abre las salidas a Android. Fichas completas en la ERS, §4.1.1.

### Los cinco invariantes

Si se rompe uno, se rompe el producto.

1. **La accesibilidad manda sobre la estética**, sin excepciones silenciosas.
2. **Las casillas no cambian de sitio.** Una app que falta se pinta apagada, pero
   no se quita: la posición es memoria muscular.
3. **Nada de color dinámico** (Material You): no garantiza el contraste.
4. **El cian de marca nunca para texto ni iconos sobre fondo claro** (2,32:1).
   Para eso está el marino.
5. **Los ajustes van detrás del PIN**, y las salidas a Android detrás del modo
   administrador.

### Lo que Coida no va a hacer

- Servidor propio, cuentas de usuario ni analítica.
- Permisos o dependencias nuevas sin acordarlo antes.
- Leer toda la agenda: solo el contacto que elige el familiar en el selector y,
  con su permiso, la foto de los números guardados en Coida (D-57).
- Impedir de verdad salir a Android: exigiría ser propietaria del dispositivo, y
  está descartado.

Los conflictos abiertos de la ERS (§7) son decisiones de los dos creadores: si
una tarea depende de uno, **para y pregunta**.

## Técnica

### Stack

- **Kotlin** + **Jetpack Compose** (Material 3). Sin XML de layouts; los XML de
  `res/` son solo tema de arranque, iconos y textos.
- Un único módulo, `:app`. minSdk 24, compileSdk/targetSdk 37.
- **AGP 9** trae Kotlin integrado: en `app/build.gradle.kts` **no** se aplica
  `org.jetbrains.kotlin.android` (el build falla con "no longer required for
  Kotlin support since AGP 9.0"). Solo el plugin del compilador de Compose.
- **Version Catalog**: las versiones se tocan solo en `gradle/libs.versions.toml`.
  Léelo antes de proponer sintaxis de Gradle: AGP 9 no se parece a AGP 8.
- **No hay pruebas automáticas.** No inventes comandos de test: se compila y se
  prueba a mano en el emulador.

### Estructura

Por feature, no por capa:

```
app/src/main/java/com/coida/
├── launcher/   Inicio y la Activity: navegación, permisos e Intents.
├── settings/   Ajustes: PIN, interfaz, contactos, apps y modo administrador.
├── assistant/  Asistente de voz: grabar, nube (solo en cloud/), hacerlo o guiar.
├── core/ui/    Tema, medidas y piezas comunes. Todo lo visual bebe de aquí.
└── data/       Apps instaladas, casillas, contactos, ajustes y PIN guardados.
```

El `namespace` es `com.coida` y el `applicationId`, `com.coida.launcher`, a
propósito: así el paquete `launcher` no acaba en `com.coida.launcher.launcher`.
La clase `R` es `com.coida.R`.

### Comandos

```bash
./gradlew assembleDebug     # compila el APK de depuración
./gradlew assembleFamilia   # la APK que se manda a mano a la familia (D-66)
./gradlew installDebug      # lo instala en el dispositivo/emulador conectado
./gradlew lint              # análisis estático: se espera 0 errores
adb shell cmd package set-home-activity com.coida.launcher/.LauncherActivity   # fijarlo como inicio
```

Hace falta `local.properties` con la ruta del SDK (en `.gitignore`):
`sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk`. Para el asistente,
además `coida.ai.apiKey=…` (Groq); el resto de claves `coida.ai.*` están en
`docs/arquitectura.md`. **La clave nunca va en git ni en un log.**

### Si se rompe el launcher

Un launcher roto deja el móvil sin pantalla de inicio. Ten esto a mano antes de
tocar el manifiesto:

```bash
adb shell cmd package set-home-activity <otro.paquete/.SuActividad>   # devolver el mando
adb uninstall com.coida.launcher                                      # último recurso
```

En el móvil: *Ajustes → Aplicaciones → Aplicaciones predeterminadas → App
principal*. Borrar datos pierde PIN, contactos y casillas; sin pregunta de
recuperación configurada, es la única salida si se olvida el PIN.

### Notas que se olvidan siempre

- El bloque `<queries>` del manifiesto no es opcional: sin él el `PackageManager`
  no ve las demás apps. **Favorito nuevo ⇒ declarar su intención ahí.**
- Los favoritos se definen por **acción** (`ACTION_DIAL`,
  `CATEGORY_APP_MESSAGING`…), no por paquete, para que valgan en cualquier marca.
- La actividad declara `HOME` + `DEFAULT` y además `LAUNCHER`. Lo último **no se
  quita al publicar**: sin él no hay botón «Abrir» en Google Play ni forma de
  llegar al aviso que la pone de inicio (D-64).
- El asistente va detrás de `BuildConfig.ASSISTANT_ENABLED` (falso en release) y
  sus permisos (micrófono, internet) solo en `src/debug/AndroidManifest.xml`
  (D-63). El manifiesto de `main` es lo que se publica.
- La bola de llamadas perdidas de «Llamadas» va detrás de
  `BuildConfig.MISSED_CALLS_ENABLED`, y `READ_CALL_LOG` solo en `src/debug` y
  `src/familia`: Google Play no se lo deja a un launcher. La variante `familia` es
  la release más la bola, y es la APK que se manda a mano (D-66).
- La release se firma con `keystore.properties` (fuera de git, como el `.jks`).
  Sin él se firma con la clave de depuración, que Play rechaza.
  `./gradlew bundleRelease` deja el `.aab` en `app/build/outputs/bundle/release/`.
- Todo en español: interfaz, comentarios, commits y documentación. Los
  identificadores de código, en inglés.
