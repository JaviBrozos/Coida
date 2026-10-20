# DOADO — Especificación de Requisitos

Borrador inicial de la ERS de DOADO: recoge lo ya construido en el repositorio y deja marcado lo que falta por definir.

| Campo | Valor |
| --- | --- |
| Proyecto | DOADO — launcher de Android para personas mayores con asistente de IA |
| Versión | 0.9 (borrador) |
| Fecha | 20/10/2026 |
| Autores | Gonzalo Gómez Blanco y Javier Brozos Martínez |
| Estado | Borrador |

### Historia de revisiones

| Versión | Fecha | Descripción | Autor |
| --- | --- | --- | --- |
| 0.1 | 28/09/2026 | Estructura inicial; actores, casos de uso y requisitos no funcionales extraídos del código y del README | Gonzalo Gómez Blanco y Javier Brozos Martínez |
| 0.2 | 29/09/2026 | RNF-SEG-01 y RNF-SEG-02 implementados; CU-07 y CU-13 pasan de parciales a completos | Javier Brozos Martínez |
| 0.3 | 30/09/2026 | El producto pasa a dos piezas: se retiran del alcance un caso de uso, un conflicto y dos términos del glosario. Los identificadores no se renumeran | Javier Brozos Martínez |
| 0.4 | 30/09/2026 | CU-16 implementado y CON-05 cerrado con la pregunta de recuperación del PIN (implementada por Gonzalo el 29/09, decisión D-23) | Javier Brozos Martínez |
| 0.5 | 30/09/2026 | RNF-SEG-03 nuevo e implementado: límite de intentos con bloqueo progresivo en la puerta de ajustes (D-24). Se corrige la excepción de CU-06 | Javier Brozos Martínez |
| 0.6 | 13/10/2026 | Se cierran CON-01 a CON-04 (D-25 a D-27) y se abre CON-06. CU-04, CU-05 y CU-15 pasan a «En pruebas» y se detallan. RNF-PRI-02 y RNF-DIS-01 se concretan; RNF-USA-03 nuevo. CU-05 pasa a conversación, con «sí» dicho (D-31) | Javier Brozos Martínez |
| 0.7 | 18/10/2026 | Se retira CU-13 (instalar una app recomendada): Ajustes deja de proponer apps; se instalan desde la tienda del modo administrador (CU-14, D-33). Los identificadores no se renumeran | Javier Brozos Martínez |
| 0.8 | 19/10/2026 | CU-02: los accesos directos van en la rejilla del inicio, tras «Tus contactos», y sin límite (D-38). Una persona puede tener botón de llamada y de WhatsApp a la vez (D-40). En CU-05 el asistente los sitúa ahí | Javier Brozos Martínez |
| 0.9 | 20/10/2026 | RNF-ACC-02: con tres por fila las casillas son casi cuadradas, de 96 dp de lado como mínimo, y cuatro por fila nunca (D-45). CU-02: los accesos directos pueden ir en una segunda pantalla del inicio, con pestañas (D-46). CU-08 añade la foto de fondo (D-47). CU-14: el inicio y Ajustes avisan si Doado no es la pantalla de inicio y ofrecen ponerlo (D-48) | Javier Brozos Martínez |

## 1. Introducción

DOADO es un launcher (pantalla de inicio) de Android para personas mayores que tienen dificultades con el móvil. Reduce lo que hay en pantalla, lo agranda y sube el contraste, y separa dos caras: una pantalla de inicio fija para la persona mayor y unos ajustes protegidos por PIN para el familiar que la configura.

El producto completo tiene dos piezas: el launcher (implementado) y un asistente de voz con IA que guía al usuario y actúa en el móvil (pendiente, y la principal propuesta de valor).

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
| ACT-03 | Asistente de IA | Actor secundario (sistema). Interpreta órdenes habladas de ACT-01 y, según elija ACT-02, las ejecuta tras confirmar o guía paso a paso. Solo actúa sobre los contactos guardados y las casillas del inicio. | Asistente de voz | Se procesa en la nube (D-25). Nunca actúa sin un «Sí» pulsado (D-26). |

El modo administrador no es un actor aparte: es un estado temporal en el que el familiar tiene más permisos. Por eso se modela como caso de uso (CU-12 a CU-14).

### 4.1.2 Casos de uso del sistema

14 casos de uso: 11 ya funcionan en el launcher y los 3 del asistente están «En pruebas»: programados y compilados, pendientes de probar con voz real. La fuente «Código» indica que el caso se ha extraído del repositorio. Los identificadores no se renumeran: los huecos de CU-03 y CU-13 son casos retirados del alcance.

| ID | Caso de uso | Actor | Importancia | Estado | Fuente |
| --- | --- | --- | --- | --- | --- |
| CU-01 | Abrir una app desde el inicio | ACT-01 | Alta | Implementado | Código |
| CU-02 | Contactar con una persona de un toque | ACT-01 | Alta | Implementado | Código |
| CU-04 | Pedir ayuda al asistente | ACT-01, ACT-03 | Alta | En pruebas | Descripción del proyecto |
| CU-05 | Realizar una acción por voz | ACT-01, ACT-03 | Alta | En pruebas | README |
| CU-06 | Acceder a los ajustes con PIN | ACT-02 | Alta | Implementado | Código |
| CU-07 | Cambiar el PIN | ACT-02 | Media | Implementado | Código |
| CU-08 | Configurar la interfaz | ACT-02 | Alta | Implementado | Código |
| CU-09 | Gestionar los contactos de un toque | ACT-02 | Alta | Implementado | Código |
| CU-10 | Gestionar las apps del inicio | ACT-02 | Alta | Implementado | Código |
| CU-11 | Abrir los ajustes de sonido y pantalla | ACT-02 | Baja | Implementado | Código |
| CU-12 | Activar y desactivar el modo administrador | ACT-02 | Media | Implementado | Código |
| CU-14 | Abrir la tienda, los ajustes de Android o cambiar el inicio | ACT-02 | Media | Implementado | Código |
| CU-15 | Configurar el asistente | ACT-02 | Media | En pruebas | Propuesta |
| CU-16 | Recuperar un PIN olvidado | ACT-02 | Media | Implementado | Código |

CU-07 figuraba como parcial en la versión 0.1 y se completó el 29/09/2026: pide ahora el PIN anterior antes de cambiarlo.

#### Descripciones detalladas

Se detallan los casos que más condicionan el diseño. El resto se describirá con el mismo formato conforme se validen.

##### CU-02. Contactar con una persona de un toque

| Campo | Valor |
| --- | --- |
| Actor | ACT-01 Persona mayor |
| Descripción | La persona mayor pulsa el acceso directo de un contacto en el inicio y el móvil le llama o abre su chat de WhatsApp, según lo configurado. |
| Precondición | El familiar ha añadido el contacto y lo ha marcado para el inicio (CU-09). No hay límite: salen en la rejilla del inicio, tras el rótulo «Tus contactos» (D-38). Si el familiar lo elige, van en una segunda pantalla del inicio, a la que se llega deslizando o con la pestaña «Tus contactos» (D-46). Una persona puede tener dos accesos directos, uno para llamar y otro para WhatsApp (D-40). |
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

- 3: PIN incorrecto: el sistema lo indica y vuelve a pedirlo. Desde el cuarto fallo seguido en la misma visita muestra una nota grande con un botón para volver al inicio; desde el quinto, bloquea la puerta un tiempo creciente (RNF-SEG-03).
- 1..3: El usuario puede volver al inicio sin entrar.

Comentario: desde el 29/09/2026 el PIN se guarda derivado con PBKDF2 y sal, en un fichero propio que no entra en la copia de seguridad (RNF-SEG-01), y salir de Doado cierra los ajustes (RNF-SEG-02). Como el PIN ya no se puede leer de ningún sitio, un PIN olvidado se recupera con la pregunta que el familiar fija al crearlo o cambiarlo (CU-16); sin pregunta configurada, la única salida es borrar los datos de la app. Desde el 30/09/2026 el PIN y la respuesta comparten un límite de intentos con bloqueo progresivo (RNF-SEG-03) que solo afecta a esta puerta: el inicio sigue funcionando.

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
| Descripción | La persona mayor pide algo hablando (por ejemplo, «llama a María») y el asistente lo hace tras confirmarlo o le guía para hacerlo, según haya elegido el familiar (CU-15). |
| Precondición | El familiar ha activado el asistente (CU-15) y el móvil tiene conexión. En el inicio se ve el círculo del asistente, abajo en el centro. |
| Postcondición | Modo «Lo hace»: la llamada, el chat o la app quedan abiertos. Modo «Explica cómo»: el usuario ha visto los pasos y vuelve al inicio con el botón que debe pulsar marcado. |
| Importancia | Alta |

Acciones admitidas, y solo estas: llamar a un contacto guardado, abrir su WhatsApp y abrir una casilla del inicio. El número sale siempre de lo guardado, nunca de lo que diga el modelo.

**Secuencia normal**

1. El usuario pulsa el círculo del asistente.
2. El sistema escucha y muestra «Te escucho» con un botón «Ya he terminado». Da la frase por acabada tras 2,5 s de silencio o al pulsar el botón (RNF-USA-03).
3. El sistema transcribe la voz, muestra lo que ha entendido e interpreta la orden.
4. Modo «Lo hace»: el sistema pregunta en voz alta y por escrito («¿Llamo a María?»). Al acabar de hablar, da un pitido y vuelve a escuchar solo, con la pregunta en pantalla.
5. El usuario pulsa «Sí» o lo dice («sí», «vale», «llámala»).
6. El sistema vuelve al inicio y ejecuta la acción.

La conversación sigue así tras cada respuesta del sistema: el usuario puede contestar hablando («no, mejor a Luis», «siguiente», «repite», «gracias») sin pulsar nada. El sistema recuerda lo hablado mientras el asistente está abierto (D-31).

**Secuencia alternativa: modo «Explica cómo»**

4. El sistema enseña y lee los pasos de uno en uno («Mira en "Tus contactos", debajo de las apps: el botón de María está en la fila 1, a la izquierda»).
5. El usuario pasa de paso con «Siguiente» o lo vuelve a oír con «Repetir».
6. Al terminar, el sistema vuelve al inicio con la casilla o el contacto marcado (relleno de color y borde grueso, anunciado por TalkBack), sin mover nada. La marca se quita al pulsar cualquier cosa o el botón de inicio del móvil; no desaparece sola, ni al apagarse la pantalla.

**Excepciones**

- 2: No se oye voz en 10 s: el sistema dice «No te he oído» y ofrece probar otra vez.
- 3: Orden no entendida: el sistema pide que la repita. No ejecuta nada.
- 3: Hay dudas entre varios contactos o casillas: el sistema pregunta y muestra un botón grande por opción.
- 3: Orden fuera de lo permitido (comprar, marcar un número dictado…): el sistema explica que no puede hacerlo.
- 3: Sin conexión, o el servicio falla: mensaje en lenguaje llano que dice qué hacer; el inicio sigue funcionando (RNF-DIS-01).
- 5: El usuario pulsa o dice «No»: el sistema no hace nada y lo dice.
- 5: El usuario contesta con algo que no es un sí limpio («sí, pero a Luis»): el sistema no actúa; lo interpreta como una orden nueva y vuelve a preguntar.
- 5: El usuario no dice nada en 10 s: el sistema deja de escuchar sin dar error y se queda en la misma pantalla, con «Hablar otra vez».
- 1..5: El usuario pulsa «Cancelar» o sale de Doado: el sistema deja de escuchar y descarta la petición. Mientras el asistente está abierto, la pantalla no se apaga sola.
- 2: Con TalkBack, el sistema espera a que TalkBack anuncie «Te escucho» y da un pitido antes de grabar.

Comentario: la confirmación es obligatoria en todas las acciones (D-26), pulsada o dicha (D-31). Las órdenes se procesan en la nube (D-25).

##### CU-04. Pedir ayuda al asistente

| Campo | Valor |
| --- | --- |
| Actor | ACT-01 Persona mayor, ACT-03 Asistente de IA |
| Descripción | La persona mayor pregunta cómo hacer algo con el móvil («¿cómo hago una foto?») y el asistente se lo explica en pasos cortos. |
| Precondición | La misma que CU-05. |
| Postcondición | El usuario ha visto y oído la explicación. |
| Importancia | Alta |

**Secuencia normal**

1-3. Como en CU-05.
4. El sistema enseña y lee la explicación, en un máximo de cuatro pasos y uno a uno.
5. El usuario pulsa «Terminar» y vuelve al inicio.

**Excepciones**

- Las de CU-05.
- 3: Pregunta médica, legal o de dinero: el sistema no responde y sugiere hablarlo con la familia o el médico.
- 3: Emergencia: el sistema indica cómo llamar al 112, pero no marca él ningún número.

Comentario: si la duda es sobre algo de Doado («¿cómo llamo a María?»), se sigue CU-05 en modo «Explica cómo», con pasos fijos y la casilla marcada. Si es sobre el móvil en general, los pasos los redacta el modelo y pueden no cuadrar con cada marca.

##### CU-15. Configurar el asistente

| Campo | Valor |
| --- | --- |
| Actor | ACT-02 Familiar o cuidador |
| Descripción | El familiar enciende o apaga el asistente y elige si actúa o explica. |
| Precondición | Haber entrado en los ajustes (CU-06) y que la app tenga configurado el servicio de IA. |
| Postcondición | El círculo del asistente aparece o desaparece del inicio, con el modo elegido. |
| Importancia | Media |

**Secuencia normal**

1. El usuario entra en Ajustes → Asistente de voz.
2. El sistema explica que la voz se envía a un servicio externo para entenderla.
3. El usuario enciende «Botón del asistente en el inicio».
4. Si falta el permiso del micrófono, el sistema lo pide en ese momento, con el familiar delante.
5. El usuario elige «Lo hace» o «Explica cómo». Por defecto, «Explica cómo».
6. Opcionalmente, pulsa «Probar el asistente».

**Excepciones**

- 4: Se deniega el permiso: el interruptor se queda apagado y una nota explica cómo darlo.
- 1: Sin servicio configurado: el sistema lo indica y no deja encenderlo.

### 4.2 Requisitos no funcionales

En DOADO los no funcionales son el núcleo del producto, no un añadido: la accesibilidad es lo que lo diferencia de un launcher normal. Cada requisito lleva un criterio que se puede comprobar.

| ID | Categoría | Requisito | Criterio de aceptación | Fuente |
| --- | --- | --- | --- | --- |
| RNF-ACC-01 | Accesibilidad | Contraste suficiente entre texto y fondo | Mínimo 4,5:1; 7:1 para texto y bordes. El cian de marca no se usa para texto ni iconos sobre fondo claro | CLAUDE.md |
| RNF-ACC-02 | Accesibilidad | Objetivos táctiles grandes | Ningún elemento pulsable por debajo de 48 dp. Casillas del inicio de 168 dp de alto como mínimo con dos por fila y 112 dp con una. Con tres por fila, casi cuadradas: nunca menos de 96 dp de lado, y cuatro por fila nunca (D-45). El nombre se lee entero, sin límite de líneas. Excepción: si el ancho baja después de elegir tres (otro móvil, otro tamaño de pantalla), no se cambia solo de columnas; Interfaz lo avisa | CLAUDE.md |
| RNF-ACC-03 | Accesibilidad | El texto respeta el tamaño de letra del sistema | Todo texto en sp desde la escala del tema (≈1,5× Material), con 3 niveles extra configurables. Con techo en tres sitios, para que no se coman la pantalla: 2,0 en total, 1,5 en la cabecera y las pestañas del inicio, y 1,25 dentro de las casillas de tres por fila (D-22, D-38, D-45) | CLAUDE.md |
| RNF-ACC-04 | Accesibilidad | Separación visual por borde, no por sombra | Ningún componente se distingue solo por sombra | CLAUDE.md |
| RNF-ACC-05 | Accesibilidad | Colores fijos, sin color dinámico | Material You desactivado en el tema | CLAUDE.md |
| RNF-ACC-06 | Accesibilidad | Posiciones estables | Una app no disponible se muestra apagada en su sitio, nunca se quita | CLAUDE.md |
| RNF-ACC-07 | Accesibilidad | Compatible con lector de pantalla | Todo lo pulsable tiene etiqueta de TalkBack; los iconos con texto al lado son decorativos | CLAUDE.md |
| RNF-USA-01 | Usabilidad | Tareas básicas sin aprendizaje | Cualquier tarea básica (abrir una app, llamar a un contacto) se hace con un solo toque desde el inicio. Excepción elegida por el familiar: con los contactos en otra pantalla, llamar son dos toques (la pestaña y el contacto), D-46 | Código |
| RNF-USA-02 | Usabilidad | La persona mayor no puede desconfigurar el móvil sin querer | Los ajustes y las salidas a Android solo son accesibles tras el PIN o el modo administrador | README |
| RNF-USA-03 | Usabilidad | El asistente no corta a quien habla despacio | Espera hasta 10 s a que empiece a hablar, da la frase por acabada tras 2,5 s de silencio y corta a los 30 s. Siempre hay un botón «Ya he terminado». Todo lo que dice en voz alta se ve escrito, con «Repetir». Valores de partida en `VoiceTuning.kt`, a ajustar con personas reales | Propuesta |
| RNF-SEG-01 | Seguridad | El PIN se guarda protegido | PIN derivado con PBKDF2 y sal aleatoria, en un fichero de preferencias propio que queda fuera de las reglas de copia de seguridad. Cumplido desde el 29/09/2026 | README (límite conocido) |
| RNF-SEG-02 | Seguridad | Los ajustes no quedan abiertos | Salir de Doado cierra los ajustes: al volver hay que repetir el PIN. Se exceptúa la ida y vuelta que inicia el propio Doado (tienda, selector de contactos, ajustes de Android). Cumplido desde el 29/09/2026 | README (límite conocido) |
| RNF-SEG-03 | Seguridad | No se puede probar el PIN a ciegas | Un solo contador de fallos para el PIN y la respuesta de recuperación. Los fallos 1 a 4 no bloquean y, desde el cuarto de la misma visita, se ofrece volver al inicio con un botón grande; el 5.º bloquea 30 s, el 6.º 1 min, el 7.º 5 min, el 8.º 15 min y del 9.º en adelante 1 h. El bloqueo sobrevive a cerrar la app, a cambiar la hora y a reiniciar, y no afecta al inicio. Cumplido desde el 30/09/2026 | Propuesta (D-24) |
| RNF-PRI-01 | Privacidad | Acceso mínimo a la agenda | Solo se lee el contacto elegido en el selector, sin permiso sobre toda la agenda | Código |
| RNF-PRI-02 | Privacidad | Tratamiento de datos del asistente conforme al RGPD | Al servicio externo solo salen el audio de cada frase, su transcripción, las últimas frases de la conversación en curso y los nombres de contactos y casillas; nunca los números de teléfono. Ni el audio ni la conversación se guardan en el móvil: se olvidan al cerrar el asistente. El familiar lo acepta al activar el asistente, tras leer la nota. Pendiente antes de publicar: base legal formal y contrato con el proveedor | Propuesta |
| RNF-DIS-01 | Disponibilidad | Las funciones básicas funcionan sin conexión | Abrir apps y llamar no dependen de internet. Sin conexión, el asistente dice en lenguaje llano que la necesita y que los botones siguen funcionando | Propuesta |
| RNF-COM-01 | Compatibilidad | Funciona en móviles de cualquier marca | minSdk 24; favoritos definidos por acción de Android, no por paquete | CLAUDE.md |
| RNF-IDI-01 | Idioma | Interfaz en español | Pendiente: decidir si se añade gallego | README |

## 5. Matriz de rastreabilidad requisitos/fuentes

Cada caso de uso se enlaza con el origen que lo justifica. Los marcados como «Propuesta» son nuevos y todavía no tienen respaldo en el código ni en el README.

| Caso de uso | Código | README / CLAUDE.md | Propuesta |
| --- | --- | --- | --- |
| CU-01. Abrir una app desde el inicio | X | X |  |
| CU-02. Contactar con una persona de un toque | X | X |  |
| CU-04. Pedir ayuda al asistente |  |  | X |
| CU-05. Realizar una acción por voz |  | X |  |
| CU-06. Acceder a los ajustes con PIN | X | X |  |
| CU-07. Cambiar el PIN | X | X |  |
| CU-08. Configurar la interfaz | X | X |  |
| CU-09. Gestionar los contactos de un toque | X | X |  |
| CU-10. Gestionar las apps del inicio | X | X |  |
| CU-11. Abrir los ajustes de sonido y pantalla | X | X |  |
| CU-12. Activar y desactivar el modo administrador | X | X |  |
| CU-14. Abrir la tienda, los ajustes de Android o cambiar el inicio | X | X |  |
| CU-15. Configurar el asistente |  |  | X |
| CU-16. Recuperar un PIN olvidado | X | X |  |

## 6. Glosario de términos

| Término | Definición |
| --- | --- |
| Launcher | Pantalla de inicio de Android: la app que se abre al pulsar Inicio y desde la que se lanzan las demás. |
| Casilla | Botón grande del inicio que abre una app. |
| Acceso directo de persona | Botón del inicio con el nombre de un contacto que le llama o abre su WhatsApp. |
| Modo administrador | Periodo de 15 minutos en el que DOADO habilita la tienda, los ajustes de Android y el cambio de pantalla de inicio. |
| PIN de ajustes | Código de 4 cifras que protege los ajustes del familiar. |
| Asistente | Componente de IA que recibe órdenes habladas y actúa o guía dentro del móvil. |
| dp / sp | Unidades de Android independientes de la densidad de pantalla; sp además escala con el tamaño de letra del sistema. |
| TalkBack | Lector de pantalla de Android. |
| RGPD | Reglamento General de Protección de Datos de la UE. |

## 7. Conflictos pendientes de resolución

Una decisión abierta, del asistente. No bloquea las pruebas, pero sí publicar. Los conflictos cerrados no se listan y están razonados en `docs/decisiones.md`: CON-05 (recuperar un PIN olvidado, D-23, 29/09/2026), y el 13/10/2026 CON-01 (la voz se procesa en la nube, D-25), CON-02 (ninguna acción sin confirmación, D-26), CON-03 (botón fijo en el inicio, D-28) y CON-04 (actúa o guía, lo elige el familiar, D-27).

| ID | Conflicto | Opciones | Afecta a |
| --- | --- | --- | --- |
| CON-06 | Cómo se protege la clave del servicio de IA sin servidor propio | Clave dentro de la app (hoy; se puede extraer del APK) · Un intermediario mínimo (rompe «sin servidor propio») · Que cada familiar ponga su propia clave. Propuesta con costes en [`propuestas/con-06-clave-asistente.md`](propuestas/con-06-clave-asistente.md) | CU-04, CU-05, RNF-PRI-02 |

## Apéndices

### Anexo A. Actas de reunión

Cada reunión entre los creadores se registra con el formato de acta (código ACTA-0X, fecha, participantes, información recopilada y acuerdos). De momento no hay ninguna.

### Anexo B. Prototipos de pantallas

Las pantallas del launcher ya están diseñadas en la carpeta [design](../design) del repositorio y capturadas en [docs/capturas](capturas). El asistente se programó sin prototipo previo; sus capturas quedan pendientes, y las del inicio se han quedado viejas (Ajustes ya no es una barra, D-28).
