#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

BUILD_DIR="$ROOT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
DIST_DIR="$ROOT_DIR/dist"
LIB_DIR="$DIST_DIR/lib"
MANIFEST_PATH="$BUILD_DIR/MANIFEST.MF"

rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$CLASSES_DIR" "$LIB_DIR"

CLASSPATH="ojdbc8.jar:log4j-api-2.24.1.jar:log4j-core-2.24.1.jar:jar/org-apache-commons-lang.jar:jar/activation-1.1.jar"

javac -cp "$CLASSPATH" -d "$CLASSES_DIR" ./*.java

cat > "$MANIFEST_PATH" <<'EOF'
Main-Class: EffacerPackageUI
Class-Path: lib/ojdbc8.jar lib/log4j-api-2.24.1.jar lib/log4j-core-2.24.1.jar lib/org-apache-commons-lang.jar lib/activation-1.1.jar

EOF

jar cfm "$DIST_DIR/EffacerPackageUI-autonome.jar" "$MANIFEST_PATH" -C "$CLASSES_DIR" .

cp "ojdbc8.jar" "$LIB_DIR/"
cp "log4j-api-2.24.1.jar" "$LIB_DIR/"
cp "log4j-core-2.24.1.jar" "$LIB_DIR/"
cp "jar/org-apache-commons-lang.jar" "$LIB_DIR/"
cp "jar/activation-1.1.jar" "$LIB_DIR/"
cp "config.properties-exemple" "$DIST_DIR/config.properties-exemple"

if [ -f "config.properties" ]; then
  cp "config.properties" "$DIST_DIR/config.properties"
fi

cat > "$DIST_DIR/run-ui.sh" <<'EOF'
#!/bin/bash
set -e
cd "$( cd "$( dirname "$0" )" && pwd )"

if [ ! -f config.properties ]; then
  echo "[ERREUR] Fichier config.properties manquant."
  echo "Copiez config.properties-exemple en config.properties puis renseignez les acces DB."
  exit 1
fi

java -jar EffacerPackageUI-autonome.jar
EOF
chmod +x "$DIST_DIR/run-ui.sh"

cat > "$DIST_DIR/run-ui.bat" <<'EOF'
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
EOF

echo "Package autonome genere dans: $DIST_DIR"
echo "Lancement Linux: $DIST_DIR/run-ui.sh"
