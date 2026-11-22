<#
.SYNOPSIS
    Builds the technical report PDF from docs/report/relatorio.md (F-017).

.DESCRIPTION
    Wraps the Markdown in an HTML page (rendered by marked.js, print CSS for A4) and prints it to
    PDF with Microsoft Edge in headless mode. Output: docs/report/AMOV2627.2018013532.2018044613_Relatorio.pdf.
    Needs an internet connection the first time (marked.js comes from cdn.jsdelivr.net).

.EXAMPLE
    .\scripts\build-report.ps1
#>
param(
    [string]$Source = "docs/report/relatorio.md",
    [string]$Output = "docs/report/AMOV2627.2018013532.2018044613_Relatorio.pdf"
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$src = if ([IO.Path]::IsPathRooted($Source)) { $Source } else { Join-Path $root $Source }
$out = if ([IO.Path]::IsPathRooted($Output)) { $Output } else { Join-Path $root $Output }
$html = Join-Path (Split-Path $src -Parent) "relatorio.html"

$edge = @(
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $edge) { throw "Microsoft Edge not found" }

$md = Get-Content $src -Raw -Encoding UTF8
# Keep the Markdown verbatim inside a script tag; only a closing script tag could break it.
$md = $md -replace '</script', '<\/script'

$page = @"
<!doctype html>
<html lang="pt-PT">
<head>
<meta charset="utf-8">
<title>CityFix - Relat&oacute;rio t&eacute;cnico</title>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<style>
  @page { size: A4; margin: 20mm 18mm 20mm 18mm; }
  body { font-family: "Segoe UI", Calibri, Arial, sans-serif; font-size: 10.5pt; line-height: 1.45; color: #1a1a1a; }
  h1 { font-size: 20pt; margin: 0 0 10pt; color: #1f3a68; }
  h2 { font-size: 15pt; margin: 18pt 0 8pt; color: #1f3a68; border-bottom: 1px solid #c9d3e3; padding-bottom: 3pt; }
  h3 { font-size: 12pt; margin: 14pt 0 6pt; color: #24456f; }
  h4 { font-size: 11pt; margin: 10pt 0 4pt; }
  h2, h3, h4 { break-after: avoid; }
  p, li { orphans: 3; widows: 3; }
  table { border-collapse: collapse; width: 100%; margin: 8pt 0; font-size: 9.5pt; break-inside: auto; }
  tr { break-inside: avoid; }
  th, td { border: 1px solid #b9c3d3; padding: 3pt 5pt; vertical-align: top; text-align: left; }
  th { background: #e8eef7; }
  code { font-family: Consolas, "Courier New", monospace; font-size: 9pt; background: #f2f4f7; padding: 0 2pt; }
  pre { background: #f2f4f7; padding: 6pt; font-size: 8.5pt; white-space: pre-wrap; break-inside: avoid; }
  pre code { background: none; padding: 0; }
  blockquote { margin: 8pt 0; padding: 14pt; border: 1.5px dashed #8a9bb8; background: #f7f9fc; color: #4a5a75; text-align: center; }
  img { max-width: 100%; display: block; margin: 8pt auto; break-inside: avoid; }
  .page-break { break-after: page; }
</style>
</head>
<body>
<div id="content"></div>
<script id="md" type="text/markdown">
$md
</script>
<script>
  document.getElementById('content').innerHTML = marked.parse(document.getElementById('md').textContent);
</script>
</body>
</html>
"@
Set-Content -Path $html -Value $page -Encoding UTF8

$uri = ([Uri](Resolve-Path $html).Path).AbsoluteUri
if (Test-Path $out) { Remove-Item $out }
# Start-Process: Edge logs harmless messages on stderr, which would stop the script otherwise.
$edgeArgs = @('--headless=new', '--disable-gpu', '--no-pdf-header-footer', '--virtual-time-budget=10000',
    "`"--print-to-pdf=$out`"", $uri)
Start-Process -FilePath $edge -ArgumentList $edgeArgs -Wait -WindowStyle Hidden
if (-not (Test-Path $out)) { throw "PDF not created: $out" }
Remove-Item $html
Get-Item $out | Select-Object FullName, Length, LastWriteTime
