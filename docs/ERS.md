# DOADO — Especificación de Requisitos

Borrador inicial de la ERS de DOADO: recoge lo ya construido en el repositorio y deja marcado lo que falta por definir.

| Campo | Valor |
| --- | --- |
| Proyecto | DOADO — launcher de Android para personas mayores con asistente de IA |
| Versión | 0.1 (borrador) |
| Fecha | 28/09/2026 |
| Autores | Gonzalo Gómez Blanco y Javier Brozos Martínez |
| Estado | Borrador |

### Historia de revisiones

| Versión | Fecha | Descripción | Autor |
| --- | --- | --- | --- |
| 0.1 | 28/09/2026 | Estructura inicial; actores, casos de uso y requisitos no funcionales extraídos del código y del README | Gonzalo Gómez Blanco y Javier Brozos Martínez |

## 1. Introducción

DOADO es un launcher (pantalla de inicio) de Android para personas mayores que tienen dificultades con el móvil. Reduce lo que hay en pantalla, lo agranda y sube el contraste, y separa dos caras: una pantalla de inicio fija para la persona mayor y unos ajustes protegidos por PIN para el familiar que la configura.

El producto completo tiene tres piezas: el launcher (implementado), un botón flotante de «atrás» sobre cualquier app (pendiente) y un asistente de voz con IA que guía al usuario y actúa en el móvil (pendiente, y la principal propuesta de valor).

Este documento tiene dos objetivos:

- Documentar lo ya construido como requisitos verificables, para no perder decisiones que hoy solo están en el código.
- Definir las piezas pendientes, sobre todo el asistente, antes de empezar a programarlas.

## 2. Participantes en el proyecto

DOADO es un proyecto personal de dos creadores, que comparten por igual el diseño, el desarrollo, el repositorio y este documento.

| Nombre | Rol |
| --- | --- |
| Gonzalo Gómez Blanco | Creador: análisis, diseño y desarrollo |
| Javier Brozos Martínez | Creador: análisis, diseño y desarrollo |

## 3. Descripción del sistema actual

Hoy una persona mayor con un Android tiene soluciones sueltas: launchers simplificados, modos fáciles del fabricante, móviles específicos y asistentes de voz generalistas. Ninguna junta en un mismo producto una pantalla de inicio accesible que no se desconfigura, el control del familiar y un asistente de IA pensado para ella.

### Situación de partida

- La persona mayor usa el launcher de serie del fabricante, con muchos iconos pequeños, notificaciones y gestos que no domina.
- Un familiar le configura el móvil de vez en cuando, y ella lo desconfigura sin querer: mueve iconos, entra en ajustes o cambia de pantalla.
- Las tareas reales son pocas: llamar a unas personas concretas, WhatsApp y fotos. Todo lo demás estorba.

### Alternativas existentes

| Alternativa | Tipo | Qué hace bien | Por qué no basta |
| --- | --- | --- | --- |
| [BIG Launcher](https://biglauncher.com/home/en/) | Launcher para mayores | Botones grandes en 6 disposiciones, botón SOS que envía la ubicación GPS por SMS, PIN del cuidador para bloquear ajustes y ocultar apps | La versión completa es de pago y el teléfono o los SMS grandes son apps aparte, también de pago. Su web no menciona funciones de voz ni de IA |
| [Otros launchers de Google Play](https://www.xataka.com/basics/launchers-android-para-personas-mayores) (Simple Launcher, Grand Launcher, Senior Homescreen…) | Launcher para mayores | Iconos y letra grandes, contactos como accesos directos; algunos con botón de emergencia o recordatorio de medicación | Varios son de pago o con anuncios, y se limitan a agrandar la interfaz: no guían al usuario |
| [Modo fácil de Samsung](https://androidphoria.com/tutoriales/modo-facil-de-samsung-que-es-y-como-se-activa) | Modo del fabricante | Iconos y texto más grandes, alto contraste, pulsación larga contra toques accidentales, panel de contactos rápidos | Solo existe en móviles Samsung y se desactiva con el mismo interruptor de Ajustes: no está pensado para que la persona mayor no lo desconfigure |
| [Móviles Doro](https://play.google.com/store/apps/details?id=com.doro.apps.mydoro.relative&hl=es_EC) + app Response by Doro | Móvil específico | El familiar recibe las alertas del botón de ayuda, ajusta brillo, volumen y contraste a distancia y ve la ubicación | Obliga a comprar un Doro; no sirve para el Android que la persona ya tiene |
| [Gemini en Android](https://support.google.com/gemini/answer/15574928?hl=es) | Asistente de voz generalista | Llama y envía mensajes de WhatsApp con órdenes en lenguaje natural | No está pensado para mayores: hay que configurar permisos y conexiones, y no lee mensajes ni envía imágenes por WhatsApp. No convive con una interfaz simplificada |
| [ElliQ](https://www.fundacionbankinter.org/noticias/elliq-la-ia-que-combate-la-soledad-y-revoluciona-el-cuidado-de-los-mayores/) | Robot de compañía con IA | IA proactiva: conversa, propone actividades, recuerda la medicación y facilita el contacto con la familia | Es un dispositivo aparte que no maneja el móvil, y no está claro que se venda en España |

### Qué aporta DOADO

1. **Las tres piezas en un solo producto**: interfaz simplificada, control del familiar y asistente de IA. Hoy hay que combinar un launcher, un asistente generalista y, a veces, un móvil específico.
2. **Un asistente hecho para mayores**: limitado a lo que el familiar ha configurado, que confirma antes de actuar y que puede guiar paso a paso en vez de solo ejecutar. Gemini hace lo contrario: es abierto y da por hecho que el usuario sabe configurarlo.
3. **Funciona en cualquier Android**: no exige comprar un Doro ni tener un Samsung. Los favoritos se definen por acción de Android, no por marca.
4. **Accesibilidad como requisito medible**: no solo iconos grandes, sino contraste mínimo 7:1 en texto, objetivos de 48 dp o más, sin color dinámico y sin iconos que cambien de sitio (sección 4.2).
5. **Bloqueado de verdad para la persona mayor**: PIN para los ajustes y modo administrador temporal para las salidas a Android, que se cierra solo a los 15 minutos. Se evita que el móvil quede abierto porque alguien olvidó volver a bloquearlo.

**Lo que otros tienen y DOADO todavía no**: botón SOS con ubicación (BIG Launcher, Doro), configuración a distancia por el familiar (Doro) y recordatorios de medicación (algunos launchers, ElliQ). Son candidatos a nuevos casos de uso.

Fuentes consultadas el 29/09/2026. La lista de launchers de Google Play procede de un artículo de diciembre de 2021, así que precios y funciones pueden haber cambiado.

## 4. Catálogo de requisitos

### 4.1.1 Definición de los actores

Tres actores: dos personas con distinto nivel de acceso y el asistente, que actúa en nombre de la persona mayor. El familiar puede hacer todo lo que hace la persona mayor, además de configurar el móvil.

| ID | Actor | Descripción | Sinónimos | Comentarios |
| --- | --- | --- | --- | --- |
| ACT-01 | Persona mayor | Usuario final. Llama, escribe y hace fotos desde la pantalla de inicio, siempre de la misma forma. No entra en los ajustes. | Usuario, mayor | Actor principal. Se asume visión reducida, poca destreza con pantallas táctiles y poca experiencia previa con smartphones. |
| ACT-02 | Familiar o cuidador | Configura el móvil tras el PIN: apps visibles, contactos de un toque, tamaño de letra y tema. Cuando necesita tocar Android por debajo (instalar apps, ajustes del sistema, cambiar la pantalla de inicio), activa el modo administrador durante 15 minutos (CU-12). | Configurador, tutor | Hereda de ACT-01. Suele estar presente de forma ocasional, no a diario. |
| ACT-03 | Asistente de IA | Actor secundario (sistema). Interpreta órdenes habladas de ACT-01 y ejecuta acciones en el móvil, o guía paso a paso. | Asistente de voz | Pendiente de definir. Qué puede hacer sin confirmación es un conflicto abierto (sección 7). |

El modo administrador no es un actor aparte: es un estado temporal en el que el familiar tiene más permisos. Por eso se modela como caso de uso (CU-12 a CU-14).

### 4.1.2 Casos de uso del sistema

16 casos de uso: 10 ya funcionan en el launcher, 2 están a medias y 4 son pendientes, casi todos del asistente. La fuente «Código» indica que el caso se ha extraído del repositorio.

| ID | Caso de uso | Actor | Importancia | Estado | Fuente |
| --- | --- | --- | --- | --- | --- |
| CU-01 | Abrir una app desde el inicio | ACT-01 | Alta | Implementado | Código |
| CU-02 | Contactar con una persona de un toque | ACT-01 | Alta | Implementado | Código |
| CU-03 | Volver atrás desde cualquier app | ACT-01 | Alta | Pendiente | README |
| CU-04 | Pedir ayuda al asistente | ACT-01, ACT-03 | Alta | Pendiente | Descripción del proyecto |
| CU-05 | Realizar una acción por voz | ACT-01, ACT-03 | Alta | Pendiente | README |
| CU-06 | Acceder a los ajustes con PIN | ACT-02 | Alta | Implementado | Código |
| CU-07 | Cambiar el PIN | ACT-02 | Media | Parcial | Código |
| CU-08 | Configurar la interfaz | ACT-02 | Alta | Implementado | Código |
| CU-09 | Gestionar los contactos de un toque | ACT-02 | Alta | Implementado | Código |
| CU-10 | Gestionar las apps del inicio | ACT-02 | Alta | Implementado | Código |
| CU-11 | Abrir los ajustes de sonido y pantalla | ACT-02 | Baja | Implementado | Código |
| CU-12 | Activar y desactivar el modo administrador | ACT-02 | Media | Implementado | Código |
| CU-13 | Instalar una app recomendada | ACT-02 | Media | Parcial | Código |
| CU-14 | Abrir la tienda, los ajustes de Android o cambiar el inicio | ACT-02 | Media | Implementado | Código |
| CU-15 | Configurar el asistente | ACT-02 | Media | Pendiente | Propuesta |
| CU-16 | Recuperar un PIN olvidado | ACT-02 | Media | Pendiente | README (límite conocido) |

CU-07 figura como parcial porque no pide el PIN anterior; CU-13, porque el botón Instalar se deja pulsar sin modo administrador.

#### Descripciones detalladas

Se detallan los cuatro casos que más condicionan el diseño. El resto se describirá con el mismo formato conforme se validen.

##### CU-02. Contactar con una persona de un toque

| Campo | Valor |
| --- | --- |
| Actor | ACT-01 Persona mayor |
| Descripción | La persona mayor pulsa el acceso directo de un contacto en el inicio y el móvil le llama o abre su chat de WhatsApp, según lo configurado. |
| Precondición | El familiar ha añadido el contacto y lo ha marcado para el inicio (CU-09). Caben 4 en el inicio. |
| Postcondición | Se inicia la llamada o se abre el chat de WhatsApp de ese contacto. |
| Importancia | Alta |

**Secuencia normal**

1. El usuario pulsa el acceso directo del contacto.
2. Si la acción es llamar y DOADO tiene permiso de llamada, el sistema llama directamente.
3. Si la acción es WhatsApp, el sistema convierte el número a formato internacional y abre el chat.

**Excepciones**

- 2: Sin permiso de llamada, el sistema abre el marcador con el número escrito y el usuario pulsa llamar.
- 3: Si WhatsApp no está instalado, el acceso directo aparece apagado y no se puede pulsar.

Comentario: el número se copia al añadir el contacto; si cambia en la agenda, DOADO conserva el antiguo (límite asumido).

##### CU-06. Acceder a los ajustes con PIN

| Campo | Valor |
| --- | --- |
| Actor | ACT-02 Familiar o cuidador |
| Descripción | El familiar entra en los ajustes introduciendo un PIN de 4 cifras. La primera vez, el sistema le pide crearlo. |
| Precondición | Estar en la pantalla de inicio. |
| Postcondición | Se muestra el índice de ajustes. |
| Importancia | Alta |

**Secuencia normal**

1. El usuario pulsa el botón de Ajustes del inicio.
2. Si no existe PIN, el sistema pide crear uno de 4 cifras, lo pide repetido para comprobarlo y lo guarda.
3. Si existe, el sistema pide el PIN y lo comprueba.
4. El sistema muestra los ajustes.

**Excepciones**

- 3: PIN incorrecto: el sistema lo indica y vuelve a pedirlo. Falta definir si hay límite de intentos.
- 1..3: El usuario puede volver al inicio sin entrar.

Comentario: el PIN se guarda hoy en texto plano y entra en la copia de seguridad en la nube (ver RNF-SEG-01). Volver desde otra app reabre los ajustes sin pedir el PIN de nuevo.

##### CU-12. Activar y desactivar el modo administrador

| Campo | Valor |
| --- | --- |
| Actor | ACT-02 Familiar o cuidador |
| Descripción | El familiar abre durante 15 minutos las salidas que DOADO mantiene cerradas: tienda, ajustes de Android y cambio de pantalla de inicio. |
| Precondición | Haber entrado en los ajustes (CU-06). |
| Postcondición | Las salidas quedan habilitadas hasta que se cierre el modo o pase el tiempo. |
| Importancia | Media |

**Secuencia normal**

1. El usuario entra en Modo administrador y lo activa.
2. El sistema guarda la hora de cierre (ahora + 15 minutos) y muestra la cuenta atrás.
3. El inicio muestra un aviso con la hora a la que se cerrará.
4. Al acabar el tiempo, el sistema cierra el modo solo.

**Excepciones**

- 4: El usuario puede cerrarlo antes manualmente.

Comentario: el modo no impide salir a Android de verdad; solo cierra las salidas propias de DOADO. La cuenta atrás depende de la hora del sistema.

##### CU-05. Realizar una acción por voz

| Campo | Valor |
| --- | --- |
| Actor | ACT-01 Persona mayor, ACT-03 Asistente de IA |
| Descripción | La persona mayor pide algo hablando (por ejemplo, «llama a mi hija») y el asistente lo ejecuta o le guía para hacerlo. |
| Precondición | Pendiente: cómo se activa el asistente (botón, palabra clave, siempre escuchando). |
| Postcondición | Pendiente: la acción queda hecha o el usuario sabe cómo hacerla. |
| Importancia | Alta |

**Secuencia normal (propuesta a validar)**

1. El usuario activa el asistente.
2. El usuario dice lo que quiere.
3. El asistente interpreta la orden y la repite en voz alta para confirmar.
4. El usuario confirma.
5. El asistente ejecuta la acción.

**Excepciones (propuesta a validar)**

- 3: Orden no entendida: el asistente pide que la repita o ofrece opciones.
- 3: Orden fuera de lo permitido: el asistente explica que no puede hacerlo.
- 4: El usuario dice que no: el asistente cancela sin hacer nada.

Comentario: qué acciones admite y cuáles exigen confirmación se decide en la sección 7.

### 4.2 Requisitos no funcionales

En DOADO los no funcionales son el núcleo del producto, no un añadido: la accesibilidad es lo que lo diferencia de un launcher normal. Cada requisito lleva un criterio que se puede comprobar.

| ID | Categoría | Requisito | Criterio de aceptación | Fuente |
| --- | --- | --- | --- | --- |
| RNF-ACC-01 | Accesibilidad | Contraste suficiente entre texto y fondo | Mínimo 4,5:1; 7:1 para texto y bordes. El cian de marca no se usa para texto ni iconos sobre fondo claro | CLAUDE.md |
| RNF-ACC-02 | Accesibilidad | Objetivos táctiles grandes | Ningún elemento pulsable por debajo de 48 dp; casillas del inicio de más de 168 dp | CLAUDE.md |
| RNF-ACC-03 | Accesibilidad | El texto respeta el tamaño de letra del sistema | Todo texto en sp desde la escala del tema (≈1,5× Material), con 3 niveles extra configurables | CLAUDE.md |
| RNF-ACC-04 | Accesibilidad | Separación visual por borde, no por sombra | Ningún componente se distingue solo por sombra | CLAUDE.md |
| RNF-ACC-05 | Accesibilidad | Colores fijos, sin color dinámico | Material You desactivado en el tema | CLAUDE.md |
| RNF-ACC-06 | Accesibilidad | Posiciones estables | Una app no disponible se muestra apagada en su sitio, nunca se quita | CLAUDE.md |
| RNF-ACC-07 | Accesibilidad | Compatible con lector de pantalla | Todo lo pulsable tiene etiqueta de TalkBack; los iconos con texto al lado son decorativos | CLAUDE.md |
| RNF-USA-01 | Usabilidad | Tareas básicas sin aprendizaje | Cualquier tarea básica (abrir una app, llamar a un contacto) se hace con un solo toque desde el inicio | Código |
| RNF-USA-02 | Usabilidad | La persona mayor no puede desconfigurar el móvil sin querer | Los ajustes y las salidas a Android solo son accesibles tras el PIN o el modo administrador | README |
| RNF-SEG-01 | Seguridad | El PIN se guarda protegido | PIN cifrado o con hash y excluido de la copia de seguridad en la nube. Hoy no se cumple | README (límite conocido) |
| RNF-SEG-02 | Seguridad | Los ajustes no quedan abiertos | Volver desde otra app exige de nuevo el PIN. Hoy no se cumple | README (límite conocido) |
| RNF-PRI-01 | Privacidad | Acceso mínimo a la agenda | Solo se lee el contacto elegido en el selector, sin permiso sobre toda la agenda | Código |
| RNF-PRI-02 | Privacidad | Tratamiento de datos del asistente conforme al RGPD | Pendiente: definir qué audio y datos salen del móvil, con qué base legal y durante cuánto tiempo | Propuesta |
| RNF-DIS-01 | Disponibilidad | Las funciones básicas funcionan sin conexión | Abrir apps y llamar no dependen de internet. Pendiente: qué hace el asistente sin conexión | Propuesta |
| RNF-COM-01 | Compatibilidad | Funciona en móviles de cualquier marca | minSdk 24; favoritos definidos por acción de Android, no por paquete | CLAUDE.md |
| RNF-IDI-01 | Idioma | Interfaz en español | Pendiente: decidir si se añade gallego | README |

## 5. Matriz de rastreabilidad requisitos/fuentes

Cada caso de uso se enlaza con el origen que lo justifica. Los marcados como «Propuesta» son nuevos y todavía no tienen respaldo en el código ni en el README.

| Caso de uso | Código | README / CLAUDE.md | Propuesta |
| --- | --- | --- | --- |
| CU-01. Abrir una app desde el inicio | X | X |  |
| CU-02. Contactar con una persona de un toque | X | X |  |
| CU-03. Volver atrás desde cualquier app |  | X |  |
| CU-04. Pedir ayuda al asistente |  |  | X |
| CU-05. Realizar una acción por voz |  | X |  |
| CU-06. Acceder a los ajustes con PIN | X | X |  |
| CU-07. Cambiar el PIN | X | X |  |
| CU-08. Configurar la interfaz | X | X |  |
| CU-09. Gestionar los contactos de un toque | X | X |  |
| CU-10. Gestionar las apps del inicio | X | X |  |
| CU-11. Abrir los ajustes de sonido y pantalla | X | X |  |
| CU-12. Activar y desactivar el modo administrador | X | X |  |
| CU-13. Instalar una app recomendada | X | X |  |
| CU-14. Abrir la tienda, los ajustes de Android o cambiar el inicio | X | X |  |
| CU-15. Configurar el asistente |  |  | X |
| CU-16. Recuperar un PIN olvidado |  | X |  |

## 6. Glosario de términos

| Término | Definición |
| --- | --- |
| Launcher | Pantalla de inicio de Android: la app que se abre al pulsar Inicio y desde la que se lanzan las demás. |
| Casilla | Botón grande del inicio que abre una app. |
| Acceso directo de persona | Botón del inicio con el nombre de un contacto que le llama o abre su WhatsApp. |
| Modo administrador | Periodo de 15 minutos en el que DOADO habilita la tienda, los ajustes de Android y el cambio de pantalla de inicio. |
| PIN de ajustes | Código de 4 cifras que protege los ajustes del familiar. |
| Asistente | Componente de IA que recibe órdenes habladas y actúa o guía dentro del móvil. |
| Botón flotante de «atrás» | Botón dibujado por un servicio de accesibilidad sobre cualquier app para volver atrás. |
| Servicio de accesibilidad | Componente de Android con permisos para dibujar sobre otras apps y actuar en ellas. |
| dp / sp | Unidades de Android independientes de la densidad de pantalla; sp además escala con el tamaño de letra del sistema. |
| TalkBack | Lector de pantalla de Android. |
| RGPD | Reglamento General de Protección de Datos de la UE. |

## 7. Conflictos pendientes de resolución

Seis decisiones abiertas; las cuatro primeras bloquean el diseño del asistente.

| ID | Conflicto | Opciones | Afecta a |
| --- | --- | --- | --- |
| CON-01 | Dónde se procesa la voz y la IA | En el dispositivo (privado, sin conexión, menos capaz) · En la nube (más capaz, coste y RGPD) · Mixto | CU-04, CU-05, RNF-PRI-02, RNF-DIS-01 |
| CON-02 | Qué acciones puede ejecutar el asistente sin confirmación | Ninguna · Solo las reversibles (abrir una app) · Todas las configuradas por el familiar | CU-05, CU-15 |
| CON-03 | Cómo se activa el asistente | Botón fijo en el inicio · Palabra clave · Ambos | CU-04, CU-05 |
| CON-04 | Alcance del asistente: actúa o solo guía | Ejecuta acciones · Explica paso a paso cómo hacerlas · Ambas según el caso | CU-04, CU-05 |
| CON-05 | Cómo recuperar un PIN olvidado sin abrir un agujero de seguridad | Borrar datos de la app (actual) · Pregunta de recuperación · Código enviado al familiar | CU-06, CU-16 |
| CON-06 | Botón «atrás» con servicio de accesibilidad | Implica un permiso muy amplio y revisión estricta en Google Play; valorar alternativas | CU-03 |

## Apéndices

### Anexo A. Actas de reunión

Cada reunión entre los creadores se registra con el formato de acta (código ACTA-0X, fecha, participantes, información recopilada y acuerdos). De momento no hay ninguna.

### Anexo B. Prototipos de pantallas

Las pantallas del launcher ya están diseñadas en la carpeta [design](../design) del repositorio y capturadas en [docs/capturas](capturas). Pendiente: prototipos del asistente y del botón «atrás».
