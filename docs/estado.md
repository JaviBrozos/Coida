# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 09/11/2026 · Javier Brozos**

## Dónde estamos

- La app pasa a llamarse **Coida** (`com.coida.launcher`), con una C por icono (D-62).
- **1.0.0 lista para Google Play**: `bundleRelease` compila, firma desde
  `keystore.properties`, sin asistente, sin internet y solo con `CALL_PHONE` y
  `READ_CONTACTS` (D-63). Documentos para publicar en `publishing/`.
- Launcher completo: inicio y ocho pantallas de ajustes tras el PIN. Probado el 28/10
  con dos personas mayores (D-54 a D-59).
- Asistente de voz en pruebas, solo en depuración (CU-04, CU-05, CU-15; D-56, D-58).
- La web (`web/`, `doadoapp.com`), el vídeo, la presentación, el logotipo y la skill
  de marca siguen con el nombre y la D de Doado.

## Sin probar

- **El asistente con voz real y ruido de casa** (D-56, D-58).
- **Coida en un móvil real**: instalada desde Play, el botón «Abrir» y el aviso para
  ponerla de inicio. En el emulador (API 37), la release va bien: inicio, PIN,
  ajustes, giro, letra al 200 % en 360 dp, modo avión, matar el proceso, reiniciar
  y tableta simulada.
- Añadir un contacto con los permisos denegados: el emulador no tenía contactos.
- Las fotos de los contactos con contactos que tengan foto.
- «Colocar las casillas» con TalkBack y con letra grande girado (ver «Siguiente»).
- La web en un móvil real, en Safari y en Firefox, y con TalkBack y VoiceOver.

## Siguiente

1. **Contarle a Gonzalo D-62 y D-63** (nombre y versión sin asistente).
2. Generar el keystore, subir la 1.0.0 a prueba cerrada y seguir la lista de Play
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
| 09/11 | Javier | Coida 1.0.0 para Google Play. Renombrada (D-62): paquetes, identificadores, textos e icono nuevo, una C con visto revisada con el auditor de marca. Asistente apagado en release, sin clave ni permisos de micrófono e internet (D-63). `LAUNCHER` se queda (D-64). versionCode 1, firma por `keystore.properties`, atrás predictivo en el inicio, barras al cambiar de configuración. Repaso de accesibilidad: guion en Ajustes, cuenta atrás apilada, etiquetas de «Volver». `ConfigRepository` y UUID de instalación. Documentos en `publishing/`. Probado en el emulador (API 37) |
| 30/10 | Javier | Web otra vez continua (D-61): el menú salta a cada sección, «Inicio» también en escritorio, mockup con la app encajada, hero anclado con tres pantallas al deslizar, vuelven las animaciones del principio y textos en neutro. Publicada en `doadoapp.com` |
| 30/10 | Javier | Web rehecha (D-60): siete secciones, móvil en 3D en el hero, capturas nuevas de la 1.4 y textos al día (D-54 a D-59). Publicada en `doadoapp.com` |
