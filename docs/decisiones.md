# Decisiones

> **Fuente única de**: por qué Doado es como es y qué se descartó por el camino.
> Las entradas **no se reescriben**: si una decisión cambia, se añade otra nueva
> que diga a cuál sustituye. Así se ve la historia y no se repiten los debates.
>
> Las decisiones **todavía abiertas** no están aquí: viven en la ERS, sección 7,
> y son sobre todo del asistente. No las cierres por tu cuenta.
>
> Última revisión: 18/10/2026.

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
