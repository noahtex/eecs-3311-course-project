param(
    [string]$PlantUmlJar = (Join-Path $PSScriptRoot 'tools/plantuml.jar')
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $PlantUmlJar)) {
    throw 'Provide -PlantUmlJar with a local PlantUML 1.2025.10 JAR. See README.md.'
}
$sources = Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.puml' |
    Where-Object { $_.Name -ne 'style.puml' } |
    Sort-Object Name
foreach ($source in $sources) {
    & java '-Djava.awt.headless=true' -jar $PlantUmlJar -charset UTF-8 -failfast2 -tsvg $source.FullName
    if ($LASTEXITCODE -ne 0) { throw "SVG rendering failed: $($source.Name)" }
    & java '-Djava.awt.headless=true' -jar $PlantUmlJar -charset UTF-8 -failfast2 -tpng $source.FullName
    if ($LASTEXITCODE -ne 0) { throw "PNG rendering failed: $($source.Name)" }
}
Write-Output "Rendered $($sources.Count) UML sources to SVG and PNG."
