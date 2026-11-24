# CON-06: cómo proteger la clave del asistente

13/10/2026 · Javier Brozos · Propuesta para decidir con Gonzalo. Versión editable y
con comentarios: [documento en Claude](https://claude.ai/code/artifact/75639ac1-bd38-4791-8788-b9331c1246e4).

> Esto es una propuesta, no una decisión. El conflicto vive en la ERS (§7, CON-06);
> cuando se decida, va a `docs/decisiones.md` y este fichero se queda como contexto.

## En resumen

Antes de publicar en Google Play hay que sacar la clave de Groq de la app: hoy viaja dentro del APK y cualquiera puede extraerla. **Propongo un intermediario mínimo en la nube, pagado con una suscripción de 1,99 a 2,99 € al mes para el asistente**, con el launcher gratis.

Cuesta unos 0,30 € al mes por usuario intenso y deja entre 1,10 y 1,80 € de margen. A cambio, Coida deja de ser «sin servidor propio» y pasamos a ser responsables del tratamiento según el RGPD. Lo tenemos que decidir los dos: es CON-06 en la ERS.

## El problema

La clave vive en `local.properties` y se copia dentro del APK al compilar. Para probar entre nosotros vale; publicado, cualquiera puede bajarse el APK, sacar la clave en minutos y gastar nuestra cuenta de Groq.

Cifrarla no lo arregla: si la app puede descifrarla para usarla, quien tenga el APK también puede. La única forma segura es que la clave no esté en el móvil.

## Las tres opciones

Solo el intermediario es a la vez seguro y usable por una familia normal.

| Opción | Cómo funciona | Seguridad | Para la familia | Coste para nosotros | ¿Sirve para publicar? |
| --- | --- | --- | --- | --- | --- |
| A. Clave en el APK | Como ahora | Ninguna: la clave se extrae | Nada que hacer | Pagamos todo el uso, sin control | No |
| B. Intermediario | La app llama a una función nuestra en la nube, que es la única con la clave | Alta, con Play Integrity y comprobación de la suscripción | Nada que hacer; paga una suscripción | Unos 0,30 €/mes por usuario, cubierto por la suscripción | Sí |
| C. Clave del familiar | Cada familiar crea su clave en Groq y la pone en Ajustes, guardada en el Keystore | Alta | Darse de alta en Groq y copiar una clave: mucha barrera | Cero | Sí, pero casi nadie la usaría |

## Recomendación: intermediario y suscripción

La opción B resuelve la seguridad y el cobro con la misma pieza.

- **El launcher sigue gratis.** Lo que se paga es el asistente, con unos días de prueba.
- **Paga el familiar**, desde Ajustes y detrás del PIN. La persona mayor nunca ve una pantalla de pago.
- **Sin cuentas de usuario.** La suscripción de Google Play ya identifica a quien paga; no hay registro ni contraseñas.
- **La clave no sale nunca del intermediario**, y el intermediario solo responde a nuestra app sin modificar y con la suscripción activa.
- **Cambiar de proveedor de IA sigue siendo fácil**: se cambia en el intermediario y la app ni se entera.

El precio y el tiempo de prueba están por decidir (ver las preguntas al final).

## Los números

La IA cuesta entre 0,07 y 0,30 € al mes por usuario, y el intermediario es gratis hasta unos 3.000 usuarios intensos. Son estimaciones de uso, no medidas: hay que confirmarlas en las pruebas.

| Uso | Frases al mes | Whisper (mínimo de 10 s por frase) | Modelo de lenguaje | Total aprox. |
| --- | --- | --- | --- | --- |
| Normal: 5 frases al día | 150 | 0,05 $ | 0,02 $ | **0,07 €** |
| Intenso: 20 frases al día | 600 | 0,19 $ | 0,10 $ | **0,30 €** |

Supuestos: unos 1.200 tokens de entrada por consulta al modelo (prompt y conversación), 150 de salida, y que la mitad de las frases son respuestas cortas («sí», «siguiente») que no llegan al modelo. Precios de Groq: Whisper large-v3 a 0,111 $ por hora de audio, gpt-oss-120b a 0,15 $ por millón de tokens de entrada y 0,60 $ de salida ([eesel.ai](https://eesel.ai/blog/groq-pricing)). Con Whisper turbo (0,04 $/h) el audio costaría un tercio, si entiende igual de bien a las personas mayores.

**Intermediario** en Cloudflare Workers: gratis hasta 100.000 peticiones al día; después, 5 $ al mes con 10 millones de peticiones ([Cloudflare](https://developers.cloudflare.com/workers/platform/pricing/)).

**Lo que queda de cada suscripción**, con IVA del 21 % y la comisión de Google Play del 15 % en suscripciones en la UE ([PricePush](https://pricepush.app/blog/google-play-subscription-fees-2026-real-math)):

| Precio al mes | Sin IVA | Tras Google Play | Menos IA (uso intenso) | Margen por usuario |
| --- | --- | --- | --- | --- |
| 1,99 € | 1,64 € | 1,40 € | 0,30 € | **1,10 €** |
| 2,99 € | 2,47 € | 2,10 € | 0,30 € | **1,80 €** |

Faltan los impuestos sobre el beneficio, que dependen de cómo nos demos de alta.

## Cómo funcionaría

La app ya está preparada: todo lo que habla con el proveedor vive en `cloud/`, y cambiarlo no toca nada más.

![La clave solo vive en el intermediario; el móvil nunca la ve. 1: el móvil manda la voz al intermediario. 2: el intermediario comprueba con Google Play la suscripción y que la app no está modificada. 3: llama a Groq con la clave y recibe el texto. 4: devuelve la respuesta al móvil.](con-06-intermediario.png)

Si Google Play dice que no hay suscripción o que la app está modificada, el intermediario no llama a Groq y la app lo explica en lenguaje llano. El launcher sigue funcionando entero.

## Qué implica

El coste técnico es pequeño; lo que pesa son las obligaciones de vender y de tratar datos.

| Ámbito | Qué cambia | Qué hay que hacer |
| --- | --- | --- |
| Reglas de Coida | «Sin servidor propio» pasa a «un intermediario mínimo que no guarda datos». «Sin cuentas» se mantiene | Decisión nueva en `docs/decisiones.md` y corregir `CLAUDE.md`, ERS y normas |
| RGPD | Pasamos a ser responsables del tratamiento de la voz | Política de privacidad, acuerdo de tratamiento de datos con Groq, y que el intermediario no registre audio ni texto |
| Google Play | Cobro con Google Play Billing y formulario de «Seguridad de los datos» | Cuenta de comerciante verificada; declarar que el audio se comparte con un tercero |
| Fiscal | Vender es actividad económica | Darnos de alta (autónomos o sociedad) y facturar; consultarlo con una gestoría |
| Usuarios mayores | Un cobro que no se entiende acaba en reclamación | Darse de baja fácil desde Ajustes, avisos claros antes de cobrar y periodo de prueba |
| Sin conexión o sin pagar | El asistente no funciona | El launcher sigue entero; el asistente lo dice en lenguaje llano (ya existe RNF-DIS-01) |

## Lo que tenemos que decidir

- [ ] ¿Intermediario (B), clave del familiar (C) o las dos, con C como opción avanzada?
- [ ] ¿Aceptamos cambiar «sin servidor propio» por «un intermediario que no guarda datos»?
- [ ] Precio: ¿1,99 o 2,99 € al mes? ¿Hay pago anual?
- [ ] Prueba gratis: ¿7, 14 o 30 días?
- [ ] ¿Algún uso gratis sin suscripción (por ejemplo, 10 frases al mes) o el asistente es solo de pago?
- [ ] ¿Cloudflare Workers, Firebase Functions u otro sitio para el intermediario?
- [ ] ¿Cómo nos damos de alta para vender, y quién lleva la parte legal?
- [ ] ¿Seguimos con Groq al publicar o comparamos antes con otros proveedores (calidad con voces mayores, precio, servidores en la UE)?

## Próximos pasos

Si elegimos el intermediario, el orden sería este; nada de esto bloquea seguir probando con la clave en el APK.

1. Apuntar la decisión en `docs/decisiones.md` (la siguiente D libre), cerrar CON-06 en la ERS y corregir las normas.
2. Montar el intermediario con dos rutas (transcribir e interpretar), sin registros de contenido.
3. Cambiar en la app solo `cloud/`: apuntar al intermediario y mandar el comprobante de Play Integrity.
4. Añadir la suscripción con Google Play Billing en Ajustes → Asistente de voz, detrás del PIN.
5. Probar con familias reales unas semanas para medir el uso de verdad y ajustar el precio.
6. Antes de publicar: política de privacidad, acuerdo con el proveedor, formulario de Play y alta fiscal.
