"""Mete las dos Inter horneadas en src/fuentes.js, en base64.

Así la vista previa (index.html) se abre con doble clic, sin servidor: desde
file:// el navegador no deja cargar fuentes sueltas.

Uso:
    py tools/empaquetar_fuentes.py
"""

import base64
import pathlib

raiz = pathlib.Path(__file__).resolve().parent.parent
fuentes = {
    "semibold": raiz / "fonts" / "inter-semibold-ss04.ttf",
    "bold": raiz / "fonts" / "inter-bold-ss04.ttf",
}
partes = [f"  {k}: '{base64.b64encode(p.read_bytes()).decode('ascii')}'," for k, p in fuentes.items()]
texto = ("// Generado por tools/empaquetar_fuentes.py: Inter (OFL) con ss04 horneado. No editar a mano.\n"
         "window.FUENTES = {\n" + "\n".join(partes) + "\n};\n")
(raiz / "src" / "fuentes.js").write_text(texto, encoding="ascii")
print("src/fuentes.js:", round(len(texto) / 1024), "KB")
