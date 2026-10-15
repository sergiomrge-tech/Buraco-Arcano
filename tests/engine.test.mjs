import test from "node:test";
import assert from "node:assert/strict";
import {shuffledDeck,isValidRun,BuracoGame,bestMeld,cardValue,teamOf} from "../src/engine.js";
const card=(s,r,id="test")=>({s,r,id:id+"-"+s+"-"+r});
test("o embaralhamento gera 104 cartas unicas e muda entre partidas",()=>{
 const deck=shuffledDeck();
 assert.equal(deck.length,104);
 assert.equal(new Set(deck.map(c=>c.id)).size,104);
 const orders=new Set(Array.from({length:10},()=>shuffledDeck().map(c=>c.id).join(",")));
 assert.equal(orders.size,10);
});
test("distribuicao inicial 11 por jogador, 2 mortos de 11, 37 no monte, 1 no lixo",()=>{
 const g=new BuracoGame("dificil");
 assert.deepEqual(g.hands.map(h=>h.length),[11,11,11,11]);
 assert.deepEqual(g.deads.map(d=>d.length),[11,11]);
 assert.equal(g.stock.length,37);
 assert.equal(g.discardPile.length,1);
 assert.equal(g.phase,"draw");
 assert.equal(totalCards(g),104);
});
test("o Buraco aberto aceita sequencias, nao aceita trincas",()=>{
 assert.equal(isValidRun([card(0,3),card(0,4),card(0,5)]).clean,true);
 assert.equal(isValidRun([card(0,3),card(1,3),card(2,3)]),null);
 assert.equal(isValidRun([card(0,4),card(0,4,"copy2"),card(0,5)]),null);
});
test("curinga 2 so pode aparecer uma vez fora de posicao natural",()=>{
 assert.equal(isValidRun([card(0,5),card(0,7),card(1,2)]).wild,1);
 assert.equal(isValidRun([card(0,1),card(0,2),card(0,3)]).clean,true);
 assert.equal(isValidRun([card(0,5),card(0,7),card(1,2),card(2,2)]),null);
});
test("canastras limpas e sujas sao reconhecidas",()=>{
 const clean=Array.from({length:7},(_,i)=>card(3,4+i,String(i)));
 const dirty=[card(3,4),card(3,5),card(3,7),card(3,8),card(3,9),card(3,10),card(2,2)];
 assert.equal(isValidRun(clean).clean,true);
 assert.equal(isValidRun(dirty).clean,false);
});
test("jogada nao pode comecar sem comprar",()=>{
 const g=new BuracoGame();
 assert.equal(g.discard(0,g.hands[0][0].id),false);
 assert.equal(g.meld(0,g.hands[0].slice(0,3).map(c=>c.id)),false);
 assert.equal(g.buyStock(1),false);
 assert.equal(g.buyStock(0),true);
 assert.equal(g.buyStock(0),false);
 assert.equal(g.phase,"meld");
 assert.equal(totalCards(g),104);
});
test("descarte muda o turno e nao permite descarte duplo",()=>{
 const g=new BuracoGame();
 g.buyStock(0);
 const id=g.hands[0][0].id;
 assert.equal(g.discard(0,id),true);
 assert.equal(g.player,1);
 assert.equal(g.discard(0,id),false);
 assert.equal(totalCards(g),104);
});
test("duplas sao alternadas corretamente",()=>{
 assert.equal(teamOf(0),0);assert.equal(teamOf(2),0);assert.equal(teamOf(1),1);assert.equal(teamOf(3),1);
});
test("a IA identifica uma sequencia na mao",()=>{
 const found=bestMeld([card(0,3),card(0,4),card(0,5),card(1,11)],100);
 assert.equal(found.cards.length,3);
 assert.equal(found.run.clean,true);
});
function totalCards(g){
 return g.stock.length+g.discardPile.length+
 g.hands.reduce((s,h)=>s+h.length,0)+
 g.deads.reduce((s,d)=>s+(d?.length||0),0)+
 g.melds.flat().reduce((s,m)=>s+m.cards.length,0);
}
test("todas as cartas permanecem contabilizadas durante bots nas quatro dificuldades",()=>{
 for(const difficulty of ["facil","normal","dificil","lendario"]){
  const g=new BuracoGame(difficulty);
  for(let i=0;i<12&&g.winner===null;i++){
   const before=g.player;g.aiPlay(before);
   assert.equal(totalCards(g),104,"cartas perdidas em "+difficulty);
   assert.notEqual(g.player,before,"bot deveria passar turno em "+difficulty);
  }
 }
});
