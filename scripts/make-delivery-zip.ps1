<#
.SYNOPSIS
    Builds the delivery ZIP AMOV2627.2018013532.2018044613.zip from a git ref and checks it.

.DESCRIPTION
    Steps (D-015, Gate 1 Q6):
      0. With -Ref main: git fetch, and fail if main differs from origin/main.
      1. git archive of -Ref (default main) into a temporary staging folder, as CityFix/.
      2. Copy -GoogleServices to CityFix/app/google-services.json and -Report next to CityFix/.
      3. Fail if .gradle, build, app/build, local.properties or a keystore is in the tree.
      4. Zip with tar.exe, list the entries and check them again, then delete the staging folder.
      5. Unless -SkipBuild: unzip into a second temporary folder and run assembleDebug there.
    Both files are parameters, so no secret is ever committed. The script writes only to
    %TEMP% and -OutDir, never into the repository.

.EXAMPLE
    .\scripts\make-delivery-zip.ps1 -GoogleServices C:\keys\google-services.json -Report C:\docs\Relatorio.pdf
#>
param(
    [Parameter(Mandatory = $true)] [string] $GoogleServices,
    [Parameter(Mandatory = $true)] [string] $Report,
    [string] $Ref = 'main',
    [string] $OutDir = [Environment]::GetFolderPath('Desktop'),
    [switch] $SkipBuild,
    # Gate 1 Q8: leave out .claude/, CLAUDE.md and docs/work/ only if Francisco agrees.
    [switch] $ExcludeAgentFiles
)

$ErrorActionPreference = 'Stop'

function Fail([string] $message) {
    Write-Host "ERROR: $message" -ForegroundColor Red
    # The staging folder holds a copy of google-services.json: say where it is so it can be deleted.
    if ($script:stage) { Write-Host "Staging folder left behind, delete it by hand: $script:stage" }
    exit 1
}

$stage = $null

# --- Parameters -------------------------------------------------------------------------------
if (-not (Test-Path $GoogleServices -PathType Leaf)) { Fail "google-services.json not found: $GoogleServices" }
if (-not (Test-Path $Report -PathType Leaf)) { Fail "report not found: $Report" }
if ([IO.Path]::GetExtension($Report).ToLower() -ne '.pdf') { Fail "the report must be a .pdf: $Report" }
if (-not (Test-Path $OutDir -PathType Container)) { Fail "output folder not found: $OutDir" }
$GoogleServices = (Resolve-Path $GoogleServices).Path
$Report = (Resolve-Path $Report).Path
$OutDir = (Resolve-Path $OutDir).Path

# The repository is the folder above scripts/.
$repo = Split-Path -Parent $PSScriptRoot
$name = 'AMOV2627.2018013532.2018044613'
$pdfName = "${name}_Relatorio.pdf"
$zip = Join-Path $OutDir "$name.zip"
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

$commit = (& git -C $repo rev-parse --verify --quiet $Ref)
if ($LASTEXITCODE -ne 0) { Fail "unknown git ref: $Ref" }

# The local main may be behind origin/main if the last merge was pushed from the other machine.
if ($Ref -eq 'main') {
    & git -C $repo fetch origin
    if ($LASTEXITCODE -ne 0) { Fail 'git fetch failed' }
    $remote = (& git -C $repo rev-parse --verify --quiet 'origin/main')
    if ($remote -and $remote -ne $commit) { Fail 'main differs from origin/main: pull or push first' }
}
Write-Host "Source: $Ref ($commit)"

# --- 1. Export the tracked files of the ref (never the working tree) -------------------------
$stage = New-Item -ItemType Directory -Path (Join-Path $env:TEMP "cfs-$stamp")
$srcZip = Join-Path $stage 'src.zip'
$out = Join-Path $stage 'out'
$archiveArgs = @('-C', $repo, 'archive', '--format=zip', '--prefix=CityFix/', '-o', $srcZip, $Ref)
if ($ExcludeAgentFiles) {
    $archiveArgs += @('--', '.', ':(exclude).claude', ':(exclude)CLAUDE.md', ':(exclude)docs/work')
}
& git @archiveArgs
if ($LASTEXITCODE -ne 0) { Fail 'git archive failed' }
Expand-Archive -Path $srcZip -DestinationPath $out
Remove-Item $srcZip
$project = Join-Path $out 'CityFix'

# --- 2. Add the two files that are not in git --------------------------------------------------
Copy-Item $GoogleServices (Join-Path $project 'app\google-services.json')
Copy-Item $Report (Join-Path $out $pdfName)

# --- 3. Guard: nothing the brief excludes, no signing keys -----------------------------------
foreach ($excluded in @('.gradle', 'build', 'app\build', 'local.properties')) {
    if (Test-Path (Join-Path $project $excluded)) { Fail "excluded path present: CityFix\$excluded" }
}
$forbidden = Get-ChildItem -Path $out -Recurse -Force -File |
    Where-Object { $_.Name -eq 'local.properties' -or $_.Extension -in @('.jks', '.keystore') }
if ($forbidden) { Fail "forbidden file in the tree: $($forbidden[0].FullName)" }

# --- 4. Zip and check the entries ---------------------------------------------------------------
if (Test-Path $zip) { Fail "the ZIP already exists, move or delete it first: $zip" }
& tar.exe -a -c -f $zip -C $out CityFix $pdfName
if ($LASTEXITCODE -ne 0) { Fail 'tar failed' }

$entries = & tar.exe -t -f $zip
if ($LASTEXITCODE -ne 0) { Fail 'cannot list the ZIP' }
foreach ($entry in $entries) {
    if ($entry.Contains('\')) { Fail "entry with a backslash: $entry" }
    if ($entry -match '(^|/)(\.gradle|build)/' -or $entry -match 'local\.properties') {
        Fail "excluded entry in the ZIP: $entry"
    }
}
if ($entries -notcontains 'CityFix/app/google-services.json') { Fail 'google-services.json missing from the ZIP' }
if ($entries -notcontains $pdfName) { Fail 'report PDF missing from the ZIP' }

# The checks passed: remove the staging folder (it holds a copy of google-services.json).
Remove-Item -Recurse -Force $stage.FullName
$stage = $null

# --- 5. Build the unzipped copy, as the grader would -------------------------------------------
if (-not $SkipBuild) {
    $verify = New-Item -ItemType Directory -Path (Join-Path $env:TEMP "cfv-$stamp")
    Expand-Archive -Path $zip -DestinationPath $verify
    # The SDK path comes from this machine's local.properties (read only, never copied).
    $localProps = Join-Path $repo 'local.properties'
    if (-not $env:ANDROID_HOME -and (Test-Path $localProps)) {
        $line = Get-Content $localProps | Where-Object { $_ -like 'sdk.dir=*' } | Select-Object -First 1
        if ($line) { $env:ANDROID_HOME = $line.Substring(8).Replace('\:', ':').Replace('\\', '\') }
    }
    if (-not $env:ANDROID_HOME) { Fail 'ANDROID_HOME is not set and local.properties has no sdk.dir' }
    # gradlew.bat needs Java: fall back to the JDK that ships with Android Studio.
    $studioJbr = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
    if (-not $env:JAVA_HOME -and (Test-Path $studioJbr -PathType Container)) { $env:JAVA_HOME = $studioJbr }
    if (-not $env:JAVA_HOME) { Fail "JAVA_HOME is not set: set it to Android Studio's jbr folder" }
    $verifyProject = Join-Path $verify 'CityFix'
    & (Join-Path $verifyProject 'gradlew.bat') -p $verifyProject assembleDebug
    if ($LASTEXITCODE -ne 0) { Fail "assembleDebug failed in $verifyProject" }
    Write-Host "Build: assembleDebug passed in $verifyProject"
}

# --- Summary -------------------------------------------------------------------------------------
$sizeMb = [Math]::Round((Get-Item $zip).Length / 1MB, 2)
Write-Host "ZIP:     $zip"
Write-Host "Size:    $sizeMb MB"
Write-Host "Source:  $Ref ($commit)"
Write-Host "Entries: $($entries.Count)"
