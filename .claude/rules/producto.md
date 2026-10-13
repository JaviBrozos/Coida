# Producto: qué es Doado y qué no se rediscute

Doado es la pantalla de inicio de Android para una persona mayor, con unos
ajustes detrás de un PIN para el familiar que se la configura. El asistente de
voz con IA es la principal propuesta de valor del producto; está programado y en
pruebas, con sus normas en `.claude/rules/asistente.md`.

## Vocabulario

Usa estas palabras, en la interfaz y al hablar del proyecto. El glosario completo
está en la ERS, sección 6.

- **Casilla**: el botón grande del inicio que abre una app. No es «tarjeta», ni
  «tile», ni «icono».
- **Acceso directo**: el botón del inicio con el nombre de una persona.
- **Modo administrador**: los 15 minutos en los que se abren las salidas a
  Android. No es un usuario ni un perfil: es un estado.
- **Inicio**: la pantalla principal. No la llames «home» ni «escritorio».
- **Familiar**: quien configura. No «tutor», «admin» ni «cuidador» a secas.

## Quién es quién

- **ACT-01, la persona mayor**: usa el inicio. Nunca ve un ajuste, un permiso,
  un error técnico ni un diálogo que pueda romper algo.
- **ACT-02, el familiar**: entra con PIN y configura. Es quien lee los textos
  largos, no ella.
- **ACT-03, el asistente**: oye a la persona mayor y, según elija el familiar,
  hace lo que pide tras confirmarlo o le explica cómo hacerlo. Solo con los
  contactos guardados y las casillas del inicio.

Fichas completas en la ERS, sección 4.1.1.

## Decisiones cerradas: no las reabras

Están razonadas en `docs/decisiones.md`. Si crees que una debería cambiar,
dilo y espera respuesta; no la cambies dentro de otra tarea.

- Los favoritos se definen por acción de Android, nunca por paquete.
- Sin color dinámico (Material You).
- Las casillas no se reordenan ni se ocultan cuando una app falta.
- Los ajustes van tras el PIN; las salidas a Android, tras el modo administrador.
- El modo administrador **no** promete impedir salir a Android: solo cierra las
  salidas de Doado. No propongas *device owner* ni *lock task* sin abrir el tema
  aparte.
- Preferencias en `SharedPreferences`; sin base de datos.
- Sin librería de navegación.
- El asistente se procesa en la nube, con un cliente en formato OpenAI que
  permite cambiar de proveedor desde `local.properties` (D-25).
- El asistente **nunca** actúa sin un «Sí», pulsado o dicho; el dicho lo reconoce el móvil, no la IA (D-26, D-31).
- Actuar o guiar lo elige el familiar; de fábrica, guía (D-27).
- Se activa con el círculo fijo del inicio, no con palabra clave; Ajustes es el
  botón pequeño de la esquina (D-28).

## Conflictos abiertos: no los cierres tú

La ERS, sección 7, recoge las decisiones sin tomar. Hoy queda una, del
asistente: CON-06, cómo proteger la clave del servicio de IA sin servidor propio.
No bloquea las pruebas, pero sí publicar.

Si una tarea depende de una de ellas, **para y pregunta**. Son decisiones de
producto de los dos creadores, no detalles de implementación.

## Lo que Doado no hace

Sin servidor propio, sin cuentas, sin analítica, sin leer toda la agenda. No
añadas permisos ni dependencias nuevas sin pedirlo antes de forma explícita. El
asistente habla con un servicio externo, pero solo le manda la voz de la orden y
nombres, nunca números de teléfono.

## Antes de proponer una función nueva

Mírala en la ERS: puede que ya sea un caso de uso (CU-01 a CU-16) con estado y
prioridad. Si lo es, cítalo por su ID. Si no lo es, es una propuesta nueva y se
dice como tal.
