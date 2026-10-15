package com.buracoarcano;
import java.util.*;
public final class ReleaseSmokeTest {
 static int checks=0;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void rejected(String save){try{GameEngine.restore(save,n->0);throw new AssertionError("Save incorreto aceito");}catch(IllegalArgumentException expected){checks++;}}
 public static void main(String[] args){
  check(GameEngine.value(new GameEngine.Card(0,0,2))==20,"2 vale 20");
  List<GameEngine.Card> clean=new ArrayList<>();for(int r=1;r<=13;r++)clean.add(new GameEngine.Card(0,0,r));
  check(GameEngine.validate(clean).clean,"2 natural mantém limpa");
  clean.add(new GameEngine.Card(1,0,1));check(GameEngine.validate(clean)!=null,"Ás a Ás com 14 cartas");
  check(GameEngine.meldBonus(new GameEngine.Meld(clean))==1000,"Bônus 14 cartas");
  check(GameEngine.validate(Arrays.asList(clean.get(0),clean.get(0),clean.get(1)))==null,"ID duplicado rejeitado");
  for(String d:Arrays.asList("facil","normal","dificil","lendario")){
   Random r=new Random(62);GameEngine g=new GameEngine(d,r::nextInt);
   check(g.totalCards()==104&&g.stock.size()==37,"Distribuição inicial");
   String save=g.checkpoint();GameEngine restored=GameEngine.restore(save,r::nextInt);check(save.equals(restored.checkpoint()),"Restaurar compra");
   g.buyStock(0);save=g.checkpoint();check(save.equals(GameEngine.restore(save,r::nextInt).checkpoint()),"Restaurar jogada");
   g.discard(0,g.hands[0].get(0).id);
   for(int i=0;i<3;i++){int before=g.player;g.aiPlay(before);check(g.player!=before||g.stockEnded,"Bot conclui turno");check(g.totalCards()==104,"Conserva cartas");}
   check(g.player==0||g.stockEnded,"Volta ao humano");
   rejected(save.replaceFirst("0-","9-"));
  }
  rejected("BA1|save quebrado");
  System.out.println(checks+" verificações dirigidas aprovadas. Sem enumeração exaustiva.");
 }
}
