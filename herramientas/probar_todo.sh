#!/usr/bin/env bash
# Corre TODAS las suites de E0–E7 que se pueden correr sin Android SDK, guardando la salida COMPLETA de cada una.
# Uso: herramientas/probar_todo.sh [DIR_LOGS]   (por defecto un directorio temporal). Sale con 1 si alguna falla.
# Requiere: Python 3.12+, PostgreSQL en /usr/lib/postgresql/<v>/bin (se prueban todas las versiones que haya de
# 16 y 17), Node 22, Playwright global, age, gpg y kotlinc 1.9.24 en ~/.cache/checa-banco (docs/banco/calificar.sh).
set -uo pipefail
raiz=$(cd "$(dirname "$0")/.." && pwd); cd "$raiz"
logs=${1:-$(mktemp -d)}; mkdir -p "$logs"; logs=$(cd "$logs" && pwd); mal=0; resumen="$logs/RESUMEN.txt"; : > "$resumen"
{ echo "commit: $(git rev-parse HEAD)$(git diff --quiet || echo ' +cambios sin commit')"; echo "fecha_utc: $(date -u +%FT%TZ)"; uname -sr
  python3 --version; node --version; java -version 2>&1 | grep version; age --version; gpg --version | head -1
  for v in 16 17; do [ -x /usr/lib/postgresql/$v/bin/postgres ] && /usr/lib/postgresql/$v/bin/postgres --version; done
  echo "kotlinc: $(cat ~/.cache/checa-banco/kotlinc/build.txt 2>/dev/null)"; echo "playwright: $(node -e "console.log(require('$(npm root -g)/playwright/package.json').version)" 2>/dev/null)"
} > "$logs/entorno.txt" 2>&1
paso() { # paso <archivo_log> <descripción> <comando…>
  local n="$1" f="$logs/$1" d="$2"; shift 2
  { echo "# $d"; echo "# \$ $*"; echo "# inicio $(date -u +%FT%TZ)"; } > "$f"
  bash -o pipefail -c "$*" >> "$f" 2>&1; local c=$?
  echo "# exit=$c fin $(date -u +%FT%TZ)" >> "$f"
  printf '%-22s %-6s %s | %s\n' "$n" "$([ $c = 0 ] && echo OK || echo FALLA)" "$d" "$(grep -hE '^Ran [0-9]+ tests|^# pass|ok, [0-9]+ fallan|^ACIERTOS|commits revisados' "$f" | tr '\n' ' ')" | tee -a "$resumen"
  [ $c = 0 ] || mal=1
}
paso e0-respaldo.log "E0 respaldo (servidor SIMULADO)" "cd respaldo-e0 && python3 -W ignore -m unittest discover -s pruebas -v"
for v in 16 17; do
  [ -x /usr/lib/postgresql/$v/bin/postgres ] || { printf '%-34s %-6s %s\n' "auditoria-pg$v.log" "NO-EJEC" "no hay PostgreSQL $v instalado" | tee -a "$resumen"; continue; }
  paso auditoria-pg$v.log "E1–E7 SQL/Python en PostgreSQL $v LOCAL (stubs de Supabase)" "cd auditoria/pruebas && PG_BIN=/usr/lib/postgresql/$v/bin python3 -W ignore -c 'import pg_local; p = pg_local.Pg(); print(\"servidor:\", p.version()); p.cerrar()' && PG_BIN=/usr/lib/postgresql/$v/bin python3 -W ignore -m unittest discover -p 'test_*.py' -v"
done
paso e2-node.log "E2 contrato y manejador (Node, dependencias simuladas)" "node --experimental-strip-types --no-warnings --test auditoria/pruebas/e2/registrar.test.ts"
paso e3-jvm.log "E3 núcleo Kotlin en JVM (no Android)" "herramientas/jvm/probar.sh 2>&1 | grep -v '^Picked up'"
paso e3-stubs.log "E3 TrazaAndroid compila contra STUBS (no es el SDK)" "herramientas/jvm/compilar_traza_android.sh 2>&1 | grep -v '^Picked up'"
paso e3-guardia.log "E3 guardia: el lector no cambió" "python3 herramientas/guardia_parser.py"
paso banco.log "Banco del lector (Banco.kt en JVM)" "docs/banco/calificar.sh 2>&1 | grep -v '^Picked up' && cat ~/.cache/checa-banco/fallas.txt"
paso e6-e7-ui.log "E6–E7 página con Playwright (cliente SIMULADO)" "NODE_PATH=\$(npm root -g) node --test auditoria/pruebas/e6/admin_ui.test.js"
paso mensajes.log "D5 mensajes de commit desde 5abadea" "python3 herramientas/revisar_mensajes.py 5abadea75dd15a82648e4e63a89f9e66ac3cdfc2..HEAD"
echo; [ $mal = 0 ] && echo "TODO OK — logs en $logs" | tee -a "$resumen" || echo "HAY FALLAS — logs en $logs" | tee -a "$resumen"; exit $mal
