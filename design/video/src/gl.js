'use strict';
/* Compositor 3D con WebGL2.

   Pinta capas planas (texturas sacadas de un canvas 2D) con perspectiva de verdad:
   inclinaciones, giros y pliegues. Solo se usa donde hace falta perspectiva; lo
   plano va en 2D, que sale más nítido. Muestreo con mipmaps y anisotropía, y
   bordes con antialiasado multimuestra. */
(function () {
  const D = window.D;
  const cv = document.createElement('canvas');
  cv.width = D.W;
  cv.height = D.H;
  const gl = cv.getContext('webgl2', { antialias: true, alpha: true, premultipliedAlpha: true, preserveDrawingBuffer: true });
  if (!gl) throw new Error('Este navegador no tiene WebGL2');
  const aniso = gl.getExtension('EXT_texture_filter_anisotropic');

  const VS = `#version 300 es
  in vec2 aUV;
  uniform mat4 uM;     // de píxeles locales (origen en el centro de la capa) a píxeles del lienzo
  uniform vec2 uSize;  // tamaño de la capa en píxeles del mundo
  uniform vec2 uView;  // tamaño del lienzo
  uniform vec2 uEye;   // centro óptico, en píxeles del lienzo
  uniform float uF;    // distancia focal en píxeles: a z = 0 la escala es 1
  out vec2 vUV;
  void main() {
    vec4 p = uM * vec4((aUV - 0.5) * uSize, 0.0, 1.0);
    float w = (uF + p.z) / uF;
    // En pantalla: ojo + (p.xy - ojo) / w. Se escribe homogéneo para que la textura
    // se interpole con corrección de perspectiva.
    vec2 sc = uEye * w + (p.xy - uEye);
    gl_Position = vec4(sc.x / (uView.x * 0.5) - w, w - sc.y / (uView.y * 0.5), 0.0, w);
    vUV = aUV;
  }`;
  const FS = `#version 300 es
  precision highp float;
  in vec2 vUV;
  uniform sampler2D uTex;
  uniform float uAlpha;
  uniform float uDim;
  out vec4 o;
  void main() {
    vec4 c = texture(uTex, vUV);
    o = vec4(c.rgb * uDim, c.a) * uAlpha;
  }`;

  function shader(tipo, src) {
    const s = gl.createShader(tipo);
    gl.shaderSource(s, src);
    gl.compileShader(s);
    if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(s));
    return s;
  }
  const prog = gl.createProgram();
  gl.attachShader(prog, shader(gl.VERTEX_SHADER, VS));
  gl.attachShader(prog, shader(gl.FRAGMENT_SHADER, FS));
  gl.linkProgram(prog);
  if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(prog));
  const U = {};
  for (const n of ['uM', 'uSize', 'uView', 'uEye', 'uF', 'uTex', 'uAlpha', 'uDim']) U[n] = gl.getUniformLocation(prog, n);
  const vao = gl.createVertexArray();
  gl.bindVertexArray(vao);
  const vb = gl.createBuffer();
  gl.bindBuffer(gl.ARRAY_BUFFER, vb);
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([0, 0, 1, 0, 0, 1, 1, 1]), gl.STATIC_DRAW);
  const loc = gl.getAttribLocation(prog, 'aUV');
  gl.enableVertexAttribArray(loc);
  gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);

  // ---- Matrices 4x4 por columnas -------------------------------------------
  const M = {
    id: () => new Float32Array([1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]),
    mul(a, b) {
      const o = new Float32Array(16);
      for (let c = 0; c < 4; c++) for (let r = 0; r < 4; r++) {
        let s = 0;
        for (let k = 0; k < 4; k++) s += a[k * 4 + r] * b[c * 4 + k];
        o[c * 4 + r] = s;
      }
      return o;
    },
    t: (x, y, z = 0) => new Float32Array([1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, y, z, 1]),
    s: (x, y = x, z = 1) => new Float32Array([x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1]),
    // Ejes con y hacia abajo y z hacia el fondo. rx > 0: el borde de arriba se aleja.
    rx(a) { const c = Math.cos(a), s = Math.sin(a); return new Float32Array([1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1]); },
    // ry > 0: el borde derecho se aleja.
    ry(a) { const c = Math.cos(a), s = Math.sin(a); return new Float32Array([c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1]); },
    // rz > 0: gira en el sentido de las agujas del reloj.
    rz(a) { const c = Math.cos(a), s = Math.sin(a); return new Float32Array([c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]); },
  };
  M.chain = (...ms) => ms.reduce((a, b) => M.mul(a, b));
  // Transformación habitual: posición, giros, escala y pivote (desde el centro de la capa).
  M.pose = (o) => M.chain(
    M.t(o.x || 0, o.y || 0, o.z || 0),
    M.ry(o.ry || 0), M.rx(o.rx || 0), M.rz(o.rz || 0),
    M.s(o.sx != null ? o.sx : o.s != null ? o.s : 1, o.sy != null ? o.sy : o.s != null ? o.s : 1),
    M.t(-(o.px || 0), -(o.py || 0), 0),
  );

  // ---- Texturas: se pintan una vez y se guardan --------------------------------
  const texturas = new Map();
  const PAD = 4; // píxeles transparentes alrededor: bordes suaves al inclinar
  function tex(clave, w, h, res, pintar) {
    const hay = texturas.get(clave);
    if (hay) return hay;
    const cw = Math.ceil(w * res) + PAD * 2, ch = Math.ceil(h * res) + PAD * 2;
    const c = document.createElement('canvas');
    c.width = cw;
    c.height = ch;
    const x = c.getContext('2d');
    x.translate(PAD, PAD);
    x.scale(res, res);
    pintar(x, w, h);
    const t = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, t);
    gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, true);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, c);
    gl.generateMipmap(gl.TEXTURE_2D);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    if (aniso) gl.texParameterf(gl.TEXTURE_2D, aniso.TEXTURE_MAX_ANISOTROPY_EXT, 8);
    const r = { t, w: cw / res, h: ch / res, canvas: c };
    texturas.set(clave, r);
    return r;
  }

  // items: { tex, m (matriz) o una pose, alpha, dim, zs (orden: mayor = más al fondo) }
  function render(items, o = {}) {
    gl.viewport(0, 0, D.W, D.H);
    gl.clearColor(0, 0, 0, 0);
    gl.clear(gl.COLOR_BUFFER_BIT);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
    gl.useProgram(prog);
    gl.bindVertexArray(vao);
    gl.uniform2f(U.uView, D.W, D.H);
    const eye = o.eye || [D.STAGE.cx, D.STAGE.cy];
    gl.uniform2f(U.uEye, eye[0], eye[1]);
    gl.uniform1f(U.uF, o.f || 2200);
    gl.uniform1i(U.uTex, 0);
    gl.activeTexture(gl.TEXTURE0);
    const lista = items
      .filter((it) => it && (it.alpha == null || it.alpha > 0.002))
      .map((it, i) => {
        const m = it.m || M.pose(it);
        return { it, m, z: it.zs != null ? it.zs : m[14], i };
      });
    lista.sort((a, b) => b.z - a.z || a.i - b.i);
    for (const { it, m } of lista) {
      gl.uniformMatrix4fv(U.uM, false, m);
      gl.uniform2f(U.uSize, it.tex.w, it.tex.h);
      gl.uniform1f(U.uAlpha, it.alpha == null ? 1 : it.alpha);
      gl.uniform1f(U.uDim, it.dim == null ? 1 : it.dim);
      gl.bindTexture(gl.TEXTURE_2D, it.tex.t);
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
    }
    return cv;
  }

  D.GL = { canvas: cv, gl, tex, render, M };
})();
