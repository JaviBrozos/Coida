'use strict';
/* Música y efectos del vídeo, sintetizados con Web Audio en un OfflineAudioContext.

   Tema propio a 120 BPM en re mayor: bombo limpio, bajo cálido a contratiempo, un
   pad de acordes de novena y un arpegio de «pluck» con eco. Nada aleatorio sin
   semilla: el render suena igual cada vez. Los efectos son suaves, por debajo de la
   música, y caen en los mismos instantes que las animaciones (lee D.T). */
(function () {
  const D = window.D;
  const SR = 48000;
  const BEAT = 0.5;

  const N = { D2: 73.42, A1: 55.0, B1: 61.74, G1: 49.0 };
  const ACORDES = {
    Dmaj9: [146.83, 185.0, 220.0, 277.18, 329.63],
    Bm9: [123.47, 146.83, 185.0, 220.0, 277.18],
    Gmaj9: [98.0, 123.47, 146.83, 185.0, 220.0],
    A69: [110.0, 138.59, 164.81, 185.0, 246.94],
  };
  const RAIZ = { Dmaj9: N.D2, Bm9: N.B1, Gmaj9: N.G1, A69: N.A1 };


  D.renderAudio = async () => {
    const T = D.T;
    const ctx = new OfflineAudioContext(2, SR * D.DUR, SR);
    const rnd = D.rng(20261022);

    // ---- Buses ----
    const master = ctx.createGain();
    const comp = ctx.createDynamicsCompressor();
    comp.threshold.value = -14; comp.knee.value = 10; comp.ratio.value = 2.5;
    comp.attack.value = 0.006; comp.release.value = 0.16;
    master.connect(comp).connect(ctx.destination);
    const music = ctx.createGain(); music.gain.value = 0.85; music.connect(master);
    const sfx = ctx.createGain(); sfx.gain.value = 0.42; sfx.connect(master);
    const drums = ctx.createGain(); drums.gain.value = 0.9; drums.connect(music);
    const duck = ctx.createGain(); duck.connect(music); // pad y bajo, que se apartan del bombo

    // Reverb: respuesta de impulso generada con la semilla, que cae en unos 2,6 s.
    const ir = ctx.createBuffer(2, SR * 2.6, SR);
    for (let c = 0; c < 2; c++) {
      const d = ir.getChannelData(c);
      let lp = 0;
      for (let i = 0; i < d.length; i++) {
        const t = i / SR;
        lp = lp * 0.55 + (rnd() * 2 - 1) * 0.45;
        d[i] = lp * Math.exp(-t / 0.6) * (t < 0.012 ? t / 0.012 : 1);
      }
    }
    const reverb = ctx.createConvolver(); reverb.buffer = ir; reverb.normalize = true;
    const revOut = ctx.createGain(); revOut.gain.value = 0.32;
    reverb.connect(revOut).connect(master);
    // Eco a corchea con puntillo, oscurecido en cada vuelta.
    const delay = ctx.createDelay(1); delay.delayTime.value = BEAT * 0.75;
    const fb = ctx.createGain(); fb.gain.value = 0.3;
    const dlp = ctx.createBiquadFilter(); dlp.type = 'lowpass'; dlp.frequency.value = 2600;
    delay.connect(dlp).connect(fb).connect(delay);
    const delOut = ctx.createGain(); delOut.gain.value = 0.5;
    dlp.connect(delOut); delOut.connect(master); delOut.connect(reverb);

    // Ruido blanco con semilla, estéreo.
    const ruido = ctx.createBuffer(2, SR * 2, SR);
    for (let c = 0; c < 2; c++) { const d = ruido.getChannelData(c); for (let i = 0; i < d.length; i++) d[i] = rnd() * 2 - 1; }
    let cursor = 0;
    const fuenteRuido = (t, dur) => {
      const s = ctx.createBufferSource();
      s.buffer = ruido;
      s.loop = true;
      cursor = (cursor + 0.37) % 1.6;
      s.start(t, cursor);
      s.stop(t + dur + 0.05);
      return s;
    };
    const env = (g, t, pico, ataque, caida, fin = 0.0001) => {
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(pico, t + ataque);
      g.gain.exponentialRampToValueAtTime(fin, t + ataque + caida);
    };
    const pan = (v) => { const p = ctx.createStereoPanner(); p.pan.value = v; return p; };

    // ---- Instrumentos ----
    const kicks = [];
    function kick(t, v = 1) {
      kicks.push(t);
      const o = ctx.createOscillator(); o.type = 'sine';
      o.frequency.setValueAtTime(150, t);
      o.frequency.exponentialRampToValueAtTime(54, t + 0.07);
      o.frequency.exponentialRampToValueAtTime(44, t + 0.32);
      const g = ctx.createGain(); env(g, t, 0.9 * v, 0.003, 0.42);
      o.connect(g).connect(drums); o.start(t); o.stop(t + 0.5);
      const n = fuenteRuido(t, 0.02);
      const hp = ctx.createBiquadFilter(); hp.type = 'highpass'; hp.frequency.value = 2200;
      const g2 = ctx.createGain(); env(g2, t, 0.08 * v, 0.001, 0.012);
      n.connect(hp).connect(g2).connect(drums);
    }
    function hat(t, v = 1, abierto = false) {
      const n = fuenteRuido(t, abierto ? 0.3 : 0.08);
      const hp = ctx.createBiquadFilter(); hp.type = 'highpass'; hp.frequency.value = 7600;
      const g = ctx.createGain(); env(g, t, 0.16 * v, 0.001, abierto ? 0.22 : 0.045);
      n.connect(hp).connect(g).connect(pan(0.22)).connect(drums);
    }
    function clap(t, v = 1) {
      const n = fuenteRuido(t, 0.3);
      const bp = ctx.createBiquadFilter(); bp.type = 'bandpass'; bp.frequency.value = 1500; bp.Q.value = 0.9;
      const g = ctx.createGain();
      g.gain.setValueAtTime(0.0001, t);
      for (const [d, a] of [[0, 0.42], [0.012, 0.38], [0.024, 0.44]]) {
        g.gain.setValueAtTime(0.0001, t + d);
        g.gain.linearRampToValueAtTime(a * v, t + d + 0.002);
        g.gain.exponentialRampToValueAtTime(0.06 * v, t + d + 0.011);
      }
      g.gain.exponentialRampToValueAtTime(0.0001, t + 0.2);
      n.connect(bp).connect(g);
      g.connect(drums);
      const s = ctx.createGain(); s.gain.value = 0.35; g.connect(s).connect(reverb);
    }
    function bass(t, f, dur, v = 1) {
      const o1 = ctx.createOscillator(); o1.type = 'sawtooth'; o1.frequency.value = f;
      const o2 = ctx.createOscillator(); o2.type = 'sine'; o2.frequency.value = f;
      const lp = ctx.createBiquadFilter(); lp.type = 'lowpass'; lp.Q.value = 3;
      lp.frequency.setValueAtTime(1150, t); lp.frequency.exponentialRampToValueAtTime(260, t + 0.16);
      const m = ctx.createGain(); m.gain.value = 0.5;
      const g = ctx.createGain();
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(0.34 * v, t + 0.006);
      g.gain.exponentialRampToValueAtTime(0.2 * v, t + dur * 0.7);
      g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
      o1.connect(m).connect(lp); o2.connect(lp); lp.connect(g).connect(duck);
      o1.start(t); o2.start(t); o1.stop(t + dur + 0.02); o2.stop(t + dur + 0.02);
    }
    const padFiltro = ctx.createBiquadFilter(); padFiltro.type = 'lowpass'; padFiltro.Q.value = 0.6;
    const padBus = ctx.createGain(); padBus.gain.value = 0.5;
    padFiltro.connect(padBus).connect(duck);
    const padRev = ctx.createGain(); padRev.gain.value = 0.45; padBus.connect(padRev).connect(reverb);
    function pad(notas, t0, t1, v = 1) {
      notas.forEach((f, i) => {
        const g = ctx.createGain();
        const a = (0.05 * v) / Math.sqrt(notas.length / 5);
        g.gain.setValueAtTime(0.0001, t0);
        g.gain.linearRampToValueAtTime(a, t0 + 0.45);
        g.gain.setValueAtTime(a, t1 - 0.05);
        g.gain.exponentialRampToValueAtTime(0.0001, t1 + 0.9);
        const p = pan(-0.5 + i / (notas.length - 1));
        for (const det of [-6, 6]) {
          const o = ctx.createOscillator(); o.type = 'sawtooth'; o.frequency.value = f; o.detune.value = det;
          o.connect(g); o.start(t0); o.stop(t1 + 1);
        }
        const tr = ctx.createOscillator(); tr.type = 'triangle'; tr.frequency.value = f * 2;
        const tg = ctx.createGain(); tg.gain.value = 0.6; tr.connect(tg).connect(g); tr.start(t0); tr.stop(t1 + 1);
        g.connect(p).connect(padFiltro);
      });
    }
    function pluck(t, f, v = 1, p = 0) {
      const o = ctx.createOscillator(); o.type = 'triangle'; o.frequency.value = f;
      const o2 = ctx.createOscillator(); o2.type = 'sine'; o2.frequency.value = f * 2;
      const g2 = ctx.createGain(); g2.gain.value = 0.35;
      const lp = ctx.createBiquadFilter(); lp.type = 'lowpass';
      lp.frequency.setValueAtTime(4800, t); lp.frequency.exponentialRampToValueAtTime(1300, t + 0.3);
      const g = ctx.createGain(); env(g, t, 0.16 * v, 0.002, 0.55);
      o.connect(lp); o2.connect(g2).connect(lp); lp.connect(g);
      const out = pan(p); g.connect(out); out.connect(music);
      const ds = ctx.createGain(); ds.gain.value = 0.34; out.connect(ds).connect(delay);
      const rs = ctx.createGain(); rs.gain.value = 0.22; out.connect(rs).connect(reverb);
      o.start(t); o2.start(t); o.stop(t + 0.7); o2.stop(t + 0.7);
    }
    function bell(t, f, v = 1, dest = music) {
      [[1, 1, 2.2], [2, 0.36, 1.4], [3.01, 0.2, 0.9], [4.2, 0.1, 0.5]].forEach(([k, a, dec]) => {
        const o = ctx.createOscillator(); o.type = 'sine'; o.frequency.value = f * k;
        const g = ctx.createGain(); env(g, t, 0.07 * v * a, 0.004, dec);
        o.connect(g); g.connect(dest);
        const r = ctx.createGain(); r.gain.value = 0.55; g.connect(r).connect(reverb);
        o.start(t); o.stop(t + dec + 0.1);
      });
    }
    function sub(t0, t1, f, v = 1) {
      const o = ctx.createOscillator(); o.type = 'sine'; o.frequency.value = f;
      const g = ctx.createGain();
      g.gain.setValueAtTime(0.0001, t0);
      g.gain.exponentialRampToValueAtTime(0.22 * v, t1);
      g.gain.exponentialRampToValueAtTime(0.0001, t1 + 0.9);
      o.connect(g).connect(music); o.start(t0); o.stop(t1 + 1);
    }

    // ---- Efectos ----
    function whoosh(t, dur, v = 1, sube = true) {
      const n = fuenteRuido(t, dur);
      const bp = ctx.createBiquadFilter(); bp.type = 'bandpass'; bp.Q.value = 1.1;
      bp.frequency.setValueAtTime(sube ? 380 : 3200, t);
      bp.frequency.exponentialRampToValueAtTime(sube ? 3200 : 380, t + dur);
      const g = ctx.createGain();
      g.gain.setValueAtTime(0.0001, t);
      g.gain.exponentialRampToValueAtTime(0.32 * v, t + dur * 0.45);
      g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
      const p = ctx.createStereoPanner();
      p.pan.setValueAtTime(-0.35, t); p.pan.linearRampToValueAtTime(0.35, t + dur);
      n.connect(bp).connect(g).connect(p).connect(sfx);
    }
    function tick(t, f = 2500, v = 1) {
      const o = ctx.createOscillator(); o.type = 'sine'; o.frequency.value = f;
      const g = ctx.createGain(); env(g, t, 0.16 * v, 0.001, 0.03);
      o.connect(g).connect(sfx); o.start(t); o.stop(t + 0.06);
    }
    function tecla(t, v = 1) {
      const n = fuenteRuido(t, 0.02);
      const bp = ctx.createBiquadFilter(); bp.type = 'bandpass'; bp.frequency.value = 3400; bp.Q.value = 1.6;
      const g = ctx.createGain(); env(g, t, 0.34 * v, 0.0008, 0.014);
      n.connect(bp).connect(g).connect(sfx);
      tick(t, 1850, 0.35 * v);
    }
    function elegir(t, v = 1) {
      pluck(t, 880, 0.55 * v, -0.1);
      pluck(t + 0.06, 1318.5, 0.5 * v, 0.1);
    }
    function pulsar(t, v = 1) {
      tick(t, 1250, 0.7 * v);
      const n = fuenteRuido(t, 0.03);
      const lp = ctx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 1800;
      const g = ctx.createGain(); env(g, t, 0.25 * v, 0.001, 0.025);
      n.connect(lp).connect(g).connect(sfx);
    }
    function subida(t0, t1, v = 1) {
      const n = fuenteRuido(t0, t1 - t0);
      const bp = ctx.createBiquadFilter(); bp.type = 'bandpass'; bp.Q.value = 1.8;
      bp.frequency.setValueAtTime(300, t0); bp.frequency.exponentialRampToValueAtTime(5200, t1);
      const g = ctx.createGain();
      g.gain.setValueAtTime(0.0001, t0); g.gain.exponentialRampToValueAtTime(0.26 * v, t1 - 0.02);
      g.gain.linearRampToValueAtTime(0.0001, t1 + 0.01);
      n.connect(bp).connect(g).connect(sfx);
      const r = ctx.createGain(); r.gain.value = 0.4; g.connect(r).connect(reverb);
      const o = ctx.createOscillator(); o.type = 'sine';
      o.frequency.setValueAtTime(220, t0); o.frequency.exponentialRampToValueAtTime(880, t1);
      const go = ctx.createGain();
      go.gain.setValueAtTime(0.0001, t0); go.gain.exponentialRampToValueAtTime(0.05 * v, t1 - 0.02);
      go.gain.linearRampToValueAtTime(0.0001, t1 + 0.01);
      o.connect(go).connect(sfx); o.start(t0); o.stop(t1 + 0.05);
    }
    function impacto(t, v = 1) {
      const o = ctx.createOscillator(); o.type = 'sine';
      o.frequency.setValueAtTime(64, t); o.frequency.exponentialRampToValueAtTime(41, t + 0.7);
      const g = ctx.createGain(); env(g, t, 0.36 * v, 0.004, 0.9);
      o.connect(g).connect(sfx); o.start(t); o.stop(t + 1);
      const n = fuenteRuido(t, 0.5);
      const lp = ctx.createBiquadFilter(); lp.type = 'lowpass'; lp.frequency.value = 900;
      const g2 = ctx.createGain(); env(g2, t, 0.16 * v, 0.002, 0.35);
      n.connect(lp).connect(g2).connect(sfx);
      const r = ctx.createGain(); r.gain.value = 0.5; g2.connect(r).connect(reverb);
    }
    function aire(t0, t1, v = 1) {
      // Barrido inverso hacia un instante (como un platillo al revés), muy suave.
      const n = fuenteRuido(t0, t1 - t0);
      const hp = ctx.createBiquadFilter(); hp.type = 'highpass'; hp.frequency.value = 2400;
      const g = ctx.createGain();
      g.gain.setValueAtTime(0.0001, t0); g.gain.exponentialRampToValueAtTime(0.16 * v, t1 - 0.015);
      g.gain.linearRampToValueAtTime(0.0001, t1 + 0.01);
      n.connect(hp).connect(g).connect(sfx);
    }

    // ---- Arreglo ----
    // El guion va en tiempo de escena (D.T); el vídeo dura D.DUR y lo pone a media
    // velocidad con pausas de lectura. O() pasa un instante de escena a tiempo de vídeo:
    // así cada acento cae en el fotograma de su animación. El tempo sigue en 120 BPM.
    const O = D.out, FIN = D.DUR, X = 2;
    const SEC = { g1: [O(2), O(7.5)], br: [O(7.5), O(10)], g2: [O(10), O(16)], fin: [O(16), FIN] };
    const compases = [];
    const llenar = (a, b, ciclo) => {
      let i = 0;
      for (let t = a; t < b - 1e-6; t += 2 * BEAT * 2, i++) {
        const ac = ciclo[i % ciclo.length], hasta = Math.min(b, t + 4 * BEAT);
        const ult = compases[compases.length - 1];
        if (ult && ult[0] === ac && Math.abs(ult[2] - t) < 1e-6) ult[2] = hasta; else compases.push([ac, t, hasta]);
      }
    };
    llenar(0, SEC.g1[0], ['Dmaj9']);
    llenar(SEC.g1[0], SEC.g1[1], ['Bm9', 'Gmaj9', 'A69', 'Dmaj9']);
    llenar(SEC.br[0], SEC.br[1], ['Bm9']);
    llenar(SEC.g2[0], SEC.g2[1], ['Dmaj9', 'Bm9', 'Gmaj9', 'A69']);
    llenar(SEC.fin[0], FIN, ['Dmaj9']);
    const acordeEn = (t) => (compases.find(([, x, y]) => t >= x - 1e-6 && t < y - 1e-6) || compases[compases.length - 1])[0];
    for (const [ac, a, b] of compases) pad(ACORDES[ac], a, b, a >= SEC.fin[0] - 1e-6 ? 0.9 : 1);
    // Filtro del pad: se abre al empezar, se cierra en el respiro del relleno de marca,
    // vuelve a abrirse con la subida y se apaga al final.
    const pf = padFiltro.frequency;
    pf.setValueAtTime(700, 0); pf.exponentialRampToValueAtTime(2000, O(1.8));
    pf.setValueAtTime(2000, SEC.br[0]); pf.exponentialRampToValueAtTime(520, SEC.br[0] + 1.2);
    pf.setValueAtTime(520, O(8.5)); pf.exponentialRampToValueAtTime(2600, SEC.g2[0]);
    pf.setValueAtTime(2400, SEC.fin[0]); pf.exponentialRampToValueAtTime(900, FIN);
    // Antes del ritmo, un pulso suave que lo anuncia mientras llega la firma y sube el inicio.
    for (let t = O(0.62), k = 0; t < SEC.g1[0] - 1e-6; t += BEAT, k++) {
      kick(t, D.lerp(0.26, 0.5, (t - O(0.62)) / (SEC.g1[0] - O(0.62))));
      if (t > O(1.0)) hat(t + BEAT / 2, 0.4);
    }
    // Batería y bajo en los dos tramos con ritmo; el bombo final cierra la firma.
    for (const [a, b] of [SEC.g1, SEC.g2]) {
      let k = 0;
      for (let t = a; t < b - 1e-6; t += BEAT, k++) {
        kick(t);
        hat(t + BEAT / 2, 0.85, k % 4 === 3);
        bass(t + BEAT / 2, RAIZ[acordeEn(t)], 0.22);
        if (k % 2 === 1) clap(t, 0.85);
        // En el segundo tramo, semicorcheas fantasma: más empuje sin más volumen.
        if (a === SEC.g2[0]) { hat(t + BEAT / 4, 0.3); hat(t + (3 * BEAT) / 4, 0.22); }
      }
    }
    kick(SEC.br[0], 0.9);
    clap(SEC.br[0], 0.7);
    kick(SEC.fin[0], 0.9);
    bass(SEC.fin[0], N.D2, 1.6, 0.85);
    // Arpegio del pluck: tonos del acorde, discreto, en dos tramos.
    const patron = [0, 2, 1, 3, 2, 4, 3, 1];
    for (const [a, b] of [[O(4), SEC.g1[1]], [SEC.g2[0], SEC.g2[1] - BEAT]]) {
      let i = 0;
      for (let t = a; t < b - 1e-9; t += BEAT / 2, i++) {
        const f = ACORDES[acordeEn(t)][patron[i % 8]] * 4;
        pluck(t, f, i % 4 === 0 ? 0.9 : 0.55, i % 2 ? 0.25 : -0.25);
      }
    }
    // Dos notas para el titular de entrada.
    pluck(O(T.l1[0]), 587.33, 0.8, -0.2);
    pluck(O(T.l2[0]), 880.0, 0.75, 0.2);

    // ---- Acentos sincronizados con la imagen (duraciones al doble, como el movimiento) ----
    sub(0, O(0.5), N.D2, 0.9);
    bell(O(0.06), 1174.66, 0.7);
    aire(O(1.33), O(T.phoneIn[0]) + 0.02, 0.7);
    whoosh(O(T.phoneIn[0]), 0.5 * X, 0.7);
    whoosh(O(T.push[0]), 0.7 * X, 0.45, true);
    tick(O(T.dockIn[0] + 0.1), 1900, 0.5);
    T.rows.forEach((t, i) => tick(O(t + 0.04), 1500 + i * 220, 0.55));
    whoosh(O(T.strip[0]), 0.9 * X, 0.4, false);
    tick(O(T.fieldIn[0] + 0.04), 2200, 0.5);
    // Un toque suave por cada palabra que aparece de lo oído.
    const tt = T.typing, palabras = 3;
    for (let i = 0; i < palabras; i++) tecla(O(tt[0] + ((i + 1) * (tt[1] - tt[0])) / palabras), 0.6);
    elegir(O(T.select));
    whoosh(O(T.expand[0] - 0.02), 0.42 * X, 0.55);
    whoosh(O(T.slide[0]), 0.32 * X, 0.35, false);
    pulsar(O(T.press));
    bell(SEC.br[0], 880, 0.8); bell(SEC.br[0], 1174.66, 0.6);
    whoosh(SEC.br[0], 0.5 * X, 0.45);
    // El «hecho» del visto: dos notas que suben.
    pluck(O(T.check2[0]), 880, 0.6, -0.1); pluck(O(T.check2[0]) + 0.12, 1174.66, 0.6, 0.1);
    subida(O(T.unfill[0]), SEC.g2[0], 1);
    impacto(SEC.g2[0], 0.8);
    whoosh(O(T.sl3[0]), 0.55 * X, 0.4);
    [0, 0.06, 0.12].forEach((d, i) => pluck(O(T.spread[0] + d), [1174.66, 1479.98, 1760.0][i], 0.35, -0.3 + i * 0.3));
    whoosh(O(T.sleevesOut[0]), 0.45 * X, 0.3, false);
    aire(O(14.0), O(15.0), 0.9);
    whoosh(O(T.fold[0]), 0.5 * X, 0.3, false);
    bell(O(15.0), 1174.66, 0.8); bell(O(15.0), 1479.98, 0.45);
    sub(O(14.6), O(15.0), N.D2, 0.7);
    whoosh(O(T.slideLeft[0]), 0.5 * X, 0.25);
    bell(SEC.fin[0], 587.33, 0.7); bell(SEC.fin[0], 880, 0.55); bell(SEC.fin[0] + 0.02, 1174.66, 0.5);
    impacto(SEC.fin[0], 0.45);

    // Bombeo: pad y bajo se apartan un poco en cada bombo.
    const dg = duck.gain;
    dg.setValueAtTime(1, 0);
    for (const t of kicks.sort((a, b) => a - b)) {
      dg.setValueAtTime(1, t);
      dg.linearRampToValueAtTime(0.6, t + 0.012);
      dg.linearRampToValueAtTime(1, t + 0.3);
    }
    // Final: se apaga en el último medio segundo.
    master.gain.setValueAtTime(1, D.DUR - 1.4);
    master.gain.linearRampToValueAtTime(0.0001, D.DUR);

    const buf = await ctx.startRendering();
    // Normaliza el pico a -1 dBFS.
    let pico = 0;
    for (let c = 0; c < buf.numberOfChannels; c++) { const d = buf.getChannelData(c); for (let i = 0; i < d.length; i++) pico = Math.max(pico, Math.abs(d[i])); }
    const k = pico > 0 ? 0.891 / pico : 1;
    for (let c = 0; c < buf.numberOfChannels; c++) { const d = buf.getChannelData(c); for (let i = 0; i < d.length; i++) d[i] *= k; }
    buf.pico = pico;
    return buf;
  };

  // WAV de 16 bits, con tramado triangular de semilla fija.
  D.wav = (buf) => {
    const n = buf.length, ch = buf.numberOfChannels;
    const out = new DataView(new ArrayBuffer(44 + n * ch * 2));
    const w = (o, s) => { for (let i = 0; i < s.length; i++) out.setUint8(o + i, s.charCodeAt(i)); };
    w(0, 'RIFF'); out.setUint32(4, 36 + n * ch * 2, true); w(8, 'WAVE');
    w(12, 'fmt '); out.setUint32(16, 16, true); out.setUint16(20, 1, true); out.setUint16(22, ch, true);
    out.setUint32(24, SR, true); out.setUint32(28, SR * ch * 2, true); out.setUint16(32, ch * 2, true); out.setUint16(34, 16, true);
    w(36, 'data'); out.setUint32(40, n * ch * 2, true);
    const r = D.rng(7);
    const datos = [...Array(ch)].map((_, c) => buf.getChannelData(c));
    let o = 44;
    for (let i = 0; i < n; i++) for (let c = 0; c < ch; c++) {
      const v = datos[c][i] * 32767 + (r() - r());
      out.setInt16(o, Math.max(-32768, Math.min(32767, Math.round(v))), true);
      o += 2;
    }
    return new Blob([out.buffer], { type: 'audio/wav' });
  };

  // AAC-LC a 192 kb/s. Devuelve las tramas, la configuración (ASC) y el retardo del
  // codificador en muestras, medido descodificando y comparando con el original.
  D.encodeAAC = async (buf) => {
    const chunks = [];
    let asc = null;
    const enc = new AudioEncoder({
      output: (c, meta) => {
        if (meta && meta.decoderConfig && meta.decoderConfig.description && !asc) asc = new Uint8Array(meta.decoderConfig.description);
        const b = new Uint8Array(c.byteLength); c.copyTo(b);
        chunks.push({ data: b, ts: c.timestamp, dur: c.duration });
      },
      error: (e) => { throw e; },
    });
    enc.configure({ codec: 'mp4a.40.2', sampleRate: SR, numberOfChannels: 2, bitrate: 192000 });
    const L = buf.getChannelData(0), R = buf.getChannelData(1);
    const PASO = 4800;
    for (let i = 0; i < buf.length; i += PASO) {
      const n = Math.min(PASO, buf.length - i);
      const plano = new Float32Array(n * 2);
      plano.set(L.subarray(i, i + n), 0);
      plano.set(R.subarray(i, i + n), n);
      const ad = new AudioData({ format: 'f32-planar', sampleRate: SR, numberOfFrames: n, numberOfChannels: 2, timestamp: Math.round((i * 1e6) / SR), data: plano });
      enc.encode(ad);
      ad.close();
    }
    await enc.flush();
    enc.close();
    if (!asc) throw new Error('El codificador AAC no dio su configuración');

    // Retardo: se descodifica y se busca el desfase que mejor encaja con el original.
    const pcm = [];
    const dec = new AudioDecoder({
      output: (ad) => {
        const n = ad.numberOfFrames;
        const a = new Float32Array(n);
        ad.copyTo(a, { planeIndex: 0, format: 'f32-planar' });
        pcm.push(a);
        ad.close();
      },
      error: (e) => { throw e; },
    });
    dec.configure({ codec: 'mp4a.40.2', sampleRate: SR, numberOfChannels: 2, description: asc });
    for (const c of chunks) dec.decode(new EncodedAudioChunk({ type: 'key', timestamp: c.ts, duration: c.dur, data: c.data }));
    await dec.flush();
    dec.close();
    const total = pcm.reduce((s, a) => s + a.length, 0);
    const d = new Float32Array(total);
    let o = 0; for (const a of pcm) { d.set(a, o); o += a.length; }
    const ini = Math.round(0.05 * SR), largo = Math.round(1.2 * SR);
    let mejor = 0, lag = 0;
    for (let k = 0; k <= 4096; k++) {
      let s = 0;
      for (let i = ini; i < ini + largo; i += 2) s += L[i] * d[i + k];
      if (s > mejor) { mejor = s; lag = k; }
    }
    return { chunks, asc, delay: lag, decodedFrames: total, firstTs: chunks[0].ts, firstDur: chunks[0].dur };
  };
})();
