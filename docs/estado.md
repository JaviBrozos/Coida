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

## Seguridad de la puerta de ajustes: probada en emulador

El 29/09/2026 se cerró el bloque de seguridad de la puerta de ajustes:
**RNF-SEG-01** (PIN derivado con PBKDF2 y sal, en `doado_security`, fuera de las
copias de seguridad, migrando el que hubiera en claro), **RNF-SEG-02** (salir de
Doado cierra los ajustes), **CU-07** (cambiar el PIN pide el anterior) y
**CU-13** (el botón deja de aparentar que instala sin modo administrador).

**RNF-SEG-01 comprobado en un dispositivo**: con Doado puesto como lanzador en el
emulador, el fichero `doado_security.xml` guarda el PIN como `sal$derivado` en
Base64, nunca en claro.

**Probado a mano el 29/09/2026** (Gonzalo; emulador Pixel 10 Pro XL, API 37,
partiendo de datos borrados con `adb shell pm clear com.doado.launcher`):

| Comprobación | Resultado |
| --- | --- |
| Crear el PIN, salir y volver a entrar | Correcto |
| Irse a otra app por recientes y volver: vuelve al inicio y Ajustes pide el PIN | Correcto |
| Abrir la tienda desde Aplicaciones y volver con Atrás: sigue en Aplicaciones, sin PIN | Correcto |
| Cambiar el PIN: pide primero el actual, rechaza uno erróneo y el nuevo sustituye al viejo | Correcto |
| Instalar sin modo administrador: no abre la tienda, avisa y lleva al modo administrador | Correcto |

Matices vistos al probar, todos esperados:

- Salir de Doado no pide el PIN al volver: devuelve al inicio, y el PIN se pide
  al pulsar Ajustes.
- Volver de la tienda con el botón **Inicio** también devuelve al inicio (Inicio
  lleva siempre ahí); solo **Atrás** conserva los ajustes.
- El modo administrador sigue activo tras salir y volver: dura sus 15 minutos.

**Sin probar**: la migración desde una versión anterior con el PIN en claro.
Exige instalar una versión antigua y se deja así salvo que dé problemas.

## Accesibilidad: auditoría del 29/09/2026

Un agente especializado (`.claude/agents/accesibilidad.md`, reutilizable) revisó
la interfaz entera contra las normas del proyecto y WCAG 2.2 AA. Encontró **11
incumplimientos, 2 críticos**. El informe completo está en un documento aparte.

**Aplicado y compilando** (9 arreglos, `lint` 0 errores): el gris de apagado
separado en dos tokens por tema —daba 3,21:1 sobre el blanco del tema claro—,
suelo de escala de letra para que Doado nunca encoja el texto por debajo de
18 sp, casilla con alto mínimo en vez de fijo, motivo de deshabilitado en los
botones del modo administrador, y cuatro arreglos menores de TalkBack y
lenguaje. `docs/diseno.md` corregido: medía los contrastes contra el fondo
equivocado.

**Abierto**: G-3 (cinco `Toast` que desaparecen solos), M-2 (borde a 3,18:1), M-3
(destello oscuro al arrancar en tema claro) y M-4 (medidas a pelo).

**Confirmado en el emulador al 200 % de letra**: los nombres de casilla se
truncan («Mensaj…», «Contact…»). C-1 **no** se reprodujo a esa escala: hace falta
además el ajuste propio de Doado en «Muy grande».

**C-1 reproducido el 29/09/2026** (Gonzalo; Pixel 10 Pro XL, API 37): Android al
200 % y Doado en «Muy grande». El reloj y la fecha ocupaban más de media pantalla,
la rejilla quedaba en una franja de un cuarto de alto (se desplaza, pero apenas se
ve) y los nombres se partían a media palabra («Cáma / ra», «Conta / ctos»). Con
Android al 200 % y Doado en normal, todo se leía bien.

**C-1 arreglado y probado el 29/09/2026**: techo de 2,0 a la escala total y de
1,3 al reloj y la fecha (D-22). Con Android al 200 % y Doado en «Muy grande», el
reloj cabe en una línea y se ven las cuatro casillas con el nombre entero.
Consecuencia a vigilar: con Android ya al máximo, cambiar la letra en Doado no se
nota, y el familiar puede pensar que el ajuste no funciona. Candidato a un aviso
en Ajustes → Interfaz.

## Lo siguiente, por orden

1. **Probar en un móvil o emulador los arreglos de la auditoría de accesibilidad.**
   El bloque de seguridad ya está probado (ver arriba).
2. **Decidir si RNF-ACC-02 miente o miente el código**: la norma promete casillas
   de más de 168 dp y a una columna miden 112–150 dp. O se sube el mínimo, o se
   corrigen los tres documentos que lo afirman. No es implementación.
3. **Sacar el `PackageManager` del hilo principal** y cachear los iconos. Ya
   estaba anotado abajo como deuda, pero la auditoría lo sube de prioridad: en un
   launcher corre en cada pulsación de Inicio, y un ANR es lo único que perjudica
   a la vez a la persona mayor y a la visibilidad en Google Play.
4. **Cerrar los conflictos del asistente** (ERS §7, CON-01 a CON-04): dónde se
   procesa la voz, qué puede ejecutar sin confirmar, cómo se activa y si actúa o
   solo guía. Bloquean todo el diseño de la pieza.
5. **CON-05, el PIN olvidado**, que ahora aprieta más: al guardarse derivado ya
   no se puede recuperar, solo borrar los datos de la app. La parte barata
   —avisar de ello al crearlo— es una cadena de texto y no espera al conflicto.
6. **Botón flotante de «atrás»** (CU-03), con el aviso de CON-06: el servicio de
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
