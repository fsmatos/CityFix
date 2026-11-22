// Turns docs/report/relatorio.md into a list of blocks for build-report-docx.ps1 (F-017).
// Handles the Markdown the report uses: headings, paragraphs, bullet/numbered lists, tables,
// images, fenced code and blockquotes, with **bold**, *italic*, `code` and [links](url) inline.
// Usage: node report-md-to-json.js <relatorio.md> <out.json>
const fs = require('fs');
const path = require('path');

const [, , src, out] = process.argv;
const baseDir = path.dirname(path.resolve(src));
const lines = fs.readFileSync(src, 'utf8').replace(/\r/g, '').split('\n');

// Inline Markdown -> plain text plus formatting spans {s, e, k} (k: b, i, c).
function inline(md) {
  let text = '';
  const spans = [];
  let i = 0;
  const open = {};
  while (i < md.length) {
    if (md[i] === '`') {
      const j = md.indexOf('`', i + 1);
      if (j > i) {
        const s = text.length;
        text += md.slice(i + 1, j);
        spans.push({ s, e: text.length, k: 'c' });
        i = j + 1;
        continue;
      }
    }
    if (md.startsWith('**', i)) {
      if (open.b === undefined) open.b = text.length;
      else { spans.push({ s: open.b, e: text.length, k: 'b' }); delete open.b; }
      i += 2;
      continue;
    }
    if (md[i] === '*' && (open.i !== undefined || /\S/.test(md[i + 1] || ''))) {
      if (open.i === undefined) open.i = text.length;
      else { spans.push({ s: open.i, e: text.length, k: 'i' }); delete open.i; }
      i += 1;
      continue;
    }
    if (md[i] === '[') {
      const m = /^\[([^\]]+)\]\(([^)]+)\)/.exec(md.slice(i));
      if (m) {
        text += m[1];
        i += m[0].length;
        continue;
      }
    }
    if (md[i] === '\\' && i + 1 < md.length) { text += md[i + 1]; i += 2; continue; }
    text += md[i];
    i += 1;
  }
  return { text, spans: spans.filter((sp) => sp.e > sp.s) };
}

const blocks = [];
let started = false;
let i = 0;
let para = [];
const flush = () => {
  if (para.length) blocks.push({ t: 'p', ...inline(para.join(' ')) });
  para = [];
};

while (i < lines.length) {
  const line = lines[i];
  const h = /^(#{2,4})\s+(.*)$/.exec(line);
  // The cover and the index come from the template: start at the first numbered chapter.
  if (!started) {
    if (h && h[1] === '##' && /^\d+\./.test(h[2])) started = true;
    else { i++; continue; }
  }
  if (/^\s*$/.test(line) || /^<div class="page-break"><\/div>/.test(line)) { flush(); i++; continue; }
  if (h) {
    flush();
    const level = h[1].length - 1; // ## -> 1, ### -> 2, #### -> 3
    const title = h[2].replace(/^\d+(\.\d+)*\.?\s+/, '');
    blocks.push({ t: 'h', level, ...inline(title) });
    i++;
    continue;
  }
  if (line.startsWith('```')) {
    flush();
    const code = [];
    i++;
    while (i < lines.length && !lines[i].startsWith('```')) code.push(lines[i++]);
    i++;
    blocks.push({ t: 'code', text: code.join('\n') });
    continue;
  }
  const img = /^!\[([^\]]*)\]\(([^)]+)\)\s*$/.exec(line);
  if (img) {
    flush();
    blocks.push({ t: 'img', caption: img[1], file: path.resolve(baseDir, img[2]) });
    i++;
    continue;
  }
  if (line.startsWith('|')) {
    flush();
    const rows = [];
    while (i < lines.length && lines[i].startsWith('|')) {
      const cells = lines[i].trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim());
      if (!cells.every((c) => /^:?-{2,}:?$/.test(c))) rows.push(cells.map(inline));
      i++;
    }
    blocks.push({ t: 'table', rows });
    continue;
  }
  const li = /^(\s*)([-*]|\d+\.)\s+(.*)$/.exec(line);
  if (li) {
    flush();
    let textLine = li[3];
    i++;
    // continuation lines (indented, not a new item)
    while (i < lines.length && /^\s{2,}\S/.test(lines[i]) && !/^\s*([-*]|\d+\.)\s+/.test(lines[i])) {
      textLine += ' ' + lines[i].trim();
      i++;
    }
    blocks.push({
      t: 'li',
      level: Math.min(Math.floor(li[1].length / 2), 2),
      ordered: /\d/.test(li[2]),
      ...inline(textLine),
    });
    continue;
  }
  if (line.startsWith('>')) {
    flush();
    const q = [];
    while (i < lines.length && lines[i].startsWith('>')) q.push(lines[i++].replace(/^>\s?/, ''));
    blocks.push({ t: 'quote', ...inline(q.join(' ')) });
    continue;
  }
  para.push(line.trim());
  i++;
}
flush();
fs.writeFileSync(out, JSON.stringify(blocks), 'utf8');
console.log(`${blocks.length} blocks`);
