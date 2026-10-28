'use strict';
/* Comprueba el MP4 ya escrito, como lo vería un reproductor:
   - lo abre en un <video> (el demultiplexor de Chrome) y lee duración y tamaño;
   - descodifica todos los fotogramas con VideoDecoder y mira cuántos salen y con qué
     espaciado;
   - descodifica el audio y mide su nivel;
   - saca una hoja de contacto del vídeo codificado (no del render). */
(async function () {
  const q = new URLSearchParams(location.search);
  const ruta = q.get('mp4') || 'out/doado-18s.mp4';
  const log = (s) => fetch('/log', { method: 'POST', body: String(s) });
  const send = (n, b) => fetch('/out/' + n, { method: 'POST', body: b });
  const fin = (s) => fetch('/done', { method: 'POST', body: s });
  const png = (c) => new Promise((r) => c.toBlob(r, 'image/png'));
  const rep = {};
  try {
    const d = new Uint8Array(await (await fetch(ruta)).arrayBuffer());
    const dv = new DataView(d.buffer);
    const U32 = (o) => dv.getUint32(o);
    const tipo = (o) => String.fromCharCode(d[o], d[o + 1], d[o + 2], d[o + 3]);
    function* cajas(ini, finB) { let o = ini; while (o + 8 <= finB) { const t = U32(o); yield { tipo: tipo(o + 4), o, c: o + 8, f: o + t }; o += t; } }
    const hijo = (b, t) => { for (const x of cajas(b.c, b.f)) if (x.tipo === t) return x; return null; };
    const moov = [...cajas(0, d.length)].find((b) => b.tipo === 'moov');
    const traks = [...cajas(moov.c, moov.f)].filter((b) => b.tipo === 'trak');
    const pistas = traks.map((tk) => {
      const mdia = hijo(tk, 'mdia'), stbl = hijo(hijo(mdia, 'minf'), 'stbl');
      const hd = hijo(mdia, 'hdlr');
      const tabla = (t) => hijo(stbl, t);
      const stsz = tabla('stsz'), stco = tabla('stco'), stsc = tabla('stsc'), stss = tabla('stss'), stsd = tabla('stsd');
      const n = U32(stsz.c + 8);
      const tam = [...Array(n)].map((_, i) => U32(stsz.c + 12 + 4 * i));
      const offs = [...Array(U32(stco.c + 4))].map((_, i) => U32(stco.c + 8 + 4 * i));
      const filas = [...Array(U32(stsc.c + 4))].map((_, i) => [U32(stsc.c + 8 + 12 * i), U32(stsc.c + 12 + 12 * i)]);
      const muestras = [];
      offs.forEach((off, ci) => {
        const m = filas.filter((f) => f[0] <= ci + 1).pop()[1];
        let o = off;
        for (let k = 0; k < m; k++) { const i = muestras.length; muestras.push({ o, n: tam[i] }); o += tam[i]; }
      });
      const claves = new Set();
      if (stss) for (let i = 0; i < U32(stss.c + 4); i++) claves.add(U32(stss.c + 8 + 4 * i) - 1);
      const ent = stsd.c + 8;
      return { tipo: tipo(hd.c + 8), muestras, claves, ent, entTipo: tipo(ent + 4), entFin: ent + U32(ent) };
    });
    const v = pistas.find((p) => p.tipo === 'vide'), a = pistas.find((p) => p.tipo === 'soun');
    let avcC = null;
    for (const b of cajas(v.ent + 8 + 78, v.entFin)) if (b.tipo === 'avcC') avcC = d.slice(b.c, b.f);
    let asc = null;
    (function buscar(o, f) { for (let i = o; i < f - 2; i++) if (d[i] === 0x05 && d[i - 1] !== 0x05 && i > o + 20) { asc = d.slice(i + 2, i + 2 + d[i + 1]); return; } })(a.ent + 36, a.entFin);
    const hex = (x) => x.toString(16).padStart(2, '0');
    const codec = `avc1.${hex(avcC[1])}${hex(avcC[2])}${hex(avcC[3])}`;
    rep.codec = codec;
    rep.muestrasVideo = v.muestras.length;
    rep.muestrasAudio = a.muestras.length;

    // Descodificar todo el vídeo.
    const W = 1920, H = 1080;
    const hojaCols = 6, tw = 320, th = 180, cada = 30;
    const hoja = document.createElement('canvas');
    hoja.width = hojaCols * tw; hoja.height = Math.ceil(v.muestras.length / cada / hojaCols) * (th + 24);
    const hx = hoja.getContext('2d'); hx.fillStyle = '#000'; hx.fillRect(0, 0, hoja.width, hoja.height);
    const fotos = (q.get('fotos') || '').split(',').filter(Boolean).map(Number);
    const recortes = (q.get('recortes') || '').split(';').filter(Boolean).map((r) => r.split(',').map(Number));
    const sueltos = [];
    let salidos = 0;
    const tiempos = [];
    const dec = new VideoDecoder({
      output: (fr) => {
        tiempos.push(fr.timestamp);
        const i = salidos++;
        if (i % cada === 0) {
          const k = i / cada;
          hx.drawImage(fr, (k % hojaCols) * tw, Math.floor(k / hojaCols) * (th + 24), tw, th);
          hx.fillStyle = '#ddd'; hx.font = '16px sans-serif';
          hx.fillText((i / 60).toFixed(2) + ' s', (k % hojaCols) * tw + 6, Math.floor(k / hojaCols) * (th + 24) + th + 18);
        }
        if (fotos.some((t) => Math.round(t * 60) === i)) {
          const c = document.createElement('canvas'); c.width = W; c.height = H;
          c.getContext('2d').drawImage(fr, 0, 0);
          sueltos.push([i, c]);
        }
        // Recortes ampliados del vídeo ya descodificado: t,x,y,ancho,alto,zoom; varios con «;».
        for (const r of recortes) if (Math.round(r[0] * 60) === i) {
          const [, x, y, w, h, z] = r;
          const c = document.createElement('canvas'); c.width = w * z; c.height = h * z;
          const cx = c.getContext('2d'); cx.imageSmoothingEnabled = false;
          cx.drawImage(fr, x, y, w, h, 0, 0, w * z, h * z);
          sueltos.push([`rec_${r.join('_')}`, c]);
        }
        fr.close();
      },
      error: (e) => { rep.errorDecodificar = String(e); },
    });
    dec.configure({ codec, codedWidth: W, codedHeight: H, description: avcC });
    v.muestras.forEach((m, i) => {
      dec.decode(new EncodedVideoChunk({ type: v.claves.has(i) ? 'key' : 'delta', timestamp: Math.round((i * 1e6) / 60), duration: Math.round(1e6 / 60), data: d.subarray(m.o, m.o + m.n) }));
    });
    await dec.flush();
    rep.fotogramasDescodificados = salidos;
    const difs = new Set();
    for (let i = 1; i < tiempos.length; i++) difs.add(tiempos[i] - tiempos[i - 1]);
    rep.espaciadosMicros = [...difs].sort();
    await send('verif-hoja.png', await png(hoja));
    for (const [i, c] of sueltos) await send(typeof i === 'string' ? `${i}.png` : `verif_${(i / 60).toFixed(3)}.png`, await png(c));

    // Audio.
    let suma = 0, n = 0, pico = 0;
    const ad = new AudioDecoder({
      output: (x) => {
        const b = new Float32Array(x.numberOfFrames);
        x.copyTo(b, { planeIndex: 0, format: 'f32-planar' });
        for (const s of b) { suma += s * s; pico = Math.max(pico, Math.abs(s)); }
        n += b.length;
        x.close();
      },
      error: (e) => { rep.errorAudio = String(e); },
    });
    ad.configure({ codec: 'mp4a.40.2', sampleRate: 48000, numberOfChannels: 2, description: asc });
    a.muestras.forEach((m, i) => ad.decode(new EncodedAudioChunk({ type: 'key', timestamp: Math.round((i * 1024 * 1e6) / 48000), data: d.subarray(m.o, m.o + m.n) })));
    await ad.flush();
    rep.audioSegundos = n / 48000;
    rep.audioRmsDb = 10 * Math.log10(suma / n);
    rep.audioPicoDb = 20 * Math.log10(pico);

    // Como lo abriría una página: <video>.
    const vid = document.createElement('video');
    vid.muted = true;
    vid.preload = 'auto';
    vid.src = ruta;
    await new Promise((r, j) => { vid.onloadedmetadata = r; vid.onerror = () => j(new Error('El <video> no lo abre: ' + (vid.error && vid.error.message))); });
    rep.videoElemento = { duracion: vid.duration, ancho: vid.videoWidth, alto: vid.videoHeight };
    await log('[verificar] ' + JSON.stringify(rep));
    await send('verificacion.json', JSON.stringify(rep, null, 1));
    await fin('ok');
  } catch (e) {
    await log('ERROR ' + (e.stack || e));
    await fin('ERROR ' + e);
  }
})();
