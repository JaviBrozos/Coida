# Estado del trabajo

> **Este es el único documento que caduca.** Mira la fecha antes de fiarte: si
> lleva semanas sin tocarse y el código sí se ha movido, no te lo creas y mira el
> repositorio.
>
> Sirve de traspaso entre sesiones de trabajo: qué hay hecho, qué está a medias y
> qué toca después. El estado por caso de uso (CU-01…CU-16) está en la ERS; aquí
> va lo que no cabe en una tabla de requisitos.

**Última actualización: 29/09/2026.**

## Hecho

- **Launcher completo**: pantalla de inicio (reloj, accesos directos de
  contactos, rejilla de casillas de una o dos columnas, entrada a Ajustes) y las
  seis pantallas de ajustes tras el PIN: Interfaz, Contactos, Aplicaciones,
  Sonido y pantalla, Modo administrador y cambio de PIN.
- Probado a mano en emulador (Pixel, Android 16): alta de PIN, cambio de tema,
  instalación desde la tienda y desbloqueo del modo administrador con su cuenta
  atrás. `./gradlew lint` sin errores.
- **Documentación**: ERS (Gonzalo), README con capturas, presentación en diez
  láminas con su PDF, y esta capa de `CLAUDE.md` + `docs/` + `.claude/rules/`.

## A medias

- **CU-07 Cambiar el PIN**: no pide el PIN anterior.
- **CU-13 Instalar una app recomendada**: el botón «Instalar» se deja pulsar sin
  modo administrador; avisa y redirige, pero aparenta funcionar.
- **RNF-SEG-02**: volver desde otra app reabre la pantalla de ajustes donde se
  estaba, sin pedir el PIN otra vez.

## Lo siguiente, por orden de urgencia

1. **RNF-SEG-01 — el PIN**. Hoy se guarda en texto plano en `doado_settings`, y
   ese fichero entra en la copia de seguridad en la nube. Hay que guardarlo con
   hash y sal, y sacarlo de `backup_rules.xml` y `data_extraction_rules.xml`.
   Es lo único de la lista que es un fallo de seguridad, no una funcionalidad
   que falte.
2. **Cerrar los conflictos del asistente** (ERS §7, CON-01 a CON-04): dónde se
   procesa la voz, qué puede ejecutar sin confirmar, cómo se activa y si actúa o
   solo guía. Bloquean todo el diseño de la pieza.
3. **Las dos cosas a medias** de arriba (CU-07 y CU-13).
4. **Botón flotante de «atrás»** (CU-03), con el aviso de CON-06: el servicio de
   accesibilidad implica un permiso muy amplio y revisión estricta en Google
   Play.

## Deuda técnica anotada

- `PackageManager` se consulta en el hilo principal en `onCreate` y `onResume`, y
  los iconos reales se recargan a 192 px sin caché en cada vuelta al inicio.
- `DoadoDimens.TileIconSize` y `MinTouchTarget` están declarados y sin usar;
  `AppTile` y `HomeScreen` llevan medidas a pelo.
- No hay pruebas automáticas de ningún tipo.
- Solo español, sin `values-xx` (RNF-IDI-01 deja abierto si se añade gallego).
- Las apps recomendadas están fijas en `SuggestedApps.kt` y duplicadas en el
  `<queries>` del manifiesto: añadir una tercera obliga a tocar los dos sitios.

## Cómo dejar esto al terminar una sesión

Actualiza la fecha de arriba y las tres primeras secciones. Si tomaste una
decisión que descartó una alternativa razonable, añade una entrada a
`docs/decisiones.md` en el mismo commit.
