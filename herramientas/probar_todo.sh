#!/usr/bin/env bash
# Corre TODAS las pruebas de E0–E7 que se pueden correr en el contenedor. Sale con 1 si alguna falla.
# Requiere: Python 3.12+, Postgres 16 (/usr/lib/postgresql/16/bin), Node 22, Playwright global, age y gpg,
# y kotlinc 1.9.24 en ~/.cache/checa-banco (lo baja docs/banco/calificar.sh).
set -uo pipefail
raiz=$(cd "$(dirname "$0")/.." && pwd); cd "$raiz"; mal=0
paso() { echo; echo "=== $1"; shift; "$@" || { echo "FALLÓ: $*"; mal=1; }; }
paso "E0 respaldo (servidor simulado)" bash -o pipefail -c "cd respaldo-e0 && python3 -W ignore -m unittest discover -s pruebas 2>&1 | tail -3"
paso "E1–E7 Postgres local" bash -o pipefail -c "cd auditoria/pruebas && python3 -W ignore -m unittest discover -p 'test_*.py' 2>&1 | tail -3"
paso "E2 contrato y manejador (Node)" bash -o pipefail -c "node --experimental-strip-types --no-warnings --test auditoria/pruebas/e2/registrar.test.ts 2>&1 | grep -E '^# (pass|fail)'"
paso "E3 núcleo Kotlin (JVM)" bash -o pipefail -c "herramientas/jvm/probar.sh 2>&1 | grep -v Picked | tail -1"
paso "E3 TrazaAndroid contra stubs" bash -o pipefail -c "herramientas/jvm/compilar_traza_android.sh 2>&1 | grep -v Picked"
paso "E3 guardia del lector" python3 herramientas/guardia_parser.py
paso "Banco del lector" bash -o pipefail -c "docs/banco/calificar.sh 2>&1 | grep ACIERTOS"
paso "E6–E7 página (Playwright)" bash -o pipefail -c "NODE_PATH=\$(npm root -g) node --test auditoria/pruebas/e6/admin_ui.test.js 2>&1 | grep -E '^# (pass|fail)'"
paso "D5 mensajes de commit de esta entrega" python3 herramientas/revisar_mensajes.py 5abadea75dd15a82648e4e63a89f9e66ac3cdfc2..HEAD
echo; [ $mal = 0 ] && echo "TODO OK" || echo "HAY FALLAS"; exit $mal
