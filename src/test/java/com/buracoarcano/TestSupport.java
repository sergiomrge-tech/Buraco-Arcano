package com.buracoarcano;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Funções de apoio às suítes, sem dependências externas. */
public final class TestSupport {
    private TestSupport() {}
    public static void check(boolean value,String message) {
        if(!value)throw new AssertionError(message);
    }
    public static GameEngine game(long seed,String difficulty) {
        Random rng=new Random(seed);
        return new GameEngine(difficulty,rng::nextInt);
    }
    public static GameEngine.Card card(int copy,int suit,int rank) {
        return new GameEngine.Card(copy,suit,rank);
    }
    public static List<GameEngine.Card> deck() {
        List<GameEngine.Card> list=new ArrayList<>();
        for(int copy=0;copy<2;copy++)for(int suit=0;suit<4;suit++)
            for(int rank=1;rank<=13;rank++)list.add(card(copy,suit,rank));
        return list;
    }
    public static void invariant(GameEngine g,String state) {
        check(g.totalCards()==104,state+": contagem "+g.totalCards()+" diferente de 104");
        Set<String> ids=new HashSet<>();
        for(GameEngine.Card c:g.stock)check(ids.add(c.id),state+": duplicata no monte "+c.id);
        for(GameEngine.Card c:g.discardPile)check(ids.add(c.id),state+": duplicata no descarte "+c.id);
        for(List<GameEngine.Card> h:g.hands)for(GameEngine.Card c:h)
            check(ids.add(c.id),state+": duplicata em mão "+c.id);
        for(List<GameEngine.Card> d:g.deads)if(d!=null)for(GameEngine.Card c:d)
            check(ids.add(c.id),state+": duplicata em morto "+c.id);
        for(List<GameEngine.Meld> ms:g.melds)for(GameEngine.Meld m:ms)
            for(GameEngine.Card c:m.cards)check(ids.add(c.id),state+": duplicata em jogo "+c.id);
        check(ids.size()==104,state+": faltam cartas");
        for(List<GameEngine.Meld> ms:g.melds)for(GameEngine.Meld m:ms)
            check(GameEngine.validate(m.cards)!=null,state+": jogo inválido aceito");
        check(g.player>=0&&g.player<4,state+": turno inválido");
        check(g.phase.equals("draw")||g.phase.equals("meld")||g.phase.equals("end"),state+": fase desconhecida");
        check(g.stateJson().contains("\"totalCards\":104"),state+": snapshot divergente");
    }
    public static boolean containsAllIds(GameEngine g,Set<String> expected) {
        Set<String> ids=new HashSet<>();
        for(GameEngine.Card c:g.stock)ids.add(c.id);
        for(GameEngine.Card c:g.discardPile)ids.add(c.id);
        for(List<GameEngine.Card> h:g.hands)for(GameEngine.Card c:h)ids.add(c.id);
        for(List<GameEngine.Card> d:g.deads)if(d!=null)for(GameEngine.Card c:d)ids.add(c.id);
        for(List<GameEngine.Meld> ms:g.melds)for(GameEngine.Meld m:ms)for(GameEngine.Card c:m.cards)ids.add(c.id);
        return ids.equals(expected);
    }
    public static void okay(String suite,long cases) {
        System.out.println("OK | "+suite+" | "+cases+" casos");
    }
}
