import {cp,mkdir,rm,access} from 'node:fs/promises';
await access('web/java/engine.js');
await rm('dist',{recursive:true,force:true});await mkdir('dist',{recursive:true});
for(const file of ['index.html','manifest.webmanifest','sw.js','src/app.js','src/engine.js','src/style.css','web/java/engine.js','assets'])await cp(file,`dist/${file}`,{recursive:true});
console.log('dist pronto: interface e motor Java compilado.');
