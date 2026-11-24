# Coida en LinkedIn

El post para presentar el repositorio, una versión corta, la imagen y el «About»
de GitHub. Los textos van entre `<<<` y `>>>` para copiarlos tal cual. Cada cifra
sale del repositorio: si cambia algo (el número de decisiones, los permisos),
corrígelo aquí antes de publicar.

## Post principal

1.609 caracteres, hashtags incluidos.

<<<
Lo mejor que hicimos con nuestra app fue quitarle la función estrella.

Somos Javier Brozos y Gonzalo Gómez, estudiantes de Ingeniería Informática en la UPM y en la UDC. Desde septiembre construimos Coida, una pantalla de inicio de Android para personas mayores.

El problema lo conoce cualquiera con abuelos: letra pequeña, iconos que cambian de sitio y menús que no se entienden. Y el móvil lo usa una persona, pero lo configura otra.

Coida tiene dos caras:
· Un inicio con casillas grandes que nunca se mueven, contactos favoritos a un toque y una casilla de emergencias con el 112.
· Unos ajustes detrás de un PIN para el familiar, con un modo administrador de 15 minutos.

Lo probamos con dos personas mayores y cambiamos seis cosas. Les sobraba tamaño y pedían cuatro casillas por fila. Querían ver la foto de cada contacto. Y el asistente de voz con IA, que programamos entero y era nuestra gran apuesta, no les resultó útil: usaban las casillas que ya conocían. Lo quitamos de la versión pública. El código sigue en el repositorio.

Por dentro: Kotlin y Jetpack Compose, dos permisos, sin acceso a internet, el PIN derivado con PBKDF2, contrastes medidos y 68 decisiones documentadas con lo que descartamos y por qué.

Estamos preparando su lanzamiento en Google Play. Mientras tanto, todo es público y la APK se instala en cualquier Android 7.0 o posterior:
Repositorio: https://github.com/JaviBrozos/Coida
APK: https://github.com/JaviBrozos/Coida/releases/latest

¿Qué función quitarías de tu último proyecto si se lo enseñaras a quien lo va a usar?

#Android #JetpackCompose #Kotlin #Accesibilidad
>>>

## Versión corta

492 caracteres. Para un comentario, una historia o un mensaje directo.

<<<
Programamos un asistente de voz con IA para nuestra app… y lo quitamos.

Somos Javier Brozos (UPM) y Gonzalo Gómez (UDC), estudiantes de Ingeniería Informática. Coida es una pantalla de inicio de Android para personas mayores: casillas grandes que no cambian de sitio y ajustes tras un PIN para el familiar. Al probarlo con dos personas mayores, el asistente no les resultó útil, así que salió de la versión pública.

Código y APK: https://github.com/JaviBrozos/Coida

#Android #Accesibilidad
>>>

## Imagen y enlaces

- **Imagen**: [`linkedin.png`](linkedin.png) (1200 × 627). El logotipo, el lema
  «El móvil de tus mayores, fácil otra vez.», los autores y dos móviles con el
  inicio, en oscuro y en claro.
- **Texto alternativo** para la imagen (LinkedIn lo pide al subirla, en
  «Texto alternativo»):

  <<<
  Logotipo de Coida junto al lema «El móvil de tus mayores, fácil otra vez.» y los nombres de Javier Brozos (UPM) y Gonzalo Gómez (UDC). A la derecha, dos móviles con el inicio de Coida: casillas grandes de Llamadas, Mensajes, Cámara y Contactos, y los contactos favoritos.
  >>>

- **Dónde van los enlaces**: en el cuerpo del post, como están. Al llevar la
  imagen adjunta, LinkedIn no genera la tarjeta de vista previa del enlace, así
  que no compiten. Si preferís el post sin enlaces, quitad las dos líneas y poned
  el del repositorio en el primer comentario.
- **Antes de publicar**: comprobad que el repositorio ya es público, que la
  Release v1.0.1 existe con `Coida-1.0.1.apk` (la de **release**, sin asistente) y
  que la *social preview* de GitHub es `docs/img/cabecera.png`.

## Hashtags

Cuatro en el post principal, dos en la corta. Si hace falta cambiar alguno:
`#Android`, `#JetpackCompose`, `#Kotlin`, `#Accesibilidad`, `#DiseñoInclusivo`.

## About de GitHub

En el repositorio, el engranaje de «About».

**Descripción** (314 caracteres; el máximo es 350):

<<<
Pantalla de inicio de Android para personas mayores: casillas grandes que no cambian de sitio, contactos a un toque y ajustes tras un PIN para el familiar. Kotlin y Jetpack Compose, dos permisos y sin acceso a internet. Probada con personas mayores. Hecha por dos estudiantes de Ingeniería Informática (UPM y UDC).
>>>

**Website**: vacío. No hay web publicada todavía; la de `web/` se abre en local.

**Topics** (8):

<<<
android, kotlin, jetpack-compose, material3, android-launcher, accessibility, elderly, inclusive-design
>>>
