package com.buracoarcano;
import java.util.Random;
public final class Playthrough {
 public static void main(String[] args){
  for(String d:new String[]{"facil","normal","dificil","lendario"}){
   Random r=new Random(9816);GameEngine g=new GameEngine(d,r::nextInt);
   for(int round=1;round<=3;round++){
    int turns=0;while(!g.stockEnded&&turns++<2000){int p=g.player;g.aiPlay(p);if(g.totalCards()!=104)throw new AssertionError("Cartas perdidas");if(g.player==p&&!g.stockEnded)throw new AssertionError("Turno travado "+d+" mão "+g.hands[p].size());}
    if(!g.stockEnded||g.lastRoundScore==null)throw new AssertionError("Não encerrou");
    String save=g.checkpoint();if(!GameEngine.restore(save,r::nextInt).checkpoint().equals(save))throw new AssertionError("Save final");
    System.out.println(d+" rodada "+round+" encerrada; "+turns+" turnos; save final OK");if(round<3)g.newRound();
   }
  }
 }
}
