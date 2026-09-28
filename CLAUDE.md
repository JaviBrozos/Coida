# Doado

**Una app para tus mayores.**

Doado es un *launcher* (pantalla de inicio) de Android pensado para personas
mayores que se pelean con el móvil: menos cosas en pantalla, más grandes y con
más contraste que en un lanzador normal.

El producto completo son tres piezas. Ahora mismo solo está construida la
primera:

| Pieza | Estado |
| --- | --- |
| **Launcher** — pantalla de inicio con las apps favoritas | implementado (MVP) |
| **Botón flotante de "atrás"** — servicio de accesibilidad | pendiente, carpeta vacía |
| **Asistente de voz** | pendiente, carpeta vacía |

## Stack

- **Kotlin** + **Jetpack Compose** (Material 3). No hay XML de layouts: la UI es
  toda Compose. Los XML de `res/` son solo tema de arranque, iconos y textos.
- Un único módulo Gradle, `:app`.
- **AGP 9**: el plugin de Android ya trae Kotlin integrado. Por eso en
  `app/build.gradle.kts` **no** se aplica `org.jetbrains.kotlin.android`: hacerlo
  hace fallar el build con "no longer required for Kotlin support since AGP 9.0".
  Solo se aplica el plugin del compilador de Compose.
- **Gradle Kotlin DSL** (`build.gradle.kts`) y **Version Catalog**
  (`gradle/libs.versions.toml`). Las versiones se tocan **solo** en el catálogo,
  nunca a mano en un `build.gradle.kts`.
- minSdk 24, compileSdk/targetSdk 37.

## Estructura de paquetes

Se organiza **por feature, no por capa**: cada carpeta es un trozo de producto
con todo lo suyo dentro, en vez de tener un cajón de "viewmodels" y otro de
"repositories".

```
app/src/main/java/com/doado/
├── launcher/       Pantalla de inicio (reloj, contactos, casillas) y la Activity,
│                   que lleva la navegación y todo lo que abre Intents.
├── settings/       Ajustes para el familiar: PIN, interfaz, contactos, apps y
│                   modo administrador.
├── accessibility/  (vacío) Servicio del botón flotante de "atrás".
├── assistant/      (vacío) Asistente de voz.
├── core/ui/        Tema: paleta de alto contraste, escala tipográfica ampliada,
│                   medidas compartidas y piezas comunes (Components.kt).
│                   Todo lo visual bebe de aquí.
└── data/           Apps instaladas (PackageManager), casillas, contactos y ajustes
                    guardados (SharedPreferences).
```

Detalle a tener presente: el `namespace` es `com.doado` mientras que el
`applicationId` es `com.doado.launcher`. Es a propósito — así el paquete de
feature puede llamarse `launcher` sin acabar en `com.doado.launcher.launcher`.
La clase `R` es, por tanto, `com.doado.R`.

## La norma que no se salta

> **Toda UI nueva prioriza la accesibilidad sobre la estética.**

En concreto, antes de dar por buena una pantalla:

- **Contraste**: mínimo 4.5:1 con el fondo; para texto y bordes se busca 7:1.
  El cian de la marca (`DoadoCyan`) se queda en ~2.2:1 sobre blanco, así que
  **no se usa para texto ni iconos sobre fondo claro** — para eso está
  `DoadoNavy`, o `DoadoCyanDeep` si hace falta un secundario.
- **Objetivo táctil**: nunca por debajo de `DoadoDimens.MinTouchTarget` (48dp).
  Lo normal en Doado es bastante más: las casillas del grid pasan de 168dp.
- **Texto**: siempre desde `MaterialTheme.typography` (definida en
  `core/ui/Type.kt`, ~1.5x Material), siempre en `sp` para que respete el tamaño
  de letra que el usuario tenga puesto en los ajustes del sistema. Nada de
  tamaños a pelo en un composable.
- **Separación por borde, no por sombra**: una sombra suave desaparece para
  quien tiene poca sensibilidad al contraste.
- **Nada de color dinámico** (Material You). Tomar los colores del fondo de
  pantalla haría imposible garantizar el contraste. Está decidido en
  `core/ui/Theme.kt` y no debe reintroducirse.
- **Sin sitios que bailen**: si una app no está disponible, su casilla se pinta
  apagada pero **no se quita**. La posición de cada icono es memoria muscular.
- Etiquetas de TalkBack en todo lo pulsable; los iconos que van acompañados de
  texto son decorativos (`contentDescription = null`) para no oírlo dos veces.

## Comandos

```bash
./gradlew assembleDebug     # compila el APK de depuración
./gradlew installDebug      # lo instala en el dispositivo/emulador conectado
./gradlew lint              # análisis estático de Android
```

El proyecto necesita saber dónde está el SDK de Android. Android Studio crea
`local.properties` solo; para compilar desde consola en una máquina nueva hay
que crearlo (está en `.gitignore` porque es propio de cada equipo):

```
sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
```

## Notas sueltas

- La actividad de inicio declara `HOME` + `DEFAULT` (para que el sistema la
  ofrezca como lanzador) y además `LAUNCHER`, que hace que aparezca también en
  el cajón de apps. Esto último es una comodidad de desarrollo: cuando Doado sea
  el lanzador de verdad, se puede quitar.
- El bloque `<queries>` del manifiesto no es opcional. Desde Android 11 sin él
  el `PackageManager` no ve el resto de apps y el grid saldría vacío. Si se
  añade un favorito nuevo, hay que declarar también su intención ahí.
- Los favoritos se definen por **acción** (`ACTION_DIAL`, `CATEGORY_APP_MESSAGING`…)
  y no por nombre de paquete, para que funcionen sea cual sea la marca del móvil.
