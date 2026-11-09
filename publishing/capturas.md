# Capturas e imágenes para Google Play

Las capturas de `docs/capturas/` son antiguas: se hicieron antes del asistente y
del cambio de nombre. Hay que sacarlas de nuevo con la **versión de release**, para
que no aparezca el círculo del asistente (D-63).

## Lo que pide Play

| Pieza | Formato | ¿Obligatoria? | Estado |
| --- | --- | --- | --- |
| Icono | PNG de 32 bits, 512 × 512 px, hasta 1 MB, a sangre (Play redondea las esquinas) | Sí | Hecho: `publishing/icono-512.png` |
| Gráfico destacado | JPG o PNG de 24 bits sin transparencia, 1024 × 500 px | Sí | Por hacer |
| Capturas de móvil | De 2 a 8, JPG o PNG de 24 bits, lados entre 320 y 3840 px y el largo no más del doble del corto. Para recomendaciones, al menos 4 de 1080 px o más | Sí, 2 como poco | Por hacer |
| Capturas de tableta de 7″ y 10″ | Igual que las de móvil | No, pero recomendadas: la app funciona en tabletas | Por hacer |

## Lista de capturas de móvil (en este orden)

Con un emulador de móvil (1080 × 2400), Coida de release puesta como inicio,
idioma del sistema en español y la letra del sistema al 100 %. Sin notificaciones
en la barra de estado. Con contactos de ejemplo, nunca reales: «María», «Juan» y
«Centro de salud», con números inventados.

- [ ] **1. El inicio, tema oscuro** (el de fábrica): reloj, 2 por fila, las cuatro
      casillas básicas y dos contactos favoritos con foto.
- [ ] **2. El inicio, tema claro**, con 3 por fila y la casilla de Emergencias
      encendida.
- [ ] **3. Letra «Enorme»**, para enseñar que todo se agranda.
- [ ] **4. El PIN** de los ajustes.
- [ ] **5. Ajustes**, el índice, con los apartados a la vista.
- [ ] **6. Interfaz**: tamaño de letra y casillas por fila.
- [ ] **7. Contactos**: una persona con sus botones de llamada y WhatsApp.
- [ ] **8. Modo administrador** desbloqueado, con la cuenta atrás.

Antes de subirlas:

- [ ] En ninguna sale el círculo del micrófono ni la fila «Asistente de voz».
- [ ] En ninguna sale «Doado».
- [ ] Ningún dato personal real: ni nombres, ni números, ni fotos de la agenda propia.
- [ ] Un pie corto por captura si se añade texto encima («Casillas grandes que no
      se mueven», «Sus personas a un toque»…), en Inter, con los colores de marca y
      el texto con 4,5:1 de contraste como mínimo.

## Tableta (si se suben)

- [ ] El inicio en apaisado con 4 por fila: con 2 por fila las casillas son
      cuadradas y no caben en la pantalla.
- [ ] Ajustes en apaisado.

## Gráfico destacado (1024 × 500)

- [ ] Fondo marino `#0B1D2E`, la C en cian `#29B6F0` y «Coida» en Inter Bold, con el
      lema «Una app para tus mayores». Sin capturas dentro, que Play lo recorta en
      algunos sitios. Nada importante a menos de 15 % del borde.
- [ ] Pasarlo por el auditor de marca (`.claude/agents/auditor-marca.md`).

## Cómo sacar una captura limpia

```bash
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb exec-out screencap -p > captura.png
adb shell am broadcast -a com.android.systemui.demo -e command exit
```
