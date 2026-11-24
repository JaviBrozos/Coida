# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 11/11/2026 · Javier Brozos**

## Dónde estamos

- La app pasa a llamarse **Coida** (`com.coida.launcher`), con una C por icono (D-62).
- **1.0.0 lista para Google Play**: `bundleRelease` compila, firma desde
  `keystore.properties`, sin asistente, sin internet y solo con `CALL_PHONE` y
  `READ_CONTACTS` (D-63). Documentos para publicar en `publishing/`.
- Launcher completo: inicio y ocho pantallas de ajustes tras el PIN. Probado el 28/10
  con dos personas mayores (D-54 a D-59).
- Asistente de voz en pruebas, solo en depuración (CU-04, CU-05, CU-15; D-56, D-58).
- **1.0.1**: «Llamadas» abre el historial (D-65); la bola de perdidas, solo en `familia` (D-66).
- La web (`web/`, `doadoapp.com`), el vídeo, la presentación, el logotipo y la skill
  de marca siguen con el nombre y la D de Doado.

## Sin probar

- **El asistente con voz real y ruido de casa** (D-56, D-58).
- **«Llamadas» en Samsung o Xiaomi** y la bola en tema claro (en el emulador, bien).
- **Coida en un móvil real**: instalada desde Play, el botón «Abrir» y el aviso para
  ponerla de inicio. En el emulador (API 37), la release va bien: inicio, PIN,
  ajustes, giro, letra al 200 % en 360 dp, modo avión, matar el proceso, reiniciar
  y tableta simulada.
- Añadir contactos con los permisos denegados, y fotos de contactos que la tengan.
- «Colocar las casillas» con TalkBack y con letra grande girado (ver «Siguiente»).
- La web en un móvil real, en Safari y en Firefox, y con TalkBack y VoiceOver.

## Siguiente

1. **Contarle a Gonzalo D-62 a D-66** (nombre, versión sin asistente y «Llamadas»).
2. Generar el keystore (ojo: la `familia` dejará de instalarse encima de las APK de depuración, D-66), subir la 1.0.1 a prueba cerrada y seguir la lista de Play
   Console (`publishing/`). Rellenar el contacto de la política de privacidad y
   publicarla en una URL pública.
3. Comprobar que nadie usa «Coida» en Play ni como marca, y rehacer con el nombre y
   la C la web, el vídeo, la presentación, el logotipo y la skill de marca.
4. Cambiar la clave de Groq, que quedó expuesta en una sesión de trabajo. CON-06.
5. Repaso de accesibilidad del 09/11, por hacer: barra de «Colocar» a 2,0 girado,
   botones apagados sin motivo para TalkBack, campo de texto sin etiqueta, aviso de
   Contactos sin región viva, inicial de Aplicaciones a 2,0 y «Volver» de Colocar.
6. En los móviles de prueba: quitar la Doado vieja (`com.doado.launcher`).

Deuda menor: sin pruebas automáticas, solo español, isotipo sin usar (D-28) y
capturas viejas.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 11/11 | Javier | Coida 1.0.1. La casilla del teléfono pasa a «Llamadas» y abre el historial, con el teclado de reserva (D-65). Bola roja con las perdidas sin ver solo en «Llamadas», con «2 perdidas» debajo; `READ_CALL_LOG` solo en debug y en la variante nueva `familia`, que se enciende en Ajustes → Aplicaciones (CU-18, D-66). Repasado con el subagente de accesibilidad. Probado en el emulador. APK en Descargas |
| 09/11 | Javier | Coida 1.0.0 para Google Play. Renombrada (D-62): paquetes, identificadores, textos e icono nuevo, una C con visto revisada con el auditor de marca. Asistente apagado en release, sin clave ni permisos de micrófono e internet (D-63). `LAUNCHER` se queda (D-64). versionCode 1, firma por `keystore.properties`, atrás predictivo en el inicio, barras al cambiar de configuración. Repaso de accesibilidad: guion en Ajustes, cuenta atrás apilada, etiquetas de «Volver». `ConfigRepository` y UUID de instalación. Documentos en `publishing/`. Probado en el emulador (API 37) |
