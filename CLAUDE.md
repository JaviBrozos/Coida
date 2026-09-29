# Doado

**Una app para tus mayores.**

Doado es un *launcher* (pantalla de inicio) de Android para personas mayores que
se pelean con el móvil: menos cosas en pantalla, más grandes y con más contraste
que en un lanzador normal. El móvil lo usa una persona, pero lo configura otra,
así que la app tiene dos caras: una pantalla de inicio fija, y unos ajustes
detrás de un PIN.

El producto completo son tres piezas: el **launcher** (implementado), el **botón
flotante de «atrás»** (pendiente) y el **asistente de voz con IA** (pendiente, y
la principal propuesta de valor del producto).

## Dónde está cada cosa

| Necesitas… | Está en | Ojo |
| --- | --- | --- |
| Qué se pide y por qué: actores, casos de uso (CU-01…CU-16), requisitos no funcionales, glosario y conflictos abiertos | [`docs/ERS.md`](docs/ERS.md) | **Fuente única.** No dupliques casos de uso ni requisitos en otro sitio |
| Cómo está construido: navegación, estado, persistencia, intents, manifiesto | [`docs/arquitectura.md`](docs/arquitectura.md) | |
| Paleta, tipografía, medidas, iconos y componentes | [`docs/diseno.md`](docs/diseno.md) | Los valores exactos los manda el código |
| Por qué algo es como es, y qué se descartó | [`docs/decisiones.md`](docs/decisiones.md) | Se añaden entradas, no se reescriben |
| En qué punto está el trabajo hoy | [`docs/estado.md`](docs/estado.md) | Caduca: mira la fecha antes de fiarte |
| Presentación del proyecto en diez láminas | [`design/presentacion/`](design/presentacion) | El PDF está montado |
| Cómo se ve para alguien de fuera | [`README.md`](README.md) | |

Las normas de trabajo están en `.claude/rules/` y se cargan solas. **Las de UI,
datos y recursos solo aparecen al abrir un fichero de esa zona**, así que si vas
a *crear* una pantalla, una preferencia o un recurso desde cero, ábrelas tú:

- Tocar UI o Compose → `.claude/rules/ui-compose.md`
- Tocar `data/`, el manifiesto o Gradle → `.claude/rules/datos-y-android.md`
- Tocar `res/` → `.claude/rules/recursos.md`

## Quién usa Doado

| Actor | Qué hace | Dónde vive |
| --- | --- | --- |
| **ACT-01 Persona mayor** | Llama, escribe y hace fotos desde el inicio, siempre igual. No entra en los ajustes. | Pantalla de inicio |
| **ACT-02 Familiar o cuidador** | Configura el móvil: apps, contactos de un toque, letra y tema. | Ajustes, tras el PIN |
| **ACT-03 Asistente de IA** | Interpreta órdenes habladas y actúa o guía. | Pendiente |

El **modo administrador** no es un actor: es un estado temporal de 15 minutos en
el que el familiar abre las salidas a Android. La ficha completa de cada actor
está en la ERS, sección 4.1.1.

## Los cinco invariantes

Si se rompe uno de estos, se rompe el producto. El resto de decisiones, con su
contexto, están en `docs/decisiones.md`.

1. **La accesibilidad manda sobre la estética.** Siempre, y sin excepciones
   silenciosas: si una propuesta visual choca con una norma, gana la norma.
2. **Las casillas no cambian de sitio.** Una app que no está se pinta apagada,
   pero no se quita. La posición es memoria muscular.
3. **Nada de color dinámico** (Material You): con los colores del fondo de
   pantalla del usuario no se puede garantizar el contraste.
4. **El cian de marca nunca para texto ni iconos sobre fondo claro**: se queda en
   2,2:1. Para eso está el marino.
5. **Los ajustes van detrás del PIN**, y las salidas a Android detrás del modo
   administrador. Es lo que evita que el móvil se desconfigure solo.

## Lo que Doado no va a hacer

- No tiene servidor propio, cuentas de usuario ni analítica.
- No pide permisos nuevos ni dependencias nuevas sin acordarlo antes.
- No lee toda la agenda: solo el contacto que elige el familiar en el selector.
- No promete impedir salir a Android: eso exigiría ser propietaria del
  dispositivo, y está descartado (ver `docs/decisiones.md`).

## Stack

- **Kotlin** + **Jetpack Compose** (Material 3). No hay XML de layouts: la UI es
  toda Compose. Los XML de `res/` son solo tema de arranque, iconos y textos.
- Un único módulo Gradle, `:app`. minSdk 24, compileSdk/targetSdk 37.
- **AGP 9**: el plugin de Android ya trae Kotlin integrado. Por eso en
  `app/build.gradle.kts` **no** se aplica `org.jetbrains.kotlin.android`: hacerlo
  hace fallar el build con "no longer required for Kotlin support since AGP 9.0".
  Solo se aplica el plugin del compilador de Compose.
- **Version Catalog** (`gradle/libs.versions.toml`): las versiones se tocan solo
  ahí. Antes de proponer sintaxis de Gradle, léelo: AGP 9 no se parece a AGP 8.
- **No hay pruebas automáticas** (`app/src` solo tiene `main`). No inventes
  comandos de test: se verifica compilando y probando a mano en el emulador.

## Estructura

Se organiza **por feature, no por capa**: cada carpeta es un trozo de producto
con todo lo suyo dentro, en vez de un cajón de "viewmodels" y otro de
"repositories".

```
app/src/main/java/com/doado/
├── launcher/       Inicio y la Activity: navegación, permisos e Intents.
├── settings/       Ajustes: PIN, interfaz, contactos, apps y modo administrador.
├── accessibility/  (vacío) Botón flotante de «atrás».
├── assistant/      (vacío) Asistente de voz.
├── core/ui/        Tema, medidas y piezas comunes. Todo lo visual bebe de aquí.
└── data/           Apps instaladas, casillas, contactos y ajustes guardados.
```

El `namespace` es `com.doado` y el `applicationId`, `com.doado.launcher`. Es a
propósito: así el paquete de feature puede llamarse `launcher` sin acabar en
`com.doado.launcher.launcher`. La clase `R` es `com.doado.R`.

## Comandos

```bash
./gradlew assembleDebug     # compila el APK de depuración
./gradlew installDebug      # lo instala en el dispositivo/emulador conectado
./gradlew lint              # análisis estático de Android
```

Hace falta `local.properties` con la ruta del SDK (está en `.gitignore` porque es
propio de cada equipo):

```
sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
```

Para verlo **como pantalla de inicio**, instalar no basta:

```bash
adb shell cmd package set-home-activity com.doado.launcher/.LauncherActivity
```

## Si se rompe el launcher

Un launcher roto deja el móvil sin pantalla de inicio. Antes de tocar el
manifiesto, ten esto a mano:

```bash
adb shell cmd package set-home-activity <otro.paquete/.SuActividad>   # devolver el mando
adb uninstall com.doado.launcher                                      # último recurso
```

En el móvil: *Ajustes → Aplicaciones → Aplicaciones predeterminadas → App
principal*. Desinstalar o borrar datos pierde PIN, contactos y casillas; es
también la única salida cuando se olvida el PIN (conflicto CON-05 de la ERS).

## Notas que se olvidan siempre

- El bloque `<queries>` del manifiesto no es opcional: desde Android 11, sin él
  el `PackageManager` no ve el resto de apps y el inicio sale vacío. **Favorito
  nuevo ⇒ declarar también su intención ahí.**
- Los favoritos se definen por **acción** (`ACTION_DIAL`,
  `CATEGORY_APP_MESSAGING`…) y no por paquete, para que funcionen sea cual sea la
  marca del móvil.
- La actividad declara `HOME` + `DEFAULT` y además `LAUNCHER`. Esto último es
  comodidad de desarrollo: cuando Doado sea el lanzador de verdad, se puede
  quitar.
- Todo el proyecto está en español: interfaz, comentarios y mensajes de commit.
  Los identificadores de código, en inglés.
