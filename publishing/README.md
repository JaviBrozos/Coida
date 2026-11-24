# Publicar Coida en Google Play

| Fichero | Para qué |
| --- | --- |
| `privacy-policy.md` | La política de privacidad, lista para publicar en una URL pública |
| `data-safety.md` | Las respuestas de «Seguridad de los datos», justificadas con el código |
| `permissions.md` | Cada permiso, para qué sirve y qué pasa si se deniega |
| `store-listing.md` | Título, descripción breve y completa, y datos de la ficha |
| `release-notes.md` | Las notas de la 1.0.0 |
| `capturas.md` | La lista de capturas, el gráfico destacado y cómo sacarlas |
| `icono-512.png` | El icono para la ficha (512 px, 32 bits) |
| `grafico-destacado.png` | El gráfico destacado de la ficha (1024 × 500, 24 bits) |
| `linkedin.md` y `linkedin.png` | El post para presentar el proyecto en LinkedIn, con su imagen |

## 1. Generar la clave de subida (una vez, a mano)

El keystore **no va en el repositorio** y no lo genera nadie más que tú. Guárdalo
con su contraseña en un sitio seguro y con copia: si lo pierdes, hay que pedir a
Google que lo cambie.

```bash
mkdir -p "$HOME/claves"
"/c/Program Files/Android/Android Studio/jbr/bin/keytool" -genkeypair -v \
  -keystore "$HOME/claves/coida-upload.jks" -alias coida-upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Después, crea `keystore.properties` en la raíz del proyecto. Está en `.gitignore`,
igual que `*.jks`:

```properties
storeFile=C:/Users/<usuario>/claves/coida-upload.jks
storePassword=…
keyAlias=coida-upload
keyPassword=…
```

## 2. Generar el paquete

```bash
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

Sin `keystore.properties`, Gradle avisa y firma con la clave de depuración. Ese
`.aab` sirve para probar, pero Play lo rechaza.

## 3. Play Console, en orden

- [ ] **Cuenta de desarrollador** (pago único de 25 USD) y verificación de
      identidad. Si es una cuenta personal creada después de noviembre de 2023,
      Play exige una **prueba cerrada con al menos 12 testers durante 14 días
      seguidos** antes de dejar publicar en producción.
- [ ] **Crear la app**: nombre «Coida», idioma predeterminado español (España),
      tipo App y **Gratis**. Ojo: una app gratis ya no puede pasar a ser de pago.
- [ ] **Firma de apps de Play**: aceptarla. La clave de subida es la del paso 1.
- [ ] **Política de privacidad**: publicar `privacy-policy.md` en una URL
      pública (GitHub Pages necesita un repositorio público, o una página en la
      web) y pegar la URL. Rellenar antes el responsable y el correo.
- [ ] **Acceso a la app**: «Toda la funcionalidad está disponible sin acceso
      especial». No hacen falta credenciales: el PIN de los ajustes lo crea quien
      entra la primera vez.
- [ ] **Anuncios**: «No, mi app no contiene anuncios».
- [ ] **Clasificación de contenido**: cuestionario IARC, categoría «Utilidades,
      productividad, comunicación u otros». Todo «No» (sin violencia, sin
      contenido de usuarios, sin compras ni compartición de ubicación).
- [ ] **Público objetivo**: 18 años o más. No marcar menores, para no entrar en
      la política de Familias.
- [ ] **Seguridad de los datos**: según `data-safety.md` («no se recogen ni se
      comparten datos»).
- [ ] **App de noticias, de salud o del gobierno**, y funciones financieras: No.
- [ ] **Ficha de la tienda**: textos de `store-listing.md`, el icono, el gráfico
      destacado y las capturas (`capturas.md`). Categoría Personalización.
- [ ] **Países**: al menos España. La casilla de Emergencias marca el 112, que
      vale en toda la UE.
- [ ] **Prueba cerrada** con el `.aab`, las notas de `release-notes.md` y la
      lista de testers. Revisar el **informe previo al lanzamiento** (fallos,
      accesibilidad y capturas en móviles reales).
- [ ] Pasados los 14 días, **pedir acceso a producción** y publicar.

## Antes de cada versión nueva

- [ ] Subir `versionCode` (y `versionName`) en `app/build.gradle.kts`.
- [ ] Si cambian los permisos del manifiesto de `main` o hay una dependencia nueva,
      repasar `privacy-policy.md`, `data-safety.md` y `permissions.md`.
- [ ] `./gradlew lint` sin errores, y la release probada en el emulador.
