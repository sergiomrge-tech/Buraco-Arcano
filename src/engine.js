/* Buraco Arcano — motor de Buraco Aberto 2v2, sem dependências externas. */
export const SUITS=["♠","♥","♦","♣"];
export const FACES={1:"A",11:"J",12:"Q",13:"K"};
export const cardName=c=>c?(FACES[c.r]||c.r)+SUITS[c.s]:"";
export const teamOf=p=>p%2;
export const cardValue=c=>c.r===1?15:c.r===2?10:c.r>=8?10:5;
export const displaySort=(a,b)=>a.s-b.s||a.r-b.r||a.id.localeCompare(b.id);
const cryptoInt=max=>{
  if(!Number.isSafeInteger(max)||max<1)throw Error("Limite aleatório inválido");
  const range=0x100000000, limit=Math.floor(range/max)*max, x=new Uint32Array(1);
  let v;do{globalThis.crypto.getRandomValues(x);v=x[0];}while(v>=limit);
  return v%max;
};
export function shuffledDeck(){
  const deck=[];
  for(let copy=0;copy<2;copy++)for(let s=0;s<4;s++)for(let r=1;r<=13;r++)
    deck.push({id:copy+"-"+s+"-"+r,s,r});
  for(let i=deck.length-1;i>0;i--){const j=cryptoInt(i+1);[deck[i],deck[j]]=[deck[j],deck[i]];}
  return deck;
}
export function isValidRun(cards){
  if(cards.length<3||cards.length>14)return null;
  const fixed=cards.filter(c=>c.r!==2),suits=[...new Set(fixed.map(c=>c.s))];
  if(suits.length!==1)return null;
  const suit=suits[0], n=cards.length, targetEnds=14;
  for(let start=1;start<=targetEnds-n+1;start++){
    const slots=Array.from({length:n},(_,i)=>start+i);
    const ordered=[...cards].sort((a,b)=>(a.r===2?1:0)-(b.r===2?1:0));
    const recurse=(i,used,wild,order)=>{
      if(wild>1)return null;
      if(i===ordered.length)return {suit,start,clean:wild===0,wild,ordered:order.slice(),length:n};
      const c=ordered[i];
      for(let k=0;k<slots.length;k++){
        if(used.has(k))continue;
        const rank=slots[k], natural=c.r===rank||(c.r===1&&rank===14);
        const isWild=c.r===2 && !(rank===2&&c.s===suit);
        if(c.r!==2&&!natural)continue;
        if(c.r===2&&!isWild&&rank!==2)continue;
        used.add(k);order[k]=c;
        const result=recurse(i+1,used,wild+(isWild?1:0),order);
        if(result)return result;
        used.delete(k);
      }
      return null;
    };
    const result=recurse(0,new Set(),0,[]);
    if(result)return result;
  }
  return null;
}
export function meldBonus(m){
  if(m.cards.length<7)return 0;
  const check=isValidRun(m.cards);
  if(!check)return 0;
  if(!check.clean)return 100;
  // Canastras especiais: sequência integral Ás-Rei (500) ou Ás-Ás (1000).
  if(m.cards.length>=14)return 1000;
  if(m.cards.length>=13)return 500;
  return 200;
}
const handPenalty=hand=>hand.reduce((s,c)=>s+cardValue(c),0);
export function bestMeld(hand,maxChecks=15000){
  if(hand.length<3)return null;
  let best=null, checked=0;
  const size=Math.min(hand.length,42);
  for(let i=0;i<size-2;i++)for(let j=i+1;j<size-1;j++)for(let k=j+1;k<size;k++){
    if(++checked>maxChecks)break;
    let selection=[hand[i],hand[j],hand[k]], run=isValidRun(selection);
    if(!run)continue;
    let growing=true;
    while(growing&&selection.length<13){
      growing=false;
      let next=null, nextRun=null;
      for(const c of hand){
        if(selection.includes(c))continue;
        const candidate=isValidRun([...selection,c]);
        if(candidate && (!next || (candidate.clean&&!nextRun.clean))){next=c;nextRun=candidate;}
      }
      if(next){selection.push(next);run=nextRun;growing=true;}
    }
    const score=selection.length*13+(run.clean?6:0)+(selection.length>=7?110:0);
    if(!best||score>best.score)best={cards:selection,run,score};
  }
  return best;
}
export class BuracoGame{
  constructor(difficulty="normal"){this.difficulty=difficulty;this.totals=[0,0];this.round=0;this.logs=[];this.startRound();}
  log(text){this.logs.unshift(text);this.logs.length=Math.min(this.logs.length,35);this.message=text;}
  startRound(){
    const deck=shuffledDeck();
    this.round++;this.stock=deck;this.hands=[[],[],[],[]];
    for(let x=0;x<11;x++)for(let p=0;p<4;p++)this.hands[p].push(this.stock.pop());
    this.deads=[Array.from({length:11},()=>this.stock.pop()),Array.from({length:11},()=>this.stock.pop())];
    this.discardPile=[this.stock.pop()];
    this.melds=[[],[]];this.deadTaken=[false,false];this.player=0;this.phase="draw";
    this.winner=null;this.lastRoundScore=null;
    this.log("Nova rodada: 104 cartas embaralhadas, 11 para cada jogador e dois mortos.");
    return this;
  }
  counts(){return {hands:this.hands.map(h=>h.length),stock:this.stock.length,discard:this.discardPile.length,deads:this.deads.filter(Boolean).length};}
  checkTurn(player,phase){if(this.winner!==null||this.player!==player||this.phase!==phase)return false;return true;}
  buyStock(player){
    if(!this.checkTurn(player,"draw"))return false;
    if(!this.stock.length){this.refillOrEnd();if(!this.stock.length)return false;}
    const c=this.stock.pop();this.hands[player].push(c);this.phase="meld";
    this.log((player===0?"Você":this.playerName(player))+" comprou do monte.");
    return true;
  }
  buyDiscard(player){
    if(!this.checkTurn(player,"draw")||!this.discardPile.length)return false;
    const n=this.discardPile.length;
    this.hands[player].push(...this.discardPile.splice(0));
    this.phase="meld";this.log((player===0?"Você":this.playerName(player))+" recolheu "+n+" carta(s) do lixo.");
    return true;
  }
  playerName(p){return ["Você","Morgath","Lyra","Valtor"][p];}
  meld(player,ids,groupIndex=null){
    if(!this.checkTurn(player,"meld"))return false;
    const hand=this.hands[player];
    const unique=[...new Set(ids)];
    const picked=unique.map(id=>hand.find(c=>c.id===id));
    if(!picked.length||picked.some(c=>!c)){this.log("Selecione cartas da sua mão.");return false;}
    const team=teamOf(player), toExtend=Number.isInteger(groupIndex);
    if(toExtend && !this.melds[team][groupIndex]){this.log("Jogo não encontrado.");return false;}
    const combined=toExtend?[...this.melds[team][groupIndex].cards,...picked]:picked;
    const run=isValidRun(combined);
    if(!run){this.log("Sequência inválida: são necessárias 3+ cartas do mesmo naipe em ordem, com no máximo um 2 como curinga.");return false;}
    if(!this.deadTaken[team]&&hand.length===picked.length&&!this.deads.some(Boolean)&&this.melds[team].every(m=>m.cards.length<7)&&combined.length<7){
      this.log("É necessário formar uma canastra limpa antes da batida final.");return false;
    }
    this.hands[player]=hand.filter(c=>!unique.includes(c.id));
    if(toExtend)this.melds[team][groupIndex].cards=run.ordered;
    else this.melds[team].push({cards:run.ordered});
    this.log(this.playerName(player)+(toExtend?" ampliou um jogo.":" baixou uma sequência de "+picked.length+" cartas.")+(combined.length>=7?" CANASTRA "+(run.clean?"LIMPA!":"SUJA!"):""));
    this.onHandEmpty(player,false);
    return true;
  }
  discard(player,id){
    if(!this.checkTurn(player,"meld"))return false;
    const team=teamOf(player),hand=this.hands[player],idx=hand.findIndex(c=>c.id===id);
    if(idx<0){this.log("Escolha uma carta para descartar.");return false;}
    if(hand.length===1 && this.deadTaken[team] && !this.hasClean(team)){
      this.log("Sua dupla precisa ter uma canastra limpa para bater.");return false;
    }
    const [c]=hand.splice(idx,1);this.discardPile.push(c);
    this.log(this.playerName(player)+" descartou "+cardName(c)+".");
    if(hand.length===0&&this.onHandEmpty(player,true))return true;
    this.player=(this.player+1)%4;this.phase="draw";
    if(!this.stock.length)this.refillOrEnd();
    return true;
  }
  hasClean(team){return this.melds[team].some(m=>m.cards.length>=7&&isValidRun(m.cards)?.clean);}
  onHandEmpty(player,afterDiscard){
    if(this.hands[player].length)return false;
    const team=teamOf(player);
    if(!this.deadTaken[team]){
      const next=this.deads.findIndex(d=>d&&d.length);
      if(next!==-1){
        this.hands[player]=this.deads[next];this.deads[next]=null;this.deadTaken[team]=true;
        this.log(this.playerName(player)+" pegou o MORTO da dupla!");
        return false;
      }
    }
    if(this.deadTaken[team]&&this.hasClean(team)){this.finishRound(team);return true;}
    return false;
  }
  refillOrEnd(){
    if(this.stock.length||this.winner!==null)return;
    const deadIndex=this.deads.findIndex(d=>d&&d.length);
    if(deadIndex!==-1){
      this.stock=this.deads[deadIndex];this.deads[deadIndex]=null;
      this.log("O morto restante virou monte de compras.");
    }else this.finishRound(null);
  }
  finishRound(winnerTeam){
    if(this.winner!==null)return;
    this.winner=winnerTeam===null?"stock":winnerTeam;this.phase="end";
    const sums=[0,0];
    for(let t=0;t<2;t++){
      for(const m of this.melds[t]){
        sums[t]+=m.cards.reduce((s,c)=>s+cardValue(c),0)+meldBonus(m);
      }
      for(let p=t;p<4;p+=2)sums[t]-=handPenalty(this.hands[p]);
      if(!this.deadTaken[t])sums[t]-=100;
      if(winnerTeam===t)sums[t]+=100;
      this.totals[t]+=sums[t];
    }
    this.lastRoundScore=sums;
    const won=this.totals[0]>=3000||this.totals[1]>=3000;
    this.log((winnerTeam===null?"Monte esgotado — rodada encerrada.":(winnerTeam===0?"As Casas da Aurora":"As Casas da Sombra")+" bateram!")+" Pontos: "+sums[0]+" x "+sums[1]+(won?" · Partida finalizada!":""));
  }
  aiTakeDiscard(player){
    const hand=this.hands[player],pile=this.discardPile;
    if(!pile.length)return false;
    if(this.difficulty==="facil")return cryptoInt(10)<2 && pile.length<=3;
    if(pile.length>=9&&this.difficulty==="normal")return false;
    const top=pile[pile.length-1],team=teamOf(player);
    const run=bestMeld([...hand,top]);
    const important=run?.cards.some(c=>c.id===top.id) || this.melds[team].some(m=>isValidRun([...m.cards,top]));
    const gain=important?50:0;
    const threshold=this.difficulty==="normal"?55:this.difficulty==="dificil"?43:33;
    return gain+Math.min(12,pile.length*2)>=threshold;
  }
  aiPlay(player){
    if(this.player!==player||this.winner!==null)return;
    const hard=["dificil","lendario"].includes(this.difficulty), team=teamOf(player);
    if(this.phase==="draw"){
      if(this.aiTakeDiscard(player))this.buyDiscard(player);
      else this.buyStock(player);
    }
    if(this.phase!=="meld"||this.winner!==null)return;
    let plays=0;
    while(plays++<18&&this.hands[player].length>0&&this.winner===null){
      let move=null;
      for(let ix=0;ix<this.melds[team].length;ix++){
        const m=this.melds[team][ix];
        for(const c of this.hands[player]){
          if(isValidRun([...m.cards,c])){move={cards:[c],index:ix,score:25+(m.cards.length===6?120:0)};break;}
        }
        if(move)break;
      }
      const fresh=bestMeld(this.hands[player],hard?30000:7000);
      if(fresh&&(!move||fresh.score>move.score)){
        move={cards:fresh.cards,index:null,score:fresh.score};
      }
      if(!move)break;
      if(this.difficulty==="facil"&&cryptoInt(10)<4)break;
      const before=this.hands[player].length;
      this.meld(player,move.cards.map(c=>c.id),move.index);
      if(this.hands[player].length===before)break;
      if(this.phase!=="meld"||this.winner!==null)return;
    }
    const hand=this.hands[player];
    if(!hand.length)return;
    let choices=hand;
    if(hand.length===1&&this.deadTaken[team]&&!this.hasClean(team)){
      // Guarda carta e passa após tentativa de baixar; uma rodada não termina em batida ilegal.
      this.log(this.playerName(player)+" aguarda uma canastra limpa.");this.player=(player+1)%4;this.phase="draw";return;
    }
    let picked;
    if(this.difficulty==="facil"){picked=choices[cryptoInt(choices.length)];}
    else {
      const cost=c=>{
        let score=cardValue(c)/3;
        if(c.r===2)score+=42;
        for(const other of hand)if(other.id!==c.id){
          if(other.s===c.s&&Math.abs(other.r-c.r)<=2)score+=hard?18:13;
          if(other.s===c.s&&other.r===c.r)score+=4;
        }
        if(hard)for(const m of this.melds[team])if(isValidRun([...m.cards,c]))score+=75;
        if(this.difficulty==="lendario"&&this.discardPile.slice(-5).some(p=>p.s===c.s&&Math.abs(p.r-c.r)<=2))score+=14;
        return score;
      };
      picked=[...choices].sort((a,b)=>cost(a)-cost(b))[0];
    }
    this.discard(player,picked.id);
  }
}
