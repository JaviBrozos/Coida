# Decisiones

> **Fuente única de**: por qué Doado es como es y qué se descartó por el camino.
> Las entradas **no se reescriben**: si una decisión cambia, se añade otra nueva
> que diga a cuál sustituye. Así se ve la historia y no se repiten los debates.
>
> Las decisiones **todavía abiertas** no están aquí: viven en la ERS, sección 7,
> y son sobre todo del asistente. No las cierres por tu cuenta.
>
> Última revisión: 28/10/2026.

---

### D-01 · Los favoritos se definen por acción, no por paquete (sep. 2026)

**Contexto**: cada marca trae su propia app de teléfono, cámara o mensajes.
**Decisión**: una casilla guarda una *acción* de Android (`ACTION_DIAL`,
`CATEGORY_APP_MESSAGING`…) y el sistema resuelve qué app la atiende.
**Descartado**: fijar paquetes conocidos, que rompería la casilla al cambiar de
móvil.
**Consecuencia**: cada acción nueva hay que declararla también en `<queries>`.

### D-02 · Nada de color dinámico (sep. 2026)

**Contexto**: Material You toma los colores del fondo de pantalla del usuario.
**Decisión**: paleta fija, decidida en `core/ui/Theme.kt`.
**Descartado**: el color dinámico, que es lo que Material recomienda por defecto.
**Consecuencia**: con colores variables no se puede garantizar el contraste, que
es justo lo que esta app no se puede permitir perder.

### D-03 · Escala tipográfica ~1,5× la de Material, y todo en sp (sep. 2026)

**Decisión**: el cuerpo arranca en 24 sp, con pesos Medium/SemiBold e
interletrado 0.
**Descartado**: la escala de Material tal cual, y usar dp para «controlar» el
tamaño.
**Consecuencia**: el texto crece con los ajustes de accesibilidad del sistema, y
encima el familiar puede subirlo otro 30 %.

### D-04 · Separar por borde, nunca por sombra (sep. 2026)

**Decisión**: contorno macizo y `surfaceTint` transparente.
**Descartado**: la elevación de Material.
**Consecuencia**: una sombra suave desaparece para quien tiene poca sensibilidad
al contraste; un borde, no.

### D-05 · Una app que no está se apaga, no se quita (sep. 2026)

**Decisión**: la casilla se queda en su sitio, en gris y con «No disponible».
**Descartado**: ocultarla, que es lo que hace un launcher normal.
**Consecuencia**: la posición de cada icono es memoria muscular; que algo baile
de sitio es peor que un icono apagado.

### D-06 · Un solo módulo, Compose sin XML de layouts (sep. 2026)

**Decisión**: `:app` y toda la UI en Compose; los XML de `res/` son tema de
arranque, iconos y textos.
**Descartado**: modularizar por capas antes de tener producto.
**Consecuencia**: build simple y rápido. Si algún día entra el asistente como
servicio, se revisará.

### D-07 · Preferencias en `SharedPreferences`, no en DataStore ni Room (sep. 2026)

**Contexto**: lo que se guarda son cuatro ajustes, una lista de ids y unos pocos
contactos.
**Decisión**: tres ficheros de preferencias, los contactos como JSON.
**Descartado**: DataStore (asíncrono, más dependencias) y Room (una base de datos
para esto es desproporcionado).
**Consecuencia**: lectura síncrona en el arranque, que aquí es lo que se quiere:
el inicio tiene que pintar ya.

### D-08 · Sin librería de navegación (28/09/2026)

**Decisión**: un `enum Screen` y un `when` dentro de `LauncherActivity`.
**Descartado**: Navigation Compose.
**Consecuencia**: ocho pantallas planas no justifican la dependencia ni el
back stack propio; el comportamiento de INICIO y ATRÁS de un launcher es más
fácil de controlar a mano.

### D-09 · El estado vive en la Activity, sin ViewModel (28/09/2026)

**Decisión**: `mutableStateOf` en la Activity, `reload()` en cada `onResume`.
**Descartado**: ViewModel + flujos.
**Consecuencia**: no hay nada asíncrono que deba sobrevivir, y `configChanges`
evita las recreaciones. Si entra el asistente, esto se queda corto.

### D-10 · Paleta oscura con el cian como acento (28/09/2026)

**Contexto**: la primera versión usaba bloques de cian macizo con iconos negros
encima; parecía de juguete.
**Decisión**: fondo tinta, casillas elevadas, contorno fino de 2 dp, y el cian
reservado para acentos.
**Descartado**: rellenar las casillas con el color de marca, y también usar los
dos colores del logo y nada más.
**Consecuencia**: hizo falta una escala de superficies y de texto (ver
`docs/diseno.md`), pero el resultado se lee mejor y no infantiliza.

### D-11 · Iconos de trazo dibujados a medida (28/09/2026)

**Decisión**: rejilla de 24, grosor 1,7, puntas redondas, teñidos por el tema.
**Descartado**: los iconos rellenos de Material escalados a 80-100 px.
**Consecuencia**: a tamaño grande el relleno se hincha; el trazo aguanta. Cuesta
dibujar cada icono nuevo a mano.

### D-12 · Los ajustes van detrás de un PIN de cuatro cifras (28/09/2026)

**Decisión**: la primera vez se crea repitiéndolo; a partir de ahí se pide.
**Descartado**: la huella o el bloqueo del sistema (que la persona mayor también
tiene), y no proteger nada.
**Consecuencia**: el familiar puede dejar el móvil sin miedo a que se
desconfigure. Hoy el PIN se guarda en texto plano: es deuda conocida
(RNF-SEG-01 en la ERS).

### D-13 · Los contactos se eligen con el selector de Android (28/09/2026)

**Decisión**: `ACTION_PICK`, que concede permiso solo sobre el contacto elegido.
**Descartado**: pedir `READ_CONTACTS` y pintar la agenda dentro de Doado.
**Consecuencia**: privacidad por diseño (RNF-PRI-01), a cambio de que el número
se copie y se congele: si cambia en la agenda, Doado sigue con el viejo.

### D-14 · Llamar directo si hay permiso; si no, al marcador (28/09/2026)

**Decisión**: `CALL_PHONE` se pide **al añadir un contacto**, con el familiar
delante. Si se deniega, la casilla abre el marcador con el número escrito.
**Descartado**: pedir el permiso en el arranque, o en el primer toque de la
persona mayor.
**Consecuencia**: la persona mayor nunca se encuentra un diálogo de permisos.

### D-15 · WhatsApp por `wa.me`; sin videollamada (28/09/2026)

**Decisión**: abrir el chat con `https://wa.me/<número en E.164>` y
`setPackage("com.whatsapp")`.
**Descartado**: la videollamada, porque Android no ofrece una forma estándar de
iniciarla en una app de terceros.
**Consecuencia**: hay que normalizar el número con el país del móvil. Si
WhatsApp no está instalado, el acceso directo sale apagado.

### D-16 · El modo administrador acota el tiempo, no el dispositivo (28/09/2026)

**Contexto**: impedir de verdad salir a Android exige ser *device owner*
(aprovisionamiento especial, el móvil casi en propiedad de la app).
**Decisión**: cerrar bajo llave **las salidas propias de Doado** (tienda, ajustes
de Android, cambio de launcher) y abrirlas 15 minutos, con cierre automático.
**Descartado**: device owner y *lock task mode*.
**Consecuencia**: no se promete lo que no se puede cumplir; se evita que el móvil
quede abierto porque alguien olvidó volver a bloquearlo. La cuenta atrás depende
de la hora del sistema.

### D-17 · La tipografía se queda en la del sistema (28/09/2026)

**Contexto**: el diseño proponía IBM Plex Sans.
**Decisión**: seguir con `FontFamily.Default`.
**Descartado**: empaquetar una fuente o usar Google Fonts descargables.
**Consecuencia**: menos peso y cero riesgo de que falle la descarga; a cambio, la
app se ve con la fuente de cada fabricante. Revisable.

### D-19 · El PIN se deriva y vive fuera de la copia de seguridad (29/09/2026)

**Contexto**: se guardaba en claro en `doado_settings`, y ese fichero sí se
respalda: el PIN acababa en la nube legible (RNF-SEG-01).
**Decisión**: fichero propio, `doado_security`, fuera de las reglas de copia, con
el PIN derivado con PBKDF2 (60 000 iteraciones) y sal aleatoria por móvil. Al
arrancar se migra el que hubiera en claro.
**Descartado**: `EncryptedSharedPreferences` de androidx.security-crypto, que
resolvía lo mismo pero añadía una dependencia a un proyecto que hasta ahora va
con Compose y poco más.
**Consecuencia**: el PIN ya no se puede leer del fichero ni viaja en una copia,
pero tampoco se puede recuperar si se olvida: hay que borrar los datos de la app.
Eso empuja a cerrar CON-05. Y que quede dicho: con cuatro cifras hay diez mil
combinaciones, así que esto no frena a quien pueda probarlas todas.

### D-20 · Salir de Doado cierra los ajustes, salvo si la salida la abre Doado (29/09/2026)

**Contexto**: volver desde otra app devolvía a la pantalla de ajustes donde se
estaba, sin volver a pedir el PIN (RNF-SEG-02).
**Decisión**: en `onStop()` se vuelve al inicio. Se exceptúa la ida y vuelta que
arranca el propio Doado —la tienda, el selector de contactos, los ajustes de
Android—, marcada con una bandera.
**Descartado**: cerrar siempre, que echaría al familiar en mitad de una
instalación; y un tiempo de gracia por reloj, que es más difícil de explicar y de
predecir.
**Consecuencia**: el hueco real (irse por recientes y volver) queda cerrado sin
romper los flujos normales.

### D-18 · La documentación va en tres capas, y la ERS manda en los requisitos (29/09/2026)

**Contexto**: cada sesión nueva de trabajo empezaba de cero repitiendo las mismas
explicaciones.
**Decisión**: `CLAUDE.md` como índice, `.claude/rules/` con las normas de
trabajo y `docs/` con la documentación larga. Los actores, casos de uso y
requisitos viven **solo** en `docs/ERS.md`.
**Descartado**: meterlo todo en `CLAUDE.md` (se paga en cada conversación) y
escribir un documento de producto aparte que duplicaría la ERS.
**Consecuencia**: cada dato tiene un único sitio; el resto enlaza. Si el código
contradice una línea de `docs/`, se corrige la línea en el mismo commit.

### D-21 · Las listas no rebotan al llegar al final (29/09/2026)

**Contexto**: Compose estira el contenido al llegar al final de una lista. En la
rejilla del inicio ese rebote saltaba **aunque las casillas cupieran enteras**,
porque `LazyVerticalGrid` se deja arrastrar siempre: la pantalla se meneaba sin
tener nada que desplazar.
**Decisión**: apagarlo para toda la app, en `DoadoTheme`, con
`LocalOverscrollFactory provides null`.
**Descartado**: dejarlo como está, que contradice de frente el invariante de que
las casillas no cambian de sitio; y apagarlo pantalla por pantalla, que se olvida
en la siguiente que se escriba.
**Consecuencia**: se quita movimiento que nadie ha pedido, lo que agradece quien
tiene mareo o sensibilidad al movimiento. A cambio se pierde la señal de «has
llegado al final», que en el inicio no hacía falta pero en la lista larga de
Aplicaciones sí decía algo. **Pendiente**: acotar el apagado al inicio y resolver
la causa con `userScrollEnabled = false` cuando la rejilla cabe entera, dejando
el estirado en las listas largas.

### D-22 · La letra tiene techo: 2,0 en total y 1,3 en el reloj (29/09/2026)

**Contexto**: el tamaño de letra de Doado se multiplica por el de Android. Con
Android al 200 % y Doado en «Muy grande» la escala llegaba a 2,6, y el inicio se
quedaba sin sitio para la rejilla (C-1 de la auditoría).
**Decisión**: la escala total no pasa de 2,0, el máximo que Android permite por sí
solo. El reloj y la fecha tienen un techo propio de 1,3.
**Descartado**: pasar a una columna cuando la letra es grande, que es un cálculo
automático de columnas y la norma lo prohíbe; y partir los nombres con guion, que
se lee peor que un nombre entero.
**Consecuencia**: con Android al máximo, el ajuste de Doado deja de agrandar más.
La hora se ve menor de lo que pediría la escala, a cambio de que las casillas
quepan.

### D-23 · CON-05 cerrado: pregunta de recuperación que cambia el PIN, no lo devuelve (29/09/2026)

**Contexto**: al guardar el PIN con PBKDF2 (D-19) ya no se puede recuperar si se
olvida; la única salida era borrar todos los datos de la app.
**Decisión**: durante la creación o el cambio de PIN se ofrece una pregunta libre
(texto, opcional). Al contestarla correctamente el familiar puede poner **un PIN
nuevo** sin perder contactos, casillas ni ajustes. La respuesta se normaliza
(minúsculas, sin espacios extremos) para que no falle por mayúsculas.
**Descartado**:
- *Respuesta que descifra el PIN antiguo*: para eso la respuesta tendría que
  guardarse de forma reversible, creando el mismo agujero que tenía antes la
  contraseña en claro. Con la opción elegida, la respuesta vive en `doado_security`
  —fuera de backup, igual que el PIN— pero en texto plano; quien tenga acceso ADB
  puede leerla. Eso ya era así con el PIN antiguo y no es peor.
- *Solo avisar al crear el PIN*: la parte barata, descartada porque no resuelve el
  problema y sí genera frustración.
**Consecuencia**: la pregunta de recuperación no exige la solución técnica más
robusta porque no protege datos sensibles: solo es una vía extra para que el
familiar recupere el control del móvil sin perder la configuración.

### D-24 · Límite de intentos: bloqueo progresivo solo en la puerta de ajustes (30/09/2026)

**Contexto**: ni el PIN ni la respuesta de recuperación tenían límite, así que se
podían probar las diez mil combinaciones a mano. Además, la persona mayor puede
llegar a la pantalla del PIN trasteando y equivocarse sin saber salir.
**Decisión**: escala parecida a la de un móvil: los fallos 1 a 4 no bloquean; el
5.º bloquea 30 s, el 6.º 1 min, el 7.º 5 min, el 8.º 15 min y del 9.º en adelante
1 h cada vez. Un solo contador para el PIN, el PIN actual al cambiarlo y la
respuesta, para que no se esquive alternando. Desde el 4.º fallo de la misma
visita sale arriba una nota de usted, dirigida a la persona mayor, con un botón
grande para volver al inicio; la nota cuenta solo esa visita para no aparecer de
primeras días después y empujar el teclado hacia abajo. El tiempo se mide con el reloj monotónico y el número de arranque, de
modo que ni cambiar la hora ni reiniciar lo acortan.
**Descartado**:
- *Bloquear el launcher entero*: la persona mayor se quedaría sin teléfono por
  culpa de unos toques al azar.
- *Borrar los datos tras N fallos*: el familiar perdería contactos y casillas por
  un nieto trasteando.
- *Medir con la hora del sistema*: se cambia desde los ajustes de Android.
- *Un contador por cada vía*: duplica los intentos disponibles.
**Consecuencia**: el familiar que se equivoca muchas veces espera hasta una hora
entre intentos. El contador no caduca con el tiempo: solo se pone a cero con un
acierto o al poner un PIN nuevo.

### D-25 · CON-01 cerrado: la voz se procesa en la nube, con Groq para las pruebas (13/10/2026)

**Contexto**: el asistente tiene que entender a una persona mayor que habla
despacio, duda y llama a la gente por apodos. Para probar hace falta un servicio
gratuito que se pueda cambiar después sin reescribir nada.
**Decisión**: grabamos el audio nosotros y lo mandamos a Whisper large-v3; la
transcripción la interpreta un modelo de lenguaje (gpt-oss), los dos en Groq. La
respuesta la lee el motor de voz del propio móvil. El cliente habla el formato de
OpenAI, que entienden casi todos los proveedores: cambiar de servicio es cambiar
la URL, los modelos y la clave en `local.properties`.
**Descartado**:
- *Gemini gratis*: su plan gratuito no se puede usar en apps con usuarios de la
  UE, y Google entrena con lo que se le envía.
- *El reconocedor de Android* (`SpeechRecognizer`): no sube audio, pero decide
  solo cuándo has terminado, y esos cortes afectan de 2 a 5 veces más a las
  personas mayores, que hacen pausas largas (benchmark «ASR Age Gap»).
- *Todo en el dispositivo*: hoy no entiende lenguaje libre con la calidad que hace
  falta en un móvil corriente.
**Consecuencia**: el asistente necesita internet y los permisos `INTERNET` y
`RECORD_AUDIO`. Groq no entrena con los datos ni los guarda por defecto. La clave
va dentro del APK, y eso queda abierto como CON-06.

### D-26 · CON-02 cerrado: ninguna acción sin confirmación (13/10/2026)

**Decisión**: antes de llamar, abrir un WhatsApp o abrir una app, el asistente
pregunta en voz alta y por escrito («¿Llamo a María?») y espera a que se pulse
«Sí». La pregunta la redacta el código, no el modelo.
**Descartado**: confirmar solo las llamadas y los mensajes, y dejarlo a elección
del familiar.
**Consecuencia**: un toque más en cada orden, a cambio de que una transcripción
mala nunca acabe en una llamada equivocada.

### D-27 · CON-04 cerrado: actúa o guía, y lo elige el familiar (13/10/2026)

**Decisión**: dos modos en Ajustes → Asistente de voz, «Lo hace» y «Explica
cómo». De fábrica, «Explica cómo». Al guiar hacia algo de Doado, los pasos son
fijos (no los inventa el modelo) y al terminar se marca la casilla o el contacto
que hay que pulsar, relleno de acento y con borde grueso, sin mover nada. La marca
se quita con un toque o con INICIO, nunca sola.
**Descartado**: que el asistente decida en cada caso si actúa o explica.
**Consecuencia**: quien quiere que su madre aprenda elige guiar; quien quiere que
se las arregle, elige actuar. Las dudas sobre el móvil en general siempre se
explican.

### D-28 · CON-03 cerrado: un círculo fijo en el inicio, y Ajustes a la esquina (13/10/2026)

**Decisión**: el asistente se abre con un círculo de 96 dp, abajo en el centro,
que no se desplaza con la rejilla. La rejilla le reserva su franja, así que nunca
tapa una casilla. Ajustes deja de ser una barra a lo ancho y pasa a un botón de
64 dp arriba a la derecha, donde estaba el isotipo, que sale del inicio.
**Descartado**: palabra clave o escucha permanente (micrófono siempre abierto,
batería y falsas activaciones), y el círculo en la esquina inferior.
**Consecuencia**: Ajustes queda lejos del pulgar de la persona mayor, que es quien
no tiene que entrar. Con el asistente apagado no hay círculo ni franja, y la
rejilla gana el alto que ocupaba la barra.

### D-29 · El asistente vive en un controlador, sin ViewModel (13/10/2026)

**Contexto**: D-09 avisaba de que el estado en la Activity se quedaría corto con
el asistente, que es asíncrono.
**Decisión**: `AssistantController`, una clase normal con el `lifecycleScope` de la
Activity y su estado en `mutableStateOf`.
**Descartado**: ViewModel, que añade dependencia y sirve para sobrevivir a
recreaciones que aquí no hay (`configChanges`).
**Consecuencia**: salir de Doado cancela la escucha y la petición en curso, que es
lo que se quiere: nadie vuelve a una pregunta a medias.

### D-30 · Sin dependencias nuevas para el asistente (13/10/2026)

**Decisión**: `HttpURLConnection` para la red, `org.json` para el JSON,
`AudioRecord` para grabar y `TextToSpeech` para hablar. Todo viene con Android.
**Descartado**: OkHttp y kotlinx.serialization.
**Consecuencia**: más líneas en el cliente HTTP (el envío del audio se monta a
mano), pero el proyecto sigue con Compose y poco más.

### D-31 · El asistente conversa, y un «sí» dicho también vale (13/10/2026)

**Contexto**: pulsar el círculo para cada respuesta obligaba a la persona mayor
a pasar de hablar a tocar y otra vez a hablar. Se pidió que el micrófono se
volviera a abrir solo, como en una conversación.
**Decisión**: cuando el móvil acaba de hablar, el micrófono se reabre solo, con
un pitido corto y la pregunta todavía en pantalla. **Cambia D-26**: para actuar
vale un «Sí» pulsado o dicho en voz alta. «Sí», «no», «siguiente», «repite» y
«gracias» se reconocen en el propio móvil con listas cortas de palabras, y una
respuesta solo cuenta si todas sus palabras son de esa respuesta o de relleno:
«sí, llama a Luis» no confirma la llamada a María, va al modelo y vuelve a
preguntar. El modelo recibe las últimas frases de la conversación (hasta seis
turnos), que viven en memoria y se olvidan al cerrar. Si no oye nada en 10 s,
apaga el micrófono y se queda en la misma pantalla con «Hablar otra vez». Tras
dos «no te he entendido» seguidos tampoco lo reabre: con la tele de fondo, el
ruido se transcribiría sin fin.
**Descartado**: dejar el «Sí» solo pulsado, que es más seguro pero rompe la
conversación; que la IA decida si algo es un sí, que es justo lo que no puede
equivocarse; y cerrar el asistente tras un silencio, que haría desaparecer la
pregunta sola.
**Consecuencia**: una transcripción mala de un «sí» claro podría llamar a quien
se preguntaba. La pregunta la redacta el código y nombra a la persona, y el sí
tiene que ser corto y limpio, así que el riesgo es el de pulsar «Sí» sin leer.
Con TalkBack, la espera antes de grabar crece con lo que acaba de leer.

### D-32 · Interfaz más sobria: grupos, platos y una respuesta al toque (18/10/2026)

**Contexto**: cada fila de ajustes iba en su caja y cada icono suelto sobre la
casilla; se veía como un prototipo. Se pidió un cambio de imagen hacia algo más
profesional, siguiendo los principios de diseño de Apple (skill `apple-design`
en `.claude/skills/`) sin tocar la accesibilidad.
**Decisión**: las filas de un mismo apartado van en un solo bloque con contorno y
una raya de 2 dp entre ellas; los iconos y las iniciales van sobre un plato
hundido del color del fondo; las opciones excluyentes son un selector
segmentado, en columna con tres opciones o con letra grande; «Volver» lleva la
palabra escrita; las pantallas cambian con un fundido de 180 ms; y un toque que
cuenta da una vibración corta de teclado. Lo auditó el agente de accesibilidad en
tres rondas antes de cerrarlo.
**Descartado**, de lo que propone la skill: superficies translúcidas y sombras
para dar profundidad (D-04), interletrado negativo en los títulos (D-03),
animaciones con muelle, gestos de arrastre o de lanzar, y confirmar solo lo
irreversible (D-26). También la nota rellena sin contorno: el relleno solo
separaba a 1,13:1 en claro, y eso es elevación.
**Consecuencia**: con letra grande el contacto del inicio pierde la inicial, para
que el nombre no se parta. Durante el fundido, la pantalla que sale no recibe
toques ni foco de TalkBack. Con «Quitar animaciones» de Android, el fundido es
un corte.

### D-33 · Doado no propone apps para instalar (18/10/2026)

**Contexto**: Ajustes → Aplicaciones tenía un apartado «Recomendadas»
(WhatsApp y Sofascore) con un botón para instalarlas, que llevaba a la tienda
con el modo administrador. Se pidió quitarlo.
**Decisión**: fuera el apartado y su botón; CU-13 se retira de la ERS sin
renumerar. Si una de esas apps está instalada, sale con las demás en «Otras apps
de este móvil» y se pone en el inicio igual. Para instalar, «Abrir la tienda de
apps» del modo administrador (CU-14). Del código solo queda el nombre de esas
apps, para pintar su casilla apagada si se desinstalan.
**Descartado**: mantener una lista curada de apps, que había que elegir y
revisar, y que nombraba a apps concretas dentro del producto.
**Consecuencia**: instalar una app pide un paso más, buscarla en la tienda.
Desaparece también la puesta automática en el inicio de lo que se instalaba
desde ahí.

### D-34 · Iconos redibujados con un mismo lenguaje (19/10/2026)

**Contexto**: los iconos cumplían D-11, pero cada uno tenía sus radios y
proporciones. Una auditoría de la identidad visual
(`docs/propuestas/marca.md`) pidió darles un acabado más profesional.
**Decisión**: se redibujan los 19 con las reglas de D-11 (rejilla de 24, trazo
1,7, puntas redondas, teñidos por el tema) y además: zona viva de 20, radio 3 en
los contenedores grandes y 2,5 en los pequeños, y el círculo como motivo
(objetivo, cabeza, tiradores, cerradura, centro del engranaje). «Volver» pasa a
flecha con asta y «Aplicaciones» lleva un círculo como cuarta casilla, en lugar
del «+».
**Descartado**: iconos rellenos o de dos tonos, que a 72 dp se ven pesados
(D-11), y librerías de terceros.
**Consecuencia**: mismos nombres de fichero, así que el código no cambia.

### D-35 · Los iconos van sin plato (19/10/2026)

**Contexto**: D-32 puso los iconos de casillas y filas sobre un plato hundido.
Se pidió quitar ese cuadrado y dejar solo la silueta dentro del contorno de la
casilla, también para los iconos de las demás apps.
**Decisión**: sustituye a esa parte de D-32. Iconos de trazo e iconos de apps van
directamente sobre la casilla o la fila. En una casilla señalada por el
asistente, el icono pasa a `onPrimary`. El plato se queda solo en las iniciales
de los contactos y en el círculo del micrófono de la pantalla del asistente.
**Descartado**: mantener el plato, que hacía de cuadrado dentro del cuadrado.
**Consecuencia**: el cian queda a 6,34:1 sobre la casilla y a 5,27:1 sobre el
fondo de pulsado, por encima del 3:1 que piden los gráficos (antes, 7,86:1 sobre
el plato). Desaparecen `TilePlateSize`, `TilePlateSizeCompact` y
`PlateCornerRadius`.

### D-36 · El logo sigue siendo la D con visto, redondeada (19/10/2026)

**Contexto**: la propuesta de marca (`docs/propuestas/marca.md`) ofrecía afinar
la D con visto (A), una D con hueco redondo (B) y un bocadillo (C). El icono
adaptativo llevaba además un cuadrado dentro y el visto se perdía en pequeño.
**Decisión**: se queda la D con visto, con las esquinas de la recta redondeadas
(radio 6 en rejilla de 64) y un visto nuevo: más grande, con el brazo largo más
alto, trazo de 5 (9,6 % del alto de la D) y puntas redondas, en hueco
(`evenOdd`). El icono adaptativo lleva delante solo la D, sin inset ni cuadrado;
la capa monocroma es la misma figura, visto incluido; hay PNG de mdpi a
xxxhdpi; y el logo toma los colores de `Color.kt`.
**Descartado**: B y C. También dejar el visto fino, que a 48 px medía 1,2 px.
**Consecuencia**: el texto del logotipo sigue en Baloo 2 y Quicksand hasta que
se decida la letra (pendiente en la propuesta). Las láminas de
`design/presentacion/` aún llevan el isotipo antiguo.

### D-37 · El micrófono graba sin supresor de ruido ni ganancia automática (19/10/2026)

**Contexto**: el asistente oía mal incluso voces jóvenes. El grabador encendía el
supresor de ruido y la ganancia automática del móvil, contaba voz solo si
triplicaba el ruido de fondo, tomaba el primer trozo grabado como medida del
ruido y enviaba todo, silencios incluidos.
**Decisión**: sin supresor ni ganancia, como trae la fuente
`VOICE_RECOGNITION`. Para empezar a contar voz basta con 2,5 veces el ruido, y
para seguir, con 1,6. La medida del ruido baja deprisa y sube despacio, así que
hablar nada más sonar el pitido ya no deja el umbral por encima de la voz. A
Whisper solo le llega el tramo con voz, con medio segundo antes y 0,8 s
después. La barra de volumen pasa a escala logarítmica. Los tiempos de
RNF-USA-03 no cambian.
**Descartado**: dejar los dos efectos. El supresor deja artefactos que confunden
a Whisper, que ya tolera el ruido. La ganancia sube también el silencio y
aplana la voz, y eso dejaba una voz floja por debajo del umbral, de modo que la
frase se cortaba a medias.
**Consecuencia**: en depuración, el grabador deja en el log (`DoadoVoz`) cifras
de cada grabación (duración, voz, ruido, pico y lo enviado), sin audio ni
texto, para ajustar `VoiceTuning` con voces reales.

### D-38 · Contactos en la rejilla y el asistente flotando encima (19/10/2026)

**Contexto**: probando el APK, el inicio tenía tres pisos: los contactos en un
bloque fijo arriba (como mucho cuatro), la rejilla de apps en medio, que era lo
único que se desplazaba y se quedaba en un cuadro pequeño, y el círculo del
asistente con su franja reservada abajo. El reloj se llevaba casi un cuarto de
la pantalla.
**Decisión**: una sola rejilla que se desplaza entera. Primero las apps y luego,
tras el rótulo «Tus contactos» (de fila entera), los accesos directos con la
misma forma y tamaño que una casilla: la inicial en su círculo ocupa el sitio
del icono. Sin límite de contactos. El rótulo no dice «Contactos» porque así se
llama la casilla de la agenda. El círculo del asistente flota encima, con la
rejilla pasando por sus lados y un aro de 8 dp del color del fondo que lo separa
de los contornos sin sombra (D-04). La rejilla acaba con un margen de su alto,
así que toda casilla puede quedar entera a la vista. Si no cabe todo, el alto
de casilla se ajusta para que al empezar quepan filas enteras (y el rótulo, si
cae ahí) por encima del círculo, y la siguiente solo asome por sus lados. Pulsar
Inicio devuelve la rejilla arriba. Al guiar, la rejilla solo se desplaza si la
casilla no se ve entera, y para un contacto se sube hasta su rótulo. El reloj
pasa a `headlineLarge` y la fecha a `bodySmall` en `onBackground` (7:1 también
en claro), con techo de escala 1,5 en vez de 1,3.
**Descartado**: el bloque fijo de contactos, que comía alto y limitaba a cuatro,
y la franja reservada al círculo de D-28, que dejaba la rejilla en un cuadro.
Esto sustituye a la norma «nada flotante puede tapar una casilla, ni al
desplazar». Ahora el círculo puede tapar parte de una casilla, también si el
desplazamiento se detiene ahí, pero nunca la deja sin poder verse entera. Lo
revisó el agente de accesibilidad; quedan abiertos el tamaño mínimo de casilla
en una columna (112 dp frente a los 168 de RNF-ACC-02) y el orden de lectura
de TalkBack, que deja el círculo para el final.
**Consecuencia**: con muchas casillas, los contactos quedan debajo y hay que
desplazar para llegar; el rótulo marca dónde empiezan. Las posiciones siguen
fijas: un contacto no cambia de sitio salvo que el familiar lo quite o añada
otro antes.

### D-39 · Ajustes se abre con un engranaje (19/10/2026)

**Contexto**: el botón de Ajustes de la esquina del inicio llevaba dos
deslizadores, y el engranaje quedaba para los ajustes de Android en el modo
administrador.
**Decisión**: el botón de la esquina lleva el engranaje, que es el dibujo de
«ajustes» que el familiar ya conoce de cualquier móvil (principio de
familiaridad de la skill `apple-design`). Los deslizadores se quedan en la fila
«Interfaz», y el fichero pasa a llamarse `ic_sliders`. El botón de los ajustes
de Android sigue con el engranaje: también son ajustes, y lleva su texto.
**Descartado**: mantener los deslizadores en la esquina, que había que aprender,
y dibujar un icono distinto para los ajustes de Android.

### D-40 · Una persona puede tener botón de llamada y de WhatsApp (19/10/2026)

**Contexto**: cada contacto tenía un único acceso directo y un selector
Llamada / WhatsApp. Para tener los dos había que añadir a la persona dos veces
desde la agenda, y el asistente veía a dos personas con el mismo nombre.
**Decisión**: en Ajustes → Contactos hay una tarjeta por persona con dos
interruptores, «Botón para llamar» y «Botón de WhatsApp». Se pueden encender
los dos. Por dentro cada casilla sigue siendo su propio elemento de
`doado_contacts`, y se agrupan por persona comparando las 9 últimas cifras del
número. Encender un botón que no existía lo añade al final, para no mover
casillas; apagarlo lo deja guardado y fuera del inicio. Volver a elegir en la
agenda a alguien que ya está no lo duplica: avisa. Al asistente se le cuenta
cada persona una vez, con sus botones. Un acceso directo de WhatsApp sin
WhatsApp dice «Falta WhatsApp» y no un «No disponible» a secas.
El aviso de «ya está en la lista» es una nota fija, no un mensaje que se va
solo, y en la tarjeta la fila de WhatsApp dice «Sale apagado: falta WhatsApp».
**Descartado**: un tercer valor «Las dos» en el selector, que obligaba a pintar
dos casillas desde un solo elemento y a cambiar el formato guardado, y un botón
de «Añadir también…».


### D-41 · La respuesta de recuperación se guarda derivada, como el PIN (05/10/2026)

Gonzalo la escribió como D-24 en su rama, a la vez que el límite de intentos
ocupaba ese número aquí; al juntar las dos en `main` pasa a D-41.
**Contexto**: D-23 dejó la respuesta en claro, razonando que no era peor que el
PIN antiguo. Pero el PIN antiguo en claro es justo lo que D-19 corrigió, y la
respuesta abre la misma puerta que el PIN.
**Decisión**: la respuesta se deriva con PBKDF2 y sal, con la misma función que
el PIN. Comprobarla solo exige derivar lo escrito y comparar, así que no hace
falta que sea legible. La que hubiera en claro se migra al arrancar.
**Descartado**: dejarla en claro por ser «solo conveniencia». Sustituye esa parte
de D-23; el resto de D-23 sigue en pie.
**Consecuencia**: nadie puede leer la respuesta del fichero. Cuenta en el mismo
límite de intentos que el PIN (D-24).

### D-42 · Al contestar a una pregunta, basta con menos voz (20/10/2026)

**Contexto**: en la prueba con voz real, un «no» seco tras «¿Llamo a …?» no hacía
nada ni decía nada; «no, no» y «no, déjalo» sí funcionaban. La grabadora solo
cuenta como dicho lo que pasa de 400 ms de voz (`MIN_SPEECH_MS`), y un «no» o un
«sí» secos se quedan por debajo: la grabación se tiraba y el asistente se quedaba
callado en la pregunta, que es justo cuando más importa que entienda un «no».
**Decisión**: al contestar a una pregunta del asistente (la escucha que se reabre
sola tras hablar), el mínimo baja a 200 ms (`MIN_REPLY_SPEECH_MS`). La primera
orden sigue con 400 ms.
**Descartado**: bajar el mínimo para todo, que dejaría pasar golpes y carraspeos
como primera orden. Esperar que la persona diga algo más largo, que es justo lo
que no se le puede pedir.
**Consecuencia**: un golpe corto puede llegar al servicio y transcribirse como
algo. No actúa por eso: la respuesta la decide `VoiceReplies` y solo confirma un
sí limpio (D-31). Queda por probar con una voz mayor y con ruido de fondo.

### D-43 · El PackageManager, fuera del hilo principal (20/10/2026)

**Contexto**: `reload()` preguntaba al `PackageManager` en el hilo principal al
arrancar y en cada `onResume`, cargando un icono por casilla. Es IPC con el
sistema: con muchas apps o un móvil lento arriesga un «la aplicación no
responde» (ANR), que Google Play cuenta en contra.
**Decisión**: esas consultas van en `loadApps()`, en `Dispatchers.IO`, y una
nueva cancela la anterior. Al volver al inicio se queda lo que ya se veía hasta
que llega lo nuevo. En el arranque, el inicio no se pinta hasta la primera carga:
se ve el fondo del tema unas décimas.
**Descartado**: pintar el inicio vacío y rellenarlo al llegar, porque las
casillas aparecerían de golpe y empujarían a los contactos (invariante 2);
y una pantalla de bienvenida, que pedía una dependencia nueva.
**Consecuencia**: abrir una casilla sigue resolviendo su intención en el hilo
principal, pero es una sola consulta y la dispara un toque.

### D-44 · La letra pasa a ser Atkinson Hyperlegible Next (20/10/2026)

Propuesta de Javier en `docs/propuestas/marca.md`, aceptada por Gonzalo el 20/10.
Sustituye a D-17.
**Contexto**: la app usaba la letra del sistema (D-17), así que se veía distinta
en cada marca, y la marca iba con cuatro letras sueltas (Baloo 2 y Quicksand en
el logo, IBM Plex Sans en `design/`, Segoe UI en la presentación).
**Decisión**: Atkinson Hyperlegible Next, del Braille Institute (licencia OFL,
que va dentro de cada fichero), para la app y la marca. Va en el APK, en
`res/font/`, con cuatro grosores recortados al alfabeto latino (unos 150 KB).
SemiBold en cuerpo y etiquetas (el cuerpo iba en Medium) y Bold en titulares.
Es más ancha que Roboto: al 200 % y dos columnas en un móvil de 360 dp,
«Contactos» pide tres líneas, así que las casillas de app admiten cuatro, como
las de contacto.
**Descartado**: seguir con la del sistema, y descargarla con Google Fonts
descargables, que puede fallar sin red justo al arrancar.
**Consecuencia**: la app se ve igual en todos los móviles y las letras que se
confunden (I, l, 1; 0 y O) dejan de confundirse. Falta pasar el texto del logo a
trazos con esta letra.

### D-45 · Hasta tres casillas por fila, casi cuadradas; cuatro, no (20/10/2026)

**Contexto**: Javier pidió elegir 2, 3 o 4 casillas por fila, según la vista y la
soltura de cada persona. Hasta ahora eran una o dos, y la norma prohibía un
tercer valor.
**Decisión**: 1, 2 o 3 por fila, de fábrica 2. Tres solo se ofrece si cada
casilla queda en 96 dp de ancho o más (un móvil de 360 dp justo llega); si ya
estaba puesto, se sigue enseñando. Con tres, por petición de Javier, las
casillas son **casi cuadradas** y no se estiran a lo alto: miden de alto lo que
de ancho, o lo que pida el nombre más largo si no cabe (se mide el texto de
verdad), y todas las de la rejilla igual. El icono baja a 48 dp, el nombre a
`labelMedium` (20 sp) sin límite de líneas, y la letra de las casillas crece con
la del sistema solo hasta 1,25. La línea de debajo del nombre («Llamada», «Falta
WhatsApp») se queda en 18 sp: «WhatsApp» es una marca, no se parte por sílabas, y
más grande salía «Wh / atsApp». Con una o dos por fila, los nombres tampoco
tienen ya tope de líneas: un nombre cortado con «…» no se reconoce. Si la letra pasa de ahí, Ajustes → Interfaz
avisa de que se lee mejor con dos por fila; el inicio no cambia de columnas
solo. Al guiar, el asistente dice izquierda, centro o derecha.
**Descartado**: cuatro por fila, que el agente de accesibilidad rechazó con
cifras: casillas de 68 a 81 dp de ancho y 44 a 57 dp para el nombre, donde no
cabe ni «Men-» a la letra normal. Mantener los 168 dp de alto con tres, que
daba casillas el doble de altas que anchas. Y pasar solo a menos columnas con la
letra grande, que mueve las casillas sin que nadie lo pida (invariante 2).
**Consecuencia**: con tres por fila, la casilla baja de los 168 dp de alto a unos
115-140 dp en un móvil de 411 dp; sigue siendo más del doble del mínimo de 48.
RNF-ACC-02 se reescribe (ERS 0.9). Con la letra al máximo, los nombres no crecen
tanto como el resto de la app; el aviso de Interfaz lo dice.

### D-46 · Los contactos, también en una segunda pantalla del inicio (20/10/2026)

**Contexto**: Javier quería, como opción, los contactos en otra pantalla del
inicio a la que se pasa deslizando, como en los móviles normales. El agente de
accesibilidad prefería dejarlos como estaban (D-38) y rechazaba cualquier cosa
a la que solo se llegue deslizando: es un gesto como única forma de hacer algo,
choca con el «Atrás» del borde y con el temblor de manos.
**Decisión**: de fábrica, como estaban. Con «En otra pantalla», el inicio tiene
dos: las apps y los contactos, con la misma rejilla y las mismas casillas. Se
pasa deslizando, pero arriba hay siempre dos pestañas grandes, «Aplicaciones» y
«Contactos», que dicen en cuál se está y llevan a la otra: deslizar es un atajo.
La pestaña dice «Contactos» y no «Tus contactos» (D-38) porque con la letra muy
grande no cabía; la casilla de la agenda está en la otra pantalla. Las pestañas
llevan el mismo techo de letra que la cabecera (1,5). Pulsar Inicio vuelve a las apps. Al guiar, el inicio pasa solo a la
pantalla de lo señalado, y el asistente dice cómo llegar la próxima vez.
**Descartado**: llegar solo deslizando, sin pestañas; una fila lateral de
contactos con flechas, que se probó y Javier descartó porque no era lo que
pedía; y una pantalla de contactos fuera del inicio, detrás de un botón, que
añade un paso a llamar (RNF-USA-01).
**Consecuencia**: con los contactos aparte, llamar a alguien cuesta un toque más
(la pestaña). Por eso no es lo de fábrica. Las pestañas se comen una franja de
la rejilla.

### D-47 · Foto de fondo en el inicio, con franjas lisas (20/10/2026)

**Contexto**: a muchas personas mayores les gusta ver una foto suya de fondo, y
la mayoría de móviles la tienen. Doado no la dejaba poner.
**Decisión**: el familiar elige una foto en Ajustes → Interfaz con el selector de
fotos de Android, que no pide permisos. Se guarda una copia reducida al tamaño
de la pantalla y girada como la hizo la cámara (`files/home_wallpaper.jpg`). Las
casillas y las pestañas ya son opacas y con contorno; el reloj y el rótulo «Tus
contactos» van sobre una franja lisa del color del fondo, con contorno, para que
el contraste se mida contra ella y no contra la foto. La foto no entra en la
barra de estado. Las franjas se pintan desde el primer fotograma, aunque la
foto llegue después, para que nada se mueva bajo el dedo. Si no se puede leer,
el inicio sale sin foto.
**Descartado**: sacar los colores de la foto, que es color dinámico (D-02), y
poner el texto encima con sombra o con un velo, que no garantiza el contraste.
También guardarla en la copia de seguridad: es privada y pesa.
**Consecuencia**: no es lo más accesible y por eso no viene puesta. En un móvil
nuevo se vuelve a elegir.

### D-48 · Avisar si Doado no es la pantalla de inicio (20/10/2026)

**Contexto**: si Doado no es la pantalla de inicio, al pulsar Inicio sale otra y
todo lo que se configura no sirve de nada. Para elegirla había que ir al modo
administrador, donde nadie lo busca al estrenar el móvil.
**Decisión**: si Doado no tiene el papel de pantalla de inicio, lo dicen arriba
el propio inicio y Ajustes, con un botón «Poner Doado como pantalla de inicio».
Los dos avisos llevan una equis para quien solo está probando Doado: al cerrarlo,
o al decir que no en el diálogo, el aviso explica cómo ponerlo más adelante
(Ajustes → Modo administrador → «Elegir pantalla de inicio»), con su propia equis.
En el inicio, cerrado se queda cerrado (`home_notice_dismissed`); en Ajustes
vuelve a salir entero cada vez que se entra.
Desde Android 10 se pregunta al sistema con `RoleManager` y el botón abre su
diálogo, que solo pregunta por Doado y no saca a otra parte del móvil; antes,
se resuelve la intención de inicio y el botón lleva a los ajustes de Android
donde se elige. Desde el inicio no: ahí el botón lleva a Ajustes de Doado, tras
el PIN, porque los ajustes de Android son una salida a Android (invariante 5). Se comprueba al volver a Doado, junto a las consultas de D-43.
El botón del modo administrador pasa a «Elegir pantalla de inicio».
**Descartado**: dejarlo solo en el modo administrador. Preguntar al
`PackageManager` desde Android 11, que sin declarar las intenciones de inicio en
`<queries>` no ve los otros launchers y contestaba que Doado ya lo era.
**Consecuencia**: el aviso del inicio lo ve casi siempre el familiar: mientras
Doado no es la pantalla de inicio, la persona mayor, al pulsar Inicio, ve la
otra. Sin permisos nuevos.

### D-49 · El PIN se deriva con 10 000 vueltas, no 60 000 (20/10/2026)

**Contexto**: tras la cuarta cifra, el PIN tardaba casi un segundo en abrir los
ajustes. Medido en el emulador: 924 ms solo en las 60 000 vueltas de PBKDF2 de
D-19; la pausa del cuarto punto y el fundido apenas contaban. Derivar a la vez
que la pausa no bastaba.
**Decisión**: el PIN se deriva con 10 000 vueltas (unos 120 ms en el emulador).
Las vueltas se guardan con él (`vueltas$sal$derivado`): un PIN de antes, guardado
como `sal$derivado` con 60 000, se sigue aceptando y se vuelve a guardar con las
nuevas la primera vez que se acierta. La respuesta de recuperación sigue con
60 000. La derivación arranca a la vez que la pausa del cuarto punto, que baja de
250 a 120 ms, y solo `check()` compara y cuenta el intento.
**Descartado**: dejar 60 000, que no protegía más de verdad: con cuatro cifras,
quien copie el fichero prueba las diez mil igual de rápido con unas vueltas que
con otras, y lo que frena es el límite de intentos (D-24), que no cambia.
**Consecuencia**: la respuesta de recuperación, que es texto libre y donde las
vueltas sí cuentan, conserva las suyas. Lo decidió Javier el 20/10.

### D-50 · La letra se queda en Inter (21/10/2026)

Sustituye a D-44. Javier puso Inter en prueba el 20/10, en lugar de Atkinson, y
Gonzalo eligió quedarse con ella el 21/10.
**Contexto**: D-44 metió Atkinson Hyperlegible Next en la app y en la marca. Al
verla en el emulador, Javier la cambió por Inter para probar, y quedó por decidir
entre los dos.
**Decisión**: Inter, solo en SemiBold y Bold, y siempre con el juego `ss04` para
que la I mayúscula y la l minúscula no se confundan («Isabel» y no «lsabel»).
Para la app y para la marca. Atkinson sale de `res/font/`; sus ficheros se quedan
en `design/propuestas-marca/fuente/` como parte de la propuesta.
**Descartado**: Atkinson Hyperlegible Next (D-44), hecha para quien ve poco y
más ligera (unos 150 KB frente a unos 840 KB), pero que no convenció al verla.
**Consecuencia**: sin `ss04`, Inter vuelve a confundir la I y la l: cualquier
pieza fuera de la app (presentación, web, Google Play) tiene que activarlo.
Falta pasar el texto del logo a Inter Bold en trazos.

### D-51 · Una casilla «Emergencias 112», que abre el teléfono con el número puesto (21/10/2026)

Acordado entre Gonzalo y Javier el 21/10. CU-17 en la ERS.
**Contexto**: el público de Doado es el que más puede necesitar llamar al 112
(una caída, un mareo, estar solo en casa), y otros launchers para mayores
tienen un botón de emergencia. Doado solo tenía al asistente explicando cómo
marcar el 112.
**Decisión**: una casilla básica más, «Emergencias 112», con una sirena en el
rojo de error del tema (`error`: 6,54:1 en claro y 8,67:1 en oscuro sobre la
casilla) y el nombre en el color de siempre. Abre el teléfono con
`ACTION_DIAL` y `tel:112`: el número queda puesto y la llamada la hace la
persona con el botón verde. Viene apagada de fábrica (`onHomeByDefault = false`)
y la enciende el familiar en Ajustes → Aplicaciones, donde una nota explica
qué hace. Como toda casilla, al encenderla va al final. El asistente la ve con
la marca «emergencias»: si alguien habla de una emergencia o pide el 112, la
abre tras un «Sí» o guía hasta ella; si no está, explica cómo marcar.
**Descartado**: llamar sin pasar por el teléfono, que Android no permite a una
app (`ACTION_CALL` no vale para números de emergencia) y que con una persona
mayor llevaría a llamadas por error. Un botón fijo fuera de la rejilla, que
quita sitio a todos aunque no lo usen. Encendida de fábrica, porque a quien
pulsa sin querer le haría más mal que bien. Mandar la ubicación por SMS, como
BIG Launcher y Doro, que pide permisos nuevos (ubicación y SMS) y falla sin cobertura o
sin GPS. Elegir otro número, como el de la teleasistencia, que queda como idea.
**Consecuencia**: en una emergencia es un toque más que una llamada directa.
Los móviles con Android 12 o más tienen además «Emergencia SOS» (cinco pulsaciones
del botón de encendido), que sí llama sola; se puede activar en los ajustes de
Android.

### D-52 · La web es una página estática con el móvil hecho en HTML (21/10/2026)

**Contexto**: hacía falta una web para presentar Doado a quien no abre el
repositorio, con la marca y sin prometer más de lo que hace.
**Decisión**: una sola página en `web/index.html`, sin framework ni dependencias,
que se abre sin servidor. Usa la marca de la skill: colores de `Color.kt`, la
Inter de la app con `ss04` (D-50), copiada en `web/fonts/` con su licencia, e
iconos copiados de `res/drawable`. Es oscura de fábrica, como la app, y en claro
manda el marino. El hero es un móvil de Doado hecho en HTML, con las medidas de
`DoadoDimens` a escala, que se puede tocar: casillas, contactos, el PIN y el
asistente en sus dos modos, más unos selectores de letra, casillas por fila y
tema. Las capturas reales van en la parte del familiar, copiadas en `web/img/`.
La llamada a la acción es instalarlo: cuatro pasos y el comando de `adb`. Encima va
una capa de movimiento aparte (`mv-*`) con un botón «Pausar movimiento», que
arranca quieta si el sistema pide reducir movimiento; en reposo no esconde nada.
**Descartado**: la Inter de Google Fonts, que pesa menos pero no trae `ss04`
(solo `calt`, `ccmp`, `frac`, `tnum` y poco más): la I y la l volvían a ser
iguales. Un hero con capturas o con vídeo, que no se pueden probar. Una
lista de espera con formulario queda fuera por ahora: sin servidor propio, los
correos los recogería un proveedor de terceros, y eso lo deciden los dos creadores.
**Consecuencia**: la web repite el estado del proyecto (asistente en pruebas, APK
de prueba, fuera de Google Play) y la letra de la marca: si cambian, se corrige
`web/` en el mismo commit. La copia publicada como artifact sale del mismo
fichero, entre las marcas `ARTIFACT:START` y `ARTIFACT:END`, sin el envoltorio
y con la letra incrustada, porque el visor solo deja cargar letras de Google Fonts.
La web pesa: unos 840 KB de letra, lo mismo que en la app. Para publicarla hay una copia en el repo privado `JaviBrozos/doado-web`, que no
se actualiza sola: el original es `web/`.

### D-53 · El vídeo de marca se pinta en Chrome sin interfaz, con la casilla de María de protagonista (22/10/2026)

**Contexto**: Javier pidió un vídeo de 18 s a partir de un encargo pensado para
un anuncio de música (guion segundo a segundo, una portada protagonista y una
barra de reproducción fija abajo), pero para Doado, con los colores de la marca y
su diseño gráfico. En la máquina no hay ffmpeg ni Node.
**Decisión**: se respeta el guion del encargo con el contenido de Doado: el inicio
con las casillas de fábrica, «Tus contactos», «A su medida», el asistente («Te
escucho», «¿Llamo a María?», «Sí, llama») y la firma con su visto. La portada la
hace la casilla de María; la barra fija, la del asistente; las fundas de discos,
seis apartados de ajustes. Todo es función del tiempo (`window.seek(t)`) y se pinta
en Chrome sin interfaz: canvas 2D, WebGL2 para la perspectiva, desenfoque de
movimiento con cinco subfotogramas, H.264 y AAC con WebCodecs, con el codificador
de la tarjeta, y un empaquetador MP4 propio. Python solo hace de servidor, con la
biblioteca estándar. La música es propia, sintetizada con Web Audio a 120 BPM. La
Inter lleva `ss04` horneado en el `cmap`, porque el canvas no deja activar rasgos
OpenType.
**Descartado**: instalar ffmpeg o Node, que habría metido programas nuevos en la
máquina. El codificador de vídeo por software de Chrome, que se quedaba en unos
4 Mb/s y dejaba bloques en las escenas oscuras. La portada abstracta que pedía el
encargo, porque Javier pidió usar el diseño que ya existe. Cualquier logo o
elemento de Spotify.
**Consecuencia**: el vídeo se vuelve a sacar con un comando
(`design/video/README.md`). Si cambian los iconos, la letra, los colores o los
textos de la app, hay que regenerar los ficheros derivados y volver a renderizar.
El MP4, el WAV y el cartel van en `design/video/out/`. La música se comprobó con
espectrograma y niveles, pero nadie la ha escuchado aún.
**Revisión del mismo día**: a 18 s había textos que no se podían leer. Javier pidió
media velocidad, y además se para en cada texto hasta cumplir 1 s más 1 s por cada
15 caracteres (la regla que propuso el auditor de marca): el vídeo dura 53,2 s. El
guion sigue escrito en 18 s de escena y un mapa de tiempo (`WARP` en `escenas.js`)
lo convierte; la música se rehízo para esa duración con el mismo mapa. Descartado:
solo ralentizar, porque varios textos seguían sin llegar ni a un segundo entero en
pantalla. A 10 Mb/s pesa unos 67 MB.

### D-54 · Casillas siempre cuadradas, de 1 a 4 por fila y con la letra que quepa (28/10/2026)

Pedido por Javier tras probar Doado con dos personas mayores: una con buena vista
y soltura con el móvil, y otra mayor y con menos manejo. Sustituye a D-45 en el
tope y en la forma.
**Contexto**: a las dos les sobraba tamaño. Las casillas de dos por fila se
estiraban hasta 300 dp de alto para llenar la pantalla y cambiaban de forma según
cuántas hubiera: en un móvil bajo solo se veía una fila. Pedían tres o cuatro por
fila. En el Samsung antiguo del abuelo, con menos de 360 dp de ancho, Interfaz ni
siquiera ofrecía la de tres, y solo se podían poner dos por fila. También
molestaban las palabras partidas («Mensaje/s»).
**Decisión**:
- Con dos o más por fila, la casilla es **cuadrada** (`aspectRatio(1f)`) y nunca
  se estira. Con una por fila, es una fila de alto fijo (112 dp).
- Se ofrecen siempre 1, 2, 3 y 4 por fila, mida lo que mida la pantalla. Con
  cuatro, el hueco entre casillas baja a 10 dp.
- El icono, los márgenes y el radio de las esquinas van en proporción al lado.
- El texto nunca parte una palabra: baja de tamaño hasta que cada palabra cabe en
  su línea y el texto entero en su hueco (`core/ui/FitText.kt`), hasta unos 8 dp en pantalla.
- Todas las casillas de un apartado llevan la misma letra, la del nombre que menos
  cabe, para que la rejilla se lea pareja. Solo si ni así cabe, sale «…».

**Descartado**:
- El `TextAutoSize` de Compose, que no detecta una palabra partida por letras.
- Partir por sílabas con guion, que dependía de que el móvil tuviera los patrones
  del español.
- Dejar el máximo en tres, cuando las dos personas pidieron cuatro.

**Consecuencia**: con cuatro por fila en un móvil de 360 dp, la casilla queda en
unos 70 dp y el nombre puede bajar de los 18 sp del suelo de la ERS. RNF-ACC-02 y
RNF-ACC-03 quedan con esta excepción. El área pulsable sigue por encima de 48 dp.

### D-55 · Un tamaño de letra por debajo del de siempre (28/10/2026)

Pedido por Javier en la misma prueba.
**Contexto**: a las dos personas el tamaño «Normal» les parecía grande, e incluso
pidieron uno menos.
**Decisión**:
- Cuatro tamaños sobre la escala de Android: «Normal» (0,85), «Grande» (1), «Muy
  grande» (1,15) y «Enorme» (1,3). El antiguo «Normal» pasa a llamarse «Grande».
- La letra se guarda en una clave nueva, `text_size_v2`. Si solo está la vieja
  (`text_size`), se traduce un escalón hacia arriba: quien ya usaba Doado conserva
  exactamente su tamaño.
- Una instalación nueva arranca en el «Normal» nuevo.
- El suelo de la escala total baja de 1 a 0,85.

**Descartado**: cambiar el valor de la clave vieja, que habría encogido la letra a
quien ya la tenía elegida. Quitar los tamaños grandes, que siguen ahí para quien
los necesite.
**Consecuencia**: con «Normal», la letra más pequeña baja de 18 a unos 15 sp. El
suelo de 18 sp deja de ser absoluto.

### D-56 · El asistente dice lo que va a hacer y lo hace, sin pedir un «Sí» (28/10/2026)

Pedido por Javier en la misma prueba. Sustituye a D-26, a D-31 y al modo de
fábrica de D-27.
**Contexto**: la pregunta de confirmación («¿Llamo a María?») liaba muchísimo a
las dos personas. Además, el micrófono se volvía a abrir solo tras cada frase y
recogía la tele y las voces de la casa.
**Decisión**:
- En «Lo hace», el asistente dice «Llamo a María» y llama al acabar la frase, sin
  preguntar. Mientras lo dice hay un botón grande «Cancelar».
- Solo vuelve a abrir el micrófono por su cuenta si no ha entendido u oído la
  orden, para pedir que se la repitan, y una sola vez. Después se queda con
  «Probar otra vez».
- En la guía, al aclarar entre dos personas y en las respuestas se avanza con
  botones. «Hablar otra vez» sirve para decirle otra cosa.
- El prompt pide al modelo que ante la duda aclare o diga que no ha entendido,
  nunca que adivine. La lista blanca, y que el número salga del almacén, no
  cambian.
- El modo de fábrica pasa a ser «Lo hace». Quien ya lo tenía elegido conserva lo
  suyo.
- Fuera `VoiceReplies`: ya no hay respuestas cortas que reconocer.

**Descartado**:
- Actuar al instante sin decir nada, que no deja ver ni oír a tiempo si ha
  entendido mal.
- Mantener la confirmación solo dicha o solo pulsada, que seguía siendo una
  segunda pregunta.

**Consecuencia**: si entiende mal, llama a quien no es, salvo que se pulse
«Cancelar» mientras lo dice: al menos 2,5 s, y con TalkBack lo que tarde en leerlo y 6 s más. Ante dos personas
posibles sigue preguntando, con un botón para cada una.

### D-57 · Permiso de contactos, solo para la foto de los guardados en Doado (28/10/2026)

Pedido por Javier en la misma prueba.
**Contexto**: las dos personas querían ver en Doado la foto que cada contacto
tiene en la agenda. El selector de contactos solo da acceso a la fila elegida, y
la foto está en otra: sin `READ_CONTACTS`, Android no deja abrirla.
**Decisión**:
- Se añade `READ_CONTACTS`. Se pide al añadir un contacto, con el familiar delante.
  Si se deniega, Ajustes → Contactos lo explica y ofrece pedirlo otra vez.
- Con el permiso, `ContactPhotos` busca **solo** los números guardados en Doado,
  uno a uno, y lee su foto.
- Las fotos viven en memoria: no se copian a disco, no salen del móvil y no van al
  asistente. Se vuelven a leer al volver al inicio, por si cambian.
- Sin foto, o sin permiso, sale la inicial de siempre.

**Descartado**: copiar la foto al elegir el contacto sin pedir permiso, que
Android no permite. Leer la agenda entera para ofrecer fotos, que es justo lo que
Doado no hace.
**Consecuencia**: Doado pasa a tener cuatro permisos. La regla de no leer toda la
agenda se matiza: el permiso lo permitiría, pero el código solo pregunta por los
números guardados. Para Google Play habrá que contarlo en la política de
privacidad.

### D-58 · El final de la frase se mide contra la propia voz (28/10/2026)

Revisa los tiempos de D-37.
**Contexto**: en la prueba con dos personas mayores, el micrófono seguía abierto
2,5 s después de la orden y alargaba la grabación con el ruido de fondo. Whisper
lo transcribía todo y el modelo se liaba.
**Decisión**:
- Para seguir contando voz, ahora hay que llegar a una cuarta parte del nivel medio
  de quien habla (`SPEECH_DROP_RATIO`, unos −12 dB). La tele o alguien al fondo
  suenan más flojos que la voz pegada al móvil, y cuentan como silencio.
- El silencio que cierra la frase baja de 2,5 s a 1,2 s.
- El tope de la grabación baja de 30 s a 10 s.
- Para empezar a contar voz hace falta más volumen: un suelo de 250 en vez de 150,
  y 3,2 veces el ruido en vez de 2,5.
- Los márgenes del audio que se envía bajan a 0,3 s.
- Sigue sin supresor de ruido ni ganancia automática.

**Descartado**: volver al supresor de ruido de Android, que D-37 ya quitó porque
estropeaba lo que oye Whisper.
**Consecuencia**: quien hace una pausa de más de 1,2 s a mitad de frase queda
cortado. Entonces el asistente no la entiende y pide que se la repitan. Son
valores de partida: se afinan con el registro `DoadoVoz` en el móvil real.

### D-59 · El familiar coloca las casillas arrastrándolas (28/10/2026)

Pedido por Javier.
**Contexto**: las casillas salían en el orden en que se iban poniendo, y no había
forma de decidir dónde va cada una.
**Decisión**:
- Una pantalla nueva, Ajustes → «Colocar las casillas», a la que también se llega
  desde Aplicaciones. Es una réplica del inicio, con las mismas columnas.
- Se mantiene pulsada una casilla, se levanta (rellena de acento) y se arrastra.
  Las demás le hacen hueco, y cerca de los bordes la rejilla se desplaza sola.
- Como alternativa, se toca una casilla y luego su sitio, o se usan «Mover antes» y
  «Mover después». TalkBack los ofrece también como acciones.
- Las apps y los contactos se colocan cada uno en su apartado.
- El orden se guarda en las mismas claves de siempre.

**Descartado**: solo botones, que con muchas casillas es lento. Una librería de
arrastre, que sería una dependencia nueva.
**Consecuencia**: arrastrar ya no está prohibido en toda la app. Se permite en las
pantallas del familiar, nunca en lo que usa la persona mayor, y siempre con otra
forma de hacerlo. Las casillas siguen sin moverse solas: solo las mueve el
familiar.
