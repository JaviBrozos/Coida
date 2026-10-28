'use strict';
/* Lo que hace render.html: pinta fotogramas y manda los resultados a tools/servidor.py.

   Modos (?modo=):
   - hojas: hojas de contacto y fotogramas sueltos, sin codificar. Para revisar rápido.
     Parámetros: desde, hasta, paso, cols, tw, nombre, fotos (lista de tiempos), lint=1
   - final: el MP4 entero, 1080 fotogramas a 60 fps, con desenfoque de movimiento
     (sub subfotogramas en un obturador de obturador fotogramas), música y efectos. */
(async function () {
  const D = window.D;
  const q = new URLSearchParams(location.search);
  const modo = q.get('modo') || 'hojas';
  const log = (s) => fetch('/log', { method: 'POST', body: String(s) });
  const send = (nombre, cuerpo) => fetch('/out/' + nombre, { method: 'POST', body: cuerpo });
  const fin = (s) => fetch('/done', { method: 'POST', body: s });
  const png = (c) => new Promise((r) => c.toBlob(r, 'image/png'));
  window.addEventListener('error', (e) => { log('ERROR ' + (e.error && e.error.stack || e.message)); fin('ERROR ' + e.message); });
  window.addEventListener('unhandledrejection', (e) => { log('ERROR ' + (e.reason && e.reason.stack || e.reason)); fin('ERROR ' + e.reason); });

  const lienzo = document.createElement('canvas');
  lienzo.width = D.W;
  lienzo.height = D.H;
  D.canvas = lienzo;
  await D.ready;

  // Hoja de contacto: miniaturas con su tiempo debajo.
  function hoja(n, cols, tw) {
    const th = Math.round((tw * 9) / 16), pie = 26;
    const c = document.createElement('canvas');
    c.width = cols * tw;
    c.height = Math.ceil(n / cols) * (th + pie);
    const x = c.getContext('2d');
    x.fillStyle = '#000';
    x.fillRect(0, 0, c.width, c.height);
    return {
      c,
      put(i, fuente, etiqueta) {
        const cx = (i % cols) * tw, cy = Math.floor(i / cols) * (th + pie);
        x.drawImage(fuente, cx, cy, tw, th);
        x.fillStyle = '#ddd';
        x.font = '16px sans-serif';
        x.fillText(etiqueta, cx + 6, cy + th + 18);
      },
    };
  }

  // Textos de distinto grupo (o el mismo, si son distintos) que se pisan.
  function solapes(t) {
    const it = D.lint.items.filter((a) => a.a > 0.12);
    const res = [];
    for (let i = 0; i < it.length; i++) for (let j = i + 1; j < it.length; j++) {
      const a = it[i], b = it[j];
      const w = Math.min(a.x1, b.x1) - Math.max(a.x0, b.x0), h = Math.min(a.y1, b.y1) - Math.max(a.y0, b.y0);
      if (w > 3 && h > 3) res.push({ t: +t.toFixed(4), a: `${a.group}:${a.str}`, b: `${b.group}:${b.str}`, area: Math.round(w * h) });
    }
    return res;
  }

  async function hojas() {
    const desde = +(q.get('desde') || 0), hasta = +(q.get('hasta') || D.DUR), paso = +(q.get('paso') || 0.25);
    const tiempos = [];
    for (let t = desde; t < hasta - 1e-9; t += paso) tiempos.push(+t.toFixed(5));
    const h = hoja(tiempos.length, +(q.get('cols') || 8), +(q.get('tw') || 240));
    for (let i = 0; i < tiempos.length; i++) {
      await window.seek(tiempos[i]);
      h.put(i, lienzo, tiempos[i].toFixed(3) + ' s');
    }
    await send(q.get('nombre') || 'hoja.png', await png(h.c));
    for (const f of (q.get('fotos') || '').split(',').filter(Boolean)) {
      await window.seek(+f);
      await send(`foto_${(+f).toFixed(3)}.png`, await png(lienzo));
    }
    // Recortes ampliados: t,x,y,ancho,alto,zoom separados por comas; varios con «;».
    for (const r of (q.get('recortes') || '').split(';').filter(Boolean)) {
      const [t, x, y, w, h, z] = r.split(',').map(Number);
      await window.seek(t);
      const c = document.createElement('canvas');
      c.width = w * z; c.height = h * z;
      const cx = c.getContext('2d');
      cx.imageSmoothingEnabled = false;
      cx.drawImage(lienzo, x, y, w, h, 0, 0, w * z, h * z);
      await send(`recorte_${t.toFixed(3)}_${x}_${y}.png`, await png(c));
    }
    if (q.get('lint')) {
      D.lint.on = true;
      const todo = [];
      for (let n = 0; n < D.DUR * D.FPS; n++) {
        await window.seek(n / D.FPS);
        todo.push(...solapes(n / D.FPS));
      }
      D.lint.on = false;
      await send('solapes.json', JSON.stringify(todo, null, 1));
      await log(`[lint] ${todo.length} solapes de texto`);
    }
  }

  // Un trozo con el desenfoque de verdad, sin codificar: para revisar movimiento.
  // Parámetros: desde, hasta, cada (fotogramas), sub, obturador, fotos (tiempos).
  async function tramo() {
    const SUB = +(q.get('sub') || 5), OBT = +(q.get('obturador') || 0.5);
    const n0 = Math.round(+(q.get('desde') || 0) * D.FPS), n1 = Math.round(+(q.get('hasta') || 1) * D.FPS);
    const cada = +(q.get('cada') || 1);
    const fotos = new Set((q.get('fotos') || '').split(',').filter(Boolean).map((x) => Math.round(+x * D.FPS)));
    const acc = D.Acum(lienzo);
    const total = Math.ceil((n1 - n0) / cada);
    const h = hoja(total, +(q.get('cols') || 6), +(q.get('tw') || 320));
    let i = 0;
    for (let n = n0; n < n1; n += cada) {
      acc.clear();
      D.slice = OBT / D.FPS / SUB;
      for (let k = 0; k < SUB; k++) { await window.seek(n / D.FPS + ((k + 0.5) / SUB - 0.5) * (OBT / D.FPS)); acc.add(1 / SUB); }
      D.slice = 0;
      const c = acc.show();
      h.put(i++, c, (n / D.FPS).toFixed(3));
      if (fotos.has(n)) await send(`tramo_${(n / D.FPS).toFixed(3)}.png`, await png(c));
    }
    await send(q.get('nombre') || 'tramo.png', await png(h.c));
  }

  // La música, vista: espectrograma, nivel por tramos y marcas en los tiempos clave.
  async function audio() {
    const buf = await D.renderAudio();
    const L = buf.getChannelData(0), R = buf.getChannelData(1);
    const W = 1800, H = 700, N = 2048, paso = Math.floor(buf.length / W);
    const c = document.createElement('canvas'); c.width = W; c.height = H + 200;
    const x = c.getContext('2d');
    x.fillStyle = '#000'; x.fillRect(0, 0, W, H + 200);
    const img = x.createImageData(W, H);
    const re = new Float32Array(N), im = new Float32Array(N);
    const fft = () => {
      for (let i = 1, j = 0; i < N; i++) { let b = N >> 1; for (; j & b; b >>= 1) j ^= b; j ^= b; if (i < j) { [re[i], re[j]] = [re[j], re[i]]; [im[i], im[j]] = [im[j], im[i]]; } }
      for (let len = 2; len <= N; len <<= 1) {
        const a = (-2 * Math.PI) / len;
        for (let i = 0; i < N; i += len) for (let k = 0; k < len / 2; k++) {
          const cr = Math.cos(a * k), ci = Math.sin(a * k);
          const ur = re[i + k], ui = im[i + k];
          const vr = re[i + k + len / 2] * cr - im[i + k + len / 2] * ci, vi = re[i + k + len / 2] * ci + im[i + k + len / 2] * cr;
          re[i + k] = ur + vr; im[i + k] = ui + vi; re[i + k + len / 2] = ur - vr; im[i + k + len / 2] = ui - vi;
        }
      }
    };
    for (let col = 0; col < W; col++) {
      const s0 = col * paso;
      for (let i = 0; i < N; i++) { const w = 0.5 - 0.5 * Math.cos((2 * Math.PI * i) / N); re[i] = ((L[s0 + i] || 0) + (R[s0 + i] || 0)) * 0.5 * w; im[i] = 0; }
      fft();
      for (let row = 0; row < H; row++) {
        const f = 30 * Math.pow(16000 / 30, 1 - row / H);
        const bin = Math.min(N / 2 - 1, Math.round((f * N) / 48000));
        const db = 20 * Math.log10(Math.hypot(re[bin], im[bin]) + 1e-9);
        const v = D.clamp((db + 70) / 70);
        const o = (row * W + col) * 4;
        img.data[o] = 255 * Math.pow(v, 2.2); img.data[o + 1] = 255 * Math.pow(v, 1.2); img.data[o + 2] = 255 * Math.pow(v, 0.6); img.data[o + 3] = 255;
      }
    }
    x.putImageData(img, 0, 0);
    // Nivel RMS cada 50 ms.
    x.strokeStyle = '#29B6F0'; x.beginPath();
    const tramos = [];
    for (let t = 0; t < D.DUR; t += 0.05) {
      const a = Math.floor(t * 48000), b = Math.floor((t + 0.05) * 48000);
      let s = 0; for (let i = a; i < b; i++) s += L[i] * L[i] + R[i] * R[i];
      const db = 10 * Math.log10(s / (2 * (b - a)) + 1e-12);
      tramos.push(db);
      const px = (t / D.DUR) * W, py = H + 190 - D.clamp((db + 50) / 50) * 180;
      t === 0 ? x.moveTo(px, py) : x.lineTo(px, py);
    }
    x.stroke();
    x.fillStyle = '#fff'; x.font = '14px sans-serif';
    for (const [n, ts] of [['logo', 0], ['UI', 1.46], ['groove', 2], ['teclas', 5.02], ['elige', 5.62], ['Sí', 7.42], ['cian', 7.5], ['zoom', 8.44], ['frase', 10], ['fundas', 11.24], ['flotan', 12.5], ['D', 15], ['final', 16]]) {
      const px = (D.out(ts) / D.DUR) * W;
      x.fillRect(px, 0, 1, H + 200); x.fillText(n, px + 3, 14);
    }
    await send('audio.png', await png(c));
    const media = (a, b) => 10 * Math.log10(tramos.slice(Math.round(a / 0.05), Math.round(b / 0.05)).reduce((s, d) => s + Math.pow(10, d / 10), 0) / Math.round((b - a) / 0.05));
    const O = D.out;
    await log('[audio] pico antes de normalizar ' + buf.pico.toFixed(3) + '; RMS por tramos (dB): ' + JSON.stringify({
      entrada: media(0, O(2)).toFixed(1), ritmo1: media(O(2), O(7.5)).toFixed(1), respiro: media(O(7.5), O(10)).toFixed(1),
      ritmo2: media(O(10), O(16)).toFixed(1), final: media(O(16), D.DUR - 0.05).toFixed(1),
    }));
  }

  async function final() {
    const SUB = +(q.get('sub') || 5);
    const OBT = +(q.get('obturador') || 0.5);
    const N = D.DUR * D.FPS;
    const t0 = performance.now();

    // 1. Audio: se sintetiza entero, se guarda en WAV y se codifica en AAC.
    const audio = await D.renderAudio();
    await send('musica.wav', D.wav(audio));
    const aac = await D.encodeAAC(audio);
    await log(`[audio] ${aac.chunks.length} tramas AAC, retardo ${aac.delay} muestras, ${((performance.now() - t0) / 1000).toFixed(1)} s`);

    // 2. Vídeo.
    const acc = D.Acum(lienzo);
    const trozos = [];
    let config = null;
    const enc = new VideoEncoder({
      output: (chunk, meta) => {
        if (meta && meta.decoderConfig && !config) config = meta.decoderConfig;
        const b = new Uint8Array(chunk.byteLength);
        chunk.copyTo(b);
        trozos.push({ data: b, key: chunk.type === 'key', ts: chunk.timestamp });
      },
      error: (e) => { throw e; },
    });
    const cfg = {
      codec: 'avc1.64002A', width: D.W, height: D.H, framerate: D.FPS,
      bitrate: +(q.get('bitrate') || 16e6), bitrateMode: q.get('bitmode') || 'constant', latencyMode: 'quality',
      // Con --gpu en tools/servidor.py se puede usar el codificador de la tarjeta, que sí
      // cumple el caudal pedido; el de software se queda corto en las escenas lisas.
      hardwareAcceleration: q.get('hw') || 'prefer-software', avc: { format: 'avc' },
    };
    const ok = await VideoEncoder.isConfigSupported(cfg);
    if (!ok.supported) throw new Error('Configuración de vídeo no soportada');
    enc.configure(cfg);
    const hojaG = hoja(Math.ceil(N / 15), 8, 240);
    // Ventanas densas: en tiempo de escena por defecto (se pasan al del vídeo), o en
    // tiempo de vídeo si llegan en la consulta.
    const densas = q.get('densas')
      ? q.get('densas').split(',').map((r) => r.split('-').map(Number))
      : [[2.3, 2.55], [5.7, 5.95], [7.45, 7.7], [8.9, 9.4], [13.95, 14.2], [14.8, 15.3], [15.3, 15.9]].map(([a, b]) => [D.out(a), D.out(b)]);
    const hojasD = densas.map(([a, b]) => ({ a, b, h: hoja(Math.round((b - a) * D.FPS), 6, 320), i: 0 }));

    for (let n = 0; n < N; n++) {
      acc.clear();
      D.slice = OBT / D.FPS / SUB;
      for (let k = 0; k < SUB; k++) {
        const t = n / D.FPS + ((k + 0.5) / SUB - 0.5) * (OBT / D.FPS);
        await window.seek(t);
        acc.add(1 / SUB);
      }
      D.slice = 0;
      const datos = acc.yuv();
      const vf = new VideoFrame(datos, {
        format: 'I420', codedWidth: D.W, codedHeight: D.H,
        timestamp: Math.round((n * 1e6) / D.FPS), duration: Math.round(1e6 / D.FPS),
        colorSpace: { primaries: 'bt709', transfer: 'bt709', matrix: 'bt709', fullRange: false },
      });
      enc.encode(vf, { keyFrame: n % 60 === 0 });
      vf.close();
      const tn = n / D.FPS;
      if (n % 15 === 0) hojaG.put(n / 15, acc.show(), tn.toFixed(2) + ' s');
      for (const hd of hojasD) if (tn >= hd.a - 1e-9 && tn < hd.b - 1e-9) hd.h.put(hd.i++, acc.show(), tn.toFixed(3));
      while (enc.encodeQueueSize > 8) await new Promise((r) => setTimeout(r, 2));
      if (n % 60 === 59) await log(`[vídeo] ${n + 1}/${N} fotogramas, ${((performance.now() - t0) / 1000).toFixed(0)} s`);
    }
    await enc.flush();
    enc.close();

    // 3. MP4.
    const mp4 = D.mp4.build({
      video: { samples: trozos, width: D.W, height: D.H, fps: D.FPS, avcC: new Uint8Array(config.description) },
      audio: { samples: aac.chunks, sampleRate: 48000, channels: 2, asc: aac.asc, delay: aac.delay, frames: audio.length },
    });
    await send(q.get('nombre') || 'doado-18s.mp4', new Blob([mp4]));
    await send('hoja-final.png', await png(hojaG.c));
    for (const hd of hojasD) await send(`densa_${hd.a.toFixed(2)}-${hd.b.toFixed(2)}.png`, await png(hd.h.c));
    const claves = trozos.filter((s) => s.key).length;
    const stats = {
      fotogramas: trozos.length, claves, subfotogramas: SUB, obturador: OBT,
      perfil: config.codec, bytesVideo: trozos.reduce((s, x) => s + x.data.length, 0),
      tramasAudio: aac.chunks.length, retardoAudio: aac.delay, segundos: (performance.now() - t0) / 1000,
    };
    await send('stats.json', JSON.stringify(stats, null, 1));
    await log('[final] ' + JSON.stringify(stats));
  }

  try {
    if (modo === 'hojas') await hojas();
    else if (modo === 'tramo') await tramo();
    else if (modo === 'audio') await audio();
    else if (modo === 'final') await final();
    else throw new Error('modo desconocido: ' + modo);
    await fin('ok');
  } catch (e) {
    await log('ERROR ' + (e.stack || e));
    await fin('ERROR ' + e);
  }
})();
