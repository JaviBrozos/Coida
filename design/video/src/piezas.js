'use strict';
/* Piezas de la interfaz de Doado.

   Las del móvil se pintan en dp, con las medidas de core/ui/DoadoDimens y los roles
   de Theme.kt: quien llama escala el contexto a los píxeles por dp que quiera. Los
   textos son los de res/values/strings.xml. */
(function () {
  const D = window.D, C = D.C;

  // Roles de Theme.kt en los dos temas.
  D.TH = {
    dark: { bg: C.ink, tile: C.surface, tileHigh: C.surfaceHigh, line: C.line, fg: C.text, muted: C.textMuted, primary: C.cyan, onPrimary: C.ink, off: C.steelLight, plate: C.ink },
    light: { bg: C.white, tile: C.white, tileHigh: C.mist, line: C.navy, fg: C.navy, muted: C.cyanDeep, primary: C.navy, onPrimary: C.white, off: C.steelDeep, plate: C.mist },
  };

  // Casillas de fábrica (data/DefaultFavorites.kt) y unos contactos de ejemplo.
  D.APPS = [
    { id: 'phone', icon: 'call', label: 'Llamar' },
    { id: 'messages', icon: 'messages', label: 'Mensajes' },
    { id: 'camera', icon: 'camera', label: 'Cámara' },
    { id: 'contacts', icon: 'contacts', label: 'Contactos' },
  ];
  D.PEOPLE = [
    { id: 'maria', initial: 'M', label: 'María', sub: 'Llamada' },
    { id: 'luis', initial: 'L', label: 'Luis', sub: 'WhatsApp' },
    { id: 'carmen', initial: 'C', label: 'Carmen', sub: 'Llamada' },
    { id: 'antonio', initial: 'A', label: 'Antonio', sub: 'WhatsApp' },
  ];

  // ---- Casilla (DoadoCard) en dp ------------------------------------------------
  // o: icon | initial, label, sub, marked, off, row, iconDp, labelSp, subSp, letra
  D.tile = (ctx, x, y, w, h, th, o) => {
    const marked = !!o.marked, off = !!o.off;
    const r = 28;
    ctx.save();
    ctx.beginPath();
    ctx.roundRect(x + 1, y + 1, w - 2, h - 2, r - 1);
    ctx.fillStyle = marked ? th.primary : th.tile;
    ctx.fill();
    // Señalar sin mover ni agrandar (Components.kt): relleno de acento y contorno de
    // 4 dp de acento dentro de la misma caja.
    ctx.lineWidth = marked ? 4 : 2;
    ctx.strokeStyle = marked ? th.primary : th.line;
    ctx.stroke();
    const fg = marked ? th.onPrimary : off ? th.off : th.fg;
    const muted = marked ? th.onPrimary : off ? th.off : th.muted;
    const ic = marked ? th.onPrimary : off ? th.off : th.primary;
    const L = o.letra || 1;
    let lab = (o.labelSp || 24) * L, sub = (o.subSp || 18) * L;
    const icon = o.iconDp || 72;
    if (!o.row) {
      // Un nombre que no cabe se ajusta al ancho de la casilla, sin bajar de 18 sp.
      const hueco = w - 16;
      const ancho = Math.max(D.measure(o.label, lab).width, o.sub ? D.measure(o.sub, sub).width * (lab / sub) : 0);
      if (ancho > hueco) { const f = Math.max(18 / lab, hueco / ancho); lab *= f; sub = Math.max(18, sub * f); }
    }
    if (o.row) {
      const cy = y + h / 2;
      glifo(ctx, o, x + 24 + icon / 2, cy, icon, ic, th, marked);
      const tx = x + 24 + icon + 20;
      if (o.sub) {
        D.text(ctx, o.label, tx, cy - 3, { size: lab, color: fg, align: 'left' });
        D.text(ctx, o.sub, tx, cy + sub + 3, { size: sub, color: muted, align: 'left' });
      } else D.text(ctx, o.label, tx, cy + lab * 0.36, { size: lab, color: fg, align: 'left' });
    } else {
      const gap = 10;
      const altoTexto = lab * 1.05 + (o.sub ? sub * 1.25 : 0);
      const top = y + (h - (icon + gap + altoTexto)) / 2;
      glifo(ctx, o, x + w / 2, top + icon / 2, icon, ic, th, marked);
      D.text(ctx, o.label, x + w / 2, top + icon + gap + lab * 0.8, { size: lab, color: fg });
      if (o.sub) D.text(ctx, o.sub, x + w / 2, top + icon + gap + lab * 1.05 + sub * 0.95, { size: sub, color: muted });
    }
    ctx.restore();
  };
  function glifo(ctx, o, cx, cy, px, color, th, marked) {
    if (o.initial) {
      // InitialBadge: la inicial sobre un plato.
      ctx.beginPath();
      ctx.arc(cx, cy, px / 2, 0, Math.PI * 2);
      ctx.fillStyle = marked ? th.onPrimary : th.plate;
      ctx.fill();
      D.text(ctx, o.initial, cx, cy + px * 0.171, { size: px * 0.47, weight: 700, color: th.primary });
    } else D.icon(ctx, o.icon, cx, cy, px, color);
  }

  // ---- Pantalla de inicio: 360 x 760 dp ------------------------------------------
  // cfg: theme, cols (1, 2 o 3), letra, marked (id), skip (Set de ids que no se pintan),
  // apps, people. Devuelve el rectángulo de cada casilla, en dp.
  D.HOME_W = 360;
  D.HOME_H = 760;
  D.home = (ctx, cfg = {}) => {
    const th = D.TH[cfg.theme || 'dark'];
    const L = cfg.letra || 1, cols = cfg.cols || 2;
    const skip = cfg.skip || new Set();
    const apps = cfg.apps || D.APPS, people = cfg.people || D.PEOPLE.slice(0, cols === 3 ? 3 : 2);
    const P = 20, G = cols === 3 ? 12 : 16;
    const rects = {};
    ctx.save();
    if (!cfg.noBg) { ctx.fillStyle = th.bg; ctx.fillRect(0, 0, D.HOME_W, D.HOME_H); }
    if (!skip.has('chrome')) {
      // Barra de estado de Android, solo formas: cobertura y batería.
      ctx.fillStyle = th.fg;
      for (let i = 0; i < 4; i++) { const hh = 4 + i * 2.5; ctx.fillRect(300 + i * 5, 20 - hh, 3, hh); }
      ctx.lineWidth = 1.4; ctx.strokeStyle = th.fg;
      ctx.beginPath(); ctx.roundRect(326, 9, 20, 11, 3); ctx.stroke();
      ctx.fillRect(328.5, 11.5, 12, 6);
      // Reloj y fecha.
      D.text(ctx, '10:30', P, 84, { size: 40 * L, weight: 700, color: th.fg, align: 'left' });
      D.text(ctx, 'Miércoles, 21 de octubre', P, 84 + 30 * L, { size: 20 * L, color: th.fg, align: 'left' });
      // Ajustes: círculo de 64 dp, solo con el engranaje, en tono apagado (D-39).
      ctx.beginPath(); ctx.arc(D.HOME_W - P - 32, 40 + 32, 31, 0, Math.PI * 2);
      ctx.lineWidth = 2; ctx.strokeStyle = th.line; ctx.stroke();
      D.icon(ctx, 'gear', D.HOME_W - P - 32, 72, 32, th.muted);
    }
    // Rejilla.
    const top = 132 + (L - 1) * 40;
    const tw = (D.HOME_W - 2 * P - (cols - 1) * G) / cols;
    const th0 = cols === 3 ? 108 : 168;
    const lab = cols === 3 ? 20 : 24, sub = 18;
    const icon = cols === 1 ? 54 : cols === 3 ? 48 : 72;
    const ver = (lista, y0, prefijo) => {
      lista.forEach((a, i) => {
        const c = i % cols, r = Math.floor(i / cols);
        const x = P + c * (tw + G), y = y0 + r * (th0 + G);
        rects[a.id] = { x, y, w: tw, h: th0 };
        if (skip.has(a.id)) return;
        D.tile(ctx, x, y, tw, th0, th, {
          icon: a.icon, initial: a.initial, label: a.label, sub: a.sub, off: a.off,
          marked: cfg.marked === a.id, row: cols === 1, iconDp: icon, labelSp: lab, subSp: sub,
          letra: cols === 3 ? Math.min(L, 1.25) : L,
        });
      });
      return y0 + Math.ceil(lista.length / cols) * (th0 + G);
    };
    let y = ver(apps, top);
    if (!skip.has('people') && people.length) {
      // Rótulo de HomeScreen.kt: icono de contactos de 28 dp en tono apagado, 12 dp de
      // hueco y el texto en titleMedium (26 sp) del color de fondo.
      const cy = y + 8 + 17 * L;
      if (!skip.has('peopleLabel')) {
        D.icon(ctx, 'contacts', P + 4 + 14, cy, 28, th.muted);
        D.text(ctx, 'Tus contactos', P + 44, cy + 26 * L * 0.364, { size: 26 * L, color: th.fg, align: 'left' });
      }
      ver(people, y + 8 + 34 * L + 12);
    }
    // Círculo del asistente: 96 dp, abajo en el centro, con un aro de 8 dp del color del fondo.
    if (!skip.has('assistant')) {
      const cx = D.HOME_W / 2, cy = D.HOME_H - 28 - 48;
      ctx.beginPath(); ctx.arc(cx, cy, 48 + 8, 0, Math.PI * 2); ctx.fillStyle = th.bg; ctx.fill();
      ctx.beginPath(); ctx.arc(cx, cy, 48, 0, Math.PI * 2); ctx.fillStyle = th.primary; ctx.fill();
      D.icon(ctx, 'assistant', cx, cy, 46, th.onPrimary);
      rects.assistant = { x: cx - 48, y: cy - 48, w: 96, h: 96 };
    }
    ctx.restore();
    return rects;
  };

  // Marco del móvil alrededor de una pantalla de w x h px.
  D.phoneFrame = (ctx, w, h, bezel, radius) => {
    ctx.save();
    ctx.beginPath();
    ctx.roundRect(-bezel, -bezel, w + bezel * 2, h + bezel * 2, radius + bezel);
    ctx.fillStyle = C.navy;
    ctx.fill();
    ctx.lineWidth = 2;
    ctx.strokeStyle = D.rgba(C.line, 0.85);
    ctx.stroke();
    ctx.restore();
  };

  // Móvil pequeño, ya pintado, para la tira «A su medida».
  const minis = new Map();
  D.miniPhone = (clave, cfg, k) => {
    if (minis.has(clave)) return minis.get(clave);
    const bezel = 9, w = D.HOME_W * k, h = D.HOME_H * k, rad = 30;
    const c = document.createElement('canvas');
    c.width = Math.ceil(w + bezel * 2 + 4);
    c.height = Math.ceil(h + bezel * 2 + 4);
    const x = c.getContext('2d');
    x.translate(bezel + 2, bezel + 2);
    D.phoneFrame(x, w, h, bezel, rad);
    x.save();
    x.beginPath(); x.roundRect(0, 0, w, h, rad); x.clip();
    x.scale(k, k);
    D.home(x, cfg);
    x.restore();
    const r = { canvas: c, w: c.width, h: c.height };
    minis.set(clave, r);
    return r;
  };

  // ---- La casilla de María: el objeto protagonista ------------------------------
  // Una casilla de contacto con su inicial, como una portada. size en px.
  D.hero = (ctx, cx, cy, size, o = {}) => {
    const a = o.alpha == null ? 1 : o.alpha;
    if (a <= 0.002 || size < 1) return;
    ctx.save();
    ctx.globalAlpha *= a;
    const r = size * (28 / 168);
    const bw = Math.max(1.5, size * (2 / 168));
    ctx.beginPath();
    ctx.roundRect(cx - size / 2 + bw / 2, cy - size / 2 + bw / 2, size - bw, size - bw, r);
    ctx.fillStyle = C.surface;
    ctx.fill();
    ctx.lineWidth = bw;
    ctx.strokeStyle = o.border || C.line;
    ctx.stroke();
    const plate = size * 0.56;
    ctx.beginPath();
    ctx.arc(cx, cy, plate / 2, 0, Math.PI * 2);
    ctx.fillStyle = C.ink;
    ctx.fill();
    const la = o.letterAlpha == null ? 1 : o.letterAlpha;
    if (la > 0.002) D.text(ctx, 'M', cx, cy + plate * 0.47 * 0.364, { size: plate * 0.47, weight: 700, color: C.cyan, alpha: la });
    ctx.restore();
  };

  // ---- Barra del asistente: la pieza que se queda abajo -------------------------
  D.DOCK = { w: 720, h: 84 };
  D.DOCK.cx = D.STAGE.cx;
  D.DOCK.cy = D.STAGE.bottom - 30 - D.DOCK.h / 2;
  // escucha: 0..1, cuánto se mueve el trazo de nivel.
  D.dock = (ctx, t, o = {}) => {
    const a = o.alpha == null ? 1 : o.alpha;
    if (a <= 0.002) return;
    const { w, h } = D.DOCK;
    const cx = D.DOCK.cx, cy = D.DOCK.cy + (o.dy || 0);
    const x0 = cx - w / 2;
    ctx.save();
    ctx.globalAlpha *= a;
    ctx.beginPath();
    ctx.roundRect(x0, cy - h / 2, w, h, h / 2);
    ctx.fillStyle = D.rgba(C.surface, 0.94);
    ctx.fill();
    ctx.lineWidth = 1.5;
    ctx.strokeStyle = D.rgba(C.line, 0.7);
    ctx.stroke();
    // Botón principal: el micrófono del asistente.
    const mx = x0 + 44;
    ctx.beginPath(); ctx.arc(mx, cy, 28, 0, Math.PI * 2); ctx.fillStyle = C.cyan; ctx.fill();
    D.icon(ctx, 'assistant', mx, cy, 30, C.ink);
    D.text(ctx, 'Asistente', x0 + 90, cy - 4, { size: 24, color: C.text, align: 'left' });
    D.text(ctx, o.sub || 'Pulsa y habla', x0 + 90, cy + 24, { size: 20, color: C.textMuted, align: 'left' });
    // Pista fina: el nivel de la voz cuando escucha.
    const px0 = x0 + 300, px1 = x0 + 560;
    const esc = o.listen || 0;
    ctx.lineCap = 'round';
    const n = 40;
    for (let i = 0; i < n; i++) {
      const u = i / (n - 1);
      const x = D.lerp(px0, px1, u);
      const v = 0.5 + 0.5 * Math.sin(t * 9.3 + i * 0.77) * Math.sin(t * 5.1 + i * 0.31 + 1.3);
      const env = Math.sin(Math.PI * u);
      const hh = 3 + esc * env * (4 + 20 * v);
      ctx.strokeStyle = D.mix(C.line, C.cyan, esc * (0.55 + 0.45 * env));
      ctx.lineWidth = 3;
      ctx.beginPath(); ctx.moveTo(x, cy - hh / 2); ctx.lineTo(x, cy + hh / 2); ctx.stroke();
    }
    // «Ya he terminado» como icono (cuadrado redondeado) y el volumen.
    const sx = x0 + w - 112;
    ctx.beginPath(); ctx.arc(sx, cy, 21, 0, Math.PI * 2);
    ctx.lineWidth = 1.7; ctx.strokeStyle = C.line; ctx.stroke();
    ctx.beginPath(); ctx.roundRect(sx - 7, cy - 7, 14, 14, 2.5); ctx.fillStyle = C.textMuted; ctx.fill();
    D.icon(ctx, 'volume', x0 + w - 52, cy, 28, C.textMuted);
    ctx.restore();
  };

  // ---- Lista «Tus contactos» (SettingsGroup con filas) ----------------------------
  D.contactRow = (ctx, x, y, w, h, p, o = {}) => {
    const a = o.alpha == null ? 1 : o.alpha;
    if (a <= 0.002) return;
    ctx.save();
    ctx.globalAlpha *= a;
    const cy = y + h / 2;
    ctx.beginPath(); ctx.arc(x + 24 + 30, cy, 30, 0, Math.PI * 2); ctx.fillStyle = C.ink; ctx.fill();
    D.text(ctx, p.initial, x + 54, cy + 28 * 0.364, { size: 28, weight: 700, color: C.cyan });
    D.text(ctx, p.label, x + 104, cy - 4, { size: 26, color: C.text, align: 'left' });
    D.text(ctx, p.sub, x + 104, cy + 24, { size: 20, color: C.textMuted, align: 'left' });
    D.icon(ctx, p.sub === 'WhatsApp' ? 'messages' : 'call', x + w - 92, cy, 28, C.cyan);
    D.icon(ctx, 'chevron_right', x + w - 40, cy, 26, C.textMuted);
    ctx.restore();
  };

  // ---- Campo del asistente: «Te escucho» y lo que se ha oído ---------------------
  D.field = (ctx, cx, cy, w, h, texto, o = {}) => {
    const a = o.alpha == null ? 1 : o.alpha;
    if (a <= 0.002) return;
    ctx.save();
    ctx.globalAlpha *= a;
    ctx.beginPath();
    ctx.roundRect(cx - w / 2 + 1, cy - h / 2 + 1, w - 2, h - 2, h / 2);
    ctx.fillStyle = C.surface;
    ctx.fill();
    ctx.lineWidth = 2;
    ctx.strokeStyle = o.active ? C.cyan : C.line;
    ctx.stroke();
    const x0 = cx - w / 2;
    D.icon(ctx, 'assistant', x0 + 44, cy, 30, C.cyan);
    if (texto) D.text(ctx, texto, x0 + 80, cy + 30 * 0.364, { size: 30, color: C.text, align: 'left' });
    if (o.caret) {
      const tw = texto ? D.measure(texto, 30).width : 0;
      ctx.fillStyle = C.cyan;
      ctx.fillRect(x0 + 80 + tw + 4, cy - 17, 3, 34);
    }
    ctx.restore();
  };

  // Fila del resultado: miniatura, nombre, acción y botón de llamar.
  D.resultRow = (ctx, x, y, w, h, o = {}) => {
    const a = o.alpha == null ? 1 : o.alpha;
    if (a <= 0.002) return;
    ctx.save();
    ctx.globalAlpha *= a;
    ctx.beginPath();
    ctx.roundRect(x + 1, y + 1, w - 2, h - 2, 24);
    ctx.fillStyle = D.mix(C.surface, C.surfaceHigh, o.sel || 0);
    ctx.fill();
    ctx.lineWidth = 2;
    ctx.strokeStyle = D.mix(C.line, C.cyan, o.sel || 0);
    ctx.stroke();
    const cy = y + h / 2;
    if (o.thumb !== false) D.hero(ctx, x + 20 + 36, cy, 72);
    if ((o.textAlpha == null ? 1 : o.textAlpha) > 0.002) {
      ctx.save();
      ctx.globalAlpha *= o.textAlpha == null ? 1 : o.textAlpha;
      D.text(ctx, 'María', x + 112, cy - 4, { size: 28, color: C.text, align: 'left' });
      D.text(ctx, 'Llamada', x + 112, cy + 24, { size: 20, color: C.textMuted, align: 'left' });
      ctx.beginPath(); ctx.arc(x + w - 52, cy, 30, 0, Math.PI * 2); ctx.fillStyle = C.cyan; ctx.fill();
      D.icon(ctx, 'call', x + w - 52, cy, 30, C.ink);
      ctx.restore();
    }
    ctx.restore();
  };

  // Botón principal (PrimaryButton), relleno de acento.
  D.button = (ctx, cx, cy, w, h, label, o = {}) => {
    const a = o.alpha == null ? 1 : o.alpha;
    if (a <= 0.002) return;
    ctx.save();
    ctx.globalAlpha *= a;
    ctx.translate(cx, cy);
    ctx.scale(o.s || 1, o.s || 1);
    ctx.beginPath();
    ctx.roundRect(-w / 2, -h / 2, w, h, h / 2);
    ctx.fillStyle = o.pressed ? D.mix(C.cyan, C.cyanLight, o.pressed) : C.cyan;
    ctx.fill();
    D.icon(ctx, 'call', -w / 2 + 40, 0, 28, C.ink);
    D.text(ctx, label, 16, 26 * 0.364, { size: 26, weight: 700, color: C.ink });
    ctx.restore();
  };

  // Botón secundario (OutlineButton): solo contorno de 2 dp.
  D.outlineButton = (ctx, cx, cy, w, h, label, o = {}) => {
    const a = o.alpha == null ? 1 : o.alpha;
    if (a <= 0.002) return;
    ctx.save();
    ctx.globalAlpha *= a;
    ctx.translate(cx, cy);
    ctx.scale(o.s || 1, o.s || 1);
    ctx.beginPath();
    ctx.roundRect(-w / 2 + 1, -h / 2 + 1, w - 2, h - 2, h / 2);
    ctx.lineWidth = 2;
    ctx.strokeStyle = C.line;
    ctx.stroke();
    D.text(ctx, label, 0, 26 * 0.364, { size: 26, weight: 700, color: C.text });
    ctx.restore();
  };

  // ---- Fundas de ajustes: piezas de diseño con letra gruesa y un motivo -----------
  // Colores con su contraste ya medido: sobre fondo claro, nunca cian (invariante 4).
  D.SLEEVES = [
    { id: 'personas', title: ['Sus', 'contactos'], bg: C.cyan, fg: C.ink, motif: 'contacts', mc: C.ink },
    { id: 'letra', title: ['Su', 'letra'], bg: C.navy, fg: C.text, motif: 'Aa', mc: C.cyan, edge: C.line },
    { id: 'asistente', title: ['Su', 'asistente'], bg: C.mist, fg: C.navy, motif: 'assistant', mc: C.navy },
    { id: 'apps', title: ['Sus', 'casillas'], bg: C.cyanDeep, fg: C.white, motif: 'apps', mc: C.cyanLight },
    { id: 'tema', title: ['Su', 'tema'], bg: C.surfaceHigh, fg: C.text, motif: 'tema', mc: C.cyan, edge: C.line },
    { id: 'pin', title: ['Tu', 'PIN'], bg: C.cyanLight, fg: C.ink, motif: 'lock', mc: C.ink },
  ];
  // Pinta una funda de s x s px en el origen.
  D.sleeve = (ctx, s, sp) => {
    ctx.save();
    ctx.beginPath();
    ctx.roundRect(0, 0, s, s, s * 0.09);
    ctx.fillStyle = sp.bg;
    ctx.fill();
    ctx.clip();
    // Motivo grande, cortado por la esquina de arriba a la derecha.
    const ms = s * 0.78, mx = s * 0.7, my = s * 0.3;
    if (sp.motif === 'Aa') {
      D.text(ctx, 'Aa', mx, my + ms * 0.28, { size: ms * 0.78, weight: 700, color: sp.mc });
    } else if (sp.motif === 'tema') {
      ctx.beginPath(); ctx.arc(mx, my, ms * 0.36, 0, Math.PI * 2);
      ctx.lineWidth = s * 0.03; ctx.strokeStyle = sp.mc; ctx.stroke();
      ctx.beginPath(); ctx.arc(mx, my, ms * 0.36, -Math.PI / 2, Math.PI / 2); ctx.closePath(); ctx.fillStyle = sp.mc; ctx.fill();
    } else {
      D.icon(ctx, sp.motif, mx, my, ms, sp.mc, { weight: 1.15 });
    }
    // Círculo como motivo de marca, pequeño, abajo a la derecha.
    ctx.beginPath(); ctx.arc(s * 0.84, s * 0.84, s * 0.045, 0, Math.PI * 2); ctx.fillStyle = sp.mc; ctx.fill();
    const fs = s * 0.13;
    D.text(ctx, sp.title[0], s * 0.09, s * 0.72, { size: fs, weight: 700, color: sp.fg, align: 'left' });
    D.text(ctx, sp.title[1], s * 0.09, s * 0.72 + fs * 1.12, { size: fs, weight: 700, color: sp.fg, align: 'left' });
    ctx.restore();
    if (sp.edge) {
      // Las fundas oscuras se separan del escenario por borde, no por sombra.
      ctx.save();
      ctx.beginPath();
      ctx.roundRect(1, 1, s - 2, s - 2, s * 0.09 - 1);
      ctx.lineWidth = 2;
      ctx.strokeStyle = sp.edge;
      ctx.stroke();
      ctx.restore();
    }
  };
})();
