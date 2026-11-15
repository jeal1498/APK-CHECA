#!/usr/bin/env bash
# E5: reproduce el lector de una versión sobre entradas ciegas.
# Uso: auditoria/reproduccion/reproducir.sh <commit|actual|DIR_FUENTES> <entradas_dir> <salida_dir>
#   commit       = Parser.kt, Moneda.kt y Tarjetas.kt de ese commit (git show)
#   actual       = los del árbol de trabajo
#   DIR_FUENTES  = una carpeta con esos tres archivos (p. ej. una copia mutada)
# Escribe <salida_dir>/predicciones.jsonl y <salida_dir>/corrida.json (huellas de todo lo que entra).
set -euo pipefail
raiz=$(cd "$(dirname "$0")/../.." && pwd)
ref=$1; ent=$(cd "$2" && pwd); sal=$3
kc="${XDG_CACHE_HOME:-$HOME/.cache}/checa-banco/kotlinc/bin/kotlinc"
[ -x "$kc" ] || { echo "Falta kotlinc: corre docs/banco/calificar.sh una vez"; exit 2; }
mkdir -p "$sal"; sal=$(cd "$sal" && pwd)
src=$(mktemp -d); m=app/src/main/java/com/karen/miruta
for f in Parser Moneda Tarjetas; do
  if [ "$ref" = actual ]; then cp "$raiz/$m/$f.kt" "$src/"
  elif [ -d "$ref" ]; then cp "$ref/$f.kt" "$src/"
  else git -C "$raiz" show "$ref:$m/$f.kt" > "$src/$f.kt"; fi
done
# Traza.Json para serializar (no depende del lector); Stub.kt = Prefs mínimo que usa Parser.
"$kc" "$src/Parser.kt" "$src/Moneda.kt" "$src/Tarjetas.kt" "$raiz/$m/Traza.kt" "$raiz/docs/banco/Stub.kt" \
  "$raiz/auditoria/reproduccion/Reproducir.kt" -include-runtime -d "$src/r.jar" 2>&1 | grep -E "error:" && exit 3 || true
java -Dfile.encoding=UTF-8 -cp "$src/r.jar" ReproducirKt "$ent" "$sal/predicciones.jsonl" | grep -v "^Picked up" >&2
python3 - "$ref" "$src" "$ent" "$sal" <<'PY'
import hashlib, json, os, subprocess, sys
ref, src, ent, sal = sys.argv[1:]
h = lambda p: hashlib.sha256(open(p, "rb").read()).hexdigest()
fuentes = {f: h(os.path.join(src, f + ".kt")) for f in ("Parser", "Moneda", "Tarjetas")}
entradas = hashlib.sha256("".join(f"{f}\t{h(os.path.join(ent, f))}\n" for f in sorted(os.listdir(ent)) if f.endswith(".txt")).encode()).hexdigest()
java = subprocess.run(["java", "-version"], capture_output=True, text=True).stderr.splitlines()
java = next((l for l in java if "version" in l), "?")
c = {"ref": ref if not os.path.isdir(ref) else "dir:" + os.path.basename(os.path.normpath(ref)), "fuentes_sha256": fuentes,
     "entradas_sha256": entradas, "n_entradas": sum(f.endswith(".txt") for f in os.listdir(ent)),
     "predicciones_sha256": h(os.path.join(sal, "predicciones.jsonl")), "java": java, "kotlinc": "1.9.24"}
json.dump(c, open(os.path.join(sal, "corrida.json"), "w"), indent=2, sort_keys=True)
print(json.dumps({k: c[k] for k in ("ref", "n_entradas", "predicciones_sha256")}))
PY
