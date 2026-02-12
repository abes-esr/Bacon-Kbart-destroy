$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$buildDir = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$distDir = Join-Path $root "dist"
$libDir = Join-Path $distDir "lib"
$manifestPath = Join-Path $buildDir "MANIFEST.MF"

if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
if (Test-Path $distDir) { Remove-Item $distDir -Recurse -Force }

New-Item -ItemType Directory -Path $classesDir | Out-Null
New-Item -ItemType Directory -Path $libDir | Out-Null

$classpath = "ojdbc8.jar;log4j-api-2.24.1.jar;log4j-core-2.24.1.jar;jar/org-apache-commons-lang.jar;jar/activation-1.1.jar"

javac -cp $classpath -d $classesDir *.java

$manifest = @"
Main-Class: EffacerPackageUI
Class-Path: lib/ojdbc8.jar lib/log4j-api-2.24.1.jar lib/log4j-core-2.24.1.jar lib/org-apache-commons-lang.jar lib/activation-1.1.jar

"@
Set-Content -Path $manifestPath -Value $manifest -Encoding Ascii

jar cfm (Join-Path $distDir "EffacerPackageUI-autonome.jar") $manifestPath -C $classesDir .

Copy-Item "ojdbc8.jar" $libDir
Copy-Item "log4j-api-2.24.1.jar" $libDir
Copy-Item "log4j-core-2.24.1.jar" $libDir
Copy-Item "jar/org-apache-commons-lang.jar" $libDir
Copy-Item "jar/activation-1.1.jar" $libDir
Copy-Item "config.properties-exemple" (Join-Path $distDir "config.properties-exemple")

if (Test-Path "config.properties") {
    Copy-Item "config.properties" (Join-Path $distDir "config.properties")
}

$bat = @"
@echo off
setlocal
cd /d "%~dp0"

if not exist config.properties (
  echo [ERREUR] Fichier config.properties manquant.
  echo Copiez config.properties-exemple en config.properties puis renseignez les acces DB.
  exit /b 1
)

java -jar EffacerPackageUI-autonome.jar
endlocal
"@
Set-Content -Path (Join-Path $distDir "run-ui.bat") -Value $bat -Encoding Ascii

$sh = @'
#!/bin/bash
set -e
cd "$( cd "$( dirname "$0" )" && pwd )"

if [ ! -f config.properties ]; then
  echo "[ERREUR] Fichier config.properties manquant."
  echo "Copiez config.properties-exemple en config.properties puis renseignez les acces DB."
  exit 1
fi

java -jar EffacerPackageUI-autonome.jar
'@
Set-Content -Path (Join-Path $distDir "run-ui.sh") -Value $sh -Encoding Ascii

Write-Host "Package autonome genere dans: $distDir"
Write-Host "Lancement Windows: $distDir\run-ui.bat"
