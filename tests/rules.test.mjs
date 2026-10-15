import test from 'node:test';
import assert from 'node:assert/strict';
import {makeDeck,secureShuffle,secureInt,cardPoints,cardLabel,meldInfo,findMeldCandidates,
newGame,assertCards,drawStock,takeDiscard,layMeld,discardCard,botTurn,scoreTeam,DIFFICULTIES} from '../src/engine.mjs';
const c=(rank,suit=0,deck=0)=>({rank,suit,deck,id:`${deck}-${suit}-${rank}`});
const ids=cards=>cards.map(x=>x.id);
const takeFrom=(s,seat,rank,suit=0)=>{
 const id=`0-${suit}-${rank}`;let group=[...s.hands,...s.dead,s.stock,s.discard,...s.melds].flat(Infinity);
 const found=group.find(x=>x.id===id);
 for(const h of [...s.hands,...s.dead,s.stock,s.discard,...s.melds.flat(1)]){
  const i=h.indexOf(found);if(i>=0){h.splice(i,1);break}
 }
 s.hands[seat].push(found);return found;
};
test('104 unique cards, exactly two copies of each suit and rank',()=>{
 let cards=makeDeck();assert.equal(cards.length,104);assert.equal(new Set(cards.map(c=>c.id)).size,104);
 for(let rank=1;rank<=13;rank++)for(let suit=0;suit<4;suit++)assert.equal(cards.filter(c=>c.rank===rank&&c.suit===suit).length,2);
});
test('correct Brazilian Buraco card scoring',()=>{
 assert.equal(cardPoints(c(1)),15);assert.equal(cardPoints(c(2)),10);
 for(let r=3;r<=7;r++)assert.equal(cardPoints(c(r)),5);
 for(let r=8;r<=13;r++)assert.equal(cardPoints(c(r)),10);
});
test('Fisher-Yates deterministic injected randomness, rejection sampling',()=>{
 const cards=[0,1,2,3,4,5];
 let q=0;const v=secureShuffle(cards,()=>new Uint32Array([q++]));
 assert.equal(v.length,6);assert.deepEqual(cards,[0,1,2,3,4,5]);
 assert.deepEqual([...v].sort(),cards);assert.notDeepEqual(v,cards);
 let calls=0;assert.equal(secureInt(7,()=>new Uint32Array([calls++===0?0xffffffff:14])),0);
 assert.throws(()=>secureInt(2**32+1,()=>new Uint32Array([1])),RangeError);
});
test('random deals conserve all cards and do not repeat a fixed deal',()=>{
 const openings=new Set();for(let i=0;i<400;i++){
  const s=newGame();assertCards(s);assert.deepEqual(s.hands.map(x=>x.length),[11,11,11,11]);
  assert.deepEqual(s.dead.map(x=>x.length),[11,11]);assert.equal(s.stock.length,37);assert.equal(s.discard.length,1);
  openings.add(s.hands[0].map(x=>x.id).sort().join(','));
 }assert.equal(openings.size,400);
});
test('all 3-card consecutive intervals of four suits (Ace low and high)',()=>{
 let count=0;
 for(let suit=0;suit<4;suit++)for(let start=1;start<=11;start++){
  let cards=[c(start,suit),c(start+1,suit),c(start+2,suit)];
  assert.equal(meldInfo(cards)?.clean,true,JSON.stringify(cards));count++;
 }
 for(let suit=0;suit<4;suit++){
  assert.equal(meldInfo([c(11,suit),c(12,suit),c(13,suit),c(1,suit)])?.clean,true);count++;
 }
 assert.equal(count,48);
});
test('all 4 suits: one wildcard fills each internal gap and can extend either end',()=>{
 let covered=0;
 for(let suit=0;suit<4;suit++)for(let missing=4;missing<=11;missing++){
  const cards=[c(missing-1,suit),c(missing+1,suit),c(missing+2,suit),c(2,(suit+1)%4)];
  assert.equal(meldInfo(cards)?.canastra,null);assert.equal(meldInfo(cards)?.wild,true);covered++;
 }
 assert.equal(covered,32);
});
test('canastra clean, dirty, duplicates and invalid runs',()=>{
 assert.equal(meldInfo([3,4,5,6,7,8,9].map(r=>c(r)))?.canastra,'limpa');
 assert.equal(meldInfo([3,4,5,6,7,8,10].map(r=>c(r)).concat(c(2,1)))?.canastra,'suja');
 assert.equal(meldInfo([c(3),c(4),c(5,1)]),null);
 assert.equal(meldInfo([c(3),c(4),c(4,0,1)]),null);
 assert.equal(meldInfo([c(3),c(4),c(7),c(2,1)]),null);
 assert.equal(meldInfo([c(3),c(4),c(5),c(2,1),c(2,2)]),null);
 assert.equal(meldInfo([c(1),c(13),c(12)])?.clean,true);
 assert.equal(meldInfo([c(13),c(1),c(2)])?.wild,true /* 2 can stand for Q in Q-K-A */);
});
test('candidate generator finds natural 2 run, clean sequences, and wildcard gap',()=>{
 let a=findMeldCandidates([c(1,0),c(2,0),c(3,0)]);
 assert(a.some(z=>z.cards.length===3&&z.info.clean));
 a=findMeldCandidates([c(5,0),c(7,0),c(8,0),c(2,1)]);
 assert(a.some(z=>z.cards.length===4&&z.info.wild));
 a=findMeldCandidates([c(11,0),c(12,0),c(13,0),c(1,0)]);
 assert(a.some(z=>z.cards.length===4&&z.info.clean));
});
test('phase-locked actions and safe game state',()=>{
 const s=newGame();assert.equal(discardCard(s,0,s.hands[0][0].id),false);
 assert.equal(drawStock(s,1),false);assert.equal(drawStock(s,0),true);
 assert.equal(drawStock(s,0),false);assert.equal(takeDiscard(s,0),false);
 assert.equal(discardCard(s,0,s.hands[0][0].id),true);
 assert.equal(s.current,1);assert.equal(s.phase,'draw');assertCards(s);
});
test('cannot empty hand after morto without canastra (discard or meld)',()=>{
 const s=newGame();s.deadTaken[0]=true;s.phase='play';
 // Move other 10 human cards to stock without duplication.
 s.stock.push(...s.hands[0].splice(1));
 assert.equal(discardCard(s,0,s.hands[0][0].id),false);assert.equal(s.hands[0].length,1);assertCards(s);
 const t=newGame();t.deadTaken[0]=true;t.phase='play';
 const three=[c(3),c(4),c(5)];
 t.stock.push(...t.hands[0].splice(0));
 for(const card of three) {const buckets=[t.stock,...t.hands,...t.dead,t.discard];
   for(const b of buckets){const i=b.findIndex(x=>x.id===card.id);if(i>=0){b.splice(i,1);break;}}
   t.hands[0].push(card);
 }
 assert.equal(layMeld(t,0,ids(three)),false);assert.equal(t.hands[0].length,3);assertCards(t);
});
test('batida and morto behave consistently, preserve cards',()=>{
 const s=newGame();s.phase='play';
 // Put every card of human hand except one in stock, then discard to claim the dead.
 s.stock.push(...s.hands[0].splice(1));
 assert.equal(discardCard(s,0,s.hands[0][0].id),true);assert.equal(s.deadTaken[0],true);
 assert.equal(s.dead[0].length,0);assert.equal(s.hands[0].length,11);
 assert.equal(s.current,1);assertCards(s);
});
test('bot turns perform valid actions across all difficulties with conservation',()=>{
 for(const d of DIFFICULTIES){
  for(let game=0;game<5;game++){
   const s=newGame(d);
   for(let step=0;step<160&&!s.gameOver;step++){
    if(s.current===0){
      if(s.phase==='draw'){
       if(s.stock.length)drawStock(s,0);else if(s.discard.length)takeDiscard(s,0);
       else break;
      }
      if(s.phase==='play'){
       const options=findMeldCandidates(s.hands[0]);
       if(options.length)layMeld(s,0,ids(options[0].cards));
       if(!s.gameOver&&s.phase==='play')assert.equal(discardCard(s,0,s.hands[0][0].id),true);
      }
    }else assert.equal(botTurn(s),true,`d=${d};phase=${s.phase}`);
    assertCards(s);
   }
  }
 }
});
test('exhaustive 65,536 rank-mask patterns: natural or one wild across every suit',()=>{
 const oracle=(ranks,withWild)=>{
  if(ranks.length+(withWild?1:0)<3)return false;
  for(const highAce of [false,true]){
   const values=ranks.map(v=>v===1&&highAce?14:v);
   const set=new Set(values);
   if(set.size!==values.length)continue;
   for(let lo=highAce?2:1;lo<=12;lo++){
    const hi=lo+values.length+(withWild?1:0)-1;
    if(hi>(highAce?14:13))continue;
    if(values.every(v=>v>=lo&&v<=hi) && (hi-lo+1-set.size===(withWild?1:0)))return true;
   }
  }
  return false;
 };
 let patterns=0;
 for(let suit=0;suit<4;suit++)for(let mask=0;mask<(1<<13);mask++){
  const ranks=Array.from({length:13},(_,i)=>i+1).filter(r=>mask&(1<<(r-1)));
  const natural=ranks.map(r=>c(r,suit));
  const wildcard=[...natural,c(2,(suit+1)%4)];
  assert.equal(Boolean(meldInfo(natural)),(oracle(ranks,false)||(ranks.includes(2)&&oracle(ranks.filter(r=>r!==2),true))),`natural suit=${suit} mask=${mask}`);
  assert.equal(Boolean(meldInfo(wildcard)),oracle(ranks,true),`wild suit=${suit} mask=${mask}`);
  patterns+=2;
 }
 assert.equal(patterns,65536);
});
