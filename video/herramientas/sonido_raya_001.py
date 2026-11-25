import re,subprocess,sys
src=open('/home/user/APK-CHECA/video/src/Raya/Completo.tsx').read()
G={k:round(float(v)*30) for k,v in re.findall(r'(\w+): s\(([\d.]+)\)',src)}
INI=879
ev=[(G['menos'],'rechazar'),(G['oferta']-4,'popup-entra'),(G['cobrar'],'considerar'),(G['gasolina']+6,'rechazar'),(G['datos']-2,'popup-entra'),(G['elegir'],'aceptar'),(INI+18,'logo-semaforo'),(INI+62,'popup-entra')]
print(ev)
D='/home/user/PWA-Checa-Multimedia/marca/sonido/mp3/'
cmd=['ffmpeg','-y','-loglevel','error','-f','lavfi','-t','35.8','-i','anullsrc=r=48000:cl=mono']
fl=[]
for i,(fr,n) in enumerate(ev):
    cmd+=['-i',D+n+'.mp3']; ms=int(fr/30*1000); fl.append(f'[{i+1}]adelay={ms}|{ms},volume=0.8[a{i}]')
fl.append('[0]'+''.join(f'[a{i}]' for i in range(len(ev)))+f'amix=inputs={len(ev)+1}:normalize=0,atrim=0:35.8[s]')
cmd+=['-filter_complex',';'.join(fl),'-map','[s]','-ar','48000','-ac','1',sys.argv[1]]
subprocess.run(cmd,check=True)
