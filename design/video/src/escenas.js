'use strict';
/* El guion del vídeo: 18 s, una idea por segundo, cada escena función del tiempo.

   La casilla de María es el objeto protagonista (el papel que en el encargo tenía la
   portada del disco): la misma pieza pasa por el resultado del asistente, la portada,
   el sello de marca y el zoom. Su posición sale de una sola función, heroState(t),
   para que cada relevo empiece exactamente donde acabó el anterior. */
(function () {
  const D = window.D, C = D.C, S = D.STAGE, E = D.ease;
  const deg = Math.PI / 180;
  const CY = 500; // centro de las composiciones cuando está la barra del asistente

  // Tiempos clave, en segundos. El audio (src/audio.js) lee los mismos.
  const T = (D.T = {
    check1: [0.2, 0.56],
    idMove: [0.6, 0.98], idWord: [0.9, 1.2], l1: [0.84, 1.24], l2: [0.92, 1.32], s2Out: [1.33, 1.48],
    phoneIn: [1.46, 2.36],
    push: [2.4, 3.2], baseFade: [2.46, 2.86], h4In: [2.82, 3.18], dockIn: [2.44, 2.96],
    tilesOut: [3.2, 3.5], h4Out: [3.19, 3.35], h5In: [3.34, 3.68], rows: [3.38, 3.45, 3.52, 3.59], s5Out: [3.86, 3.98],
    strip: [3.98, 4.96], h6In: [4.04, 4.38], sub6In: [4.12, 4.46], s6Out: [4.7, 4.86],
    fieldIn: [4.9, 5.14], typing: [5.02, 5.48], rowIn: [5.36, 5.64], select: 5.62,
    s7Out: [5.78, 5.96], expand: [5.8, 6.3], t8a: [6.1, 6.44], t8b: [6.18, 6.52],
    t8Out: [6.56, 6.7], slide: [6.6, 6.92], i1: [6.86, 7.2], i2: [6.93, 7.26], i3: [7.0, 7.32], btn: [7.06, 7.32], press: 7.42,
    fill: [7.5, 7.86], contract: [7.54, 7.98], idDark: [7.84, 8.14], check2: [7.96, 8.24], dockOut: [7.5, 7.78],
    idDarkOut: [8.26, 8.38], unfill: [8.3, 8.6], zoom: [8.44, 9.74], dockBack: [8.56, 8.96],
    st1: [9.8, 10.14], st2: [9.88, 10.22], stOut: [11.02, 11.2],
    h13In: [11.22, 11.56], sl3: [11.24, 11.84], spread: [11.88, 12.32], sub13: [12.04, 12.38], s13Out: [12.4, 12.54],
    sleevesOut: [12.44, 12.82], coversIn: [12.5, 13.06], float: [12.5, 14.0],
    converge: [14.0, 14.84], fold: [14.5, 15.04], logo2: [14.66, 15.16], check3: [14.94, 15.22], dockAway: [14.0, 14.36],
    slideLeft: [15.2, 15.78], word: [15.4, 15.94], tag1: [15.96, 16.32], tag2: [16.06, 16.42],
  });
  const P = (t, r, fn = E.brand) => D.ep(t, r[0], r[1], fn);
  const between = (t, a, b) => t >= a && t <= b;

  // ---- Mapa de tiempo: el guion va escrito en 18 s de escena; el vídeo lo pone a
  // media velocidad y para en seco donde hay texto que leer, ya entero en pantalla.
  // Cada pausa es [instante de escena, segundos de vídeo que dura].
  const WARP = {
    scale: 2,
    holds: [
      // Regla de lectura: al menos 1 s más 1 s por cada 15 caracteres, contando lo que
      // ya se lee mientras el texto entra.
      [1.325, 2.0],  // «Una app / para tus mayores»
      [3.185, 1.6],  // «Lo de cada día»
      [3.845, 2.0],  // «Tus contactos» y sus cuatro filas
      [4.55, 3.0],   // «A su medida» y su línea (la tira sigue moviéndose)
      [5.75, 1.2],   // «Llama a María» y su resultado
      [6.54, 1.2],   // «María / Llamada»
      [7.32, 2.2],   // «¿Llamo a María?», sus dos líneas y los botones
      [8.25, 0.6],   // la firma oscura sobre el cian
      [10.6, 1.0],   // «Que el móvil / vuelva a ser suyo»
      [12.39, 2.4],  // «Tú lo configuras una vez», las seis fundas y su línea
    ],
  };
  D.SCENE_DUR = 18;
  // Tiempo de escena -> tiempo de vídeo. En el instante de una pausa vale su principio.
  D.out = (t) => WARP.scale * t + WARP.holds.reduce((s, [h, d]) => s + (t > h ? d : 0), 0);
  // Tiempo de vídeo -> tiempo de escena.
  D.sceneAt = (u) => {
    let t0 = 0, u0 = 0;
    for (const [h, d] of WARP.holds) {
      const uh = u0 + (h - t0) * WARP.scale;
      if (u < uh) return t0 + (u - u0) / WARP.scale;
      if (u < uh + d) return h;
      t0 = h; u0 = uh + d;
    }
    return t0 + (u - u0) / WARP.scale;
  };
  D.DUR = D.out(D.SCENE_DUR);
  D.u = 0;
  // Progreso medido en tiempo de vídeo: para lo que no debe pararse en una pausa
  // (la tira de inicios, las casillas que flotan), que estira su recorrido.
  const PU = (r, fn = E.brand) => fn(D.prog(D.u, D.out(r[0]), D.out(r[1])));

  // ---- 1 y 2: la D llega, se dibuja su visto y se hace firma ---------------------
  const ID = { y: 440, box: 62, word: 40, gap: 16 };
  let idL = null;
  function idLayout() {
    if (idL) return idL;
    const dW = (ID.box * 48) / 64;
    const tw = D.measure('Doado', ID.word, 700).width;
    const left = S.cx - (dW + ID.gap + tw) / 2;
    return (idL = { mx: left + dW / 2, tx: left + dW + ID.gap });
  }
  function sceneOpen(ctx, t) {
    if (t > T.s2Out[1] + 0.02) return;
    const L = idLayout();
    const grow = D.settle(D.prog(t, 0, 0.62));
    const mv = P(t, T.idMove, E.inOut);
    const out = P(t, T.s2Out, E.exit);
    const box = D.lerp(200 * grow, ID.box, mv);
    const x = D.lerp(S.cx, L.mx, mv), y = D.lerp(S.cy, ID.y, mv) - out * 26;
    D.mark(ctx, x, y, box, C.cyan, { check: P(t, T.check1, E.inOut), alpha: 1 - out });
    D.line(ctx, 'Doado', L.tx, ID.y + ID.word * 0.364, { size: ID.word, weight: 700, color: C.text, align: 'left' }, P(t, T.idWord), out);
    D.line(ctx, 'Una app', S.cx, 562, { size: 68, weight: 700, color: C.text }, P(t, T.l1), out);
    D.line(ctx, 'para tus mayores', S.cx, 646, { size: 68, weight: 700, color: C.cyan }, P(t, T.l2), out);
  }

  // ---- 3 a 5: el inicio sube, la cámara se acerca a tres casillas, contactos -------
  const K3 = 0.86, BEZ = 11, SRAD = 40;
  const PH = { w: D.HOME_W * K3, h: D.HOME_H * K3 };
  const PUSH = 2.72;
  let phoneTex = null, tileTex = null, rowY = 0;
  function phoneAssets() {
    if (phoneTex) return;
    const cfg = { theme: 'dark', cols: 3 };
    const medida = document.createElement('canvas').getContext('2d');
    const rects = D.home(medida, cfg);
    phoneTex = D.GL.tex('phone-base', PH.w + BEZ * 2, PH.h + BEZ * 2, 2, (x) => {
      x.translate(BEZ, BEZ);
      D.phoneFrame(x, PH.w, PH.h, BEZ, SRAD);
      x.save();
      x.beginPath(); x.roundRect(0, 0, PH.w, PH.h, SRAD); x.clip();
      x.scale(K3, K3);
      D.home(x, { ...cfg, skip: new Set(['phone', 'messages', 'camera']) });
      x.restore();
    });
    tileTex = ['phone', 'messages', 'camera'].map((id) => {
      const r = rects[id];
      const a = D.APPS.find((q) => q.id === id);
      return {
        lx: -PH.w / 2 + (r.x + r.w / 2) * K3,
        ly: -PH.h / 2 + (r.y + r.h / 2) * K3,
        h: r.h * K3,
        tex: D.GL.tex('tile-' + id, r.w * K3, r.h * K3, 2.9, (x) => {
          x.scale(K3, K3);
          D.tile(x, 0, 0, r.w, r.h, D.TH.dark, { icon: a.icon, label: a.label, iconDp: 48, labelSp: 20 });
        }),
      };
    });
    rowY = tileTex[0].ly;
  }
  function phonePose(t) {
    const u = P(t, T.phoneIn, E.brand);
    const a = P(t, [T.phoneIn[0], T.phoneIn[0] + 0.24], E.linear);
    let s = D.lerp(0.94, 1, u), y = S.cy + D.lerp(430, 0, u);
    const rx = D.lerp(28, 0, u) * deg;
    const pu = P(t, T.push, E.soft);
    if (pu > 0) {
      // La fila de arriba viaja recta hasta CY mientras la escala crece en proporción.
      s = Math.exp(Math.log(PUSH) * pu);
      y = D.lerp(S.cy + rowY, CY, pu) - rowY * s;
    }
    return { x: S.cx, y, s, rx, a };
  }
  function phoneItems(t) {
    if (!between(t, T.phoneIn[0], T.tilesOut[1] + 0.12)) return [];
    phoneAssets();
    const M = D.GL.M;
    const pp = phonePose(t);
    const pm = M.chain(M.t(pp.x, pp.y, 0), M.rx(pp.rx), M.s(pp.s, pp.s));
    const items = [{ tex: phoneTex, m: pm, alpha: pp.a * (1 - P(t, T.baseFade, E.linear)), zs: 10 }];
    tileTex.forEach((tt, i) => {
      const w = P(t, [T.tilesOut[0] + i * 0.03, T.tilesOut[1] + i * 0.03], E.exit);
      const m = M.chain(pm, M.t(tt.lx, tt.ly + (w * 46) / pp.s, 0), M.s(1 - 0.16 * w));
      items.push({ tex: tt.tex, m, alpha: pp.a * (1 - w), zs: 0 });
    });
    return items;
  }
  function sceneTilesText(ctx, t) {
    if (!between(t, T.h4In[0], T.h4Out[1] + 0.02)) return;
    phoneAssets();
    const y = CY - (tileTex[0].h * PUSH) / 2 - 58;
    D.line(ctx, 'Lo de cada día', S.cx, y, { size: 44, weight: 700, color: C.text }, P(t, T.h4In), P(t, T.h4Out, E.exit));
  }
  function sceneList(ctx, t) {
    if (!between(t, T.h5In[0], T.s5Out[1] + 0.02)) return;
    const out = P(t, T.s5Out, E.exit);
    D.line(ctx, 'Tus contactos', S.cx, 300, { size: 44, weight: 700, color: C.text }, P(t, T.h5In), out);
    const w = 660, rh = 92, x = S.cx - w / 2, y0 = 340;
    const card = P(t, [T.rows[0] - 0.06, T.rows[0] + 0.26]);
    const a = card * (1 - out);
    if (a <= 0.002) return;
    ctx.save();
    ctx.globalAlpha *= a;
    ctx.translate(0, (1 - card) * 18 - out * 22);
    ctx.beginPath();
    ctx.roundRect(x + 1, y0 + 1, w - 2, rh * 4 - 2, 24);
    ctx.fillStyle = C.surface; ctx.fill();
    ctx.lineWidth = 2; ctx.strokeStyle = C.line; ctx.stroke();
    D.PEOPLE.forEach((p, i) => {
      const r = P(t, [T.rows[i], T.rows[i] + 0.24]);
      if (i > 0) {
        ctx.save();
        ctx.globalAlpha *= r;
        ctx.fillStyle = C.line;
        ctx.fillRect(x + 24, y0 + i * rh - 1, w - 48, 2);
        ctx.restore();
      }
      ctx.save();
      ctx.translate(0, (1 - r) * 16);
      D.contactRow(ctx, x, y0 + i * rh, w, rh, p, { alpha: r });
      ctx.restore();
    });
    ctx.restore();
  }

  // ---- 6: «A su medida», una tira de inicios distintos -----------------------------
  const STRIP = [
    { key: 'm1', cfg: { theme: 'dark', cols: 2 } },
    { key: 'm2', cfg: { theme: 'light', cols: 2 } },
    { key: 'm3', cfg: { theme: 'dark', cols: 1, letra: 1.3 } },
    { key: 'm4', cfg: { theme: 'dark', cols: 3 } },
    { key: 'm5', cfg: { theme: 'light', cols: 1, letra: 1.15 } },
    { key: 'm6', cfg: { theme: 'dark', cols: 2, marked: 'camera' } },
    { key: 'm7', cfg: { theme: 'light', cols: 3 } },
  ];
  function sceneStrip(ctx, t) {
    if (!between(t, T.strip[0], T.s6Out[1] + 0.08)) return;
    const out = P(t, [T.s6Out[0], T.s6Out[1] + 0.08], E.exit);
    D.line(ctx, 'A su medida', S.cx, 254, { size: 44, weight: 700, color: C.text }, P(t, T.h6In), P(t, T.s6Out, E.exit));
    D.line(ctx, 'Letra, casillas y tema, como mejor lo vea', S.cx, 300, { size: 32, weight: 600, color: C.textMuted }, P(t, T.sub6In), P(t, T.s6Out, E.exit));
    const off = D.lerp(700, -190, PU(T.strip, E.soft)) - out * 280;
    const a = P(t, [T.strip[0], T.strip[0] + 0.16], E.linear) * (1 - out);
    if (a <= 0.002) return;
    ctx.save();
    ctx.globalAlpha *= a;
    STRIP.forEach((m, i) => {
      const mp = D.miniPhone(m.key, m.cfg, 0.56);
      const cx = S.cx + off + (i - 3) * 262;
      if (cx + mp.w / 2 < S.x || cx - mp.w / 2 > S.right) return;
      ctx.drawImage(mp.canvas, cx - mp.w / 2, 566 - mp.h / 2);
    });
    ctx.restore();
  }

  // ---- 7: el asistente oye «Llama a María» -------------------------------------
  const FIELD_Y = 450, ROW = { x: S.cx - 320, y: 524, w: 640, h: 104 };
  const DICHO = 'Llama a María';
  // Lo oído aparece por palabras, como lo transcribe el asistente: no se escribe.
  const PALABRAS = DICHO.split(' ');
  D.typedAt = (t) => PALABRAS.slice(0, Math.floor(D.prog(t, T.typing[0], T.typing[1]) * PALABRAS.length + 1e-6)).join(' ').length;
  function sceneSearch(ctx, t) {
    if (!between(t, T.fieldIn[0], T.s7Out[1] + 0.02)) return;
    const out = P(t, T.s7Out, E.exit);
    const fin = P(t, T.fieldIn);
    D.line(ctx, 'Te escucho', S.cx, 368, { size: 32, weight: 600, color: C.textMuted }, P(t, [T.fieldIn[0] + 0.02, T.fieldIn[1] + 0.06]), out);
    ctx.save();
    ctx.translate(S.cx, FIELD_Y + (1 - fin) * 14 - out * 18);
    const sc = D.lerp(0.97, 1, fin);
    ctx.scale(sc, sc);
    ctx.translate(-S.cx, -FIELD_Y);
    D.field(ctx, S.cx, FIELD_Y, 640, 88, DICHO.slice(0, D.typedAt(t)), { alpha: fin * (1 - out), active: true });
    ctx.restore();
    const rin = P(t, T.rowIn);
    const sel = P(t, [T.select, T.select + 0.1], E.inOut);
    D.resultRow(ctx, ROW.x, ROW.y + (1 - rin) * 16, ROW.w, ROW.h, { alpha: rin * (1 - out), sel, thumb: false });
  }

  // ---- 8 a 11: la casilla protagonista -----------------------------------------
  const BTN = { x: 900 + 130, y: 610, w: 260, h: 80 };
  const HERO_ZOOM = 18;
  // Centro del sello de marca y del zoom: cerca del centro óptico de lo que queda
  // por encima de la barra, para que la casilla crezca hacia todos los lados a la vez.
  const SELLO_Y = 470;
  function heroState(t) {
    if (t < T.rowIn[0]) return null;
    const rin = P(t, T.rowIn);
    const tx = ROW.x + 20 + 36, ty = ROW.y + ROW.h / 2;
    if (t < T.expand[0]) return { x: tx, y: ty + (1 - rin) * 16, s: 72, a: rin, zoom: 1 };
    const e = P(t, T.expand, E.inOut);
    let x = D.lerp(tx, S.cx, e), y = D.lerp(ty, 430, e), s = D.lerp(72, 360, e);
    const sl = P(t, T.slide, E.inOut);
    x = D.lerp(x, 660, sl); y = D.lerp(y, 470, sl);
    const ct = P(t, T.contract, E.inOut);
    x = D.lerp(x, S.cx, ct); y = D.lerp(y, SELLO_Y, ct); s = D.lerp(s, 210, ct);
    const z = P(t, T.zoom, E.inOut);
    const zoom = Math.exp(Math.log(HERO_ZOOM) * z);
    // La inicial se disuelve en la segunda mitad del zoom: el plato, del color del
    // escenario, acaba ocupándolo todo y deja el escenario oscuro otra vez.
    return { x, y, s: s * zoom, a: 1, zoom, letter: 1 - D.ep(z, 0.6, 0.9, E.inOut) };
  }
  let capaSuma = null, capaMuestra = null;
  function drawHero(ctx, t) {
    if (t > T.zoom[1] + 0.001) return;
    const h = heroState(t);
    if (!h) return;
    // D.slice viene en tiempo de vídeo; aquí se trabaja en tiempo de escena.
    const slice = (D.slice || 0) / WARP.scale;
    const rapido = slice > 0 && between(t, T.zoom[0] + 0.05, T.zoom[1] - 0.05);
    if (!rapido) { heroDraw(ctx, h); return; }
    // Desenfoque de movimiento controlado: seis muestras dentro de cada subfotograma.
    // Cada muestra se pinta normal en su lienzo y luego se suma al acumulado con
    // «lighter» y peso 1/6: así se promedia bien también la transparencia.
    const nuevo = () => { const c = document.createElement('canvas'); c.width = D.W; c.height = D.H; return c; };
    if (!capaSuma) { capaSuma = nuevo(); capaMuestra = nuevo(); }
    const acc = capaSuma.getContext('2d'), m = capaMuestra.getContext('2d');
    acc.setTransform(1, 0, 0, 1, 0, 0);
    acc.globalCompositeOperation = 'source-over';
    acc.globalAlpha = 1;
    acc.clearRect(S.x, S.y, S.w, S.h);
    const n = 6;
    for (let j = 0; j < n; j++) {
      const hj = heroState(t + (j / (n - 1) - 0.5) * slice);
      m.setTransform(1, 0, 0, 1, 0, 0);
      m.globalCompositeOperation = 'source-over';
      m.globalAlpha = 1;
      m.clearRect(S.x, S.y, S.w, S.h);
      m.save();
      m.beginPath(); m.rect(S.x, S.y, S.w, S.h); m.clip();
      heroDraw(m, hj);
      m.restore();
      acc.globalCompositeOperation = 'lighter';
      acc.globalAlpha = 1 / n;
      acc.drawImage(capaMuestra, S.x, S.y, S.w, S.h, S.x, S.y, S.w, S.h);
    }
    acc.globalCompositeOperation = 'source-over';
    acc.globalAlpha = 1;
    ctx.drawImage(capaSuma, S.x, S.y, S.w, S.h, S.x, S.y, S.w, S.h);
  }
  function heroDraw(ctx, h) {
    if (h.zoom <= 1.0001) { D.hero(ctx, h.x, h.y, h.s, { alpha: h.a }); return; }
    // En el zoom solo se pinta lo que cae dentro del escenario: el plato y la inicial.
    D.hero(ctx, h.x, h.y, h.s, { alpha: h.a, letterAlpha: h.letter });
  }

  function sceneDetail(ctx, t) {
    if (between(t, T.t8a[0], T.t8Out[1] + 0.02)) {
      const out = P(t, T.t8Out, E.exit);
      D.line(ctx, 'María', S.cx, 690, { size: 48, weight: 700, color: C.text }, P(t, T.t8a), out);
      D.line(ctx, 'Llamada', S.cx, 740, { size: 34, weight: 600, color: C.cyan }, P(t, T.t8b), out);
    }
    // A las 7,6 el cian ya lo tapa entero: a partir de ahí no se pinta.
    if (between(t, T.i1[0], 7.6)) {
      const X = 900;
      D.line(ctx, '¿Llamo a María?', X, 418, { size: 56, weight: 700, color: C.text, align: 'left' }, P(t, T.i1));
      D.line(ctx, 'Siempre pregunta antes', X, 476, { size: 34, weight: 600, color: C.cyan, align: 'left' }, P(t, T.i2));
      D.line(ctx, 'Solo llama tras un «Sí»', X, 526, { size: 32, weight: 600, color: C.textMuted, align: 'left' }, P(t, T.i3));
      const b = P(t, T.btn);
      const press = Math.max(0, 1 - Math.abs(t - T.press) / 0.09);
      D.button(ctx, BTN.x, BTN.y, BTN.w, BTN.h, 'Sí, llama', { alpha: b, s: D.lerp(0.92, 1, b) * (1 - 0.04 * press), pressed: press });
      // Como en la app: junto al «Sí» siempre está el «No».
      D.outlineButton(ctx, BTN.x + BTN.w / 2 + 20 + 80, BTN.y, 160, BTN.h, 'No', { alpha: b, s: D.lerp(0.92, 1, b) });
    }
  }

  // ---- 10 y 11: el escenario se llena de cian, la marca, y vuelta a lo oscuro -----
  function sceneBrand(ctx, t) {
    if (!between(t, T.fill[0], T.unfill[1] + 0.01)) return;
    const r = P(t, T.fill, E.brand) * 1080;
    ctx.beginPath(); ctx.arc(BTN.x, BTN.y, r, 0, Math.PI * 2);
    ctx.fillStyle = C.cyan; ctx.fill();
    const h = heroState(t);
    const r2 = P(t, T.unfill, E.brand) * 1100;
    if (r2 > 0.5) {
      ctx.beginPath(); ctx.arc(h.x, h.y, r2, 0, Math.PI * 2);
      ctx.fillStyle = C.ink; ctx.fill();
    }
    // Firma oscura bajo la casilla: la D en tinta, con su visto dibujándose (el «hecho»).
    if (between(t, T.idDark[0], T.idDarkOut[1])) {
      const out = P(t, T.idDarkOut, E.exit);
      const box = 66, dW = (box * 48) / 64, gap = 16;
      const tw = D.measure('Doado', 42, 700).width;
      const left = S.cx - (dW + gap + tw) / 2;
      const y = SELLO_Y + 105 + 72;
      const g = D.settle(P(t, [T.idDark[0], T.idDark[0] + 0.3], E.linear));
      D.mark(ctx, left + dW / 2, y, box * g, C.ink, { check: P(t, T.check2, E.inOut), alpha: 1 - out });
      D.line(ctx, 'Doado', left + dW + gap, y + 42 * 0.364, { size: 42, weight: 700, color: C.ink, align: 'left' }, P(t, [T.idDark[0] + 0.06, T.idDark[1] + 0.06]), out);
    }
  }

  // ---- 12: la frase -----------------------------------------------------------
  function sceneStatement(ctx, t) {
    if (!between(t, T.st1[0], T.stOut[1] + 0.02)) return;
    const out = P(t, T.stOut, E.exit);
    D.line(ctx, 'Que el móvil', S.cx, 486, { size: 76, weight: 700, color: C.text }, P(t, T.st1), out);
    D.line(ctx, 'vuelva a ser suyo', S.cx, 576, { size: 76, weight: 700, color: C.cyan }, P(t, T.st2), out);
  }

  // ---- 13 a 15: fundas de ajustes, cuatro casillas flotando y la D -----------------
  const BIG = [
    { i: 0, x: -270, slot: -575, ry: -16, rz: 3 },
    { i: 1, x: 0, slot: -115, ry: 0, rz: -2 },
    { i: 2, x: 270, slot: 345, ry: 16, rz: -3 },
  ];
  // Abiertas en seis se quedan en 200 px: el título de cada funda sigue leyéndose.
  const SMALL = [{ i: 3, slot: -345 }, { i: 4, slot: 115 }, { i: 5, slot: 575 }];
  function sleeveTex(i) {
    const sp = D.SLEEVES[i];
    return D.GL.tex('sleeve-' + sp.id, 230, 230, 2, (x) => D.sleeve(x, 230, sp));
  }
  function sleeveItems(t) {
    if (!between(t, T.sl3[0], T.sleevesOut[1] + 0.02)) return [];
    const items = [];
    const sp = P(t, T.spread, E.inOut);
    const out = P(t, T.sleevesOut, E.exit);
    const sc = D.lerp(1, 200 / 230, sp);
    BIG.forEach((b, j) => {
      const e = P(t, [T.sl3[0] + j * 0.06, T.sl3[1] + j * 0.06 - 0.12], E.brand);
      const resto = 1 - e;
      items.push({
        tex: sleeveTex(b.i),
        x: S.cx + D.lerp(b.x, b.slot, sp) * (1 + out * 0.3), y: CY + resto * 190, z: out * 520,
        rx: resto * 24 * deg,
        ry: D.lerp(b.ry * (0.3 + 0.7 * resto), 0, sp) * deg,
        rz: D.lerp(b.rz * (0.35 + 0.65 * resto), 0, sp) * deg,
        s: sc, alpha: D.clamp(e * 1.6) * (1 - out),
      });
    });
    SMALL.forEach((b, j) => {
      const e = P(t, [T.spread[0] + 0.06 + j * 0.04, T.spread[1] + j * 0.04], E.brand);
      if (e <= 0) return;
      items.push({
        tex: sleeveTex(b.i),
        x: S.cx + b.slot * (1 + out * 0.3), y: CY, z: (1 - e) * 320 + out * 520,
        s: sc * D.lerp(0.62, 1, e), alpha: e * (1 - out),
      });
    });
    return items;
  }
  function sceneSleevesText(ctx, t) {
    if (!between(t, T.h13In[0], T.s13Out[1] + 0.02)) return;
    const out = P(t, T.s13Out, E.exit);
    D.line(ctx, 'Tú lo configuras una vez', S.cx, 290, { size: 44, weight: 700, color: C.text }, P(t, T.h13In), out);
    D.line(ctx, 'Ellos solo tienen que usarlo', S.cx, 672, { size: 32, weight: 600, color: C.textMuted }, P(t, T.sub13), out);
  }

  const COVERS = [
    { id: 'hero', x: -150, y: -14, z: -40, ry: 10, rz: -3, s: 1.0, q: [-1, -1] },
    { id: 'phone', x: 205, y: -98, z: 150, ry: -16, rz: 4, s: 0.8, q: [1, -1] },
    { id: 'camera', x: -345, y: 112, z: 240, ry: 18, rz: -5, s: 0.76, q: [-1, 1] },
    { id: 'messages', x: 350, y: 128, z: 310, ry: -12, rz: 3, s: 0.72, q: [1, 1] },
  ];
  function coverTex(id) {
    if (id === 'hero') return D.GL.tex('cover-hero', 290, 290, 1.7, (x) => D.hero(x, 145, 145, 290));
    const a = D.APPS.find((q) => q.id === id);
    return D.GL.tex('cover-' + id, 260, 260, 1.7, (x) => {
      const k = 260 / 168;
      x.scale(k, k);
      D.tile(x, 0, 0, 168, 168, D.TH.dark, { icon: a.icon, label: a.label });
    });
  }
  function coverItems(t) {
    if (!between(t, T.coversIn[0], T.fold[1] + 0.02)) return [];
    const M = D.GL.M;
    const fl = PU(T.float, E.inOut);
    const cv = P(t, T.converge, E.inOut);
    const fd = P(t, T.fold, E.inOut);
    const phi = D.lerp(-7, 7, fl) * (1 - cv) * deg;
    const bob = Math.sin(((D.u - D.out(12.5)) / WARP.scale) * 2.2) * 6 * (1 - cv);
    const parent = M.chain(M.t(S.cx, CY + bob, 0), M.ry(phi));
    return COVERS.map((c, j) => {
      const e = P(t, [T.coversIn[0] + j * 0.05, T.coversIn[1] + j * 0.05], E.brand);
      const tex = coverTex(c.id);
      const half = tex.w / 2;
      // Al converger, cada casilla va a un cuadrante pegado al centro y se queda de frente.
      const sc = D.lerp(c.s, 0.5, cv);
      const tx = D.lerp(c.x, c.q[0] * (half * 0.5 + 8), cv);
      const ty = D.lerp(c.y, c.q[1] * (half * 0.5 + 8), cv);
      const tz = D.lerp(c.z, 0, cv) + (1 - e) * 900;
      // Pliegue hacia atrás sobre el borde que da al centro: se esconden detrás de la D.
      const pivot = -c.q[0] * half;
      const local = M.chain(
        M.t(tx - c.q[0] * half * sc, ty, tz),
        M.ry((D.lerp(c.ry, 0, cv) * deg) + c.q[0] * fd * 95 * deg),
        M.rz(D.lerp(c.rz, 0, cv) * deg),
        M.s(sc, sc),
        M.t(-pivot, 0, 0),
      );
      return { tex, m: M.mul(parent, local), alpha: e * (1 - D.ep(fd, 0.25, 0.6, E.linear)), dim: D.lerp(1, 0.86, D.clamp(c.z / 310)) * (1 - cv) + cv, zs: tz + j * 0.01 };
    });
  }

  // ---- 15 y 16: la D en el centro, firma grande y la frase final ------------------
  const LOCK = { box: 176, size: 150, gap: 52, y: 470 };
  let lockL = null;
  function lockLayout() {
    if (lockL) return lockL;
    const dW = (LOCK.box * 48) / 64;
    const tw = D.measure('Doado', LOCK.size, 700).width;
    const left = S.cx - (dW + LOCK.gap + tw) / 2;
    return (lockL = { mx: left + dW / 2, tx: left + dW + LOCK.gap, dW });
  }
  function sceneLogo(ctx, t) {
    if (t < T.logo2[0]) return;
    const L = lockLayout();
    const g = D.settle(P(t, T.logo2, E.linear));
    const mv = P(t, T.slideLeft, E.inOut);
    const box = D.lerp(190 * g, LOCK.box, mv);
    const x = D.lerp(S.cx, L.mx, mv), y = D.lerp(CY, LOCK.y, mv);
    D.mark(ctx, x, y, box, C.cyan, { check: P(t, T.check3, E.inOut) });
    // La palabra va atada a donde esté la D en cada instante, siempre a la misma
    // distancia, y entra subiendo como los demás títulos: nunca se acercan.
    const borde = x + (box * 48) / 64 / 2 + LOCK.gap * (box / LOCK.box);
    D.line(ctx, 'Doado', borde, y + LOCK.size * 0.364, { size: LOCK.size, weight: 700, color: C.text, align: 'left' }, P(t, T.word));
    D.line(ctx, 'Una app para tus mayores.', S.cx, 642, { size: 46, weight: 600, color: C.text }, P(t, T.tag1));
    D.line(ctx, 'Fácil, cada día.', S.cx, 702, { size: 46, weight: 600, color: C.cyan }, P(t, T.tag2));
  }

  // ---- La barra del asistente ---------------------------------------------------
  function dockState(t) {
    let a, dy;
    if (t < T.dockOut[0]) { const u = P(t, T.dockIn); a = u; dy = (1 - u) * 56; }
    else if (t < T.dockBack[0]) { const u = P(t, T.dockOut, E.exit); a = 1 - u; dy = u * 56; }
    else { const u = P(t, T.dockBack), w = P(t, T.dockAway, E.exit); a = u * (1 - w); dy = (1 - u) * 56 + w * 30; }
    const listen = D.ep(t, 4.94, 5.12, E.inOut) * (1 - D.ep(t, 5.6, 5.84, E.inOut));
    return { alpha: a, dy, listen };
  }

  // ---- El fotograma entero ------------------------------------------------------
  D.drawFrame = (ctx, t) => {
    D.lint.items = [];
    ctx.save();
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.globalAlpha = 1;
    D.drawBackground(ctx);
    ctx.save();
    D.stagePath(ctx);
    ctx.clip();
    ctx.fillStyle = C.ink;
    ctx.fillRect(S.x, S.y, S.w, S.h);

    D.lint.group = 'apertura'; sceneOpen(ctx, t);
    const a = phoneItems(t);
    if (a.length) ctx.drawImage(D.GL.render(a, { eye: [S.cx, S.cy] }), 0, 0);
    D.lint.group = 'casillas'; sceneTilesText(ctx, t);
    D.lint.group = 'contactos'; sceneList(ctx, t);
    D.lint.group = 'tira'; sceneStrip(ctx, t);
    D.lint.group = 'asistente'; sceneSearch(ctx, t);
    D.lint.group = 'detalle'; sceneDetail(ctx, t);
    D.lint.group = 'marca'; sceneBrand(ctx, t);
    D.lint.group = 'heroe'; drawHero(ctx, t);
    D.lint.group = 'frase'; sceneStatement(ctx, t);
    const b = sleeveItems(t).concat(coverItems(t));
    if (b.length) ctx.drawImage(D.GL.render(b, { eye: [S.cx, CY] }), 0, 0);
    D.lint.group = 'fundas'; sceneSleevesText(ctx, t);
    D.lint.group = 'final'; sceneLogo(ctx, t);
    D.lint.group = 'barra';
    const ds = dockState(t);
    D.dock(ctx, D.u / WARP.scale, ds);

    ctx.restore();
    D.drawStageEdge(ctx);
    ctx.restore();
  };

  // ---- API: window.seek(t) -------------------------------------------------------
  D.ready = (async () => { await D.fontsReady; })();
  D.canvas = null;
  // t es tiempo de vídeo (0 a D.DUR); la escena se pinta en su propio tiempo.
  window.seek = async (t) => {
    await D.ready;
    if (!D.canvas) throw new Error('Falta D.canvas');
    D.u = D.clamp(t, 0, D.DUR - 1e-6);
    D.drawFrame(D.canvas.getContext('2d'), D.sceneAt(D.u));
  };
})();
