// Turns the blocks from report-md-to-json.js into plain HTML that Word imports in one call
// (build-report-docx.ps1). Headings become h1-h3 (Word's Título 1-3), captions are marked with
// @FIG@ / @TAB@ so the build script can turn them into numbered captions.
// Usage: node report-json-to-html.js <blocks.json> <out.html>
const fs = require('fs');
const { pathToFileURL } = require('url');

const [, , src, out] = process.argv;
const blocks = JSON.parse(fs.readFileSync(src, 'utf8'));

const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function rich({ text, spans }) {
  // open/close tags at span boundaries (spans may nest but do not cross in the report)
  const events = [];
  for (const sp of spans || []) {
    const tag = { b: 'b', i: 'i', c: 'code' }[sp.k];
    events.push({ at: sp.s, open: true, tag, len: sp.e - sp.s });
    events.push({ at: sp.e, open: false, tag, len: sp.e - sp.s });
  }
  events.sort((a, b) => a.at - b.at || (a.open === b.open ? (a.open ? b.len - a.len : a.len - b.len) : a.open ? 1 : -1));
  let html = '';
  let pos = 0;
  for (const ev of events) {
    html += esc(text.slice(pos, ev.at));
    pos = ev.at;
    html += ev.open ? `<${ev.tag}>` : `</${ev.tag}>`;
  }
  return html + esc(text.slice(pos));
}

let html = '<html><head><meta charset="utf-8"><style>code{font-family:Consolas;font-size:9pt}</style></head><body>\n';
let list = null; // 'ul' | 'ol'
let lastHeading = '';
const closeList = () => {
  if (list) html += `</${list}>\n`;
  list = null;
};

for (const b of blocks) {
  if (b.t !== 'li') closeList();
  switch (b.t) {
    case 'h':
      html += `<h${b.level}>${rich(b)}</h${b.level}>\n`;
      lastHeading = b.text;
      break;
    case 'p':
      html += `<p>${rich(b)}</p>\n`;
      break;
    case 'quote':
      html += `<p><i>${rich(b)}</i></p>\n`;
      break;
    case 'li': {
      const want = b.ordered ? 'ol' : 'ul';
      if (list !== want) {
        closeList();
        html += `<${want}>\n`;
        list = want;
      }
      html += `<li>${rich(b)}</li>\n`;
      break;
    }
    case 'code':
      for (const line of b.text.split('\n')) html += `<p class="code"><code>${esc(line) || '&nbsp;'}</code></p>\n`;
      break;
    case 'img':
      html += `<p><img src="${pathToFileURL(b.file).href}"></p>\n<p>@FIG@ ${esc(b.caption)}</p>\n`;
      break;
    case 'table':
      html += `<p>@TAB@ ${esc(lastHeading)}</p>\n<table border="1">\n`;
      b.rows.forEach((row, r) => {
        const cell = r === 0 ? 'th' : 'td';
        html += '<tr>' + row.map((c) => `<${cell}>${c ? rich(c) : ''}</${cell}>`).join('') + '</tr>\n';
      });
      html += '</table>\n';
      break;
  }
}
closeList();
html += '</body></html>\n';
fs.writeFileSync(out, html, 'utf8');
console.log(`html written: ${out}`);
