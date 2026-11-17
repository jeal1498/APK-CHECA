#!/usr/bin/env bash
# E5: evaluación ciega de punta a punta. Las entradas (sin respuestas) van a una carpeta temporal; el lector
# solo recibe esa carpeta. Orden: preparar → sellar → reproducir → comprometer → revelar.
# Uso: auditoria/reproduccion/evaluacion_ciega.sh <commit|actual> <salida_evidencia>
set -euo pipefail
raiz=$(cd "$(dirname "$0")/../.." && pwd); ref=$1; out=$2
ev="$raiz/auditoria/reproduccion/evaluar.py"
tmp=$(mktemp -d); mkdir -p "$out"
python3 "$ev" preparar --corpus "=$raiz/app/src/test/resources/banco:banco" \
  --corpus "of=$raiz/app/src/test/resources/ofertas:pruebas_ofertas" \
  --corpus "sint=$raiz/auditoria/reproduccion/sinteticos:sintetico_regla" --salida "$tmp/c"
# Las etiquetas salen de la carpeta del lector ANTES de correrlo.
mv "$tmp/c/etiquetas.jsonl" "$out/etiquetas.jsonl"
python3 "$ev" sellar --etiquetas "$out/etiquetas.jsonl" --registro "$out/registro.json"
ls "$tmp/c" | grep -qv '^entradas$' && { echo "la carpeta del lector tiene algo más que entradas/"; exit 4; }
"$raiz/auditoria/reproduccion/reproducir.sh" "$ref" "$tmp/c/entradas" "$out/corrida"
python3 "$ev" comprometer --predicciones "$out/corrida/predicciones.jsonl" --corrida "$out/corrida/corrida.json" --registro "$out/registro.json"
python3 "$ev" revelar --etiquetas "$out/etiquetas.jsonl" --predicciones "$out/corrida/predicciones.jsonl" \
  --registro "$out/registro.json" --salida "$out/informe.json"
