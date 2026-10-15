
// Buraco Arcano — game engine; no DOM dependencies.
const SUITS = ['♠','♥','♣','♦'];
const SUIT_NAMES = ['espadas','copas','paus','ouros'];
const RANK_NAMES = {1:'A',11:'J',12:'Q',13:'K'};
const DIFFICULTIES = ['facil','normal','dificil','especialista'];
const TEAM_OF = [0,1,0,1];
const cardLabel = c => `${RANK_NAMES[c.rank]||c.rank}${SUITS[c.suit]}`;
const cardPoints = c => c.rank===1 ? 15 : c.rank===2 ? 10 : c.rank<=7 ? 5 : 10;
const isWild = c => c.rank===2;
const cardSort = (a,b) => a.suit-b.suit || a.rank-b.rank || a.id.localeCompare(b.id);

function secureInt(limit, randomWords) {
  if (!Number.isSafeInteger(limit) || limit<1 || limit>0x100000000) throw new RangeError('invalid bound');
  // Rejection sampling prevents modulo bias. Browser crypto is mandatory in live play.
  const range = 0x100000000;
  const threshold = range - range % limit;
  let value;
  do { value = randomWords()[0]; } while(value>=threshold);
  return value % limit;
}
function secureShuffle(input, randomWords = () => {
  if (!globalThis.crypto?.getRandomValues) throw new Error('Secure random generator unavailable');
  return globalThis.crypto.getRandomValues(new Uint32Array(1));
}) {
  const result = [...input];
  for(let i=result.length-1;i>0;i--){
    const j=secureInt(i+1,randomWords);
    [result[i],result[j]]=[result[j],result[i]];
  }
  return result;
}
function makeDeck() {
  const cards=[];
  for(let deck=0;deck<2;deck++) for(let suit=0;suit<4;suit++) for(let rank=1;rank<=13;rank++)
    cards.push({id:`${deck}-${suit}-${rank}`,deck,suit,rank});
  return cards;
}
const teamFor = seat => TEAM_OF[seat];

function runEvaluation(cards, wildIndex, aceHigh, suit) {
  const naturals=cards.filter((c,i)=>i!==wildIndex);
  if(naturals.some(c=>c.suit!==suit)) return null;
  const values=naturals.map(c=>c.rank===1 && aceHigh?14:c.rank).sort((a,b)=>a-b);
  if(new Set(values).size!==values.length) return null;
  if(!values.length) return null;
  const isWildUsed=wildIndex!==-1;
  if(isWildUsed && cards[wildIndex].rank!==2) return null;
  if(isWildUsed && values.length<2) return null;
  let gaps=0;
  for(let i=1;i<values.length;i++) gaps+=values[i]-values[i-1]-1;
  if(gaps<0 || gaps>(isWildUsed?1:0)) return null;
  if(!isWildUsed && gaps!==0) return null;
  const low=values[0], high=values[values.length-1];
  if(cards.length>13) return null;
  if(high>14 || low<1) return null;
  // No wrapping King to 2; a single wildcard can extend a contiguous run.
  return {suit,wild:isWildUsed,clean:!isWildUsed,length:cards.length,low,high,gaps,
    canastra:cards.length>=7 ? (isWildUsed?'suja':'limpa') : null};
}
function meldInfo(cards) {
  if(!Array.isArray(cards) || cards.length<3) return null;
  if(new Set(cards.map(c=>c.id)).size!==cards.length) return null;
  const candidates=[];
  for(const suit of [0,1,2,3]) for(const highAce of [false,true]) {
    const positions=[-1,...cards.map((c,i)=>c.rank===2?i:-1).filter(i=>i>=0)];
    for(const wildIndex of positions){
      // A natural 2 only belongs to its own suit; wildcard can be any 2.
      const value=runEvaluation(cards,wildIndex,highAce,suit);
      if(value) candidates.push(value);
    }
  }
  candidates.sort((a,b)=>Number(a.wild)-Number(b.wild) || a.low-b.low);
  return candidates[0]||null;
}
function findMeldCandidates(hand,maxResults=30) {
  // Generate all consecutive suit/rank intervals (Ace low OR Ace high).
  // Each interval can omit at most one rank, completed by a 2 as curinga.
  // Duplicate natural ranks are never used in a single sequence.
  const found=new Map();
  for(let suit=0;suit<4;suit++) for(const aceHigh of [false,true]) {
    const available=new Map();
    for(const c of hand) {
      if(c.suit!==suit)continue;
      const v=c.rank===1&&aceHigh ? 14:c.rank;
      if(!available.has(v))available.set(v,c);
    }
    for(let low=aceHigh?2:1;low<=12;low++) for(let high=low+2;high<=14 && high-low+1<=13;high++) {
      if(!aceHigh && high===14)continue;
      const selected=[];const missing=[];
      for(let rank=low;rank<=high;rank++) {
        if(available.has(rank))selected.push(available.get(rank));
        else missing.push(rank);
      }
      if(missing.length>1 || selected.length<2)continue;
      if(missing.length===1){
        const wild=hand.find(c=>c.rank===2&&!selected.some(x=>x.id===c.id));
        if(!wild)continue;
        selected.push(wild);
      }
      const info=meldInfo(selected);
      if(!info)continue;
      const key=selected.map(c=>c.id).sort().join(',');
      if(!found.has(key))found.set(key,{cards:selected,info});
    }
  }
  return [...found.values()]
    .sort((a,b)=>b.cards.length-a.cards.length || Number(a.info.wild)-Number(b.info.wild))
    .slice(0,maxResults);
}
function scoreTeam(melds, handCards, tookDead, wentOut=false) {
  let sum=0;
  for(const m of melds){
    sum+=m.reduce((n,c)=>n+cardPoints(c),0);
    const info=meldInfo(m);
    if(info?.canastra==='limpa') sum+=200;
    else if(info?.canastra==='suja') sum+=100;
  }
  sum-=handCards.reduce((n,c)=>n+cardPoints(c),0);
  if(!tookDead)sum-=100;
  if(wentOut) sum+=100;
  return sum;
}
function newGame(difficulty='normal') {
  const shuffled=secureShuffle(makeDeck());
  const hands=[[],[],[],[]];
  for(let n=0;n<11;n++) for(let seat=0;seat<4;seat++)hands[seat].push(shuffled.pop());
  const dead=[[],[]];
  for(let n=0;n<11;n++) for(let t=0;t<2;t++)dead[t].push(shuffled.pop());
  const discard=[shuffled.pop()];
  hands.forEach(h=>h.sort(cardSort));
  return {hands,dead,deadTaken:[false,false],stock:shuffled,discard,melds:[[],[]],
    current:0,phase:'draw',turn:1,round:1,score:[0,0],gameOver:false,winner:null,
    difficulty:DIFFICULTIES.includes(difficulty)?difficulty:'normal',lastMove:'As cartas foram distribuídas. Sua vez!',
    history:['Uma nova partida começou.'],botThinking:false};
}
function assertCards(state) {
  const all=[...state.hands.flat(),...state.dead.flat(),...state.stock,...state.discard,...state.melds.flat(2)];
  if(all.length!==104)throw new Error(`Card conservation violated: ${all.length} instead of 104`);
  if(new Set(all.map(c=>c.id)).size!==104)throw new Error('Duplicated card IDs');
  return true;
}
function drawStock(s, seat) {
  if(s.gameOver || s.current!==seat || s.phase!=='draw')return false;
  if(!s.stock.length){endRound(s,null,'O monte acabou.');return true;}
  s.hands[seat].push(s.stock.pop());s.hands[seat].sort(cardSort);
  s.phase='play';s.lastMove=`${seat===0?'Você':'Jogador '+(seat+1)} comprou do monte.`;
  return true;
}
function takeDiscard(s, seat) {
  if(s.gameOver || s.current!==seat || s.phase!=='draw'|| !s.discard.length)return false;
  s.hands[seat].push(...s.discard.splice(0));s.hands[seat].sort(cardSort);
  s.phase='play';s.lastMove=seat===0?'Você pegou o lixo.':'Um oponente pegou o lixo.';
  return true;
}
function layMeld(s, seat, ids, meldIndex=null) {
  if(s.gameOver || s.current!==seat || s.phase!=='play')return false;
  const idsUnique=new Set(ids);if(idsUnique.size!==ids.length || !ids.length)return false;
  const h=s.hands[seat];const chosen=ids.map(id=>h.find(c=>c.id===id));
  if(chosen.some(c=>!c))return false;
  const team=teamFor(seat);
  const target=meldIndex==null?[]:s.melds[team][meldIndex];
  if(meldIndex!=null&&!target)return false;
  const combined=[...target,...chosen];const info=meldInfo(combined);
  if(!info)return false;
  // A team that has already claimed its morto cannot empty its hand
  // unless it has a canastra (this action itself may complete it).
  const handWouldEmpty=h.length===chosen.length;
  if(handWouldEmpty && s.deadTaken[team] && !info.canastra &&
     !s.melds[team].some((m,i)=>meldIndex===i ? false : meldInfo(m)?.canastra))return false;
  s.hands[seat]=h.filter(c=>!idsUnique.has(c.id));
  if(meldIndex==null)s.melds[team].push(combined);else s.melds[team][meldIndex]=combined;
  s.lastMove=`${seat===0?'Você':'Jogador '+(seat+1)} ${meldIndex==null?'baixou':'ampliou'} uma sequência${info.canastra?' — canastra '+info.canastra:''}.`;
  if(s.hands[seat].length===0)emptyHand(s,seat,false);
  return true;
}
function emptyHand(s,seat,byDiscard){
  const t=teamFor(seat);
  if(!s.deadTaken[t] && s.dead[t].length) {
    s.hands[seat].push(...s.dead[t].splice(0));s.hands[seat].sort(cardSort);
    s.deadTaken[t]=true;
    s.lastMove += ' O morto foi conquistado!';
    if(byDiscard)s.phase='end';
  } else if(s.melds[t].some(m=>meldInfo(m)?.canastra)) {
    endRound(s,t,'Batida!');
  } else {
    // Kept as a defensive assertion; legal actions block this state.
    throw new Error('Batida sem canastra: jogada deveria ter sido bloqueada');
  }
}
function discardCard(s,seat,id) {
  if(s.gameOver||s.current!==seat||s.phase!=='play')return false;
  const index=s.hands[seat].findIndex(c=>c.id===id);
  if(index<0)return false;
  const team=teamFor(seat);
  if(s.hands[seat].length===1 && s.deadTaken[team] &&
    !s.melds[team].some(m=>meldInfo(m)?.canastra))return false;
  const [card]=s.hands[seat].splice(index,1);s.discard.push(card);
  s.lastMove=`${seat===0?'Você':'Jogador '+(seat+1)} descartou ${cardLabel(card)}.`;
  if(!s.hands[seat].length)emptyHand(s,seat,true);
  if(!s.gameOver)nextTurn(s);
  return true;
}
function nextTurn(s){s.current=(s.current+1)%4;s.phase='draw';s.turn++;s.history.push(s.lastMove);if(s.history.length>12)s.history.shift();}
function endRound(s,winningTeam=null,reason='Rodada encerrada.') {
  if(s.gameOver)return;
  const points=[0,1].map(team=>scoreTeam(s.melds[team],s.hands.filter((_,seat)=>teamFor(seat)===team).flat(),s.deadTaken[team],team===winningTeam));
  s.score[0]+=points[0];s.score[1]+=points[1];s.gameOver=true;
  s.winner=winningTeam===null?(points[0]===points[1]?null:Number(points[1]>points[0])):winningTeam;
  s.phase='finished';s.lastMove=`${reason} Casas da Aurora: ${points[0]} pontos; Casas das Sombras: ${points[1]} pontos.`;
}
const valueOfCard = c => cardPoints(c) + (c.rank===2?9:0) + (c.rank===1?3:0);
function formationPotential(h){
  let score=0;
  for(const c of h)for(const d of h)if(c.id!==d.id){
    if(c.suit===d.suit && Math.abs(c.rank-d.rank)===1) score+=6;
    if(c.suit===d.suit && Math.abs(c.rank-d.rank)===2) score+=2;
  }
  return score;
}
function chooseBotDraw(s,seat,difficulty=s.difficulty) {
  if(!s.discard.length)return 'stock';
  const hand=s.hands[seat],top=s.discard[s.discard.length-1];
  if(difficulty==='facil')return Math.random()<.15?'discard':'stock';
  const groups=[...hand,...s.discard];
  const helpful=!!findMeldCandidates(groups,100).find(x=>x.cards.some(c=>c.id===top.id));
  if(difficulty==='normal')return helpful && s.discard.length<=2?'discard':'stock';
  const future=formationPotential(groups)-formationPotential(hand);
  const topUseful=s.melds[teamFor(seat)].some(m=>meldInfo([...m,top]));
  if(difficulty==='dificil')return (topUseful || helpful && future>5)?'discard':'stock';
  // Expert avoids bloating their hand unless discard enables meaningful meld(s).
  const canPlay=topUseful || helpful;
  const deadNeeded=!s.deadTaken[teamFor(seat)];
  return canPlay && (s.discard.length<=4 || future>s.discard.length*(deadNeeded?5:3))?'discard':'stock';
}
function chooseBotDiscard(s,seat,difficulty=s.difficulty) {
  const hand=s.hands[seat];
  if(!hand.length)return null;
  const scoreCandidate=c=>{
    const remaining=hand.filter(x=>x.id!==c.id);
    let score=valueOfCard(c);
    if(difficulty==='normal')score-=formationPotential(remaining)*.12;
    if(difficulty==='dificil'||difficulty==='especialista'){
      score+=formationPotential(remaining)*.45;
      if(c.rank===2)score-=45;
      for(const m of s.melds[teamFor(seat)])if(meldInfo([...m,c]))score-=35;
      if(difficulty==='especialista'){
        const opponents=s.hands.filter((_,p)=>teamFor(p)!==teamFor(seat));
        const urgent=opponents.some(h=>h.length<=3);
        if(urgent && c.rank<=7)score+=10;
      }
    }
    return score;
  };
  if(difficulty==='facil')return hand[Math.floor(Math.random()*hand.length)].id;
  return [...hand].sort((a,b)=>scoreCandidate(b)-scoreCandidate(a))[0].id;
}
function botTurn(s,seat=s.current) {
  if(seat===0||s.current!==seat||s.gameOver)return false;
  if(s.phase==='draw') {
    if(chooseBotDraw(s,seat)==='discard')takeDiscard(s,seat);else drawStock(s,seat);
  }
  if(s.gameOver)return true;
  if(s.phase!=='play')return false;
  const skill=DIFFICULTIES.indexOf(s.difficulty);
  let budget=skill===0?1:skill===1?2:skill===2?5:8;
  while(budget-->0&&!s.gameOver&&s.phase==='play') {
    let played=false;
    for(const card of [...s.hands[seat]]) {
      for(let i=0;i<s.melds[teamFor(seat)].length;i++){
        if(meldInfo([...s.melds[teamFor(seat)][i],card]) && (skill>0 || Math.random()<.25)) {
          played=layMeld(s,seat,[card.id],i);break;
        }
      }
      if(played)break;
    }
    if(played)continue;
    const options=findMeldCandidates(s.hands[seat],100);
    const plan=options.sort((a,b)=>{
      const qa=a.cards.length*10 + (a.info.clean?4:0) + (a.info.canastra?40:0);
      const qb=b.cards.length*10 + (b.info.clean?4:0) + (b.info.canastra?40:0);
      return qb-qa;
    })[0];
    if(plan && (skill>0||Math.random()<.38)){
      played=layMeld(s,seat,plan.cards.map(c=>c.id));
    }
    if(!played)break;
  }
  if(!s.gameOver && s.phase==='play')discardCard(s,seat,chooseBotDiscard(s,seat));
  return true;
}
function hintForHuman(s) {
  if(s.current!==0||s.gameOver)return null;
  if(s.phase==='draw')return 'Compre do monte ou pegue todas as cartas do lixo.';
  for(const c of s.hands[0]) for(let i=0;i<s.melds[0].length;i++)if(meldInfo([...s.melds[0][i],c]))
    return {text:`Você pode adicionar ${cardLabel(c)} a uma sequência da sua dupla.`,ids:[c.id],meldIndex:i};
  const candidates=findMeldCandidates(s.hands[0]);
  if(candidates.length)return {text:'Existe uma sequência que você pode baixar.',ids:candidates[0].cards.map(c=>c.id),meldIndex:null};
  return {text:'Não há sequência pronta. Descarte uma carta para encerrar a vez.',ids:[],meldIndex:null};
}

export {SUITS,SUIT_NAMES,RANK_NAMES,DIFFICULTIES,TEAM_OF,cardLabel,cardPoints,isWild,cardSort,secureInt,secureShuffle,makeDeck,teamFor,meldInfo,findMeldCandidates,scoreTeam,newGame,assertCards,drawStock,takeDiscard,layMeld,emptyHand,discardCard,nextTurn,endRound,chooseBotDraw,chooseBotDiscard,botTurn,hintForHuman};
