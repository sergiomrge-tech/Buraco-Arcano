import {SUITS,SUIT_NAMES,RANK_NAMES,DIFFICULTIES,TEAM_OF,cardLabel,cardPoints,isWild,cardSort,secureInt,secureShuffle,makeDeck,teamFor,meldInfo,findMeldCandidates,scoreTeam,newGame,assertCards,drawStock,takeDiscard,layMeld,emptyHand,discardCard,nextTurn,endRound,chooseBotDraw,chooseBotDiscard,botTurn,hintForHuman} from "./engine.mjs";



const $=id=>document.getElementById(id);
const el={hand:$('hand'),stock:$('stockPile'),discard:$('discardPile'),discardFace:$('discardFace'),enemy:$('enemyMelds'),ally:$('allyMelds'),toast:$('toast'),message:$('message'),phase:$('phaseBadge'),settings:$('settingsDialog'),end:$('endDialog'),difficulty:$('botDifficulty')};
const safeStore={get(key,fallback){try{return localStorage.getItem(key)||fallback}catch{return fallback}},set(key,value){try{localStorage.setItem(key,value)}catch{}}};
let s=newGame(safeStore.get('buracoDifficulty','normal'));
let selected=new Set();let botTimer=0;let toastTimer=0;let sound=safeStore.get('buracoSound','on')!=='off';let audio;

function cardFace(c,classes='') {
  const red=[1,3].includes(c.suit);
  const suits=['♠','♥','♣','♦'];
  return `<div class="playing-card card-${red?'red':'black'} ${classes}" data-card-id="${c.id}" aria-label="${cardLabel(c)}"><div class="card-face"><strong class="card-rank">${({1:'A',11:'J',12:'Q',13:'K'})[c.rank]||c.rank}<span>${suits[c.suit]}</span></strong><span class="card-suit">${suits[c.suit]}</span><strong class="card-small">${({1:'A',11:'J',12:'Q',13:'K'})[c.rank]||c.rank} ${suits[c.suit]}</strong></div><i class="card-corner-ornament"></i><i class="card-corner-ornament second"></i></div>`;
}
function tinyBacks(count,limit=11){return Array.from({length:Math.min(count,limit)},()=>'<span class="tiny-back" aria-hidden="true"></span>').join('')}
function countCanastras(team){return s.melds[team].filter(m=>meldInfo(m)?.canastra).length}
function renderMelds(team,host) {
  const lines=s.melds[team];
  if(!lines.length){host.innerHTML=`<p class="empty-lane">${team===0?'SUAS SEQUÊNCIAS':'SEQUÊNCIAS DOS RIVAIS'}</p>`;return;}
  host.innerHTML=lines.map((cards,index)=>{
    const info=meldInfo(cards);const eligible=team===0&&s.current===0&&s.phase==='play'&&selected.size&&meldInfo([...cards,...s.hands[0].filter(c=>selected.has(c.id))]);
    return `<div class="meld-stack ${eligible?'compatible':''}" data-team="${team}" data-meld="${index}" title="${team===0?'Toque para acrescentar cartas selecionadas':'Sequência adversária'}" role="${team===0?'button':'group'}" tabindex="${team===0?'0':'-1'}">${[...cards].sort(cardSort).map(c=>cardFace(c)).join('')}${info?.canastra?`<span class="meld-badge ${info.canastra==='suja'?'dirty':''}">${info.canastra==='limpa'?'✦ LIMPA':'◆ SUJA'}</span>`:''}</div>`;
  }).join('');
}
function renderHand(){
  const hand=s.hands[0];const n=hand.length;
  const shrink=n>14 ? Math.max(-40,-Math.round((n-11)*2.7)-20) : null;
  el.hand.innerHTML=hand.map((c,i)=>{
    const position=i-(n-1)/2;
    const tilt=Math.max(-10,Math.min(10,position*1.25));
    const arc=Math.min(12,Math.pow(position/(Math.max(n-1,1)/2),2)*9);
    return `<button type="button" class="playing-card hand-card card-${[1,3].includes(c.suit)?'red':'black'} ${selected.has(c.id)?'selected':''}" data-id="${c.id}" aria-pressed="${selected.has(c.id)}" title="${cardLabel(c)}" style="--tilt:${tilt}deg;--arc:${arc}px;${shrink!==null?`margin-left:${i===0?0:shrink}px;`:''}"><div class="card-face"><strong class="card-rank">${({1:'A',11:'J',12:'Q',13:'K'})[c.rank]||c.rank}<span>${['♠','♥','♣','♦'][c.suit]}</span></strong><span class="card-suit">${['♠','♥','♣','♦'][c.suit]}</span><strong class="card-small">${({1:'A',11:'J',12:'Q',13:'K'})[c.rank]||c.rank} ${['♠','♥','♣','♦'][c.suit]}</strong></div></button>`;
  }).join('');
}
function render(){
  if(s.gameOver&&el.end.open===false){showEnd()}
  $('allyScore').textContent=String(s.score[0]).padStart(4,'0');$('enemyScore').textContent=String(s.score[1]).padStart(4,'0');
  $('westCount').textContent=s.hands[3].length;
  $('eastCount').textContent=s.hands[1].length;
  $('northHand').innerHTML=tinyBacks(s.hands[2].length);
  $('westHand').innerHTML=tinyBacks(s.hands[3].length,5);
  $('eastHand').innerHTML=tinyBacks(s.hands[1].length,5);
  $('stockCount').textContent=s.stock.length;$('discardCount').textContent=s.discard.length;
  $('allyDead').textContent=s.dead[0].length;$('enemyDead').textContent=s.dead[1].length;
  $('allyCanastras').textContent=countCanastras(0);$('enemyCanastras').textContent=countCanastras(1);
  $('roundLabel').textContent=String(s.round).padStart(2,'0');
  el.message.textContent=s.lastMove;
  if(s.discard.length){el.discardFace.outerHTML=cardFace(s.discard.at(-1),'blank-card').replace('class="playing-card','id="discardFace" class="playing-card');el.discardFace=$('discardFace')}else{el.discardFace.innerHTML='';}
  const isHuman=s.current===0&&!s.gameOver;
  el.phase.textContent=s.gameOver?'FIM DA RODADA':isHuman?'SUA VEZ':`${['','MORWEN','ELYRA','DARION'][s.current]} JOGA`;
  el.phase.classList.toggle('waiting',!isHuman);
  ['drawBtn','takeBtn'].forEach(id=>$(id).disabled=!isHuman||s.phase!=='draw');
  $('takeBtn').disabled||=s.discard.length===0;
  $('meldBtn').disabled=!isHuman||s.phase!=='play'||selected.size<1;
  $('discardBtn').disabled=!isHuman||s.phase!=='play'||selected.size!==1;
  $('hintBtn').disabled=!isHuman;
  el.stock.classList.toggle('disabled',!isHuman||s.phase!=='draw');
  el.discard.classList.toggle('disabled',!isHuman||s.phase!=='draw');
  $('handTip').textContent=s.phase==='draw'?'Compre do monte ou pegue o lixo':selected.size?`${selected.size} carta(s) selecionada(s)`:'Selecione cartas para formar sequências';
  renderMelds(1,el.enemy);renderMelds(0,el.ally);renderHand();
  // Deliberate invariant check on every UI render; catches lost cards immediately.
  assertCards(s);
}
function toast(message){clearTimeout(toastTimer);el.toast.textContent=message;el.toast.classList.add('show');toastTimer=setTimeout(()=>el.toast.classList.remove('show'),3000)}
function playSound(type){if(!sound)return;try{audio??=new(window.AudioContext||window.webkitAudioContext)();if(audio.state==='suspended')audio.resume();const osc=audio.createOscillator();const gain=audio.createGain();const now=audio.currentTime;osc.type=type==='meld'?'triangle':'sine';osc.frequency.setValueAtTime(type==='fail'?190:type==='meld'?690:380,now);osc.frequency.exponentialRampToValueAtTime(type==='meld'?980:type==='fail'?130:440,now+.11);gain.gain.setValueAtTime(.065,now);gain.gain.exponentialRampToValueAtTime(.001,now+.15);osc.connect(gain).connect(audio.destination);osc.start(now);osc.stop(now+.16)}catch{}}
function safeAction(action,error){if(s.current!==0||s.gameOver){toast('Aguarde sua vez.');return}const success=action();if(!success){toast(error||'Jogada inválida.');playSound('fail');return}playSound('ok');selected.clear();render();queueBots()}
function queueBots(){clearTimeout(botTimer);if(!s.gameOver && s.current!==0){botTimer=setTimeout(()=>{try{botTurn(s);render();queueBots()}catch(e){console.error(e);toast('Erro de lógica: '+e.message)}},640)}}
$('drawBtn').addEventListener('click',()=>safeAction(()=>drawStock(s,0),'Você já comprou nesta rodada.'));
$('takeBtn').addEventListener('click',()=>safeAction(()=>takeDiscard(s,0),'Não é possível pegar o lixo agora.'));
function doMeld(index=null){if(!selected.size){toast('Selecione pelo menos três cartas para uma nova sequência.');return}safeAction(()=>layMeld(s,0,[...selected],index),'Sequência inválida: use 3 ou mais cartas consecutivas do mesmo naipe (até um curinga 2).');playSound('meld')}
$('meldBtn').addEventListener('click',()=>doMeld());
$('discardBtn').addEventListener('click',()=>{if(selected.size!==1){toast('Escolha exatamente uma carta para descartar.');return}safeAction(()=>discardCard(s,0,[...selected][0]),'Selecione uma carta da sua mão.')});
$('hintBtn').addEventListener('click',()=>{const hint=hintForHuman(s);if(!hint)return;if(typeof hint==='string'){toast(hint);return}selected=new Set(hint.ids);render();toast(hint.text)});
el.stock.addEventListener('click',()=>$('drawBtn').click());el.discard.addEventListener('click',()=>$('takeBtn').click());
for(const e of [el.stock,el.discard])e.addEventListener('keydown',ev=>{if(ev.key==='Enter'||ev.key===' '){ev.preventDefault();e.click()}});
el.hand.addEventListener('click',event=>{const button=event.target.closest('[data-id]');if(!button||s.current!==0||s.phase!=='play')return;const id=button.dataset.id;if(selected.has(id))selected.delete(id);else selected.add(id);render();playSound('ok')});
for(const lane of [el.ally,el.enemy])lane.addEventListener('click',event=>{const meld=event.target.closest('[data-meld]');if(!meld||meld.dataset.team!=='0'||!selected.size)return;doMeld(Number(meld.dataset.meld))});
el.ally.addEventListener('keydown',event=>{if(event.key==='Enter'||event.key===' '){const target=event.target.closest('[data-meld]');if(target){event.preventDefault();doMeld(Number(target.dataset.meld))}}});
function reset(){clearTimeout(botTimer);selected.clear();if(el.end.open)el.end.close();s=newGame(el.difficulty.value);render();toast('Nova partida! Baralho embaralhado com aleatoriedade criptográfica.')}
$('newBtn').addEventListener('click',reset);$('playAgain').addEventListener('click',reset);
$('settingsBtn').addEventListener('click',()=>el.settings.showModal());
for(const btn of document.querySelectorAll('[data-close]'))btn.addEventListener('click',()=>el.settings.close());
$('settingsSave').addEventListener('click',()=>{const d=el.difficulty.value;if(DIFFICULTIES.includes(d)){s.difficulty=d;safeStore.set('buracoDifficulty',d)}sound=$('soundCheckbox').checked;safeStore.set('buracoSound',sound?'on':'off');$('soundBtn').textContent=sound?'♪':'♩';el.settings.close();toast('Preferências aplicadas à partida atual.');render()});
$('soundBtn').addEventListener('click',()=>{sound=!sound;$('soundCheckbox').checked=sound;$('soundBtn').textContent=sound?'♪':'♩';safeStore.set('buracoSound',sound?'on':'off')});
function showEnd(){$('endTitle').textContent=s.winner===0?'Vitória da Aurora!':s.winner===1?'As Sombras venceram':'A rodada terminou';$('endText').textContent=s.lastMove;el.end.showModal()}
window.__buraco={getState:()=>s,reset,select:ids=>{selected=new Set(ids);render()},playBots:()=>{while(!s.gameOver&&s.current!==0)botTurn(s);render()}};
el.difficulty.value=s.difficulty;$('soundCheckbox').checked=sound;$('soundBtn').textContent=sound?'♪':'♩';render();
