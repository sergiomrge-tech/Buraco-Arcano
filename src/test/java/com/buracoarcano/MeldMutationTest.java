package com.buracoarcano;

import java.util.ArrayList;
import java.util.List;

/** Baixar jogos, ampliá-los e impedir que jogadas inválidas consumam cartas. */
public final class MeldMutationTest {
    private static GameEngine.Card ensureInHand(GameEngine g,int p,int suit,int rank){
        for(GameEngine.Card c:g.hands[p])if(c.suit==suit&&c.rank==rank)return c;
        List<List<GameEngine.Card>> sources=new ArrayList<>();
        sources.add(g.stock);sources.add(g.discardPile);
        for(int q=0;q<4;q++)if(q!=p)sources.add(g.hands[q]);
        for(List<GameEngine.Card> d:g.deads)if(d!=null)sources.add(d);
        for(List<GameEngine.Card> from:sources)
            for(int i=0;i<from.size();i++){
                GameEngine.Card c=from.get(i);
                if(c.suit==suit&&c.rank==rank){
                    from.remove(i);g.hands[p].add(c);return c;
                }
            }
        throw new AssertionError("Carta física ausente para teste "+suit+"/"+rank);
    }
    public static long run(){
        long cases=0;
        for(int seed=0;seed<150;seed++){
            GameEngine g=TestSupport.game(seed,"normal");
            TestSupport.check(g.buyStock(0),"compra inicial");cases++;
            GameEngine.Card a=ensureInHand(g,0,0,3);
            GameEngine.Card b=ensureInHand(g,0,0,4);
            GameEngine.Card c=ensureInHand(g,0,0,5);
            String ids=a.id+","+b.id+","+c.id;
            int before=g.hands[0].size();
            TestSupport.check(!g.meld(0,ids,-2),"índice negativo não permitido");cases++;
            TestSupport.check(g.hands[0].size()==before,"jogo recusado alterou mão");cases++;
            TestSupport.check(g.meld(0,ids,-1),"3-4-5 deveria baixar");cases++;
            TestSupport.check(g.melds[0].size()==1&&g.melds[0].get(0).cards.size()==3,"primeira sequência ausente");cases++;
            TestSupport.invariant(g,"primeira baixa");

            GameEngine.Card next=ensureInHand(g,0,0,6);
            TestSupport.check(g.meld(0,next.id,0),"extensão 6 deveria funcionar");cases++;
            TestSupport.check(g.melds[0].get(0).cards.size()==4,"sequência não foi estendida");cases++;
            TestSupport.invariant(g,"extensão");

            GameEngine.Card wrong=ensureInHand(g,0,1,11);
            int handCount=g.hands[0].size();
            TestSupport.check(!g.meld(0,wrong.id,0),"naipe incorreto deveria ser rejeitado");cases++;
            TestSupport.check(g.hands[0].size()==handCount,"rejeição mutou a mão");cases++;
            TestSupport.check(!g.meld(0,wrong.id,8),"índice inexistente aceito");cases++;
            TestSupport.check(!g.meld(1,wrong.id,-1),"jogador rival poderia baixar na vez errada");cases++;
            TestSupport.invariant(g,"jogadas rejeitadas");
        }
        TestSupport.okay("Criar, ampliar e rejeitar sequências sem mutação",cases);
        return cases;
    }
    public static void main(String[] args){run();}
}
