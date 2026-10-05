# Estado del trabajo

> Único documento que caduca. **Se actualiza en cada cambio**, con fecha y autor
> (obligación de `CLAUDE.md`). Máximo unas 50 líneas: lo antiguo se borra, el
> historial completo está en git. El estado de cada caso de uso vive en la ERS.

**Última actualización: 05/10/2026 · Gonzalo Gómez**

## Dónde estamos

- Launcher completo: inicio y seis pantallas de ajustes tras el PIN.
- Puerta de ajustes segura (RNF-SEG-01/02, CU-07, CU-13) y recuperación del PIN
  con pregunta (D-23): probadas en emulador.
- Auditoría de accesibilidad aplicada y probada; C-1 arreglado con techo de
  letra (D-22).
- Asistente de voz: sin empezar, bloqueado por CON-01 a CON-04.

## Sin probar

- Migración del PIN antiguo en claro (exige instalar una versión vieja).
- Motivo de los botones deshabilitados, con TalkBack.

## Siguiente

1. Recuperación del PIN: sin límite de intentos (ni PIN ni respuesta). Decidir
   si se acepta o se añade. La respuesta ya va derivada (D-24).
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
| 05/10 | Gonzalo | Campo de texto propio (`DoadoTextField`): el de Material no se veía y nadie sabía dónde escribir la pregunta. Pendiente de probar |
| 05/10 | Gonzalo | Respuesta de recuperación derivada como el PIN, con migración (D-24). Probada en emulador, también la creación de la pregunta a mano |
| 30/09 | Javier | ERS 0.4: CU-16 implementado y CON-05 cerrado (D-23). README y arquitectura al día con el fichero del PIN y la recuperación |
| 30/09 | Javier | Producto reducido a dos piezas, launcher y asistente (ERS 0.3). Este fichero pasa a registro corto |
| 29/09 | Gonzalo | Pregunta de recuperación de PIN (D-23), probada en emulador |
| 29/09 | Gonzalo | Techo de letra (D-22): C-1 arreglado y probado |
| 29/09 | Gonzalo | Bloque de seguridad de la puerta de ajustes probado en emulador |
| 29/09 | Javier | Arreglos de la auditoría de accesibilidad |
