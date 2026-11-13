#!/usr/bin/env bash
# Califica el lector (Parser.kt) contra el banco de ofertas reales, sin Android ni Gradle.
# Uso: docs/banco/calificar.sh [commit]   (sin commit = el código como está ahora)
# Baja el compilador de Kotlin 1.9.24 desde GitHub la primera vez (~90 MB, fuera del repo).
set -euo pipefail
raiz=$(cd "$(dirname "$0")/../.." && pwd)
cache="${XDG_CACHE_HOME:-$HOME/.cache}/checa-banco"
mkdir -p "$cache"
if [ ! -x "$cache/kotlinc/bin/kotlinc" ]; then
  curl -sSL -o "$cache/kc.zip" https://github.com/JetBrains/kotlin/releases/download/v1.9.24/kotlin-compiler-1.9.24.zip
  (cd "$cache" && unzip -q kc.zip && mv kc.zip kc.zip.usado)
fi
m=app/src/main/java/com/karen/miruta
src="$cache/src"; mkdir -p "$src"
for f in Parser Moneda Tarjetas; do
  if [ $# -ge 1 ]; then git -C "$raiz" show "$1:$m/$f.kt" > "$src/$f.kt"; else cp "$raiz/$m/$f.kt" "$src/$f.kt"; fi
done
"$cache/kotlinc/bin/kotlinc" "$src"/Parser.kt "$src"/Moneda.kt "$src"/Tarjetas.kt \
  "$raiz/app/src/test/java/com/karen/miruta/Banco.kt" "$raiz/docs/banco/Stub.kt" "$raiz/docs/banco/Main.kt" \
  -include-runtime -d "$cache/banco.jar" 2>&1 | grep -E "error:" || true
java -jar "$cache/banco.jar" "$raiz/app/src/test/resources/banco" "$cache/fallas.txt" 2>&1 | grep -v "^Picked up"
echo "Fallas: $cache/fallas.txt"
