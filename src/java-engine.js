/**
 * Adaptador de apresentação. TODAS as mudanças de estado são executadas no
 * GameEngine.java compilado via TeaVM. O JavaScript apenas atualiza a interface.
 */
import * as java from "../web/java/engine.js";

export class JavaBuracoGame {
  constructor(difficulty="normal") {
    java.newGame(difficulty);
    this.logs=[];
    this.refresh();
  }
  refresh() {
    const data=JSON.parse(java.state());
    Object.assign(this,data);
    return this;
  }
  startRound() { java.newRound(); return this.refresh(); }
  buyStock(player) { const ok=java.buyStock(player); this.refresh(); return ok; }
  buyDiscard(player) { const ok=java.buyDiscard(player); this.refresh(); return ok; }
  meld(player, ids, groupIndex=null) {
    const ok=java.meld(player,ids.join(","),groupIndex===null?-1:groupIndex);
    this.refresh(); return ok;
  }
  discard(player,id) { const ok=java.discard(player,String(id)); this.refresh(); return ok; }
  aiPlay(player) { java.aiPlay(player); return this.refresh(); }
  log(message) { this.message=message;this.logs.unshift(message); }
  playerName(player) { return ["Você","Morgath","Lyra","Valtor"][player]; }
}
