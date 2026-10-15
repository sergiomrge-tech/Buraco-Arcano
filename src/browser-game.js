/**
 * Adaptador da mesa HTML para as regras em Java compiladas pelo TeaVM.
 * Todas as mutações de cartas, pontuação e decisões de bot ocorrem no Java.
 */
import * as nativeJava from "../web/java/engine.js";
export class BuracoGame {
  constructor(difficulty="normal"){
    nativeJava.newGame(difficulty);this.sync();
  }
  sync(){
    const snapshot=JSON.parse(nativeJava.state());
    Object.assign(this,snapshot);
    if(this.totalCards!==104)throw new Error("Integridade do baralho comprometida: "+this.totalCards+"/104");
    return this;
  }
  startRound(){nativeJava.newRound();return this.sync();}
  buyStock(player){const ok=nativeJava.buyStock(player);this.sync();return ok;}
  buyDiscard(player){const ok=nativeJava.buyDiscard(player);this.sync();return ok;}
  meld(player,ids,index=null){
    const ok=nativeJava.meld(player,[...ids].join(","),index===null?-1:index);
    this.sync();return ok;
  }
  discard(player,id){const ok=nativeJava.discard(player,id);this.sync();return ok;}
  aiPlay(player){nativeJava.aiPlay(player);this.sync();}
  playerName(player){return ["Você","Morgath","Lyra","Valtor"][player];}
}
