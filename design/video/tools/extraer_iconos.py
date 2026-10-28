"""Copia los iconos de la app (res/drawable/ic_*.xml) a un módulo de JavaScript.

El vídeo dibuja los mismos trazados que la app, sin redibujarlos: rejilla de 24,
trazo de 1,7 con puntas redondas (D-34). Solo biblioteca estándar.

Uso:
    py tools/extraer_iconos.py ../../app/src/main/res/drawable src/iconos.js
"""

import json
import pathlib
import sys
import xml.etree.ElementTree as ET

A = "{http://schemas.android.com/apk/res/android}"


def visible(color):
    """Un color #AARRGGBB con alfa 0 (como el relleno de los iconos de trazo) no pinta."""
    if not color or not color.startswith("#"):
        return color is not None
    return not (len(color) == 9 and color[1:3] == "00")


def main():
    origen, destino = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
    iconos = {}
    for f in sorted(origen.glob("ic_*.xml")):
        raiz = ET.parse(f).getroot()
        if raiz.tag != "vector":
            continue
        trazos = []
        for p in raiz.iter("path"):
            trazos.append({
                "d": p.get(A + "pathData"),
                "stroke": visible(p.get(A + "strokeColor")),
                "fill": visible(p.get(A + "fillColor")),
                "width": float(p.get(A + "strokeWidth", "0")),
                "evenOdd": p.get(A + "fillType") == "evenOdd",
            })
        if next(raiz.iter("group"), None) is not None:
            print(f"aviso: {f.name} tiene grupos; se ignoran sus transformaciones")
        iconos[f.stem[3:]] = {"vb": float(raiz.get(A + "viewportWidth", "24")), "paths": trazos}
    texto = ("// Generado por tools/extraer_iconos.py desde app/src/main/res/drawable. No editar a mano.\n"
             "window.ICONOS = " + json.dumps(iconos, ensure_ascii=False, indent=1) + ";\n")
    destino.write_text(texto, encoding="utf-8")
    print(f"{len(iconos)} iconos: {', '.join(iconos)}")


if __name__ == "__main__":
    main()
