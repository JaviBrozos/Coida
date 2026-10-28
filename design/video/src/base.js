'use strict';
/* Base del vídeo de Doado: medidas, colores de marca, curvas, letra, iconos y la D.

   Regla de todo el proyecto: lo que se ve en el instante t sale solo de t. Nada
   acumula estado entre fotogramas, así que cualquier fotograma se puede pintar en
   cualquier orden y sale igual. Las cachés solo guardan dibujos que no cambian. */
(function () {
  const D = (window.D = window.D || {});

  D.W = 1920;
  D.H = 1080;
  D.FPS = 60;
  D.DUR = 18;

  // Escenario: 80 % del ancho y 75 % del alto, centrado y un poco por debajo del centro.
  const S = (D.STAGE = { x: 192, y: 150, w: 1536, h: 816, r: 48 });
  S.cx = S.x + S.w / 2;
  S.cy = S.y + S.h / 2;
  S.right = S.x + S.w;
  S.bottom = S.y + S.h;

  // Colores: app/src/main/java/com/doado/core/ui/Color.kt. No hay otros.
  D.C = {
    navy: '#0B1D2E', cyan: '#29B6F0', cyanDeep: '#10618F', cyanLight: '#6CCFFA',
    ink: '#0A1622', surface: '#122A3E', surfaceHigh: '#17384F', line: '#3D7AA6',
    text: '#F2F7FB', textMuted: '#9FC4DC', mist: '#E9F3FA', mistDim: '#DCEBF5',
    steelDeep: '#54697A', steelLight: '#A3B8C6', white: '#FFFFFF',
  };

  // ---- Matemáticas ----------------------------------------------------------
  D.clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
  D.lerp = (a, b, u) => a + (b - a) * u;
  D.prog = (t, a, b) => D.clamp((t - a) / (b - a));
  const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
  D.rgba = (h, a) => { const [r, g, b] = hex(h); return `rgba(${r},${g},${b},${a})`; };
  D.mix = (h1, h2, u) => {
    const a = hex(h1), b = hex(h2);
    return `rgb(${Math.round(D.lerp(a[0], b[0], u))},${Math.round(D.lerp(a[1], b[1], u))},${Math.round(D.lerp(a[2], b[2], u))})`;
  };

  // Pseudoaleatorio con semilla: el ruido del audio y cualquier variación salen iguales siempre.
  D.rng = (seed) => () => {
    seed |= 0; seed = (seed + 0x6d2b79f5) | 0;
    let r = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    r = (r + Math.imul(r ^ (r >>> 7), 61 | r)) ^ r;
    return ((r ^ (r >>> 14)) >>> 0) / 4294967296;
  };

  // ---- Curvas ---------------------------------------------------------------
  D.bezier = (x1, y1, x2, y2) => {
    const cx = 3 * x1, bx = 3 * (x2 - x1) - cx, ax = 1 - cx - bx;
    const cy = 3 * y1, by = 3 * (y2 - y1) - cy, ay = 1 - cy - by;
    const sx = (u) => ((ax * u + bx) * u + cx) * u;
    const sy = (u) => ((ay * u + by) * u + cy) * u;
    const dx = (u) => (3 * ax * u + 2 * bx) * u + cx;
    return (x) => {
      if (x <= 0) return 0;
      if (x >= 1) return 1;
      let u = x;
      for (let i = 0; i < 8; i++) {
        const e = sx(u) - x;
        if (Math.abs(e) < 1e-7) return sy(u);
        const d = dx(u);
        if (Math.abs(d) < 1e-6) break;
        u -= e / d;
      }
      let lo = 0, hi = 1;
      u = x;
      for (let i = 0; i < 50; i++) {
        const v = sx(u);
        if (Math.abs(v - x) < 1e-7) break;
        if (v < x) lo = u; else hi = u;
        u = (lo + hi) / 2;
      }
      return sy(u);
    };
  };
  D.ease = {
    brand: D.bezier(0.2, 0, 0, 1),      // entradas: la curva de la marca
    inOut: D.bezier(0.65, 0, 0.35, 1),  // desplazamientos de un sitio a otro
    soft: D.bezier(0.45, 0, 0.2, 1),    // movimientos de cámara
    exit: D.bezier(0.4, 0, 1, 1),       // salidas: aceleran y se van
    linear: (x) => x,
  };
  // Muelle con amortiguación crítica: llega sin pasarse. Normalizado para valer 1 en u = 1.
  D.critical = (u, w = 7) => {
    const f = (x) => 1 - (1 + w * x) * Math.exp(-w * x);
    return u <= 0 ? 0 : u >= 1 ? 1 : f(u) / f(1);
  };
  // Muelle con un rebote como mucho del 3 % (la marca permite un 4 % solo en vídeo).
  D.settle = (u, z = 0.75) => {
    if (u <= 0) return 0;
    if (u >= 1) return 1;
    const w = 6.4 / z, wd = w * Math.sqrt(1 - z * z);
    const x = (s) => 1 - Math.exp(-z * w * s) * (Math.cos(wd * s) + ((z * w) / wd) * Math.sin(wd * s));
    return x(u) / x(1);
  };
  // Progreso con curva entre a y b.
  D.ep = (t, a, b, fn = D.ease.brand) => fn(D.prog(t, a, b));

  // ---- Letra: Inter de la app con ss04 horneado (tools/hornear_ss04.py) --------
  D.fontsReady = (async () => {
    const caras = [['600', window.FUENTES.semibold], ['700', window.FUENTES.bold]];
    for (const [peso, b64] of caras) {
      const bin = atob(b64);
      const buf = new Uint8Array(bin.length);
      for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i);
      const f = new FontFace('DoadoInter', buf.buffer, { weight: peso, style: 'normal' });
      await f.load();
      document.fonts.add(f);
    }
  })();
  D.font = (w, px) => `${w} ${px}px DoadoInter, Roboto, system-ui, sans-serif`;

  // ---- Registro de textos: para comprobar que ningún título pisa a otro ---------
  D.lint = { on: false, items: [] };
  function registrar(ctx, str, bx0, by0, bx1, by1, clip) {
    // Solo cuenta lo que se pinta en el fotograma, no lo que va dentro de una textura.
    if (!D.lint.on || ctx.globalAlpha < 0.04 || ctx.canvas !== D.canvas) return;
    const m = ctx.getTransform();
    const pts = [[bx0, by0], [bx1, by0], [bx0, by1], [bx1, by1]].map(([x, y]) => [m.a * x + m.c * y + m.e, m.b * x + m.d * y + m.f]);
    let x0 = Math.min(...pts.map((p) => p[0])), x1 = Math.max(...pts.map((p) => p[0]));
    let y0 = Math.min(...pts.map((p) => p[1])), y1 = Math.max(...pts.map((p) => p[1]));
    if (clip) { x0 = Math.max(x0, clip[0]); y0 = Math.max(y0, clip[1]); x1 = Math.min(x1, clip[2]); y1 = Math.min(y1, clip[3]); }
    if (x1 - x0 < 2 || y1 - y0 < 2) return;
    D.lint.items.push({ str, x0, y0, x1, y1, a: ctx.globalAlpha, group: D.lint.group || '' });
  }

  // Texto en una línea. o: size, weight, color, align, alpha, clip (rectángulo visible, para el registro)
  D.text = (ctx, str, x, y, o) => {
    ctx.save();
    ctx.font = D.font(o.weight || 600, o.size);
    ctx.textAlign = o.align || 'center';
    ctx.textBaseline = 'alphabetic';
    ctx.fillStyle = o.color;
    ctx.globalAlpha *= o.alpha == null ? 1 : o.alpha;
    if (ctx.globalAlpha > 0.002) {
      ctx.fillText(str, x, y);
      if (D.lint.on) {
        const m = ctx.measureText(str);
        registrar(ctx, str, x - m.actualBoundingBoxLeft, y - m.actualBoundingBoxAscent,
          x + m.actualBoundingBoxRight, y + m.actualBoundingBoxDescent, o.clip);
      }
    }
    ctx.restore();
  };
  D.measure = (str, size, weight = 600) => {
    const c = D._mctx || (D._mctx = document.createElement('canvas').getContext('2d'));
    c.font = D.font(weight, size);
    return c.measureText(str);
  };

  // Línea que entra subiendo desde debajo de su propia caja y sale hacia arriba,
  // siempre recortada: así un título se va antes de que el siguiente ocupe su sitio.
  D.line = (ctx, str, x, y, o, pIn, pOut = 0) => {
    if (pIn <= 0 || pOut >= 1) return;
    const size = o.size;
    const asc = size * 1.0, desc = size * 0.32, pad = size * 0.12;
    const m = D.measure(str, size, o.weight || 700);
    const w = m.width;
    const align = o.align || 'center';
    const x0 = align === 'center' ? x - w / 2 : align === 'left' || align === 'start' ? x : x - w;
    const box = [x0 - pad * 2, y - asc - pad, x0 + w + pad * 2, y + desc + pad];
    const h = box[3] - box[1];
    ctx.save();
    ctx.beginPath();
    ctx.rect(box[0], box[1], box[2] - box[0], h);
    ctx.clip();
    const dy = (1 - pIn) * h * 0.92 - pOut * h * 0.92;
    const m2 = ctx.getTransform();
    const clipScreen = [box[0] * m2.a + m2.e, box[1] * m2.d + m2.f, box[2] * m2.a + m2.e, box[3] * m2.d + m2.f];
    D.text(ctx, str, x, y + dy, { ...o, alpha: (o.alpha == null ? 1 : o.alpha) * D.lerp(0.35, 1, pIn) * (1 - pOut * 0.6), clip: clipScreen });
    ctx.restore();
  };

  // ---- Iconos de la app (src/iconos.js) ---------------------------------------
  const caminos = {};
  D.icon = (ctx, nombre, cx, cy, size, color, o = {}) => {
    const ic = window.ICONOS[nombre];
    if (!ic) throw new Error('icono desconocido: ' + nombre);
    const s = size / ic.vb;
    ctx.save();
    ctx.translate(cx - size / 2, cy - size / 2);
    ctx.scale(s, s);
    ctx.globalAlpha *= o.alpha == null ? 1 : o.alpha;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ic.paths.forEach((p, i) => {
      const k = nombre + i;
      const p2 = caminos[k] || (caminos[k] = new Path2D(p.d));
      if (p.fill) { ctx.fillStyle = color; ctx.fill(p2, p.evenOdd ? 'evenodd' : 'nonzero'); }
      if (p.stroke) { ctx.strokeStyle = color; ctx.lineWidth = (p.width || 1.7) * (o.weight || 1); ctx.stroke(p2); }
    });
    ctx.restore();
  };

  // ---- La D con su visto (D-36), según assets/tokens.json ----------------------
  // El visto es un hueco: se recorta con destination-out, así deja ver lo que haya detrás.
  const D_FORMA = new Path2D('M14,6 H30 A26,26 0 0 1 30,58 H14 A6,6 0 0 1 8,52 V12 A6,6 0 0 1 14,6 Z');
  const VISTO = [[18.8, 32.2], [25, 38.8], [39.6, 24.6]];
  const VISTO_LARGO = Math.hypot(6.2, 6.6) + Math.hypot(14.6, 14.2);
  const lienzoD = document.createElement('canvas');
  // box: lado del cuadrado de rejilla 64; la D mide 48 x 52 dentro. o.check: cuánto visto hay dibujado.
  D.mark = (ctx, cx, cy, box, color, o = {}) => {
    const check = o.check == null ? 1 : o.check;
    const alpha = o.alpha == null ? 1 : o.alpha;
    if (box < 0.5 || alpha <= 0.002) return;
    const n = Math.ceil(box) + 6;
    if (lienzoD.width < n || lienzoD.height < n) { lienzoD.width = Math.max(n, lienzoD.width); lienzoD.height = Math.max(n, lienzoD.height); }
    const c = lienzoD.getContext('2d');
    c.setTransform(1, 0, 0, 1, 0, 0);
    c.clearRect(0, 0, n + 2, n + 2);
    // La parte fraccionaria de la posición va dentro del dibujo: así se pinta nítida en cualquier sitio.
    const left = cx - box / 2, top = cy - box / 2;
    const ix = Math.floor(left) - 3, iy = Math.floor(top) - 3;
    c.translate(left - ix, top - iy);
    c.scale(box / 64, box / 64);
    c.fillStyle = color;
    c.fill(D_FORMA);
    if (check > 0.001) {
      c.globalCompositeOperation = 'destination-out';
      c.lineWidth = 5;
      c.lineCap = 'round';
      c.lineJoin = 'round';
      c.setLineDash([VISTO_LARGO * check, VISTO_LARGO * 2]);
      c.beginPath();
      c.moveTo(...VISTO[0]); c.lineTo(...VISTO[1]); c.lineTo(...VISTO[2]);
      c.stroke();
      c.globalCompositeOperation = 'source-over';
    }
    ctx.save();
    ctx.globalAlpha *= alpha;
    ctx.drawImage(lienzoD, 0, 0, n, n, ix, iy, n, n);
    ctx.restore();
  };

  // ---- Fondo atmosférico y escenario -------------------------------------------
  let fondo = null;
  function pintarFondo() {
    const c = document.createElement('canvas');
    c.width = D.W; c.height = D.H;
    const x = c.getContext('2d');
    let g = x.createLinearGradient(0, 0, 0, D.H);
    g.addColorStop(0, D.C.navy);
    g.addColorStop(1, D.C.ink);
    x.fillStyle = g; x.fillRect(0, 0, D.W, D.H);
    // Luz de cian arriba a la izquierda, que se hunde en cian profundo y marino.
    g = x.createRadialGradient(170, -90, 0, 170, -90, 1350);
    g.addColorStop(0, D.rgba(D.C.cyanLight, 0.95));
    g.addColorStop(0.16, D.rgba(D.C.cyan, 0.85));
    g.addColorStop(0.42, D.rgba(D.C.cyanDeep, 0.6));
    g.addColorStop(0.78, D.rgba(D.C.navy, 0));
    x.fillStyle = g; x.fillRect(0, 0, D.W, D.H);
    // Más hondo hacia los lados.
    g = x.createRadialGradient(1980, 260, 0, 1980, 260, 980);
    g.addColorStop(0, D.rgba(D.C.cyanDeep, 0.5));
    g.addColorStop(1, D.rgba(D.C.cyanDeep, 0));
    x.fillStyle = g; x.fillRect(0, 0, D.W, D.H);
    // Casi negro abajo.
    g = x.createLinearGradient(0, 520, 0, D.H);
    g.addColorStop(0, D.rgba(D.C.ink, 0));
    g.addColorStop(1, D.rgba(D.C.ink, 0.97));
    x.fillStyle = g; x.fillRect(0, 0, D.W, D.H);
    // El escenario se separa por borde (D-04), no por sombra: ver drawStageEdge.
    x.beginPath(); x.roundRect(S.x, S.y, S.w, S.h, S.r);
    x.fillStyle = D.C.ink; x.fill();
    // Tramado fijo de un nivel solo en los degradados de fuera: sin él salen bandas al
    // codificar. Dentro del escenario, que es liso, el tramado solo sería ruido.
    const img = x.getImageData(0, 0, D.W, D.H), p = img.data, r = D.rng(1080);
    for (let y = 0; y < D.H; y++) for (let xx = 0; xx < D.W; xx++) {
      const dentro = xx > S.x + 4 && xx < S.right - 4 && y > S.y + 4 && y < S.bottom - 4;
      const n = r() - r();
      if (dentro) continue;
      const i = (y * D.W + xx) * 4;
      p[i] += n; p[i + 1] += n; p[i + 2] += n;
    }
    x.putImageData(img, 0, 0);
    return c;
  }
  D.drawBackground = (ctx) => {
    if (!fondo) fondo = pintarFondo();
    ctx.drawImage(fondo, 0, 0);
  };
  D.stagePath = (ctx) => { ctx.beginPath(); ctx.roundRect(S.x, S.y, S.w, S.h, S.r); };
  D.drawStageEdge = (ctx) => {
    ctx.save();
    // Contorno opaco de 2 px en «línea»: 3,94:1 sobre tinta.
    ctx.beginPath(); ctx.roundRect(S.x + 1, S.y + 1, S.w - 2, S.h - 2, S.r - 1);
    ctx.lineWidth = 2;
    ctx.strokeStyle = D.C.line;
    ctx.stroke();
    ctx.restore();
  };
})();
