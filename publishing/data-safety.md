# «Seguridad de los datos» en Play Console

Respuestas para el formulario de Coida 1.0.0, cada una con su justificación en el
código. Si una versión futura cambia el manifiesto de `main` o añade una
dependencia, **hay que repasar este documento y la política de privacidad antes de
subirla**.

## Cómo entiende Google «recoger» y «compartir»

- **Recoger**: transmitir datos **fuera del dispositivo** (a vosotros o a un
  tercero). Lo que solo se procesa en el móvil no cuenta como recogido.
- **Compartir**: pasar datos a un tercero. No cuenta lo que el usuario inicia y
  espera, como abrir WhatsApp con una conversación.

Coida trata varios datos personales, pero **todo en el móvil**, así que la
respuesta al formulario es «no se recogen ni se comparten datos».

## Respuestas

| Pregunta del formulario | Respuesta | Justificación |
| --- | --- | --- |
| ¿Tu app recoge o comparte alguno de los tipos de datos de usuario obligatorios? | **No** | La release no declara `android.permission.INTERNET` (ver más abajo cómo comprobarlo): Android no le deja abrir conexiones. No hay SDK de terceros: las dependencias son AndroidX y Compose (`app/build.gradle.kts`). |
| ¿Todos los datos de usuario que recoge tu app se cifran en tránsito? | No aplica | No hay tránsito. Si el formulario la pide, no se recoge nada. |
| ¿Ofreces una forma de que los usuarios soliciten que se eliminen sus datos? | No aplica | No hay datos en servidores ni cuentas. En el móvil se borran desinstalando o con «Borrar datos». |
| ¿Tu app permite crear cuentas? | **No** | No hay registro ni inicio de sesión. Por eso no hace falta URL de eliminación de cuenta. |
| Revisión de seguridad independiente | No | Opcional. |

Al responder «No» a la primera pregunta, el formulario termina, y la ficha mostrará
«No se recogen datos» y «No se comparten datos con terceros».

## Lo que Coida trata en el móvil

No hay que declararlo, pero conviene tenerlo a mano por si Google pregunta, y
cuadra con `privacy-policy.md`:

| Dato (categoría de Play) | Qué hace Coida | Dónde está en el código |
| --- | --- | --- |
| Contactos (nombre y número) | Guarda los que elige el familiar en el selector de contactos de Android, que da acceso solo a la fila elegida | `LauncherActivity.addPickedContact`, `data/ContactShortcutsStore` (`doado_contacts`) |
| Contactos (foto) | Con `READ_CONTACTS`, lee al momento la foto de los números guardados. Se queda en memoria, no se copia | `data/ContactPhotos` |
| Apps instaladas | Lista las apps que se pueden abrir, para las casillas. Con `<queries>` acotado, sin `QUERY_ALL_PACKAGES` | `data/InstalledAppsRepository`, `AndroidManifest.xml` |
| Fotos | Copia reducida de la foto de fondo elegida con el selector de fotos, sin permisos | `data/WallpaperStore` (`files/home_wallpaper.jpg`) |
| Identificador (de la app) | UUID aleatorio por instalación, que no se usa ni sale del móvil | `data/InstallationId` (`coida_install`) |
| Otros | El PIN y la respuesta de recuperación, derivados con PBKDF2 y sal | `data/PinStore` (`doado_security`) |

### La copia de seguridad de Android

`android:allowBackup="true"` deja que la copia de seguridad del sistema (de Google
y controlada por el usuario) incluya preferencias, casillas y contactos
(`res/xml/backup_rules.xml` y `data_extraction_rules.xml`). Quedan fuera el PIN,
la foto de fondo y el identificador de instalación. Según la ayuda de Play
Console, los datos que se transfieren con la copia de seguridad de Android no se
declaran como recogidos, porque el desarrollador no tiene acceso a ellos.
**Compruébalo en la ayuda de «Seguridad de los datos» al rellenar el formulario.**

## Cómo comprobar que la release no tiene internet

```bash
./gradlew bundleRelease
# Permisos del manifiesto fusionado de release:
grep -o 'uses-permission[^/]*' app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
```

Tiene que salir solo `CALL_PHONE`, `READ_CONTACTS` y
`com.coida.launcher.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, este último
interno de AndroidX (ver `permissions.md`). Comprobado el 09/11/2026.
