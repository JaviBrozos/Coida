"""Hornea el juego ss04 de Inter en una copia de la fuente.

La marca pide Inter siempre con ss04 (D-50): la I con remates y la l con cola. El
canvas 2D del navegador no deja activar rasgos OpenType, así que aquí se lee qué
glifos cambia ss04 en la tabla GSUB y se reescribe el cmap para que esos
caracteres apunten directamente a las alternativas. El resto del fichero no se
toca. Solo biblioteca estándar.

Uso:
    py tools/hornear_ss04.py entrada.ttf salida.ttf
"""

import struct
import sys


def u16(b, o):
    return struct.unpack_from(">H", b, o)[0]


def s16(b, o):
    return struct.unpack_from(">h", b, o)[0]


def u32(b, o):
    return struct.unpack_from(">I", b, o)[0]


def leer_tablas(data):
    n = u16(data, 4)
    tablas = {}
    for i in range(n):
        tag, _, off, ln = struct.unpack_from(">4sIII", data, 12 + 16 * i)
        tablas[tag.decode("latin1")] = data[off:off + ln]
    return u32(data, 0), tablas


def cobertura(b, o):
    """Glifos de una tabla Coverage, en orden de índice de cobertura."""
    fmt = u16(b, o)
    if fmt == 1:
        return [u16(b, o + 4 + 2 * i) for i in range(u16(b, o + 2))]
    if fmt == 2:
        glifos = []
        for i in range(u16(b, o + 2)):
            ini, fin, _ = struct.unpack_from(">HHH", b, o + 4 + 6 * i)
            glifos.extend(range(ini, fin + 1))
        return glifos
    raise ValueError(f"Coverage de formato {fmt}")


def mapa_ss04(gsub):
    lista_rasgos = u16(gsub, 6)
    lista_busquedas = u16(gsub, 8)
    indices = set()
    for i in range(u16(gsub, lista_rasgos)):
        reg = lista_rasgos + 2 + 6 * i
        if gsub[reg:reg + 4] == b"ss04":
            rasgo = lista_rasgos + u16(gsub, reg + 4)
            for j in range(u16(gsub, rasgo + 2)):
                indices.add(u16(gsub, rasgo + 4 + 2 * j))
    mapa, otros = {}, []
    for li in sorted(indices):
        lo = lista_busquedas + u16(gsub, lista_busquedas + 2 + 2 * li)
        tipo = u16(gsub, lo)
        for s in range(u16(gsub, lo + 4)):
            so = lo + u16(gsub, lo + 6 + 2 * s)
            t = tipo
            if t == 7:  # extensión: apunta a la subtabla de verdad
                t = u16(gsub, so + 2)
                so = so + u32(gsub, so + 4)
            if t != 1:
                otros.append(t)
                continue
            fmt = u16(gsub, so)
            cov = cobertura(gsub, so + u16(gsub, so + 2))
            if fmt == 1:
                delta = s16(gsub, so + 4)
                for g in cov:
                    mapa[g] = (g + delta) & 0xFFFF
            else:
                for k, g in enumerate(cov):
                    mapa[g] = u16(gsub, so + 6 + 2 * k)
    return mapa, sorted(indices), otros


def leer_cmap(cmap):
    """Carácter -> glifo, uniendo los subtablas de formato 4 y 12."""
    res = {}
    n = u16(cmap, 2)
    for i in range(n):
        plat, enc, off = struct.unpack_from(">HHI", cmap, 4 + 8 * i)
        fmt = u16(cmap, off)
        if fmt == 4:
            seg2 = u16(cmap, off + 6)
            seg = seg2 // 2
            ends = off + 14
            starts = ends + seg2 + 2
            deltas = starts + seg2
            ranges = deltas + seg2
            for k in range(seg):
                fin, ini = u16(cmap, ends + 2 * k), u16(cmap, starts + 2 * k)
                d, ro = u16(cmap, deltas + 2 * k), u16(cmap, ranges + 2 * k)
                for c in range(ini, fin + 1):
                    if c == 0xFFFF:
                        continue
                    if ro == 0:
                        g = (c + d) & 0xFFFF
                    else:
                        g = u16(cmap, ranges + 2 * k + ro + 2 * (c - ini))
                        if g:
                            g = (g + d) & 0xFFFF
                    if g:
                        res.setdefault(c, g)
        elif fmt == 12:
            ng = u32(cmap, off + 12)
            for k in range(ng):
                ini, fin, g0 = struct.unpack_from(">III", cmap, off + 16 + 12 * k)
                for c in range(ini, fin + 1):
                    res[c] = g0 + (c - ini)
    return res


def formato4(mapa):
    cps = sorted(c for c in mapa if c < 0xFFFF)
    segs = []
    for c in cps:
        g = mapa[c]
        if segs and segs[-1][1] == c - 1 and segs[-1][2] + (c - segs[-1][0]) == g:
            segs[-1][1] = c
        else:
            segs.append([c, c, g])
    segs.append([0xFFFF, 0xFFFF, 0])  # segmento final obligatorio
    n = len(segs)
    rango = 2 * (1 << (n.bit_length() - 1))
    selector = (n.bit_length() - 1)
    fins = b"".join(struct.pack(">H", s[1]) for s in segs)
    inis = b"".join(struct.pack(">H", s[0]) for s in segs)
    deltas = b"".join(struct.pack(">H", ((s[2] - s[0]) & 0xFFFF) if s[0] != 0xFFFF else 1) for s in segs)
    ros = b"\x00\x00" * n
    cuerpo = struct.pack(">HHHH", 2 * n, rango, selector, 2 * n - rango) + fins + b"\x00\x00" + inis + deltas + ros
    largo = 6 + len(cuerpo)
    if largo > 0xFFFF:
        raise ValueError("cmap de formato 4 demasiado grande")
    return struct.pack(">HHH", 4, largo, 0) + cuerpo


def formato12(mapa):
    grupos = []
    for c in sorted(mapa):
        g = mapa[c]
        if grupos and grupos[-1][1] == c - 1 and grupos[-1][2] + (c - grupos[-1][0]) == g:
            grupos[-1][1] = c
        else:
            grupos.append([c, c, g])
    cuerpo = b"".join(struct.pack(">III", *gr) for gr in grupos)
    return struct.pack(">HHIII", 12, 0, 16 + len(cuerpo), 0, len(grupos)) + cuerpo


def cmap_nuevo(mapa):
    f4, f12 = formato4(mapa), formato12(mapa)
    regs = [(0, 3, "f4"), (0, 4, "f12"), (3, 1, "f4"), (3, 10, "f12")]
    base = 4 + 8 * len(regs)
    offs = {"f4": base, "f12": base + len(f4) + (-len(f4) % 4)}
    cab = struct.pack(">HH", 0, len(regs)) + b"".join(struct.pack(">HHI", p, e, offs[k]) for p, e, k in regs)
    return cab + f4 + b"\x00" * (-len(f4) % 4) + f12


def suma(b):
    b = b + b"\x00" * (-len(b) % 4)
    return sum(struct.unpack(f">{len(b) // 4}I", b)) & 0xFFFFFFFF


def escribir(sfnt, tablas):
    tags = sorted(tablas)
    n = len(tags)
    rango = 16 * (1 << (n.bit_length() - 1))
    cab = struct.pack(">IHHHH", sfnt, n, rango, n.bit_length() - 1, 16 * n - rango)
    off = 12 + 16 * n
    regs, datos = [], []
    for t in tags:
        d = tablas[t]
        if t == "head":
            d = d[:8] + b"\x00\x00\x00\x00" + d[12:]
        regs.append(struct.pack(">4sIII", t.encode("latin1"), suma(d), off, len(d)))
        datos.append(d + b"\x00" * (-len(d) % 4))
        off += len(datos[-1])
    fuente = bytearray(cab + b"".join(regs) + b"".join(datos))
    # checkSumAdjustment del head, sobre el fichero entero
    pos = 12 + 16 * n + sum(len(x) for x in datos[:tags.index("head")])
    ajuste = (0xB1B0AFBA - suma(bytes(fuente))) & 0xFFFFFFFF
    struct.pack_into(">I", fuente, pos + 8, ajuste)
    return bytes(fuente)


def main():
    entrada, salida = sys.argv[1], sys.argv[2]
    data = open(entrada, "rb").read()
    sfnt, tablas = leer_tablas(data)
    mapa, busquedas, otros = mapa_ss04(tablas["GSUB"])
    cmap = leer_cmap(tablas["cmap"])
    cambiados = []
    for c, g in sorted(cmap.items()):
        if g in mapa:
            cmap[c] = mapa[g]
            cambiados.append(c)
    print(f"ss04: búsquedas {busquedas}, {len(mapa)} sustituciones; otros tipos {otros or 'ninguno'}")
    print("caracteres cambiados:", "".join(chr(c) for c in cambiados))
    tablas["cmap"] = cmap_nuevo(cmap)
    tablas.pop("DSIG", None)  # la firma ya no valdría
    open(salida, "wb").write(escribir(sfnt, tablas))
    print(f"escrito {salida}")


if __name__ == "__main__":
    main()
