import {chromium} from 'playwright';
import {spawn} from 'node:child_process';
import {mkdir,writeFile} from 'node:fs/promises';
import assert from 'node:assert/strict';
const server=spawn('python3',['-m','http.server','8081','--directory','dist']);
const browser=await chromium.launch();const errors=[],results=[];
const ids=Array.from({length:104},(_,i)=>`${Math.floor(i/52)}-${Math.floor(i/13)%4}-${i%13+1}`);
const run=Array.from({length:8},(_,i)=>`0-1-${i+3}`),rest=ids.filter(id=>!run.includes(id)),take=n=>rest.splice(0,n);
const hand=[...run,...take(3)],others=[take(11),take(11),take(11)],deads=[take(11),take(11)],trash=take(1);
const checkpoint=['BA1|1|0|draw|normal|0|0|0|0|0|-1|-',hand.join(','),...others.map(h=>h.join(',')),...deads.map(h=>h.join(',')),rest.join(','),trash.join(','),'',''].join('\n');
async function drag(page,cdp,id,target){
 const start=await page.locator(`[data-id="${id}"]`).evaluate(el=>{const r=el.getBoundingClientRect();for(let y=12;y<r.height-3;y+=3)for(let x=3;x<28;x+=2){const p={x:r.left+x,y:r.top+y};if(document.elementFromPoint(p.x,p.y)?.closest('[data-id]')===el)return p;}throw Error('no visible touch');});
 const box=await target.boundingBox(),end={x:box.x+box.width/2,y:box.y+box.height/2};
 await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{...start,id:1}]});
 for(let n=1;n<=14;n++)await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:start.x+(end.x-start.x)*n/14,y:start.y+(end.y-start.y)*n/14,id:1}]});
 await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
}
async function select(page,list){for(const id of list)await page.locator(`[data-id="${id}"]`).evaluate(el=>el.click());}
try{
 for(let i=0;i<100;i++){try{if((await fetch('http://localhost:8081')).ok)break;}catch{}await new Promise(r=>setTimeout(r,50));}
 await mkdir('qa/melds',{recursive:true});
 for(const width of [360,412]){
 const ctx=await browser.newContext({viewport:{width,height:915},isMobile:true,hasTouch:true});const page=await ctx.newPage();page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(checkpoint=>{if(!sessionStorage.getItem('fixture')){sessionStorage.setItem('fixture','1');localStorage.setItem('buraco-arcano-java-v2',JSON.stringify({game:{checkpoint},settings:{reduced:true}}));}},checkpoint);
 await page.goto('http://localhost:8081');await page.locator('#hand [data-id]').first().waitFor();assert.equal(await page.locator('#meldDrop').isDisabled(),true);
 await page.locator('#stock').tap();await select(page,run.slice(0,3));await page.locator('#meldDrop').tap();assert.equal(await page.locator('#melds .playing-card').count(),3);
 await select(page,[run[3]]);await page.locator('[data-meld="0"]').tap();assert.equal(await page.locator('#melds .playing-card').count(),4);
 const cdp=await ctx.newCDPSession(page);await select(page,run.slice(5,8));await drag(page,cdp,run[5],page.locator('#meldDrop'));assert.equal(await page.locator('#melds .meld-row').count(),2);
 await drag(page,cdp,run[4],page.locator('[data-meld="0"]'));assert.equal(await page.locator('[data-meld="0"] .playing-card').count(),5);
 // Invalid run must leave state and selection intact, and explain the rejection.
 const remaining=await page.locator('#hand [data-id]').evaluateAll(els=>els.slice(0,3).map(el=>el.dataset.id));await select(page,remaining);const before=await page.evaluate(()=>window.__buraco.checkpoint());await page.locator('#meldDrop').tap();assert.equal(await page.evaluate(()=>window.__buraco.checkpoint()),before);assert.equal(await page.locator('#hand .selected').count(),3);
 await select(page,remaining);await page.screenshot({path:`qa/melds/mesa-${width}.png`});
 await page.reload();await page.locator('#hand [data-id]').first().waitFor();assert.equal(await page.locator('#melds .meld-row').count(),2);assert.equal(await page.evaluate(()=>window.__buraco.state().total),104);
 results.push({width,checks:['botão baixa jogo','toque acrescenta carta','arraste baixa grupo','arraste acrescenta carta','sequência inválida preserva estado','save','104 cartas']});await ctx.close();
 }
 assert.deepEqual(errors,[]);await writeFile('qa/melds/results.json',JSON.stringify({results,errors},null,2));console.log('Área de jogos e arraste aprovados em 360 e 412px');
}finally{await browser.close();server.kill();}
