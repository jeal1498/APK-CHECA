#!/bin/bash
# Uso: revisar.sh <video_final.mp4> <portada.jpg> <copy.txt> [spec.json]
# Lista de verificación automática antes de dar el video por hecho. Sale con código 1 si algo falla.
# Revisa: 1080x1920, audio, ≈30 fps, tamaño <100 MB, volumen ≈ -14 LUFS, portada como primer cuadro,
# copy (frases que Checa no dice) y, si se pasa el spec, que las escenas sumen ≤ 40 %.
V="$1"; P="$2"; C="$3"; SPEC="$4"; FAIL=0
ok(){ echo "  ✓ $1"; }; no(){ echo "  ✗ $1"; FAIL=1; }; av(){ echo "  ! $1"; }
echo "Revisando $V"
W=$(ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=p=0 "$V")
[ "$W" = "1080,1920" ] && ok "1080x1920" || no "tamaño $W (debe ser 1080,1920)"
FPS=$(ffprobe -v error -select_streams v:0 -show_entries stream=r_frame_rate -of csv=p=0 "$V" | awk -F/ '{printf "%d", $1/$2}')
[ "$FPS" = "30" ] && ok "30 fps" || av "fps $FPS (se esperan 30)"
ffprobe -v error -select_streams a:0 -show_entries stream=codec_name -of csv=p=0 "$V" | grep -q . && ok "tiene audio" || no "sin audio"
DUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$V" | awk '{printf "%.1f", $1}'); ok "duración ${DUR} s"
MB=$(( $(stat -c %s "$V") / 1048576 )); [ "$MB" -lt 95 ] && ok "${MB} MB (límite de GitHub 100 MB)" || no "${MB} MB: recomprimir con -crf 26"
LU=$(ffmpeg -nostats -i "$V" -af ebur128=peak=none -f null - 2>&1 | awk '/I:/ {v=$2} END {print v}')
if [ -n "$LU" ]; then awk -v l="$LU" 'BEGIN{exit !(l>-17 && l<-11)}' && ok "volumen ${LU} LUFS" || av "volumen ${LU} LUFS (objetivo -14)"; fi
SS=$(ffmpeg -nostats -i "$V" -i "$P" -filter_complex "[0:v]trim=end_frame=1,setpts=PTS-STARTPTS[a];[1:v]scale=1080:1920[b];[a][b]ssim" -f null - 2>&1 | grep -o 'All:[0-9.]*' | head -1 | cut -d: -f2)
[ -n "$SS" ] && awk -v s="$SS" 'BEGIN{exit !(s>0.9)}' && ok "la portada es el primer cuadro (ssim $SS)" || av "el primer cuadro no parece la portada (ssim ${SS:-n/d})"
DM="$(dirname "$C")/dm.txt"   # el link va en dm.txt (no hay link en la bio ni en el copy)
grep -q "jeal1498.github.io/checa-web" "$C" "$DM" 2>/dev/null && ok "link de descarga en copy o dm.txt" || av "ni el copy ni dm.txt traen jeal1498.github.io/checa-web"
if grep -qiE "7 días gratis|sin costo|gratis|garantiz|ruta rentable|\b[0-9]+ ?pesos\b" "$C"; then no "el copy tiene algo que Checa no dice por ahora (prueba gratis, sin costo, garantías, competencia, precios)"; else ok "copy sin frases prohibidas"; fi
if [ -f "$DM" ]; then grep -q "utm_source=" "$DM" && ok "dm.txt con utm_source" || av "dm.txt sin utm_source (el reporte diario no contaría las descargas)"; fi
if [ -n "$SPEC" ]; then
  python3 - "$SPEC" <<'PY' || FAIL=1
import json, sys
s = json.load(open(sys.argv[1])); d = s['duracion']; e = sum(x['fin'] - x['ini'] for x in s.get('escenas', []))
p = 100 * e / d; print(f"  {'✓' if p <= 40 else '✗'} escenas: {e:.1f} s de {d:.1f} s ({p:.0f} %, tope 40 %)"); sys.exit(0 if p <= 40 else 1)
PY
fi
echo; echo "A ojo, antes de dar el video por hecho:"
echo "  [ ] subtítulos de 5 palabras o menos, sin errores de transcripción («checas», «que cheques», nunca «checes»)"
echo "  [ ] ninguna escena tapa el gancho o el rótulo; el pop-up y la oferta se ven completos"
echo "  [ ] «Números de ejemplo» visible donde haya cifras, y las cuentas cuadran (neta = viaje − gasolina)"
echo "  [ ] sin logos de Uber ni DiDi, sin ciudad ni país, sin promesas de ganancia"
echo "  [ ] cierre estándar: «Descarga Checa» (con su pausa), logo, @checa.driver, @Eduardoalv9802 y el link"
exit $FAIL
