/* Buraco Arcano - regras de jogo independentes da interface.
   Não usar Math.random no embaralhamento durante partidas normais. */
export const SUITS=["♠","♥","♣","♦"];
export const LABELS={1:"A",11:"J",12:"Q",13:"K"};
export const CARD_COUNT=104;
export const DIFFICULTIES=["facil","normal","dificil","especialista"];
export function rankName(rank){return LABELS[rank]||String(rank);}
export function cardPoints(c){return c.rank===1?15:c.rank===2?20:c.rank>=8?10:5;}
export function secureRandom(max){
  if(!Number.isSafeInteger(max)||max<=0)throw Error("Intervalo inválido");
  if(globalThis.crypto&&typeof globalThis.crypto.getRandomValues==="function"){
    const limit=4294967296-(4294967296%max);
    const v=new Uint32Array(1);
    do{globalThis.crypto.getRandomValues(v);}while(v[0]>=limit);
    return v[0]%max;
  }
  /* Fallback apenas para runtimes de testes sem Web Crypto. */
  return Math.floor(Math.random()*max);
}
export function shuffled(cards,randomInt=secureRandom){
  const copy=[...cards];
  for(let i=copy.length-1;i>0;i--){let j=randomInt(i+1);if(j<0||j>i)throw Error("RNG inválido");[copy[i],copy[j]]=[copy[j],copy[i]];}
  return copy;
}
export function newDeck(){
  const a=[];
  for(let deck=0;deck<2;deck++)for(const suit of SUITS)for(let rank=1;rank<=13;rank++)a.push({id:deck*52+SUITS.indexOf(suit)*13+rank,suit,rank});
  return a;
}
export function teamFor(player){return player%2;}
export function isWild(c){return c.rank===2;}
export function describeMeld(cards){
  if(cards.length<3||cards.length>13)return null;
  const wilds=cards.filter(isWild),naturals=cards.filter(c=>!isWild(c));
  if(wilds.length>1||naturals.length<2)return null;
  const suit=naturals[0].suit;
  if(naturals.some(c=>c.suit!==suit))return null;
  let possibilities=[];
  for(const aceHigh of [false,true]){
    let values=naturals.map(c=>c.rank===1&&aceHigh?14:c.rank).sort((a,b)=>a-b);
    if(new Set(values).size!==values.length)continue;
    let min=values[0],max=values[values.length-1];
    let gaps=max-min+1-values.length;
    if(gaps>wilds.length)continue;
    let start=min,end=max;
    let missing=wilds.length-gaps;
    while(missing>0&&start>3){start--;missing--;}
    while(missing>0&&end<14){end++;missing--;}
    if(missing)continue;
    if(end-start+1!==cards.length)continue;
    possibilities.push({suit,start,end,wild:wilds.length===1,clean:wilds.length===0,canasta:cards.length>=7,points:cards.reduce((n,c)=>n+cardPoints(c),0)});
  }
  possibilities.sort((a,b)=>a.start-b.start);
  return possibilities[0]||null;
}
export function createGame({difficulty="normal",randomInt=secureRandom}={}){
  if(!DIFFICULTIES.includes(difficulty))throw Error("Dificuldade inválida");
  const deck=shuffled(newDeck(),randomInt);
  const hands=[[],[],[],[]];for(let x=0;x<11;x++)for(let p=0;p<4;p++)hands[p].push(deck.pop());
  const mortos=[[],[]];for(let x=0;x<11;x++)for(let t=0;t<2;t++)mortos[t].push(deck.pop());
  const discard=[deck.pop()];
  return {hands,mortos,stock:deck,discard,melds:[[],[]],deadTaken:[false,false],player:0,phase:"draw",difficulty,winner:null,turn:1,history:["Partida iniciada. Duas equipes, 104 cartas e dois mortos."],lastAction:"Nova partida"};
}
export function cardsOf(game,player=game.player){return game.hands[player];}
function assertTurn(g,p,phase){if(g.winner!==null)throw Error("Partida encerrada");if(p!==g.player)throw Error("Não é a sua vez");if(g.phase!==phase)throw Error("Ação indisponível nesta fase");}
function addLog(g,message){g.lastAction=message;g.history.unshift(message);g.history=g.history.slice(0,28);}
export function draw(g,player,source){
  assertTurn(g,player,"draw");
  if(source!=="stock"&&source!=="discard")throw Error("Origem inválida");
  if(source==="stock"){
    if(g.stock.length===0){if(g.discard.length<=1)throw Error("Não há cartas para comprar");const top=g.discard.pop();g.stock=shuffled(g.discard);g.discard=[top];}
    g.hands[player].push(g.stock.pop());
  }else{
    if(!g.discard.length)throw Error("Lixeira vazia");
    g.hands[player].push(...g.discard.splice(0));
  }
  g.phase="play";addLog(g,"Jogador "+(player+1)+" comprou "+(source==="stock"?"do monte":"a lixeira")+".");
}
function removeSelected(hand,ids){
  if(!ids.length||new Set(ids).size!==ids.length)throw Error("Seleção inválida");
  const found=ids.map(id=>hand.find(c=>c.id===id));
  if(found.some(c=>!c))throw Error("Carta não está na mão");
  return found;
}
function hasCleanCanasta(g,team){return g.melds[team].some(m=>{let d=describeMeld(m);return d&&d.canasta&&d.clean;});}
function acquireDead(g,p){
  const t=teamFor(p);
  if(!g.deadTaken[t]){
    if(!g.mortos[t].length)throw Error("Morto indisponível");
    g.hands[p].push(...g.mortos[t].splice(0));g.deadTaken[t]=true;
    addLog(g,"Equipe "+(t===0?"Sol":"Lua")+" pegou o morto!");
    return true;
  }
  if(hasCleanCanasta(g,t)){
    g.winner=t;g.phase="finished";addLog(g,"Batida! Vitória da Casa "+(t===0?"Solar":"Lunar")+"!");
    return true;
  }
  return false;
}
export function layMeld(g,p,ids,existingIndex=null){
  assertTurn(g,p,"play");
  const selected=removeSelected(g.hands[p],ids);
  const t=teamFor(p);
  const original=existingIndex===null?[]:g.melds[t][existingIndex];
  if(!original)throw Error("Sequência não encontrada");
  const combined=[...original,...selected];
  const description=describeMeld(combined);
  if(!description)throw Error("Precisa ser sequência válida do mesmo naipe com pelo menos 3 cartas e no máximo um 2 curinga.");
  if(g.hands[p].length===selected.length&&g.deadTaken[t]&&!g.mortos[t].length&&!hasCleanCanasta(g,t)&&!(description.canasta&&description.clean))throw Error("Para bater, sua equipe precisa de canastra limpa.");
  g.hands[p]=g.hands[p].filter(c=>!ids.includes(c.id));
  if(existingIndex===null)g.melds[t].push(combined);else g.melds[t][existingIndex]=combined;
  addLog(g,"Jogador "+(p+1)+" baixou "+selected.length+" carta(s).");
  if(g.hands[p].length===0)acquireDead(g,p);
  return description;
}
export function discardCard(g,p,id){
  assertTurn(g,p,"play");
  const hand=g.hands[p],card=hand.find(c=>c.id===id);
  if(!card)throw Error("Selecione uma carta da sua mão.");
  const t=teamFor(p);
  if(hand.length===1&&g.deadTaken[t]&&!hasCleanCanasta(g,t))throw Error("É necessária uma canastra limpa antes de bater.");
  g.hands[p]=hand.filter(c=>c.id!==id);g.discard.push(card);
  addLog(g,"Jogador "+(p+1)+" descartou "+rankName(card.rank)+card.suit+".");
  if(g.hands[p].length===0){
    if(g.deadTaken[t]){g.winner=t;g.phase="finished";addLog(g,"Batida! Vitória da Casa "+(t===0?"Solar":"Lunar")+"!");return;}
    acquireDead(g,p);
  }
  nextTurn(g);
}
export function nextTurn(g){if(g.winner!==null)return;g.player=(g.player+1)%4;g.phase="draw";g.turn++;if(g.turn>500){g.winner=hasCleanCanasta(g,0)?0:hasCleanCanasta(g,1)?1:2;g.phase="finished";}}
export function calculateScore(g,team){
  let total=0;
  for(const meld of g.melds[team]){let d=describeMeld(meld);if(!d)continue;total+=d.points;if(d.canasta)total+=d.clean?200:100;}
  for(let p=team;p<4;p+=2)total-=g.hands[p].reduce((n,c)=>n+cardPoints(c),0);
  if(!g.deadTaken[team])total-=100;
  if(g.winner===team)total+=100;
  return total;
}
export function autoMeldOptions(hand,existing=[],minSize=3){
  let options=[];
  const naturals=hand.filter(c=>!isWild(c));
  for(const suit of SUITS){
    const pool=naturals.filter(c=>c.suit===suit);
    for(let low=1;low<=13;low++)for(let high=low+2;high<=14&&high-low<=12;high++){
      const numbers=[];let missing=0;
      for(let r=low;r<=high;r++){
        if(r===2){missing++;continue;}
        let found=pool.find(c=>c.rank===(r===14?1:r)&&!numbers.some(k=>k.id===c.id));
        if(found)numbers.push(found);else missing++;
      }
      if(missing<=1){
        const wild=hand.find(c=>isWild(c));
        const candidate=missing===1&&wild?[...numbers,wild]:numbers;
        if(candidate.length>=minSize&&describeMeld(candidate)){
          const ids=candidate.map(c=>c.id);
          if(!options.some(o=>o.ids.join(",")===ids.join(",")))options.push({ids,description:describeMeld(candidate)});
        }
      }
    }
  }
  if(existing.length)for(let i=0;i<existing.length;i++){
    for(const card of hand)if(describeMeld([...existing[i],card]))options.push({ids:[card.id],index:i,description:describeMeld([...existing[i],card])});
  }
  return options;
}
export function bestDiscard(hand,level,randomInt=secureRandom){
  if(!hand.length)return null;
  if(level==="facil")return hand[randomInt(hand.length)];
  const naturals=hand.filter(c=>!isWild(c));
  function value(c){
    let v=cardPoints(c)*(level==="normal"?0.75:1.35);
    if(isWild(c))return 1000;
    const related=naturals.filter(x=>x.id!==c.id&&x.suit===c.suit);
    for(const other of related){
      let dist=Math.abs((other.rank===1?14:other.rank)-(c.rank===1?14:c.rank));
      if(dist===1)v+=level==="especialista"?40:26;
      else if(dist===2)v+=level==="especialista"?20:12;
    }
    return v;
  }
  return [...hand].sort((a,b)=>value(a)-value(b))[0];
}
export function botTurn(g,randomInt=secureRandom){
  if(g.player===0||g.winner!==null)throw Error("Turno não é de bot");
  const p=g.player,t=teamFor(p),level=g.difficulty;
  if(g.phase==="draw"){
    const top=g.discard[g.discard.length-1];
    let discardUseful=top&&autoMeldOptions([...g.hands[p],top],g.melds[t]).some(o=>o.ids.includes(top.id));
    if(g.discard.length>0&&discardUseful&&level!=="facil")draw(g,p,"discard");
    else draw(g,p,"stock");
  }
  if(g.phase!=="play")return;
  let loop=0;
  const iterations=level==="facil"?1:level==="normal"?5:level==="dificil"?9:15;
  while(g.phase==="play"&&loop++<iterations){
    const options=autoMeldOptions(g.hands[p],g.melds[t]);
    if(!options.length)break;
    options.sort((a,b)=>{
      const score=o=>o.ids.length*6+(o.description.canasta?50:0)+(o.description.clean?12:0)+(o.index!==undefined?15:0);
      return score(b)-score(a);
    });
    const choice=level==="facil"?options[randomInt(options.length)]:options[0];
    if(level==="facil"&&randomInt(100)<35)break;
    try{layMeld(g,p,choice.ids,choice.index===undefined?null:choice.index);}catch{break;}
  }
  if(g.phase==="play"){
    const c=bestDiscard(g.hands[p],level,randomInt);
    if(c)try{discardCard(g,p,c.id);}catch{
      const alternative=g.hands[p].find(x=>x.id!==c.id);
      if(alternative)discardCard(g,p,alternative.id);
      else{g.winner=2;g.phase="finished";addLog(g,"Mão bloqueada: empate técnico.");}
    }
  }
}
