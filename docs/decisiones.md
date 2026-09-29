# Decisiones

> **Fuente única de**: por qué Doado es como es y qué se descartó por el camino.
> Las entradas **no se reescriben**: si una decisión cambia, se añade otra nueva
> que diga a cuál sustituye. Así se ve la historia y no se repiten los debates.
>
> Las decisiones **todavía abiertas** no están aquí: viven en la ERS, sección 7
> (CON-01 a CON-06), y son sobre todo del asistente. No las cierres por tu
> cuenta.
>
> Última revisión: 29/09/2026.

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
