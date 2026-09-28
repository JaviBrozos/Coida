// Junta las diez láminas .dc.html en un único HTML listo para imprimir.
// Sin iframes: el contenido de cada lámina se incrusta y su CSS se acota con
// un prefijo, para que las clases repetidas (.hoja, .tit...) no se pisen.
import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

// Se ejecuta desde la carpeta que lo contiene: node montar-presentacion.mjs
// En Windows el pathname llega como /C:/..., así que se quita la barra inicial.
const dir = decodeURIComponent(new URL(".", import.meta.url).pathname).replace(/^\/(?=[A-Za-z]:)/, "");

const laminas = [
  ["Portada", "Main.dc.html"],
  ["01 · A quién sirve", "Perfiles.dc.html"],
  ["02 · Mapa de pantallas", "MapaPantallas.dc.html"],
  ["03 · Flujo de la persona mayor", "FlujoMayor.dc.html"],
  ["04 · Flujo del familiar", "FlujoFamiliar.dc.html"],
  ["05 · Flujo del administrador", "FlujoAdmin.dc.html"],
  ["06 · Cómo está construido", "Arquitectura.dc.html"],
  ["07 · Sistema visual", "SistemaVisual.dc.html"],
  ["08 · Estado y límites", "EstadoLimites.dc.html"],
  ["09 · Cómo queda documentado", "Documentacion.dc.html"],
];

/** Antepone `#id ` a cada selector para que el CSS no salga de su lámina. */
function acotarCss(css, id) {
  return css
    .split("}")
    .map((bloque) => {
      const corte = bloque.indexOf("{");
      if (corte === -1) return "";
      const selectores = bloque.slice(0, corte).trim();
      const cuerpo = bloque.slice(corte + 1).trim();
      if (!selectores) return "";
      const acotados = selectores
        .split(",")
        .map((s) => {
          const sel = s.trim();
          if (!sel) return "";
          // `body { ... }` pasa a ser la propia sección de la lámina.
          if (sel === "body" || sel === "html") return `#${id}`;
          return `#${id} ${sel}`;
        })
        .filter(Boolean)
        .join(", ");
      return `${acotados} { ${cuerpo} }`;
    })
    .filter(Boolean)
    .join("\n");
}

const estilos = [];
const secciones = [];

laminas.forEach(([titulo, fichero], indice) => {
  const id = `l${indice + 1}`;
  const bruto = readFileSync(join(dir, fichero), "utf8");

  const css = bruto.match(/<style>([\s\S]*?)<\/style>/);
  if (css) estilos.push(`/* ${titulo} */\n${acotarCss(css[1], id)}`);

  const cuerpo = bruto.match(/<x-dc>([\s\S]*?)<\/x-dc>/);
  if (!cuerpo) throw new Error(`Sin contenido en ${fichero}`);
  // Fuera el <helmet>, que solo traía el <style> ya extraído.
  const contenido = cuerpo[1].replace(/<helmet>[\s\S]*?<\/helmet>/, "").trim();

  secciones.push(
    `<section id="${id}" class="lamina" aria-label="${titulo}">\n${contenido}\n</section>`,
  );
});

const salida = `<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Doado de un vistazo</title>
<style>
  /* Generado a partir de las láminas de design/presentacion. No editar a mano. */
  * { box-sizing: border-box; }
  body { margin: 0; background: #06101A; }
  .lamina {
    width: 1280px;
    height: 720px;
    margin: 0 auto 26px;
    overflow: hidden;
    position: relative;
    font-family: "Segoe UI", system-ui, -apple-system, sans-serif;
  }
  @media print {
    body { background: #FFFFFF; }
    .lamina { margin: 0; break-after: page; }
    .lamina:last-child { break-after: auto; }
    @page { size: 1280px 720px; margin: 0; }
  }
${estilos.join("\n\n")}
</style>
</head>
<body>
${secciones.join("\n\n")}
</body>
</html>
`;

writeFileSync(join(dir, "ver-presentacion.html"), salida, "utf8");
console.log(`ver-presentacion.html: ${laminas.length} láminas, ${Math.round(salida.length / 1024)} KB`);
