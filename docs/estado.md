# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 30/09/2026 · Javier Brozos**

## Dónde estamos

- Launcher completo: inicio y seis pantallas de ajustes tras el PIN.
- Puerta de ajustes segura (RNF-SEG-01/02, CU-07, CU-13) y recuperación del PIN
  con pregunta (D-23): probadas en emulador.
- Límite de intentos con bloqueo progresivo en la puerta de ajustes
  (RNF-SEG-03, D-24).
- Auditoría de accesibilidad aplicada y probada; C-1 arreglado con techo de
  letra (D-22).
- Asistente de voz: sin empezar, bloqueado por CON-01 a CON-04.

## Sin probar

- Migración del PIN antiguo en claro (exige instalar una versión vieja).
- Pantalla de configurar la pregunta de recuperación: solo se probó el guardado.
- Motivo de los botones deshabilitados, con TalkBack.

## Siguiente

1. Auditoría de seguridad del 30/09 (23 hallazgos, ninguno crítico): decidir qué
   se aplica. Lo primero, la respuesta de recuperación en claro y sin campo de
   contraseña (SEG-01, 02, 05).
2. RNF-ACC-02 promete casillas de más de 168 dp; a una columna miden 112–150 dp.
   Decidir si se sube el mínimo o se corrige la norma.
3. Sacar el `PackageManager` del hilo principal y cachear los iconos.
4. Auditoría abierta: G-3 (Toast), M-2 (borde 3,18:1), M-3 (destello al
   arrancar en claro), M-4 (medidas a pelo).
5. Cerrar CON-01 a CON-04 para empezar el asistente.

Deuda menor: sin pruebas automáticas, solo español, apps recomendadas duplicadas
entre `SuggestedApps.kt` y `<queries>`.

## Registro

| Fecha | Autor | Cambio |
| --- | --- | --- |
| 30/09 | Javier | Límite de intentos del PIN y la respuesta, con nota para volver al inicio (ERS 0.5, D-24), probado en emulador. Teclado del PIN clásico (hueco, 0 y «Borrar»; fuera «Empezar de nuevo») y puntos centrados. La nota cuenta solo los fallos de la visita. Auditoría de seguridad hecha |
| 30/09 | Javier | ERS 0.4: CU-16 implementado y CON-05 cerrado (D-23). README y arquitectura al día con el fichero del PIN y la recuperación |
| 30/09 | Javier | Producto reducido a dos piezas, launcher y asistente (ERS 0.3). Este fichero pasa a registro corto |
| 29/09 | Gonzalo | Pregunta de recuperación de PIN (D-23), probada en emulador |
| 29/09 | Gonzalo | Techo de letra (D-22): C-1 arreglado y probado |
| 29/09 | Gonzalo | Bloque de seguridad de la puerta de ajustes probado en emulador |
| 29/09 | Javier | Arreglos de la auditoría de accesibilidad |
