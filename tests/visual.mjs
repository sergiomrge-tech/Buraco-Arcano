import assert from "node:assert/strict";
import {spawn} from "node:child_process";
import {mkdir} from "node:fs/promises";
import {chromium} from "playwright";
const server=spawn("python3",["-m","http.server","8173","--bind","127.0.0.1"],{stdio:"ignore"});
const delay=ms=>new Promise(res=>setTimeout(res,ms));
let browser;
try {
 await mkdir("screenshots",{recursive:true});await delay(1300);
 browser=await chromium.launch({headless:true,args:["--no-sandbox"]});
 for(const target of [
  {name:"desktop-1440x900",width:1440,height:900,touch:false},
  {name:"android-landscape-915x412",width:915,height:412,touch:true},
  {name:"android-portrait-412x915",width:412,height:915,touch:true}
 ]){
  const page=await browser.newPage({viewport:{width:target.width,height:target.height},deviceScaleFactor:1,hasTouch:target.touch});
  const exceptions=[];
  page.on("pageerror",error=>exceptions.push(error.message));
  const response=await page.goto("http://127.0.0.1:8173/?qa",{waitUntil:"networkidle"});
  assert.equal(response.status(),200);
  await page.locator(".play-card").first().waitFor();
  assert.equal(await page.locator(".play-card").count(),11);
  const count=await page.evaluate(()=>window.__buracoQa.getGame().stock.length);
  assert.equal(count,37);
  assert.equal(await page.locator(".seat").count(),3);
  assert.equal(await page.locator(".table-felt").count(),1);
  const gameBox=await page.locator("#game").boundingBox();
  assert.ok(gameBox.width<=target.width+1 && gameBox.height<=target.height+1,"tabuleiro excedeu o viewport visivel");
  for(const selector of ["#stock","#discard-pile","#meld-btn","#discard-btn"]){
    const box=await page.locator(selector).boundingBox();
    assert.ok(box && box.x>=-1 && box.y>=-1 && box.x+box.width<=target.width+1 && box.y+box.height<=target.height+1,selector+" saiu da tela");
  }
  await page.screenshot({path:"screenshots/"+target.name+".png",fullPage:false});
  if(target.name==="desktop-1440x900"){
   await page.locator("#settings-toggle").click();
   assert.equal(await page.locator("#difficulty option").count(),4);
   await page.locator("#modal-close").click();
   await page.locator("#stock").click();
   assert.equal(await page.locator(".play-card").count(),12);
   const first=page.locator(".play-card").first();
   await first.click();
   assert.equal(await page.locator(".play-card.selected").count(),1);
   await page.locator("#discard-btn").click();
   await delay(3500);
   assert.equal(await page.evaluate(()=>{
     const g=window.__buracoQa.getGame();
     return g.stock.length+g.discardPile.length+g.hands.flat().length+g.deads.flatMap(d=>d||[]).length+g.melds.flatMap(t=>t.flatMap(m=>m.cards)).length;
   }),104);
   await page.screenshot({path:"screenshots/turn-after-bots.png",fullPage:false});
  }
  assert.deepEqual(exceptions,[],target.name+" erros de script");
  await page.close();
 }
 console.log("QA VISUAL: 3 viewports renderizados; clique, compra, descarte e conservacao aprovados.");
}finally{
 if(browser)await browser.close();
 server.kill();
}
