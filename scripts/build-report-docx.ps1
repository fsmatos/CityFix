<#
.SYNOPSIS
    Builds the technical report in the ISEC Word template (F-017) and exports it to PDF.

.DESCRIPTION
    1. node scripts/report-md-to-json.js turns docs/report/relatorio.md into blocks.
    2. Word (COM) opens a copy of the ISEC template, fills the cover, replaces the pre-textual
       part (Resumo, Índice, Índice de figuras, Índice de quadros, Lista de siglas) and the body
       with the report, using the template's styles (Section, Título 1-3, Corpo de texto,
       Corpo de texto 2, Legenda, Source Code) and numbered captions ("Figura 4.1", "Quadro 2.1").
    3. Updates the indexes and exports the PDF.
    Needs Microsoft Word and Node.js.

.EXAMPLE
    .\scripts\build-report-docx.ps1 -Template D:\Amov\Template_MSWord_PT_EN_v2026_Licenciaturas.docx
#>
param(
    [Parameter(Mandatory = $true)][string]$Template,
    [string]$Source = "docs/report/relatorio.md",
    [string]$Docx = "docs/report/AMOV2627.2018013532.2018044613_Relatorio.docx",
    [string]$Pdf = "docs/report/AMOV2627.2018013532.2018044613_Relatorio.pdf"
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
function Resolve-RepoPath([string]$p) { if ([IO.Path]::IsPathRooted($p)) { $p } else { Join-Path $root $p } }
$src = Resolve-RepoPath $Source
$docxPath = Resolve-RepoPath $Docx
$pdfPath = Resolve-RepoPath $Pdf
$json = Join-Path $env:TEMP "cityfix-report-blocks.json"

& node (Join-Path $PSScriptRoot "report-md-to-json.js") $src $json
if ($LASTEXITCODE -ne 0) { throw "report-md-to-json.js failed" }
$blocks = Get-Content $json -Raw -Encoding UTF8 | ConvertFrom-Json

Copy-Item $Template $docxPath -Force

# Text that is not in relatorio.md: the cover, the abstract and the acronyms.
$title = "CityFix: aplicação Android de participação cívica"
$workType = "Relatório técnico do trabalho prático de Arquiteturas Móveis (2026/27)"
$authors = "Diogo Abrantes (n.º 2018013532)" + [char]11 + "Francisco Matos (n.º 2018044613)"
$date = "Coimbra, novembro de 2026"
$header = "CityFix: relatório técnico"
$resumo = @(
    "O CityFix é uma aplicação Android, desenvolvida em Kotlin com Jetpack Compose, para participação cívica num município. Os munícipes reportam, georreferenciam e acompanham ocorrências no espaço público, com fotografias e vídeos; os operadores municipais validam registos, gerem categorias e conduzem cada ocorrência pelo seu ciclo de vida, de Aberto a Encerrado.",
    "A aplicação segue a arquitetura MVVM com ecrãs stateless e stateful e usa o Firebase (Authentication, Cloud Firestore com cache offline e Cloud Storage). Os carregamentos de multimédia são feitos pelo WorkManager, com compressão e miniaturas. A localização serve para o limite de 10 km dos utilizadores anónimos, para agrupar participações a menos de 10 m e para avisar o munícipe quando a equipa municipal está a menos de 20 m. O acelerómetro sugere um reporte quando deteta um impacto forte.",
    "Este relatório descreve a arquitetura, o modelo de dados, as funcionalidades por perfil, o cumprimento do enunciado, as soluções técnicas, as opções tomadas e as suas limitações, e as oportunidades de melhoria."
)
$keywords = "Palavras-chave: Android, Kotlin, Jetpack Compose, Firebase, participação cívica, geolocalização."
$siglas = @(
    @("API", "Application Programming Interface"),
    @("APK", "Android Package"),
    @("FCM", "Firebase Cloud Messaging"),
    @("GPS", "Global Positioning System"),
    @("ISEC", "Instituto Superior de Engenharia de Coimbra"),
    @("MFA", "Multi-Factor Authentication"),
    @("MVVM", "Model-View-ViewModel"),
    @("NIF", "Número de Identificação Fiscal"),
    @("SDK", "Software Development Kit"),
    @("SMS", "Short Message Service"),
    @("UI", "User Interface")
)

$w = New-Object -ComObject Word.Application
$w.Visible = $false
$w.DisplayAlerts = 0
$o = $w.Options
$saved = @{
    Quotes = $o.AutoFormatAsYouTypeReplaceQuotes; Symbols = $o.AutoFormatAsYouTypeReplaceSymbols
    Bullets = $o.AutoFormatAsYouTypeApplyBulletedLists; Numbers = $o.AutoFormatAsYouTypeApplyNumberedLists
    Links = $o.AutoFormatAsYouTypeReplaceHyperlinks; Ordinals = $o.AutoFormatAsYouTypeReplaceOrdinals
    Fractions = $o.AutoFormatAsYouTypeReplaceFractions; Replace = $w.AutoCorrect.ReplaceText
    Spell = $o.CheckSpellingAsYouType; Grammar = $o.CheckGrammarAsYouType
}
$o.AutoFormatAsYouTypeReplaceQuotes = $false; $o.AutoFormatAsYouTypeReplaceSymbols = $false
$o.AutoFormatAsYouTypeApplyBulletedLists = $false; $o.AutoFormatAsYouTypeApplyNumberedLists = $false
$o.AutoFormatAsYouTypeReplaceHyperlinks = $false; $o.AutoFormatAsYouTypeReplaceOrdinals = $false
$o.AutoFormatAsYouTypeReplaceFractions = $false; $w.AutoCorrect.ReplaceText = $false

try {
    $d = $w.Documents.Open($docxPath)
    # Faster: no spelling or background pagination while the document is built.
    $o.CheckSpellingAsYouType = $false; $o.CheckGrammarAsYouType = $false; $o.Pagination = $false
    $w.ScreenUpdating = $false
    $d.ShowSpellingErrors = $false; $d.ShowGrammaticalErrors = $false
    $sel = $w.Selection

    # ---------- Cover ----------
    foreach ($cc in @($d.ContentControls)) {
        switch -Regex ($cc.Title + "|" + $cc.Range.Text) {
            '^Nome do Departamento' {
                foreach ($e in $cc.DropdownListEntries) { if ($e.Text -eq 'ENGENHARIA INFORMÁTICA E DE SISTEMAS') { $e.Select() } }
            }
        }
    }
    # Paragraph-level replacements (content controls removed, text kept plain).
    function Set-ParaText($para, [string]$text) {
        foreach ($cc in @($para.Range.ContentControls)) { $cc.LockContentControl = $false; $cc.Delete($false) }
        $r = $para.Range
        $r.MoveEnd(1, -1) | Out-Null
        $r.Text = $text
    }
    $cover = $d.Sections(1).Range
    $paras = @($cover.Paragraphs)
    foreach ($p in $paras) {
        $t = $p.Range.Text
        if ($t -like 'Título do trabalho*') { Set-ParaText $p $title }
        elseif ($t -like 'Escolha um item. em*') { Set-ParaText $p $workType; $p.Range.Font.Name = 'Arial' }
        elseif ($t -like 'Autor / Author*') { Set-ParaText $p 'Autores' }
        elseif ($t -like 'Nome completo do autor*') { Set-ParaText $p $authors; $p.Alignment = 0 }
        elseif ($t -like 'Coimbra, mês e ano*') { Set-ParaText $p $date }
    }
    # No supervisors: remove the supervisor blocks (from the first "Escolha um item." after the
    # authors to the paragraph before the date).
    $cover = $d.Sections(1).Range
    $start = $null; $end = $null
    foreach ($p in @($cover.Paragraphs)) {
        $t = $p.Range.Text
        if ($null -eq $start -and $t -like 'Escolha um item.*') { $start = $p.Range.Start }
        if ($t -like 'Nome completo do supervisor*') { $end = $p.Range.End }
    }
    if ($null -ne $start -and $null -ne $end) {
        $r = $d.Range($start, $end)
        foreach ($cc in @($r.ContentControls)) { $cc.LockContentControl = $false; $cc.Delete($true) }
        $d.Range($start, $d.Range($start, $start).Paragraphs(1).Range.Start + 0) | Out-Null
        # recompute end after deleting controls
        $end2 = $null
        foreach ($p in @($d.Sections(1).Range.Paragraphs)) { if ($p.Range.Text -like 'Coimbra,*') { $end2 = $p.Range.Start } }
        $d.Range($start, $end2).Delete() | Out-Null
        # keep the date low on the page with a few empty lines
        $d.Range($start, $start).InsertBefore("`r`r`r`r`r")
    }

    try { $d.BuiltInDocumentProperties.Item('Title').Value = $title } catch { }
    # Header text in every section: odd pages get the title, even pages the authors.
    foreach ($s in $d.Sections) {
        foreach ($i in 1..3) {
            $h = $s.Headers($i)
            if (-not $h.Exists) { continue }
            foreach ($pair in @(@('Implementação das regras de formatação em Microsoft® Word', $header), @('Conselho Pedagógico | Gabinete de Qualidade e Planeamento', 'Diogo Abrantes e Francisco Matos'))) {
                $f = $h.Range.Find
                $f.Execute($pair[0], $false, $false, $false, $false, $false, $true, 0, $false, $pair[1], 2) | Out-Null
            }
        }
    }
    # ---------- Helpers ----------
    # Everything is typed just before an end marker paragraph (bookmark "fim"), so the cursor never
    # leaves the section, whatever Word does with pictures, fields and tables.
    $log = Join-Path $env:TEMP "cityfix-report-build.log"
    Set-Content $log "start" -Encoding UTF8
    function Start-Section($sec, [string]$name) {
        $r = $d.Range($sec.Range.Start, $sec.Range.End - 1)
        $r.Delete() | Out-Null
        $m = $d.Range($sec.Range.Start, $sec.Range.Start)
        $m.InsertBefore("#FIM#")
        $m = $d.Range($sec.Range.Start, $sec.Range.Start + 5)
        $d.Bookmarks.Add($name, $m) | Out-Null
        $script:marker = $name
    }
    function To-Marker() {
        $m = $d.Bookmarks.Item($script:marker).Range
        $sel.SetRange($m.Start, $m.Start)
    }
    function End-Section() {
        # The bookmark can grow over inserted text: remove only the marker characters.
        $r = $d.Content
        if ($r.Find.Execute('#FIM#')) { $r.Text = '' }
    }
    function Apply-Spans([int]$base, $spans) {
        foreach ($sp in $spans) {
            $r = $d.Range($base + $sp.s, $base + $sp.e)
            switch ($sp.k) {
                'b' { $r.Font.Bold = $true }
                'i' { $r.Font.Italic = $true }
                'c' { $r.Font.Name = 'Consolas'; $r.Font.Size = 9 }
            }
        }
    }
    function Add-Para([string]$style, [string]$text, $spans = @()) {
        To-Marker
        $sel.Range.ListFormat.RemoveNumbers()
        $sel.Style = $d.Styles.Item($style)
        $sel.Font.Reset()
        $base = $sel.Start
        if ($text) { $sel.TypeText($text) }
        $sel.TypeParagraph()
        if ($spans) { Apply-Spans $base $spans }
        return $d.Range($base, $base).Paragraphs(1)
    }
    function Add-Caption([string]$label, [string]$text) {
        $p = Add-Para 'Legenda' ("$label @CH@.@N@ – $text")
        foreach ($tok in @(@('@CH@', 'STYLEREF 1 \s'), @('@N@', "SEQ $label \* ARABIC \s 1"))) {
            $r = $p.Range.Duplicate
            $f = $r.Find
            $f.ClearFormatting()
            if ($f.Execute($tok[0], $true, $false, $false, $false, $false, $true, 0)) { $d.Fields.Add($r, -1, $tok[1], $false) | Out-Null }
            else { Add-Content $log "caption token not found: $text" }
        }
    }
    function Add-Heading([string]$style, [string]$text, $spans, [bool]$newPage) {
        $p = Add-Para $style $text $spans
        if ($newPage -and -not $d.Styles.Item($style).ParagraphFormat.PageBreakBefore) { $p.PageBreakBefore = $true }
        return $p
    }

    # ---------- Pre-textual part (section 2): one HTML import too ----------
    function Esc([string]$t) { [Net.WebUtility]::HtmlEncode($t) }
    $pre = New-Object Text.StringBuilder
    [void]$pre.Append('<html><head><meta charset="utf-8"></head><body>')
    [void]$pre.Append('<h1>Resumo</h1>')
    foreach ($r in $resumo) { [void]$pre.Append("<p>$(Esc $r)</p>") }
    [void]$pre.Append("<p><b>Palavras-chave:</b> $(Esc ($keywords -replace '^Palavras-chave:\s*',''))</p>")
    [void]$pre.Append('<h1>Índice</h1><p>@TOC@</p>')
    [void]$pre.Append('<h1>Índice de figuras</h1><p>@TOF@</p>')
    [void]$pre.Append('<h1>Índice de quadros</h1><p>@TOQ@</p>')
    [void]$pre.Append('<h1>Lista de siglas e acrónimos</h1><table>')
    foreach ($s2 in $siglas) { [void]$pre.Append("<tr><td>$(Esc $s2[0])</td><td>$(Esc $s2[1])</td></tr>") }
    [void]$pre.Append('</table></body></html>')
    $preHtml = Join-Path $env:TEMP "cityfix-report-pre.html"
    [IO.File]::WriteAllText($preHtml, $pre.ToString(), (New-Object Text.UTF8Encoding $false))

    Start-Section $d.Sections(2) 'fim2'
    To-Marker
    $sel.InsertFile($preHtml, '', $false, $false, $false)
    End-Section
    $first = $true
    foreach ($p in @($d.Sections(2).Range.Paragraphs)) {
        $name = $p.Style.NameLocal
        if ($name -like 'Título 1*' -or $name -like 'Heading 1*') {
            $p.Style = $d.Styles.Item('Section')
            if (-not $first -and -not $d.Styles.Item('Section').ParagraphFormat.PageBreakBefore) { $p.PageBreakBefore = $true }
            $first = $false
        }
        elseif ($p.Range.Information(12)) { }  # inside the acronyms table
        else { $p.Style = $d.Styles.Item('Corpo de texto') }
    }
    $t = $d.Sections(2).Range.Tables(1)
    $t.Range.Style = $d.Styles.Item('Corpo de texto 2')
    $t.Borders.Enable = $false
    $t.Columns(1).Width = 70; $t.Columns(2).Width = 380
    foreach ($pair in @(@('@TOC@', 'toc'), @('@TOF@', 'Figura'), @('@TOQ@', 'Quadro'))) {
        $r = $d.Sections(2).Range
        if ($r.Find.Execute($pair[0])) {
            $r.Text = ''
            if ($pair[1] -eq 'toc') {
                $toc = $d.TablesOfContents.Add($r, $true, 1, 3, $false, '', $true, $true, '', $true)
                $toc.HeadingStyles.Add('Section', 1) | Out-Null
                $toc.HeadingStyles.Add('Section 2', 2) | Out-Null
            }
            else { $d.TablesOfFigures.Add($r, $pair[1], $true, $true) | Out-Null }
        }
    }
    Add-Content $log ("{0:HH:mm:ss} pre-textual done" -f (Get-Date))

    # ---------- Body (section 3): one HTML import, then styles and captions ----------
    $html = Join-Path $env:TEMP "cityfix-report-body.html"
    & node (Join-Path $PSScriptRoot "report-json-to-html.js") $json $html
    if ($LASTEXITCODE -ne 0) { throw "report-json-to-html.js failed" }
    Start-Section $d.Sections(3) 'fim3'
    To-Marker
    $sel.InsertFile($html, '', $false, $false, $false)
    End-Section
    Add-Content $log ("{0:HH:mm:ss} html imported" -f (Get-Date))
    $body = $d.Sections(3).Range

    # Imported HTML paragraphs use Normal: switch them to the template's body style in one go.
    function Replace-Style([string]$from, [string]$to) {
        $r = $d.Sections(3).Range
        $f = $r.Find
        $f.ClearFormatting(); $f.Replacement.ClearFormatting()
        $f.Style = $d.Styles.Item($from)
        $f.Replacement.Style = $d.Styles.Item($to)
        $f.Text = ''; $f.Replacement.Text = ''; $f.Format = $true; $f.Wrap = 0
        $f.Execute('', $false, $false, $false, $false, $false, $true, 0, $true, '', 2) | Out-Null
    }
    foreach ($st in @('Normal', 'Normal (Web)', 'Parágrafo da Lista', 'List Paragraph')) {
        try { Replace-Style $st 'Corpo de texto' } catch { }
    }
    Add-Content $log ("{0:HH:mm:ss} styles" -f (Get-Date))

    # Chapters start on a new page.
    $first = $true
    foreach ($p in @($d.Sections(3).Range.Paragraphs)) {
        if ($p.Style.NameLocal -eq 'Título 1') {
            if (-not $first -and -not $d.Styles.Item('Título 1').ParagraphFormat.PageBreakBefore) { $p.PageBreakBefore = $true }
            $first = $false
        }
    }

    # Pictures: embedded (not linked), sized, centred.
    foreach ($s in @($d.Sections(3).Range.InlineShapes)) {
        if ($s.LinkFormat) { $s.LinkFormat.SavePictureWithDocument = $true; $s.LinkFormat.BreakLink() }
    }
    foreach ($s in @($d.Sections(3).Range.InlineShapes)) {
        $s.LockAspectRatio = -1
        if ($s.Width -ge $s.Height) { $s.Width = 400 } else { $s.Height = 300 }
        if ($s.Width -gt 450) { $s.Width = 450 }
        $pp = $s.Range.Paragraphs(1)
        $pp.Alignment = 1; $pp.KeepWithNext = $true; $pp.FirstLineIndent = 0
    }
    Add-Content $log ("{0:HH:mm:ss} pictures" -f (Get-Date))

    # Tables: template table style, small body text, bold shaded header that repeats.
    foreach ($t in @($d.Sections(3).Range.Tables)) {
        $t.Style = 'Tabela com Grelha'
        $t.Range.Style = $d.Styles.Item('Corpo de texto 2')
        $t.Rows(1).Range.Font.Bold = $true
        $t.Rows(1).Shading.BackgroundPatternColor = 15132390
        $t.Rows(1).HeadingFormat = $true
        $t.PreferredWidthType = 2; $t.PreferredWidth = 100
        $t.Rows.AllowBreakAcrossPages = $true
    }
    Add-Content $log ("{0:HH:mm:ss} tables" -f (Get-Date))

    # Captions: "@FIG@ text" -> "Figura <chapter>.<n> – text" with STYLEREF/SEQ fields.
    foreach ($pair in @(@('@FIG@', 'Figura'), @('@TAB@', 'Quadro'))) {
        $tok = $pair[0]; $label = $pair[1]
        while ($true) {
            $r = $d.Sections(3).Range
            $f = $r.Find
            $f.ClearFormatting()
            if (-not $f.Execute($tok)) { break }
            $para = $r.Paragraphs(1)
            $para.Style = $d.Styles.Item('Legenda')
            $r.Text = "$label @CH@.@N@ –"
            foreach ($t2 in @(@('@CH@', 'STYLEREF 1 \s'), @('@N@', "SEQ $label \* ARABIC \s 1"))) {
                $rr = $para.Range.Duplicate
                if ($rr.Find.Execute($t2[0])) { $d.Fields.Add($rr, -1, $t2[1], $false) | Out-Null }
            }
            if ($label -eq 'Quadro') { $para.KeepWithNext = $true }
        }
    }
    Add-Content $log ("{0:HH:mm:ss} body done" -f (Get-Date))

    # ---------- Indexes, fields, save ----------
    foreach ($x in $d.TablesOfContents) { $x.Update() }
    foreach ($x in $d.TablesOfFigures) { $x.Update() }
    $o.Pagination = $true
    $d.Repaginate()
    foreach ($x in $d.TablesOfContents) { $x.UpdatePageNumbers() }
    foreach ($x in $d.TablesOfFigures) { $x.UpdatePageNumbers() }
    Add-Content $log "indexes done"
    $d.Save()
    "pages: " + $d.ComputeStatistics(2)
    $d.Close($false)
}
finally {
    $o.AutoFormatAsYouTypeReplaceQuotes = $saved.Quotes; $o.AutoFormatAsYouTypeReplaceSymbols = $saved.Symbols
    $o.AutoFormatAsYouTypeApplyBulletedLists = $saved.Bullets; $o.AutoFormatAsYouTypeApplyNumberedLists = $saved.Numbers
    $o.AutoFormatAsYouTypeReplaceHyperlinks = $saved.Links; $o.AutoFormatAsYouTypeReplaceOrdinals = $saved.Ordinals
    $o.AutoFormatAsYouTypeReplaceFractions = $saved.Fractions; $w.AutoCorrect.ReplaceText = $saved.Replace
    $o.CheckSpellingAsYouType = $saved.Spell; $o.CheckGrammarAsYouType = $saved.Grammar; $o.Pagination = $true
    $w.Quit()
}
# Export in a separate PowerShell process with its own Word: exporting from this process hung,
# even with a second Word instance.
Get-Process WINWORD -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -eq '' } | Stop-Process -Force
$exportCmd = "`$w = New-Object -ComObject Word.Application; `$w.Visible = `$false; `$w.DisplayAlerts = 0; " +
    "`$d = `$w.Documents.Open('$docxPath', `$false, `$true); " +
    "`$d.ExportAsFixedFormat('$pdfPath', 17, `$false, 0, 0, 1, 1, 0, `$true, `$false, 0); `$d.Close(`$false); `$w.Quit()"
Start-Process powershell -ArgumentList '-NoProfile', '-Command', $exportCmd -Wait -WindowStyle HiddenGet-Item $docxPath, $pdfPath | Select-Object Name, Length, LastWriteTime
