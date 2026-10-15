import { cp,mkdir,rm,readFile,writeFile,access } from 'node:fs/promises';
await access('dist/web/java/engine.js');
const destination='android/app/src/main/assets';
await rm(destination,{recursive:true,force:true});await mkdir(destination,{recursive:true});
await cp('dist',destination,{recursive:true});
// Native assets are already available offline, without a service worker cache.
const path=`${destination}/src/app.js`;
const app=await readFile(path,'utf8');
await writeFile(path,app.replace("if('serviceWorker' in navigator)","if('serviceWorker' in navigator && location.hostname!=='appassets.androidplatform.net')")
 .replace('<button data-command="fullscreen">Tela cheia</button>','')
 .replace('<button data-command="fullscreen">Usar tela cheia</button>','')
 .replace('Jogo local contra três bots. Instale pela opção Adicionar à tela inicial do navegador para jogar offline após o primeiro carregamento. Modo online com amigos ainda não implementado.','Jogo offline contra três bots. Sua partida fica salva neste aparelho.'));

console.log('APK preparado com assets locais e origem HTTPS interna.');
