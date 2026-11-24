# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 24/11/2026 · Javier Brozos**

## Dónde estamos

- **Coida 1.0.1**: launcher completo, inicio y ocho pantallas de ajustes tras el PIN.
  Probado el 28/10 con dos personas mayores (D-54 a D-59).
- **Repositorio público** (`JaviBrozos/Coida`) con la APK de release en una Release
  de GitHub, sin asistente, sin internet y solo con `CALL_PHONE` y `READ_CONTACTS`
  (D-63, D-68). Documentos para Google Play en `publishing/`.
- **Asistente de voz** fuera de lo público: a los mayores no les resultó útil
  (D-67). Sigue en depuración (CU-04, CU-05, CU-15), con la clave de cada uno.
- Marca completa con la C: skill `coida-brand`, logotipo, cabecera, gráfico de Play,
  imagen de LinkedIn y capturas nuevas. Web y presentación pasadas a Coida (D-68).
- La bola de llamadas perdidas, solo en la variante `familia` (D-66).
- El vídeo de `design/video/` sigue siendo de Doado, con la D y el asistente.

## Sin probar

- **Coida en un móvil real**: instalada desde la APK o desde Play, el botón «Abrir»
  y el aviso para ponerla de inicio. En el emulador (API 37), la release va bien.
- **«Llamadas» en Samsung o Xiaomi** y la bola en tema claro (en el emulador, bien).
- El selector de contactos de Android 17 deja marcar varios y Coida guarda uno.
- «Colocar las casillas» con TalkBack y con letra grande girado (ver «Siguiente»).
- La web en un móvil real, en Safari y en Firefox, y con TalkBack y VoiceOver.

## Siguiente

1. **Contarle a Gonzalo D-62 a D-68**: nombre, asistente fuera, repo público.
2. Subir `docs/img/cabecera.png` como *social preview* de GitHub y decidir licencia.
3. Generar el keystore, subir a prueba cerrada y seguir `publishing/README.md`.
   Rellenar el contacto de la política de privacidad y publicarla en una URL.
4. Cambiar la clave de Groq, que quedó expuesta en una sesión de trabajo (ya no
   está en `local.properties`). Desplegar la web nueva (repo `doado-web`).
5. Repaso de accesibilidad del 09/11, por hacer: barra de «Colocar» a 2,0 girado,
   botones apagados sin motivo para TalkBack, campo de texto sin etiqueta, aviso de
   Contactos sin región viva, inicial de Aplicaciones a 2,0 y «Volver» de Colocar.

Deuda menor: sin pruebas automáticas, solo español, vídeo de Doado y quitar la Doado
vieja (`com.doado.launcher`) de los móviles de prueba.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 24/11 | Javier | Repo listo para LinkedIn: público como `JaviBrozos/Coida`, Release v1.0.1 con la APK (D-68). Asistente fuera de lo público porque a los mayores no les resultó útil (D-67). Skill `coida-brand`, logotipo a trazos, piezas y capturas nuevas; web y presentación a Coida; README reescrito con el agente `presentador-repo`. Fuera la lectura de `doado.ai.*` y la clave de `local.properties` |
| 11/11 | Javier | Coida 1.0.1. «Llamadas» abre el historial (D-65). Bola de perdidas solo en la variante `familia` (CU-18, D-66). Probado en el emulador |
