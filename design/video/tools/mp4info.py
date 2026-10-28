"""Lee un MP4 caja a caja y comprueba lo que importa para el vídeo de Doado.

Sin dependencias: duración, número de muestras, que los fotogramas estén igual de
espaciados, fotogramas clave, perfil H.264, configuración AAC, lista de edición y
que cada trozo caiga dentro del mdat.

Uso:
    py tools/mp4info.py out/doado-18s.mp4
"""

import struct
import sys

CONTENEDORES = {b"moov", b"trak", b"mdia", b"minf", b"stbl", b"edts", b"dinf"}


def cajas(d, ini, fin):
    o = ini
    while o + 8 <= fin:
        tam, tipo = struct.unpack_from(">I4s", d, o)
        cab = 8
        if tam == 1:
            tam = struct.unpack_from(">Q", d, o + 8)[0]
            cab = 16
        elif tam == 0:
            tam = fin - o
        yield tipo, o, o + cab, o + tam
        o += tam


def arbol(d, ini, fin, ruta=()):
    for tipo, o, cuerpo, f in cajas(d, ini, fin):
        r = ruta + (tipo,)
        yield r, o, cuerpo, f
        if tipo in CONTENEDORES:
            yield from arbol(d, cuerpo, f, r)


def main():
    d = open(sys.argv[1], "rb").read()
    print(f"fichero: {len(d) / 1e6:.2f} MB")
    top = [(t.decode(), o, f - o) for t, o, _, f in cajas(d, 0, len(d))]
    print("cajas de primer nivel:", ", ".join(f"{t}({n / 1e6:.2f} MB)" if n > 1e5 else f"{t}({n} B)" for t, _, n in top))
    mdat = next((o, o + n) for t, o, n in top if t == "mdat")
    pistas, actual = [], None
    for r, o, c, f in arbol(d, 0, len(d)):
        t = r[-1]
        if t == b"trak":
            actual = {}
            pistas.append(actual)
        if actual is None:
            if t == b"mvhd":
                esc, dur = struct.unpack_from(">II", d, c + 12)
                print(f"mvhd: escala {esc}, duración {dur / esc:.4f} s")
            continue
        if t == b"tkhd":
            actual["id"] = struct.unpack_from(">I", d, c + 12)[0]
            actual["tk_dur"] = struct.unpack_from(">I", d, c + 20)[0]
            actual["w"], actual["h"] = (x / 65536 for x in struct.unpack_from(">II", d, c + 76))
        elif t == b"mdhd":
            actual["esc"], actual["dur"] = struct.unpack_from(">II", d, c + 12)
        elif t == b"hdlr":
            actual["tipo"] = d[c + 8:c + 12].decode()
        elif t == b"elst":
            n = struct.unpack_from(">I", d, c + 4)[0]
            actual["elst"] = [struct.unpack_from(">IiHH", d, c + 8 + 12 * i) for i in range(n)]
        elif t == b"stsd":
            e = c + 8
            fmt = d[e + 4:e + 8]
            actual["fmt"] = fmt.decode()
            if fmt == b"avc1":
                for tt, oo, cc, ff in cajas(d, e + 8 + 78, e + struct.unpack_from(">I", d, e)[0]):
                    if tt == b"avcC":
                        actual["avc"] = f"perfil {d[cc + 1]} nivel {d[cc + 3] / 10:.1f}"
                    if tt == b"colr":
                        actual["colr"] = d[cc:cc + 4].decode() + " " + "/".join(str(x) for x in struct.unpack_from(">HHH", d, cc + 4)) + f" rango completo {d[cc + 10] >> 7}"
            if fmt == b"mp4a":
                actual["canales"], _, _, _, sr = struct.unpack_from(">HHHHI", d, e + 8 + 16)
                actual["sr"] = sr >> 16
        elif t == b"stts":
            n = struct.unpack_from(">I", d, c + 4)[0]
            actual["stts"] = [struct.unpack_from(">II", d, c + 8 + 8 * i) for i in range(n)]
        elif t == b"stss":
            actual["claves"] = struct.unpack_from(">I", d, c + 4)[0]
        elif t == b"stsz":
            n = struct.unpack_from(">I", d, c + 8)[0]
            actual["tam"] = list(struct.unpack_from(f">{n}I", d, c + 12))
        elif t == b"stco":
            n = struct.unpack_from(">I", d, c + 4)[0]
            actual["stco"] = list(struct.unpack_from(f">{n}I", d, c + 8))
        elif t == b"stsc":
            n = struct.unpack_from(">I", d, c + 4)[0]
            actual["stsc"] = [struct.unpack_from(">III", d, c + 8 + 12 * i) for i in range(n)]
    ok = True
    for p in pistas:
        n = len(p["tam"])
        print(f"\npista {p['id']} ({p['tipo']}, {p['fmt']}): {n} muestras, escala {p['esc']}, duración {p['dur'] / p['esc']:.4f} s")
        deltas = sorted({dl for _, dl in p["stts"]})
        print(f"  stts: {p['stts']}  -> deltas distintos: {deltas}")
        if p["tipo"] == "vide":
            print(f"  {p['w']:.0f}x{p['h']:.0f}, {p.get('avc')}, colr {p.get('colr')}, {p.get('claves')} fotogramas clave")
            fps = p["esc"] / deltas[0] if len(deltas) == 1 else None
            print(f"  fps: {fps}  (espaciado {'uniforme' if len(deltas) == 1 else 'NO uniforme'})")
            ok &= len(deltas) == 1 and abs(p["dur"] / p["esc"] - p["tk_dur"] / 1000) < 1e-6
            primero = p["stco"][0]
            nal = d[primero + 4] & 0x1F
            print(f"  primer NAL del primer fotograma: tipo {nal}")
        else:
            print(f"  {p.get('canales')} canales a {p.get('sr')} Hz; edición {p.get('elst')}")
        # Cada trozo entero dentro del mdat.
        reparto, k = [], 0
        for i, off in enumerate(p["stco"]):
            fila = max(f for f in p["stsc"] if f[0] <= i + 1)
            m = fila[1]
            fin = off + sum(p["tam"][k:k + m])
            reparto.append(m)
            if off < mdat[0] + 8 or fin > mdat[1]:
                print(f"  ERROR: el trozo {i} se sale del mdat")
                ok = False
            k += m
        if k != n:
            print(f"  ERROR: los trozos suman {k} muestras y hay {n}")
            ok = False
        print(f"  {len(p['stco'])} trozos; {sum(p['tam']) / 1e6:.2f} MB")
    print("\nRESULTADO:", "bien" if ok else "MAL")


if __name__ == "__main__":
    main()
