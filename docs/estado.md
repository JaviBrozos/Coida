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

## Escrito, pero sin probar en un móvil

El 29/09/2026 se cerró el bloque de seguridad de la puerta de ajustes:
**RNF-SEG-01** (PIN derivado con PBKDF2 y sal, en `doado_security`, fuera de las
copias de seguridad, migrando el que hubiera en claro), **RNF-SEG-02** (salir de
Doado cierra los ajustes), **CU-07** (cambiar el PIN pide el anterior) y
**CU-13** (el botón deja de aparentar que instala sin modo administrador).

Compila y `lint` da 0 errores, pero **no se ha podido probar en el emulador**: en
la máquina donde se escribió, el emulador no se mantenía en pie. Queda por
comprobar a mano, y por ese orden:

1. Crear el PIN, salir y volver a entrar (verifica el derivado de ida y vuelta).
2. Actualizar desde una versión anterior con PIN en claro y ver que sigue
   valiendo el mismo (la migración).
3. Irse a otra app por recientes y volver: debe pedir el PIN otra vez.
4. Abrir la tienda desde Aplicaciones y volver: **no** debe pedirlo.
5. Cambiar el PIN: debe pedir primero el actual.

## Lo siguiente, por orden

1. **Probar lo de arriba en un móvil o emulador de verdad.**
2. **Cerrar los conflictos del asistente** (ERS §7, CON-01 a CON-04): dónde se
   procesa la voz, qué puede ejecutar sin confirmar, cómo se activa y si actúa o
   solo guía. Bloquean todo el diseño de la pieza.
3. **CON-05, el PIN olvidado**, que ahora aprieta más: al guardarse derivado ya
   no se puede recuperar, solo borrar los datos de la app.
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
