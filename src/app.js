import {BuracoGame,cardName,cardValue,displaySort,isValidRun,meldBonus} from "./engine.js";
const $=id=>document.getElementById(id);
const difficultyNames={facil:"Aprendiz · Fácil",normal:"Estrategista · Normal",dificil:"Mestre · Difícil",lendario:"Oráculo · Lendário"};
let difficulty=localStorage.getItem("buraco-arcano-difficulty")||"normal";
if(!difficultyNames[difficulty])difficulty="normal";
let game=new BuracoGame(difficulty),selected=new Set(),thinking=false,paused=false;
const portraits=[
  {a:"#274c69",b:"#58bba6",skin:"#dbad83",hair:"#684233",cape:"#bb7139",orn:"#d8d58d",gender:0},
  {a:"#2a1b44",b:"#884972",skin:"#c29a7e",hair:"#c9c9e2",cape:"#462a5f",orn:"#d7c0ff",gender:1},
  {a:"#175266",b:"#9ddace",skin:"#f1d5b4",hair:"#e6c48a",cape:"#3d8d88",orn:"#fff0c5",gender:1},
  {a:"#42252f",b:"#bc643e",skin:"#bf916d",hair:"#432f2e",cape:"#802d35",orn:"#fbcb78",gender:0}
];
function portraitSVG(a){
 return '<svg viewBox="0 0 110 125" aria-hidden="true" xmlns="http://www.w3.org/2000/svg"><defs><radialGradient id="bg"><stop stop-color="'+a.b+'"/><stop offset="1" stop-color="'+a.a+'"/></radialGradient><linearGradient id="steel" x2=".8" y2="1"><stop stop-color="#fff1b0"/><stop offset=".5" stop-color="'+a.orn+'"/><stop offset="1" stop-color="#5c403c"/></linearGradient></defs>'+
 '<rect width="110" height="125" fill="url(#bg)"/><circle cx="55" cy="35" r="39" fill="#fff2bd" opacity=".08"/><path d="M7 125 Q5 86 29 83 L44 75 66 75 86 83 Q110 93 105 125" fill="'+a.cape+'" stroke="'+a.orn+'" stroke-width="3"/>'+
 '<path d="M30 86 L54 111 81 85 70 82 40 82Z" fill="#171e30" opacity=".65"/><path d="M47 70 L47 85 Q55 96 64 84 L63 70" fill="'+a.skin+'"/>'+
 '<path d="M30 46 Q25 9 53 10 Q84 8 82 49 L80 69 65 79 45 79 32 67Z" fill="'+a.hair+'" stroke="#231926" stroke-width="2"/>'+
 '<path d="M36 43 Q36 18 55 17 Q77 17 75 46 L73 61 Q67 76 55 77 Q43 74 38 61Z" fill="'+a.skin+'"/>'+
 '<path d="M35 42 Q40 24 46 26 Q60 31 72 22 L78 43 Q76 16 61 13 36 11 31 33Z" fill="'+a.hair+'"/>'+
 '<path d="M40 51 Q46 47 51 52 M59 52 Q64 47 70 51" stroke="#392724" fill="none" stroke-width="2"/>'+
 '<path d="M42 55 L50 55 M61 55 L68 55" stroke="#1c2735" stroke-width="2" stroke-linecap="round"/>'+
 '<path d="M54 58 L51 65 Q55 68 59 65" stroke="#8d5a4d" fill="none" stroke-width="1.7"/>'+
 '<path d="M45 69 Q55 74 65 69" fill="none" stroke="#7b3a3c" stroke-width="1.5"/>'+
 (a.gender?'<path d="M28 40 Q26 8 55 7 Q87 9 83 50 L89 80 Q76 79 72 62 L73 31 Q50 42 34 34 L38 69 25 82Z" fill="'+a.hair+'" stroke="#3c2b2b" stroke-width="1.5"/>':'<path d="M27 42 Q28 15 55 12 Q78 12 85 42 L77 35 Q69 29 62 24 L53 33 38 34Z" fill="'+a.hair+'"/>')+
 '<path d="M31 33 Q56 7 79 31 L73 26 67 12 55 8 44 17 38 27Z" fill="url(#steel)" opacity=".8"/>'+
 '<path d="M24 96 L10 111 22 125 M85 96 L100 111 91 125" fill="none" stroke="'+a.orn+'" stroke-width="3"/>'+
 '<path d="M43 89 L55 98 67 89" stroke="'+a.orn+'" fill="none" stroke-width="2.8"/>'+
 '<path d="M55 102 L59 108 55 113 51 108Z" fill="url(#steel)"/>'+
 '<g fill="#fff" opacity=".15"><circle cx="20" cy="23" r="2"/><circle cx="89" cy="14" r="2"/><circle cx="20" cy="76" r="1"/></g></svg>';
}
for(let i=0;i<4;i++)$("avatar-"+i).innerHTML=portraitSVG(portraits[i]);
function cardMarkup(c,mini=false){
 const red=c.s===1||c.s===2;
 if(mini)return '<span class="meld-mini '+(red?'red':'')+'" title="'+cardName(c)+'">'+cardName(c)+'</span>';
 const rank={1:"A",11:"J",12:"Q",13:"K"}[c.r]||c.r, mark=["♠","♥","♦","♣"][c.s];
 return '<span class="card-corner">'+rank+'<small>'+mark+'</small></span><span class="card-center">'+mark+'</span><span class="card-corner card-bottom">'+rank+'<small>'+mark+'</small></span>';
}
function renderHands(){
 const hand=$("hand-0"),cards=[...game.hands[0]].sort(displaySort),W=$("game").clientWidth;
 const cardWidth=Math.max(36,Math.min(82,W*.055));
 const room=Math.max(200,Math.min(W*.55,780));
 const stride=cards.length>1?Math.min(cardWidth*.72,Math.max(13,(room-cardWidth)/(cards.length-1))):0;
 hand.replaceChildren();
 cards.forEach((c,i)=>{
   const button=document.createElement("button");
   button.type="button";button.className="play-card"+((c.s===1||c.s===2)?" red":"")+(selected.has(c.id)?" selected":"");
   button.dataset.cardId=c.id;button.title=cardName(c);
   button.setAttribute("aria-label",cardName(c)+(selected.has(c.id)?" selecionada":""));
   button.style.marginLeft=i?((stride-cardWidth)+"px"):"0";
   button.style.zIndex=selected.has(c.id)?"50":i;
   button.innerHTML=cardMarkup(c);
   button.addEventListener("click",()=>{if(game.player!==0||game.phase!=="meld"||thinking||paused)return;selected.has(c.id)?selected.delete(c.id):selected.add(c.id);render();});
   hand.append(button);
 });
 for(let p=1;p<4;p++){
  const el=$("hand-"+p),n=game.hands[p].length;el.replaceChildren();
  for(let i=0;i<Math.min(9,n);i++){const b=document.createElement("span");b.className="back-mini";el.append(b);}
  $("count-"+p).textContent=n+" CARTAS";
 }
 $("my-cards").textContent=game.hands[0].length+" CARTAS";
}
function renderMelds(){
 for(let t=0;t<2;t++){
   const row=$(t?"meld-enemy":"meld-friendly");row.replaceChildren();
   game.melds[t].forEach((m,i)=>{
    const div=document.createElement("div");const valid=isValidRun(m.cards);
    div.className="meld-group"+(t===0?" own":"");
    div.dataset.meldIndex=i;
    div.setAttribute("role",t===0?"button":"group");div.tabIndex=t===0?0:-1;
    div.title=t===0?"Selecione cartas e toque aqui para adicionar ao jogo":m.cards.map(cardName).join(", ");
    div.innerHTML=m.cards.map(c=>cardMarkup(c,true)).join("")+(m.cards.length>=7?'<span class="canastra-tag '+(valid?.clean?"clean":"")+'">'+(valid?.clean?"LIMPA":"SUJA")+"</span>":"");
    if(t===0){const play=()=>{if(selected.size===0){game.log("Selecione cartas antes de ampliar uma sequência.");render();return;}if(game.meld(0,[...selected],i))selected.clear();render();showEndIfNeeded();};div.addEventListener("click",play);div.addEventListener("keydown",e=>{if(e.key==="Enter")play();});}
    row.append(div);
   });
 }
}
function render(){
 renderHands();renderMelds();
 $("score-aurora").textContent=game.totals[0].toLocaleString("pt-BR");
 $("score-sombra").textContent=game.totals[1].toLocaleString("pt-BR");
 $("round-label").textContent="RODADA "+String(game.round).padStart(2,"0");
 $("stock-count").textContent=game.stock.length;
 $("discard-count").textContent=game.discardPile.length;
 const last=game.discardPile.at(-1),face=$("discard-face");
 face.className="discard-face"+(last&&(last.s===1||last.s===2)?" red":"");
 face.textContent=cardName(last)||"–";
 ["dead-left","dead-right"].forEach((id,i)=>{document.querySelector("."+id).classList.toggle("used",!game.deads[i]);});
 const active=game.player===0&&game.winner===null&&!thinking&&!paused;
 $("stock").disabled=!active||game.phase!=="draw";
 $("discard-pile").disabled=!active||game.phase!=="draw"||!game.discardPile.length;
 $("meld-btn").disabled=!active||game.phase!=="meld"||selected.size<3;
 $("discard-btn").disabled=!active||game.phase!=="meld"||selected.size!==1;
 $("game-phase").textContent=game.winner!==null?"FIM DA RODADA":game.phase==="draw"?"AGUARDANDO COMPRA":"SELECIONE E BAIXE OU DESCARTE";
 const info=game.winner!==null?"RODADA ENCERRADA":thinking?"OS RIVAIS ESTÃO JOGANDO":game.player===0?(game.phase==="draw"?"SUA VEZ · COMPRE DO MONTE OU DO LIXO":"SUA VEZ · BAIXE JOGOS OU DESCARTE"):game.playerName(game.player).toUpperCase()+" ESTÁ JOGANDO";
 $("turn-banner").textContent=info;
 document.querySelectorAll(".seat").forEach(el=>el.classList.toggle("active",+el.dataset.player===game.player&&game.winner===null));
 document.querySelector(".player-portrait").classList.toggle("active",game.player===0&&game.winner===null);
 $("message").textContent=game.message;
}
function showModal(title,body,closable=true){
 paused=true;$("modal-title").textContent=title;$("modal-body").innerHTML=body;$("overlay").hidden=false;
 $("modal-close").hidden=!closable;render();
}
function hideModal(){
 paused=false;$("overlay").hidden=true;render();if(game.player!==0&&game.winner===null)scheduleBots();
}
function showRules(){
 showModal("AS LEIS DO BURACO",'<p>Quatro casas disputam uma partida clássica em duplas. <strong>Você e Lyra</strong> jogam contra Morgath e Valtor.</p>'+
 '<ul><li>Cada jogador recebe 11 cartas; existem dois mortos de 11.</li><li>Na sua vez, compre uma carta do monte ou recolha <strong>todo o lixo aberto</strong>.</li><li>Selecione três ou mais cartas consecutivas do mesmo naipe e toque <strong>Baixar</strong>. Para ampliar um jogo da sua dupla, selecione as cartas e toque nele.</li><li>O 2 é curinga; cada sequência aceita no máximo um coringa fora da posição natural. Trincas não valem.</li><li>Uma canastra exige sete ou mais cartas. A limpa vale 200 pontos e a suja, 100.</li><li>Depois de baixar seus jogos, escolha uma carta e toque em <strong>Descartar</strong>.</li><li>Para bater no final, a dupla precisa ter pegado o morto e possuir uma canastra limpa.</li></ul>'+
 '<p class="muted">Variação: Buraco Aberto brasileiro com 104 cartas, sem curingões.</p>');
}
function showSettings(){
 const opts=Object.entries(difficultyNames).map(([v,l])=>'<option value="'+v+'" '+(game.difficulty===v?"selected":"")+'>'+l+"</option>").join("");
 showModal("SALÃO DOS ESTRATEGISTAS",'<p>Escolha a inteligência dos três personagens. As cartas são distribuídas por embaralhamento criptográfico independente em cada partida.</p><label for="difficulty">DIFICULDADE DOS BOTS</label><select id="difficulty">'+opts+'</select>'+
 '<p class="muted"><strong>Fácil:</strong> jogadas mais simples e descartes aleatórios.<br><strong>Normal:</strong> forma sequências e protege cartas úteis.<br><strong>Difícil:</strong> prioriza canastras e avalia risco dos descartes.<br><strong>Lendário:</strong> evita alimentar o lixo e protege jogos aliados.</p>'+
 '<button id="new-game-btn" class="action-btn action-gold">✦ SALVAR E INICIAR NOVA PARTIDA</button>');
 $("new-game-btn").addEventListener("click",()=>{
   difficulty=$("difficulty").value;localStorage.setItem("buraco-arcano-difficulty",difficulty);
   game=new BuracoGame(difficulty);selected.clear();thinking=false;hideModal();
 });
}
function showEndIfNeeded(){
 if(game.winner===null)return;
 const sums=game.lastRoundScore;let win;
 if(game.winner==="stock")win="MONTE ESGOTADO";else win=game.winner===0?"VITÓRIA DA AURORA":"VITÓRIA DA SOMBRA";
 const matchDone=game.totals.some(x=>x>=3000);
 showModal(win,'<p class="muted" style="text-align:center">PONTUAÇÃO DA RODADA</p><div class="result"><p>CASA DA AURORA<b>'+sums[0]+'</b></p><p>CASA DA SOMBRA<b>'+sums[1]+'</b></p></div>'+
 '<p class="muted" style="text-align:center">PLACAR ACUMULADO: '+game.totals[0]+' × '+game.totals[1]+'</p>'+
 (matchDone?'<p style="text-align:center">O torneio terminou! Uma das casas alcançou os 3.000 pontos.</p>':'<p style="text-align:center">O duelo continua até uma casa alcançar 3.000 pontos.</p>')+
 '<button id="next-round-btn" class="action-btn action-gold">'+(matchDone?"NOVA PARTIDA":"PRÓXIMA RODADA")+'</button>');
 $("next-round-btn").addEventListener("click",()=>{
   if(matchDone)game=new BuracoGame(difficulty);else game.startRound();
   selected.clear();thinking=false;hideModal();
 });
}
function scheduleBots(){
 if(thinking||paused||game.winner!==null||game.player===0)return;
 thinking=true;render();
 const step=()=>{
   if(paused){thinking=false;return;}
   if(game.winner!==null){thinking=false;render();showEndIfNeeded();return;}
   if(game.player===0){thinking=false;render();return;}
   try{game.aiPlay(game.player);}catch(error){thinking=false;game.log("Erro do bot: "+error.message);console.error(error);render();return;}
   render();if(game.winner!==null){thinking=false;showEndIfNeeded();return;}
   setTimeout(step,530);
 };
 setTimeout(step,650);
}
$("stock").addEventListener("click",()=>{if(!thinking&&!paused&&game.buyStock(0)){selected.clear();render();showEndIfNeeded();}});
$("discard-pile").addEventListener("click",()=>{if(!thinking&&!paused&&game.buyDiscard(0)){selected.clear();render();}});
$("meld-btn").addEventListener("click",()=>{if(game.meld(0,[...selected]))selected.clear();render();showEndIfNeeded();});
$("discard-btn").addEventListener("click",()=>{if(selected.size!==1)return;const card=[...selected][0];if(game.discard(0,card)){selected.clear();render();showEndIfNeeded();scheduleBots();}else render();});
$("inspect-discard").addEventListener("click",()=>{
 const cards=game.discardPile.map((c,i)=>'<div class="discard-item"><span class="discard-order">'+(i+1)+'</span><span class="inspect-card '+((c.s===1||c.s===2)?"red":"")+'">'+cardName(c)+'</span></div>').join("");
 showModal("LIXO ABERTO",'<p>Você pode examinar todas as cartas descartadas antes de decidir se vai recolher o monte inteiro.</p><div class="discard-gallery">'+cards+'</div><p class="muted">'+game.discardPile.length+' carta(s) do descarte, em ordem de chegada. A última é a do topo.</p>');
});
$("rules-toggle").addEventListener("click",showRules);
$("help-hint").addEventListener("click",showRules);
$("settings-toggle").addEventListener("click",showSettings);
$("modal-close").addEventListener("click",hideModal);
window.addEventListener("resize",render);
window.addEventListener("keydown",e=>{if(e.key==="Escape"&&!$("overlay").hidden)hideModal();});
render();
// Exposição limitada para testes automatizados do navegador; sem acesso a mãos ocultas pela interface.
if(new URLSearchParams(location.search).has("qa"))window.__buracoQa={getGame:()=>game,render};
