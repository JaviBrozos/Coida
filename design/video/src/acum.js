'use strict';
/* Acumulador de subfotogramas y paso a YUV, con WebGL2.

   Cada fotograma de salida es la media de varios subfotogramas repartidos dentro del
   obturador: lo que está quieto sale idéntico en todos y queda nítido; lo que se
   mueve se emborrona lo justo. La suma va en coma flotante y el paso a 8 bits
   redondea sin tramado: el único degradado amplio, el fondo de fuera, ya lo lleva
   horneado (src/base.js), y en el escenario liso el tramado solo sería ruido.

   La conversión es BT.709 de rango limitado, la que se declara en el MP4. Los planos
   se empaquetan de cuatro en cuatro muestras por píxel RGBA para leerlos de una vez. */
(function () {
  const D = window.D;

  D.Acum = (src) => {
    const W = D.W, H = D.H;
    const cv = document.createElement('canvas');
    cv.width = W;
    cv.height = H;
    const gl = cv.getContext('webgl2', { antialias: false, alpha: false, premultipliedAlpha: false, preserveDrawingBuffer: true });
    if (!gl.getExtension('EXT_color_buffer_float')) throw new Error('Sin texturas de coma flotante');

    const VS = `#version 300 es
    out vec2 uv;
    void main() {
      vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));
      uv = p;
      gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
    }`;
    const prog = (fs) => {
      const p = gl.createProgram();
      for (const [tipo, s] of [[gl.VERTEX_SHADER, VS], [gl.FRAGMENT_SHADER, fs]]) {
        const sh = gl.createShader(tipo);
        gl.shaderSource(sh, s);
        gl.compileShader(sh);
        if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) throw new Error(gl.getShaderInfoLog(sh));
        gl.attachShader(p, sh);
      }
      gl.linkProgram(p);
      if (!gl.getProgramParameter(p, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(p));
      return p;
    };
    const CAB = `#version 300 es
    precision highp float;
    precision highp sampler2D;
    in vec2 uv;
    out vec4 o;
    uniform sampler2D uTex;
    // Tramado fijo por píxel, entre -0,5 y 0,5 de un nivel de 8 bits.
    float tram(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453) - 0.5; }
    vec3 px(int x, int y) { return texelFetch(uTex, ivec2(x, y), 0).rgb; }
    float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
    `;
    const pSuma = prog(`${CAB}
    uniform float uW;
    void main() { o = vec4(texture(uTex, uv).rgb * uW, 1.0); }`);
    // Y: cada píxel de salida lleva cuatro muestras seguidas de la fila.
    const pY = prog(`${CAB}
    void main() {
      int r = int(gl_FragCoord.y), c = int(gl_FragCoord.x) * 4;
      vec4 v;
      for (int i = 0; i < 4; i++) {
        float y = 16.0 + 219.0 * luma(px(c + i, r));
        v[i] = y / 255.0;
      }
      o = v;
    }`);
    // U y V: media de cada bloque de 2 x 2, cuatro muestras por píxel de salida.
    const pUV = prog(`${CAB}
    uniform int uV;
    void main() {
      int r = int(gl_FragCoord.y), c = int(gl_FragCoord.x) * 4;
      vec4 v;
      for (int i = 0; i < 4; i++) {
        int x = (c + i) * 2, y = r * 2;
        float s = 0.0;
        for (int dy = 0; dy < 2; dy++) for (int dx = 0; dx < 2; dx++) {
          vec3 k = px(x + dx, y + dy);
          float l = luma(k);
          s += uV == 1 ? (k.r - l) / 1.5748 : (k.b - l) / 1.8556;
        }
        v[i] = (128.0 + 224.0 * (s * 0.25)) / 255.0;
      }
      o = v;
    }`);
    // Para mirar: el fotograma promediado, derecho, en el lienzo.
    const pVer = prog(`${CAB}
    void main() {
      vec3 c = texture(uTex, vec2(uv.x, 1.0 - uv.y)).rgb;
      o = vec4(c + tram(gl_FragCoord.xy) / 255.0, 1.0);
    }`);

    const textura = (w, h, interno, formato, tipo) => {
      const t = gl.createTexture();
      gl.bindTexture(gl.TEXTURE_2D, t);
      gl.texStorage2D(gl.TEXTURE_2D, 1, interno, w, h);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
      return t;
    };
    const fbo = (t) => {
      const f = gl.createFramebuffer();
      gl.bindFramebuffer(gl.FRAMEBUFFER, f);
      gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, t, 0);
      if (gl.checkFramebufferStatus(gl.FRAMEBUFFER) !== gl.FRAMEBUFFER_COMPLETE) throw new Error('FBO incompleto');
      return f;
    };
    const tSrc = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, tSrc);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    const tAcc = textura(W, H, gl.RGBA32F);
    const fAcc = fbo(tAcc);
    const tY = textura(W / 4, H, gl.RGBA8), fY = fbo(tY);
    const tU = textura(W / 8, H / 2, gl.RGBA8), fU = fbo(tU);
    const tV = textura(W / 8, H / 2, gl.RGBA8), fV = fbo(tV);
    const vao = gl.createVertexArray();
    const yuv = new Uint8Array((W * H * 3) / 2);

    const pasar = (p, f, w, h, tex, extra) => {
      gl.bindFramebuffer(gl.FRAMEBUFFER, f);
      gl.viewport(0, 0, w, h);
      gl.useProgram(p);
      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, tex);
      gl.uniform1i(gl.getUniformLocation(p, 'uTex'), 0);
      if (extra) extra(p);
      gl.bindVertexArray(vao);
      gl.drawArrays(gl.TRIANGLES, 0, 3);
    };

    return {
      canvas: cv,
      clear() {
        gl.bindFramebuffer(gl.FRAMEBUFFER, fAcc);
        gl.viewport(0, 0, W, H);
        gl.clearColor(0, 0, 0, 0);
        gl.clear(gl.COLOR_BUFFER_BIT);
      },
      // Suma el lienzo de origen con un peso.
      add(w) {
        gl.bindTexture(gl.TEXTURE_2D, tSrc);
        gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
        gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, false);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, src);
        gl.enable(gl.BLEND);
        gl.blendFunc(gl.ONE, gl.ONE);
        pasar(pSuma, fAcc, W, H, tSrc, (p) => gl.uniform1f(gl.getUniformLocation(p, 'uW'), w));
        gl.disable(gl.BLEND);
      },
      // Planos I420 (Y, luego U, luego V), de arriba abajo.
      yuv() {
        pasar(pY, fY, W / 4, H, tAcc);
        gl.readPixels(0, 0, W / 4, H, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array(yuv.buffer, 0, W * H));
        pasar(pUV, fU, W / 8, H / 2, tAcc, (p) => gl.uniform1i(gl.getUniformLocation(p, 'uV'), 0));
        gl.readPixels(0, 0, W / 8, H / 2, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array(yuv.buffer, W * H, (W * H) / 4));
        pasar(pUV, fV, W / 8, H / 2, tAcc, (p) => gl.uniform1i(gl.getUniformLocation(p, 'uV'), 1));
        gl.readPixels(0, 0, W / 8, H / 2, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array(yuv.buffer, (W * H * 5) / 4, (W * H) / 4));
        return yuv;
      },
      // Deja el fotograma promediado en cv, para hojas de contacto y PNG.
      show() {
        pasar(pVer, null, W, H, tAcc);
        return cv;
      },
    };
  };
})();
