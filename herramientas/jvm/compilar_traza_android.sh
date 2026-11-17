#!/usr/bin/env bash
# Comprueba que TrazaAndroid.kt compila contra stubs de tipos de Android (sin SDK). No es la compilación real.
set -euo pipefail
raiz=$(cd "$(dirname "$0")/../.." && pwd); m="$raiz/app/src/main/java/com/karen/miruta"; out=$(mktemp -d)
kc="${XDG_CACHE_HOME:-$HOME/.cache}/checa-banco/kotlinc/bin/kotlinc"
"$kc" "$m/Parser.kt" "$m/Moneda.kt" "$m/Tarjetas.kt" "$m/Traza.kt" "$m/TrazaAndroid.kt" "$raiz"/herramientas/jvm/stubs_android/*.kt -d "$out" 2>&1 \
  | grep -E "error:" && exit 1 || echo "TrazaAndroid.kt compila contra stubs (sin errores)"
