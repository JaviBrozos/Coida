'use strict';
/* Empaquetador MP4 (ISO BMFF) para una pista H.264 y una AAC.

   Escrito a mano para no depender de nada: ftyp, moov delante (arranca a
   reproducirse sin leer el fichero entero) y mdat con vídeo y audio intercalados
   cada medio segundo. El vídeo va a 60 fps exactos: base de tiempos 60000 y 1000
   por fotograma, todos iguales. El audio lleva una lista de edición que salta el
   retardo inicial del codificador AAC, para que suene a la vez que la imagen. */
(function () {
  const D = window.D;
  const te = new TextEncoder();
  const u16 = (n) => [(n >>> 8) & 255, n & 255];
  const u32 = (n) => [(n >>> 24) & 255, (n >>> 16) & 255, (n >>> 8) & 255, n & 255];
  const s4 = (s) => Array.from(te.encode(s));
  const MATRIZ = [...u32(0x00010000), ...u32(0), ...u32(0), ...u32(0), ...u32(0x00010000), ...u32(0), ...u32(0), ...u32(0), ...u32(0x40000000)];

  function unir(partes) {
    const arr = partes.map((p) => (p instanceof Uint8Array ? p : new Uint8Array(p)));
    const n = arr.reduce((s, a) => s + a.length, 0);
    const out = new Uint8Array(n);
    let o = 0;
    for (const a of arr) { out.set(a, o); o += a.length; }
    return out;
  }
  function caja(tipo, ...partes) {
    const cuerpo = unir(partes);
    return unir([u32(8 + cuerpo.length), s4(tipo), cuerpo]);
  }
  const plena = (tipo, ver, flags, ...partes) => caja(tipo, [ver, (flags >>> 16) & 255, (flags >>> 8) & 255, flags & 255], ...partes);

  function stsc(porTrozo) {
    const filas = [];
    porTrozo.forEach((n, i) => { if (!filas.length || filas[filas.length - 1][1] !== n) filas.push([i + 1, n]); });
    return plena('stsc', 0, 0, u32(filas.length), ...filas.map(([c, n]) => [...u32(c), ...u32(n), ...u32(1)]));
  }
  const stsz = (tam) => plena('stsz', 0, 0, u32(0), u32(tam.length), ...tam.map((s) => u32(s)));
  const stco = (offs) => plena('stco', 0, 0, u32(offs.length), ...offs.map((o) => u32(o)));
  const dinf = () => caja('dinf', plena('dref', 0, 0, u32(1), plena('url ', 0, 1)));
  const hdlr = (tipo, nombre) => plena('hdlr', 0, 0, u32(0), s4(tipo), u32(0), u32(0), u32(0), s4(nombre), [0]);
  const mdhd = (escala, dur) => plena('mdhd', 0, 0, u32(0), u32(0), u32(escala), u32(dur), u16(0x55c4), u16(0));
  const tkhd = (id, dur, vol, w, h) => plena('tkhd', 0, 3, u32(0), u32(0), u32(id), u32(0), u32(dur), u32(0), u32(0),
    u16(0), u16(0), u16(vol), u16(0), MATRIZ, u32(w * 65536), u32(h * 65536));

  function moov(v, a, offV, offA, mvDur) {
    // ---- Vídeo ----
    const vEsc = 60000, vDelta = 60000 / v.fps;
    const nV = v.samples.length;
    const compresor = new Array(32).fill(0);
    const nombre = s4('Doado H.264');
    compresor[0] = nombre.length;
    nombre.forEach((c, i) => { compresor[1 + i] = c; });
    const avc1 = caja('avc1',
      [0, 0, 0, 0, 0, 0], u16(1), u16(0), u16(0), u32(0), u32(0), u32(0),
      u16(v.width), u16(v.height), u32(0x00480000), u32(0x00480000), u32(0), u16(1),
      compresor, u16(0x0018), u16(0xffff),
      caja('avcC', v.avcC),
      // BT.709 de rango limitado, igual que la conversión de src/acum.js.
      caja('colr', s4('nclx'), u16(1), u16(1), u16(1), [0]),
      caja('pasp', u32(1), u32(1)));
    const claves = v.samples.map((s, i) => (s.key ? i + 1 : 0)).filter(Boolean);
    const trakV = caja('trak',
      tkhd(1, mvDur, 0, v.width, v.height),
      caja('mdia',
        mdhd(vEsc, nV * vDelta),
        hdlr('vide', 'VideoHandler'),
        caja('minf',
          plena('vmhd', 0, 1, u16(0), u16(0), u16(0), u16(0)),
          dinf(),
          caja('stbl',
            plena('stsd', 0, 0, u32(1), avc1),
            plena('stts', 0, 0, u32(1), u32(nV), u32(vDelta)),
            plena('stss', 0, 0, u32(claves.length), ...claves.map((k) => u32(k))),
            stsc(offV.porTrozo),
            stsz(v.samples.map((s) => s.data.length)),
            stco(offV.offs)))));

    // ---- Audio ----
    const nA = a.samples.length;
    const asc = Array.from(a.asc);
    const dsi = [0x05, asc.length, ...asc];
    const dcd = [0x04, 13 + dsi.length, 0x40, 0x15, 0, 0, 0, ...u32(192000), ...u32(192000), ...dsi];
    const sl = [0x06, 0x01, 0x02];
    const esd = [0x03, 3 + dcd.length + sl.length, ...u16(1), 0x00, ...dcd, ...sl];
    const mp4a = caja('mp4a',
      [0, 0, 0, 0, 0, 0], u16(1), u32(0), u32(0), u16(a.channels), u16(16), u16(0), u16(0),
      u32(a.sampleRate * 65536), plena('esds', 0, 0, esd));
    const trakA = caja('trak',
      tkhd(2, mvDur, 0x0100, 0, 0),
      // Se salta el retardo del codificador: el primer sonido cae en el fotograma 0.
      caja('edts', plena('elst', 0, 0, u32(1), u32(mvDur), u32(a.delay), u16(1), u16(0))),
      caja('mdia',
        mdhd(a.sampleRate, nA * 1024),
        hdlr('soun', 'SoundHandler'),
        caja('minf',
          plena('smhd', 0, 0, u16(0), u16(0)),
          dinf(),
          caja('stbl',
            plena('stsd', 0, 0, u32(1), mp4a),
            plena('stts', 0, 0, u32(1), u32(nA), u32(1024)),
            stsc(offA.porTrozo),
            stsz(a.samples.map((s) => s.data.length)),
            stco(offA.offs)))));

    return caja('moov',
      plena('mvhd', 0, 0, u32(0), u32(0), u32(1000), u32(mvDur), u32(0x00010000), u16(0x0100), u16(0),
        u32(0), u32(0), MATRIZ, new Array(24).fill(0), u32(3)),
      trakV, trakA);
  }

  D.mp4 = {
    build({ video, audio }) {
      const fps = video.fps;
      // El vídeo sale del codificador en orden de presentación (H.264 sin fotogramas B);
      // si no, haría falta una tabla ctts. Se comprueba en vez de suponerlo.
      video.samples.forEach((s, i) => {
        const esperado = Math.round((i * 1e6) / fps);
        if (Math.abs(s.ts - esperado) > 2) throw new Error(`Fotograma ${i} fuera de orden (${s.ts} µs)`);
      });
      const mvDur = Math.round((video.samples.length / fps) * 1000);
      // Intercalado: un trozo de vídeo y otro de audio por cada medio segundo.
      const VENT = 0.5;
      const ventanas = Math.ceil(D.DUR / VENT) + 1;
      const trozos = [];
      for (let w = 0; w < ventanas; w++) {
        const a0 = w * VENT, a1 = (w + 1) * VENT;
        const vs = video.samples.filter((s, i) => i / fps >= a0 - 1e-9 && i / fps < a1 - 1e-9);
        if (vs.length) trozos.push({ pista: 'v', muestras: vs });
        const as = audio.samples.filter((s, i) => (i * 1024) / audio.sampleRate >= a0 - 1e-9 && (i * 1024) / audio.sampleRate < a1 - 1e-9);
        if (as.length) trozos.push({ pista: 'a', muestras: as });
      }
      const calcular = (base) => {
        const offV = { offs: [], porTrozo: [] }, offA = { offs: [], porTrozo: [] };
        let o = base;
        for (const t of trozos) {
          const dest = t.pista === 'v' ? offV : offA;
          dest.offs.push(o);
          dest.porTrozo.push(t.muestras.length);
          o += t.muestras.reduce((s, m) => s + m.data.length, 0);
        }
        return { offV, offA };
      };
      const ftyp = caja('ftyp', s4('isom'), u32(512), s4('isom'), s4('iso2'), s4('avc1'), s4('mp41'));
      // El tamaño del moov no depende de los desplazamientos: se calcula una vez con
      // ceros y otra con los de verdad.
      const prueba = calcular(0);
      const tamMoov = moov(video, audio, prueba.offV, prueba.offA, mvDur).length;
      const datos = trozos.reduce((s, t) => s + t.muestras.reduce((x, m) => x + m.data.length, 0), 0);
      const inicioDatos = ftyp.length + tamMoov + 8;
      const real = calcular(inicioDatos);
      const mv = moov(video, audio, real.offV, real.offA, mvDur);
      if (mv.length !== tamMoov) throw new Error('El moov cambió de tamaño');
      const out = new Uint8Array(inicioDatos + datos);
      out.set(ftyp, 0);
      out.set(mv, ftyp.length);
      out.set(u32(8 + datos), ftyp.length + mv.length);
      out.set(s4('mdat'), ftyp.length + mv.length + 4);
      let o = inicioDatos;
      for (const t of trozos) for (const m of t.muestras) { out.set(m.data, o); o += m.data.length; }
      return out;
    },
  };
})();
