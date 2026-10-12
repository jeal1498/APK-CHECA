"""Convierte los .json de ElevenLabs en src/Checa/lineas<Nombre>.ts (palabras con tiempos).

Uso: python3 -I scripts/lineas.py public/<carpeta> src/Checa/lineas<Nombre>.ts l1 l2 l3 ...
"""
import json, sys
dirr, out_ts = sys.argv[1], sys.argv[2]; names = sys.argv[3:]
out=["// Generado desde los .json de ElevenLabs (tiempos por letra)","export type Palabra = { w: string; s: number; e: number };","export type Linea = { archivo: string; dur: number; palabras: Palabra[]; voc: [number, number, number][] };","export const LINEAS: Linea[] = ["]
for n in names:
    d=json.load(open(f"{dirr}/{n}.json")); c,s,e=d["c"],d["s"],d["e"]
    words=[];cur="";ws=we=None
    for ch,a,b in zip(c,s,e):
        if ch==" ":
            if cur: words.append((cur,ws,we)); cur=""
            continue
        if not cur: ws=a
        cur+=ch; we=b
    if cur: words.append((cur,ws,we))
    voc=[]
    for ch,a,b in zip(c,s,e):
        l=ch.lower()
        if l in "aeiouáéíóú": voc.append((round(a,3),round(b,3),1))
        elif l.isalpha(): voc.append((round(a,3),round(b,3),0.45))
    pal=",".join('{w:%s,s:%.3f,e:%.3f}'%(json.dumps(w,ensure_ascii=False),a,b) for w,a,b in words)
    out.append('  { archivo: "%s", dur: %.3f, palabras: [%s], voc: %s },'%(n,e[-1],pal,json.dumps(voc)))
out.append("];")
open(out_ts,"w").write("\n".join(out)+"\n")
