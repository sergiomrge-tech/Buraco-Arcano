import { chromium } from 'playwright';
import { spawn } from 'node:child_process';
import { mkdir,writeFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const server=spawn('python3',['-m','http.server','8080','--directory','dist']);
const browser=await chromium.launch();const errors=[],results=[];
async function visiblePoint(page,id){return page.evaluate(id=>{const el=[...document.querySelectorAll('#hand [data-id]')].find(e=>e.dataset.id===id),r=el.getBoundingClientRect();for(let y=14;y<r.height-4;y+=5)for(let x=4;x<Math.min(r.width-3,34);x+=3){const px=r.left+x,py=r.top+y;if(document.elementFromPoint(px,py)?.closest('[data-id]')===el)return {x:px,y:py};}throw Error('Carta sem área de toque: '+id);},id);}
async function slotPoint(page,index){return page.evaluate(index=>{const el=document.querySelectorAll('#hand [data-id]')[index],r=document.querySelector('#hand').getBoundingClientRect(),scale=document.querySelector('#stage').getBoundingClientRect().width/864;return {x:r.left+(parseFloat(el.style.left)+72.5)*scale,y:r.top+90*scale};},index);}
async function drag(page,cdp,id,index,{cancel=false,capture=false}={}){
 const start=await visiblePoint(page,id),end=await slotPoint(page,index);
 await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{...start,id:1}]});
 for(let n=1;n<=14;n++){await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:start.x+(end.x-start.x)*n/14,y:start.y+(end.y-start.y)*n/14,id:1}]});await page.waitForTimeout(17);}
 assert.equal(await page.locator('.hand-drag-ghost').count(),1);
 if(capture)await page.screenshot({path:'qa/hand/android-arrastando.png'});
 await cdp.send('Input.dispatchTouchEvent',{type:cancel?'touchCancel':'touchEnd',touchPoints:[]});await page.waitForTimeout(400);
 assert.equal(await page.locator('.hand-drag-ghost').count(),0);
}
try{
 for(let n=0;n<100;n++){try{if((await fetch('http://localhost:8080')).ok)break;}catch{}await new Promise(r=>setTimeout(r,100));}
 await mkdir('qa/hand',{recursive:true});
 for(const [name,width,height] of [['android',412,915],['compacto',360,800]]){
  const context=await browser.newContext({viewport:{width,height},isMobile:true,hasTouch:true});const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));const cdp=await context.newCDPSession(page);
  await page.goto('http://localhost:8080');await page.locator('#hand [data-id]').first().waitFor();await page.waitForTimeout(850);
  const order=await page.evaluate(()=>window.__buraco.state().hands[0].map(c=>c.id)),checkpoint=await page.evaluate(()=>window.__buraco.checkpoint());
  const cardHeight=await page.locator('#hand [data-id]').last().evaluate(el=>getComputedStyle(el).height);assert.equal(cardHeight,'220px');
  await drag(page,cdp,order[0],order.length-1,{capture:name==='android'});
  const moved=await page.evaluate(()=>window.__buraco.state().hands[0].map(c=>c.id));assert.deepEqual(moved,[...order.slice(1),order[0]]);assert.equal(await page.locator('#hand .selected').count(),0);assert.equal(await page.evaluate(()=>window.__buraco.checkpoint()),checkpoint);
  await page.reload();await page.locator('#hand [data-id]').first().waitFor();await page.waitForTimeout(850);assert.deepEqual(await page.evaluate(()=>window.__buraco.state().hands[0].map(c=>c.id)),moved);
  await drag(page,cdp,moved[0],moved.length-1,{cancel:true});assert.deepEqual(await page.evaluate(()=>window.__buraco.state().hands[0].map(c=>c.id)),moved);
  const point=await visiblePoint(page,moved[0]);await page.touchscreen.tap(point.x,point.y);assert.equal(await page.locator('#hand .selected').count(),1);
  await page.locator('#stock').tap();assert.equal(await page.evaluate(()=>window.__buraco.state().hands[0].length),12);assert.deepEqual(await page.evaluate(()=>window.__buraco.state().hands[0].slice(0,11).map(c=>c.id)),moved);
  const p=await visiblePoint(page,moved[0]);await page.touchscreen.tap(p.x,p.y);assert.equal(await page.locator('#hand .selected').count(),1);await page.locator('#action').tap();await page.waitForFunction(()=>window.__buraco.state().turn===0);assert.equal(await page.evaluate(()=>window.__buraco.state().total),104);
  await page.waitForTimeout(800);await page.screenshot({path:`qa/hand/${name}-cartas-maiores.png`});
  results.push({name,cardHeight,checks:['arraste touch real','sem seleção ao arrastar','ordem persistente após recarga','cancelamento restaura ordem','toque seleciona','compra preserva ordem','descarte e bots preservam 104 cartas']});await context.close();
 }
 // More than one page: hold at the right edge to move a card to the next page.
 const context=await browser.newContext({viewport:{width:412,height:915},isMobile:true,hasTouch:true});const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
 const ids=Array.from({length:104},(_,i)=>`${Math.floor(i/52)}-${Math.floor(i/13)%4}-${i%13+1}`),take=n=>ids.splice(0,n);const hands=[take(28),take(11),take(11),take(11)],deads=[take(11),take(11)],trash=take(1);
 const checkpoint=['BA1|1|0|meld|normal|0|0|0|0|0|-1|-',...hands.map(x=>x.join(',')),...deads.map(x=>x.join(',')),ids.join(','),trash.join(','),'',''].join('\n');
 await page.addInitScript(checkpoint=>{if(!sessionStorage.getItem('longHand')){sessionStorage.setItem('longHand','1');localStorage.setItem('buraco-arcano-java-v2',JSON.stringify({game:{checkpoint},settings:{reduced:true}}));}},checkpoint);
 await page.goto('http://localhost:8080');await page.locator('#hand [data-id]').first().waitFor();const cdp=await context.newCDPSession(page),id=hands[0][0],start=await visiblePoint(page,id),edge=await page.locator('#hand').boundingBox();
 await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{...start,id:2}]});await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:edge.x+edge.width-5,y:edge.y+45,id:2}]});await page.waitForTimeout(720);
 assert.match(await page.locator('#handPage').textContent(),/^2\//);await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});assert.ok((await page.evaluate(()=>window.__buraco.state().hands[0].findIndex(c=>c.id===id)))>=13);assert.equal(await page.evaluate(()=>window.__buraco.state().total),104);await context.close();
 assert.deepEqual(errors,[]);await writeFile('qa/hand/results.json',JSON.stringify({results,acrossPages:true,errors},null,2));console.log('Cartas maiores e arraste aprovados com eventos touch, incluindo troca de página.');
}finally{await browser.close();server.kill();}
