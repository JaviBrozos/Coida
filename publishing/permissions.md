# Permisos de Coida 1.0.0

Los permisos del manifiesto fusionado de release, cada uno con para qué sirve,
cuándo se pide y qué pasa si se deniega. **La persona mayor nunca ve un diálogo de
permisos**: los dos que se piden aparecen con el familiar delante, en Ajustes.

## Los que pide la app

### `android.permission.CALL_PHONE` (peligroso)

- **Para qué**: llamar con un solo toque desde el acceso directo de una persona
  (`LauncherActivity.callIntent`, con `ACTION_CALL`).
- **Cuándo se pide**: al añadir el primer contacto en Ajustes → Contactos.
- **Si se deniega**: el acceso directo abre el marcador con el número escrito
  (`ACTION_DIAL`) y hay que pulsar llamar. Nada deja de funcionar.
- **En Play**: no está entre los permisos restringidos (SMS y registro de llamadas),
  así que **no lleva formulario de declaración**. Coida no lee el registro de
  llamadas ni los SMS.
- Va con `<uses-feature android:name="android.hardware.telephony"
  android:required="false" />`, para que Play no deje fuera las tabletas sin
  teléfono.

### `android.permission.READ_CONTACTS` (peligroso)

- **Para qué**: solo para mostrar en el inicio la foto que cada contacto guardado
  en Coida tiene en la agenda (D-57, `data/ContactPhotos`). Busca por los números
  guardados en Coida, uno a uno; nunca recorre la agenda entera.
- **Cuándo se pide**: junto al anterior, al añadir un contacto, y otra vez si el
  familiar pulsa «Permitir» en Ajustes → Contactos. Esa pantalla explica para qué
  es (`contacts_photos_note`).
- **Si se deniega**: los contactos salen con su inicial.
- **En Play**: no lleva formulario de declaración, pero son datos personales y
  sensibles. Por eso hace falta la política de privacidad y que cuadre con
  «Seguridad de los datos».
- **Alternativa que se miró**: quitar el permiso y leer la foto con el acceso
  temporal que da el selector de contactos. No es fiable, porque ese acceso cubre
  la fila del teléfono elegido y no la URI de la foto. Además reabriría D-57.

### `com.coida.launcher.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (de firma)

- **Para qué**: lo añade `androidx.core` automáticamente. Protege los receptores
  dinámicos de la propia app para que otras apps no puedan enviarles nada.
- No se pide al usuario ni se ve en ningún sitio. No hay que declararlo.

## Lo que Coida usa sin pedir permiso

- **Selector de contactos** (`ACTION_PICK`): Android da acceso solo a la persona
  elegida (nombre y número).
- **Selector de fotos** (`PickVisualMedia`): Android da solo la foto elegida para
  el fondo.
- **Visibilidad de paquetes**: un bloque `<queries>` acotado (apps que se pueden
  abrir, teléfono, mensajes, cámara, contactos, WhatsApp, Play Store). **No se usa
  `QUERY_ALL_PACKAGES`**, que sí exigiría declaración en Play.
- **Pantalla de inicio**: `RoleManager.ROLE_HOME` en Android 10 o posterior, y los
  ajustes de Android en versiones anteriores. No es un permiso.

## Lo que la versión publicada no tiene

Lo usa solo el asistente de voz, que está en la versión de depuración
(`app/src/debug/AndroidManifest.xml`, D-63):

- `RECORD_AUDIO`, para escuchar la orden.
- `INTERNET`, para enviarla al servicio de IA.

Tampoco hay servicio de accesibilidad, ni permisos de SMS, de registro de llamadas,
de ubicación o de acceso a todos los archivos, ni `QUERY_ALL_PACKAGES`. Ninguno de
los permisos de la app exige formulario de declaración en Play Console.
