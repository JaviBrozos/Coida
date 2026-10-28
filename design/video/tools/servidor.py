"""Servidor local y lanzador de Chrome sin interfaz para el vídeo de Doado.

Sirve una carpeta, abre en ella una página con Chrome sin interfaz y guarda lo que
la página le manda por POST: el MP4, las hojas de contacto, los informes. Así el
render entero ocurre dentro del navegador (canvas, WebCodecs, Web Audio) y aquí
solo hace falta la biblioteca estándar de Python: nada que instalar.

Uso:
    py tools/servidor.py --pagina render.html --salida out [--consulta "a=1&b=2"]

La página habla con el servidor así:
    POST /log          una línea de texto para la consola
    POST /out/<nombre> un fichero que se guarda en la carpeta de salida
    POST /done         fin: el servidor cierra Chrome y termina
"""

import argparse
import http.server
import os
import pathlib
import subprocess
import sys
import threading
import time

CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--raiz", default=str(pathlib.Path(__file__).resolve().parent.parent))
    p.add_argument("--pagina", required=True)
    p.add_argument("--salida", required=True)
    p.add_argument("--consulta", default="")
    p.add_argument("--puerto", type=int, default=8765)
    p.add_argument("--perfil", default=os.path.join(os.environ.get("TEMP", "."), "doado-video-chrome"))
    p.add_argument("--limite", type=float, default=3600, help="segundos antes de rendirse")
    p.add_argument("--gpu", action="store_true", help="no desactivar la GPU de Chrome")
    a = p.parse_args()

    raiz = pathlib.Path(a.raiz).resolve()
    salida = pathlib.Path(a.salida)
    if not salida.is_absolute():
        salida = raiz / salida
    salida.mkdir(parents=True, exist_ok=True)
    fin = threading.Event()
    estado = {"error": None}

    class Manejador(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kw):
            super().__init__(*args, directory=str(raiz), **kw)

        extensions_map = {**http.server.SimpleHTTPRequestHandler.extensions_map,
                          ".js": "text/javascript", ".mjs": "text/javascript",
                          ".json": "application/json", ".wasm": "application/wasm"}

        def end_headers(self):
            # Sin caché: cada render lee el código recién editado.
            self.send_header("Cache-Control", "no-store")
            super().end_headers()

        def log_message(self, fmt, *args):
            pass

        def do_POST(self):
            n = int(self.headers.get("Content-Length", "0"))
            cuerpo = self.rfile.read(n) if n else b""
            ruta = self.path.split("?")[0]
            if ruta == "/log":
                print(cuerpo.decode("utf-8", "replace"), flush=True)
            elif ruta.startswith("/out/"):
                nombre = ruta[len("/out/"):]
                if "/" in nombre or "\\" in nombre or nombre.startswith("."):
                    self.send_response(400); self.end_headers(); return
                (salida / nombre).write_bytes(cuerpo)
                print(f"[servidor] guardado {nombre} ({len(cuerpo) / 1024:.0f} KB)", flush=True)
            elif ruta == "/done":
                texto = cuerpo.decode("utf-8", "replace")
                if texto.startswith("ERROR"):
                    estado["error"] = texto
                fin.set()
            self.send_response(200)
            self.send_header("Content-Length", "0")
            self.end_headers()

    class Servidor(http.server.ThreadingHTTPServer):
        def handle_error(self, request, client_address):
            # Que Chrome corte una descarga al cerrarse no es un error del render.
            if isinstance(sys.exc_info()[1], (ConnectionResetError, ConnectionAbortedError, BrokenPipeError)):
                return
            super().handle_error(request, client_address)

    servidor = Servidor(("127.0.0.1", a.puerto), Manejador)
    threading.Thread(target=servidor.serve_forever, daemon=True).start()

    url = f"http://127.0.0.1:{a.puerto}/{a.pagina}" + (f"?{a.consulta}" if a.consulta else "")
    banderas = [
        "--headless=new", f"--user-data-dir={a.perfil}", "--no-first-run",
        "--no-default-browser-check", "--disable-extensions", "--mute-audio",
        "--disable-background-timer-throttling", "--disable-renderer-backgrounding",
        "--disable-backgrounding-occluded-windows", "--window-size=1920,1080",
        "--force-device-scale-factor=1", "--hide-scrollbars",
    ]
    if not a.gpu:
        # Raster por software: más lento, pero idéntico de una ejecución a otra.
        banderas.append("--disable-gpu")
    print(f"[servidor] abriendo {url}", flush=True)
    chrome = subprocess.Popen([CHROME, *banderas, url],
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    inicio = time.time()
    try:
        while not fin.wait(1):
            if time.time() - inicio > a.limite:
                estado["error"] = "ERROR: se acabó el tiempo"
                break
            if chrome.poll() is not None:
                estado["error"] = "ERROR: Chrome se cerró solo"
                break
    finally:
        subprocess.run(["taskkill", "/T", "/F", "/PID", str(chrome.pid)],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        servidor.shutdown()
    print(f"[servidor] fin en {time.time() - inicio:.1f} s", flush=True)
    if estado["error"]:
        print(estado["error"], flush=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
