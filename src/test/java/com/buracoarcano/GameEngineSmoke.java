package com.buracoarcano;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Execução sem navegador que testa a versão Java, não um substituto JS. */
public final class GameEngineSmoke {
  private static void check(boolean condition, String description) {
    if (!condition) throw new AssertionError(description);
  }
  private static List<GameEngine.Card> cards(int suit,int...ranks) {
    List<GameEngine.Card> result=new ArrayList<>();
    for(int r:ranks) result.add(new GameEngine.Card(0,suit,r));
    return result;
  }
  private static void checkIntegrity(GameEngine game) {
    check(game.totalCards()==104,"Cartas devem ser conservadas entre monte, lixo, mortos, mãos e jogos");
    Set<String> ids=new HashSet<>();
    for(GameEngine.Card card:game.stock) check(ids.add(card.id),"Carta duplicada no monte");
    for(GameEngine.Card card:game.discardPile) check(ids.add(card.id),"Carta duplicada no lixo");
    for(List<GameEngine.Card> hand:game.hands)
      for(GameEngine.Card card:hand) check(ids.add(card.id),"Carta duplicada em mãos");
    for(List<GameEngine.Card> dead:game.deads)
      if(dead!=null) for(GameEngine.Card card:dead) check(ids.add(card.id),"Carta duplicada no morto");
    for(List<GameEngine.Meld> melds:game.melds)
      for(GameEngine.Meld meld:melds)
        for(GameEngine.Card card:meld.cards) check(ids.add(card.id),"Carta duplicada em jogos");
    check(ids.size()==104,"104 cartas únicas");
  }
  private static String fingerprint(GameEngine game) {
    StringBuilder sb=new StringBuilder();
    for(List<GameEngine.Card> h:game.hands) for(GameEngine.Card c:h)sb.append(c.id).append('/');
    return sb.toString();
  }
  public static void main(String[] args) {
    check(GameEngine.validate(cards(0,3,4,5))!=null,"Sequência válida");
    check(GameEngine.validate(cards(0,7,8,10))==null,"Sequência com lacuna inválida");
    check(GameEngine.validate(cards(0,3,4,5,6,7,8,9)).clean,"Canastra limpa");
    List<GameEngine.Card> dirty=cards(0,5,6,7,8,9,10);
    dirty.add(new GameEngine.Card(1,1,2));
    check(GameEngine.validate(dirty)!=null && !GameEngine.validate(dirty).clean,"Canastra suja");
    check(GameEngine.validate(cards(0,1,2,3))!=null,"2 natural");
    for(String difficulty:Arrays.asList("facil","normal","dificil","lendario")) {
      GameEngine g=new GameEngine(difficulty,new Random(5724)::nextInt);
      checkIntegrity(g);
      check(g.stock.size()==37,"37 cartas no monte");
      for(List<GameEngine.Card> h:g.hands)check(h.size()==11,"11 cartas por jogador");
      for(List<GameEngine.Card> d:g.deads)check(d.size()==11,"Mortos de 11");
      check(g.discardPile.size()==1,"1 carta inicial no lixo");
      for(int t=0;t<20&&g.winner==null&&!g.stockEnded;t++) {
        int before=g.player;
        if(before==0) {
          check(g.buyStock(0),"Compra humana");
          check(g.discard(0,g.hands[0].get(0).id),"Descarte humano");
        } else g.aiPlay(before);
        checkIntegrity(g);
      }
      System.out.println("Motor Java: "+difficulty+" passou distribuição, conservação e turnos.");
    }
    GameEngine a=new GameEngine("normal",new Random(42)::nextInt);
    GameEngine b=new GameEngine("normal",new Random(43)::nextInt);
    check(!fingerprint(a).equals(fingerprint(b)),"Sementes diferentes precisam distribuir cartas diferentes");
    check(a.stateJson().contains("\"totalCards\":104"),"Estado JSON serializável");
    System.out.println("QA MOTOR JAVA: APROVADO");
  }
}
