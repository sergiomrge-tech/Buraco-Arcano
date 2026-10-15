/** Presentation adapter. State transitions, run validation, scoring and hints are Java/TeaVM. */
import * as java from '../web/java/engine.js';
export const SUITS=['♠','♥','♦','♣'];
export const rank=c=>({1:'A',11:'J',12:'Q',13:'K'})[c.r]||String(c.r);
export const points=c=>c.r===2?20:c.r===1?15:c.r>=8?10:5;
export const sequence=cards=>JSON.parse(java.analyze(cards.map(c=>c.id).join(',')));
export const DIFFICULTIES={facil:'Aprendiz · Fácil',normal:'Estrategista · Normal',dificil:'Mestre · Difícil',lendario:'Oráculo · Especialista'};
export class Game {
 constructor(difficulty='normal',checkpoint=null){
  if(!checkpoint||!java.restore(checkpoint))java.newGame(Object.hasOwn(DIFFICULTIES,difficulty)?difficulty:'normal');
  this.refresh();
 }
 refresh(){const s=JSON.parse(java.state());
  const oldOrder=this.hands?.[0]?.map(c=>c.id)||[];
  this.hands=s.hands;this.hands[0].sort((a,b)=>{let i=oldOrder.indexOf(a.id),j=oldOrder.indexOf(b.id);return(i<0?999:i)-(j<0?999:j);});
  Object.assign(this,{round:s.round,difficulty:s.difficulty,turn:s.player,phase:s.phase==='meld'?'play':s.phase,over:s.winner!==null,stock:s.stock,trash:s.discardPile,dead:s.deads.filter(Boolean),taken:s.deadTaken,melds:s.melds.map(ms=>ms.map(m=>m.cards)),scores:s.totals,log:s.logs.slice().reverse(),result:s.lastRoundScore?{winner:typeof s.winner==='number'?s.winner:null,earned:s.lastRoundScore}:null});return this;
 }
 action(fn){if(!fn())throw Error(JSON.parse(java.state()).message||'Jogada indisponível nesta fase.');return this.refresh();}
 draw(from='stock'){return this.action(()=>from==='trash'?java.buyDiscard(0):java.buyStock(0));}
 meld(ids,index=-1){return this.action(()=>java.meld(0,ids.join(','),index));}
 discard(id){return this.action(()=>java.discard(0,id));}
 bot(){java.aiPlay(this.turn);return this.refresh();}
 bestMove(){return JSON.parse(java.hint());}
 setDifficulty(value){if(Object.hasOwn(DIFFICULTIES,value)){java.difficulty(value);this.refresh();}}
 checkpoint(){return java.checkpoint();}
 toJSON(){return {checkpoint:this.checkpoint()};}
 nextRound(){if(!this.over)throw Error('A rodada ainda está em andamento.');if(this.round===3)java.newGame(this.difficulty);else java.newRound();this.hands=null;return this.refresh();}
}
