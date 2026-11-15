#!/usr/bin/env bash
# Compila en la JVM (Kotlin 1.9.24, sin Android) el núcleo de trazabilidad y sus pruebas, y las corre.
# Uso: herramientas/jvm/probar.sh      (necesita haber corrido docs/banco/calificar.sh una vez para bajar kotlinc)
set -euo pipefail
raiz=$(cd "$(dirname "$0")/../.." && pwd)
kc="${XDG_CACHE_HOME:-$HOME/.cache}/checa-banco/kotlinc/bin/kotlinc"
[ -x "$kc" ] || { echo "Falta kotlinc: corre docs/banco/calificar.sh primero"; exit 2; }
m="$raiz/app/src/main/java/com/karen/miruta"; t="$raiz/app/src/test/java/com/karen/miruta"
out=$(mktemp -d)
"$kc" "$m/Parser.kt" "$m/Moneda.kt" "$m/Tarjetas.kt" "$m/Traza.kt" "$raiz/docs/banco/Stub.kt" \
  "$t/TrazaTest.kt" "$raiz/herramientas/jvm/Muestras.kt" "$raiz/herramientas/jvm/Medir.kt" "$t/GuardiaParserTest.kt" "$raiz/herramientas/jvm/junit_mini.kt" "$raiz/herramientas/jvm/Ejecutor.kt" \
  -include-runtime -d "$out/t.jar" 2>&1 | grep -E "error:|warning: unre" || true
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp "$out/t.jar" EjecutorKt com.karen.miruta.TrazaTest com.karen.miruta.GuardiaParserTest 2>&1 | grep -v "^Picked up"
if [ "${1:-}" = "--muestras" ]; then java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp "$out/t.jar" com.karen.miruta.MuestrasKt > "$2"; echo "muestras: $2"; fi
if [ "${1:-}" = "--medir" ] || [ "${3:-}" = "--medir" ]; then java -Dstdout.encoding=UTF-8 -cp "$out/t.jar" com.karen.miruta.MedirKt; fi
