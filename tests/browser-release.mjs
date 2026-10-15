import { chromium } from 'playwright';
import { spawn } from 'node:child_process';
import { mkdir,writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const server=spawn('python3',['-m','http.server','8080','--directory','dist']);
const browser=await chromium.launch({headless:true});
const errors=[];const results=[];
try {
 for(let n=0;n<100;n++){try{if((await fetch('http://localhost:8080')).ok)break;}catch{}await new Promise(r=>setTimeout(r,100));}
 await mkdir('qa/screenshots',{recursive:true});
 for(const [name,width,height] of [['android',412,915],['compacto',360,800],['desktop',1200,750]]){
  const context=await browser.newContext({viewport:{width,height},hasTouch:true,isMobile:width<600});
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://localhost:8080');await page.locator('#hand [data-id]').first().waitFor();await page.waitForTimeout(700);
  let s=await page.evaluate(()=>window.__buraco.state());assert.equal(s.total,104);assert.equal(s.hands[0].length,11);
  await page.screenshot({path:`qa/screenshots/${name}-mesa.png`});
  await page.locator('#settings').tap();await page.locator('#difficultyOption').selectOption('lendario');await page.locator('#closeDialog').tap();
  assert.equal(await page.evaluate(()=>window.__buraco.state().difficulty),'lendario');
  await page.locator('#stock').tap();assert.equal(await page.evaluate(()=>window.__buraco.state().hands[0].length),12);
  const save=await page.evaluate(()=>window.__buraco.checkpoint());await page.reload();await page.locator('#hand [data-id]').first().waitFor();assert.equal(await page.evaluate(()=>window.__buraco.checkpoint()),save);
  await page.locator('#hand [data-id]').last().tap();assert.equal(await page.locator('#hand .selected').count(),1);
  await page.locator('#action').tap();await page.waitForFunction(()=>window.__buraco.state().turn===0,{timeout:20000});
  assert.equal(await page.evaluate(()=>window.__buraco.state().total),104);
  await page.locator('#trashInspect').tap();await page.locator('#dialog').waitFor({state:'visible'});await page.locator('#closeDialog').tap();
  await page.evaluate(()=>navigator.serviceWorker.ready);await page.reload();await page.locator('#hand [data-id]').first().waitFor();
  await context.setOffline(true);await page.reload();await page.locator('#hand [data-id]').first().waitFor();assert.equal(await page.evaluate(()=>window.__buraco.state().total),104);await context.setOffline(false);
  await page.screenshot({path:`qa/screenshots/${name}-jogando.png`});
  results.push({name,viewport:[width,height],checks:['104 cartas','toque compra/seleção/descarte','dificuldade','retomar save','3 bots','lixo aberto','recarga offline']});await context.close();
 }
 assert.deepEqual(errors,[]);await writeFile('qa/browser-results.json',JSON.stringify({results,errors},null,2));
 console.log('Fluxos aprovados em 3 tamanhos. Capturas reais em qa/screenshots.');
} finally {await browser.close();server.kill();}
