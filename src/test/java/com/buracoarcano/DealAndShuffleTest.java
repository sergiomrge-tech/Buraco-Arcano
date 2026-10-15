package com.buracoarcano;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Distribuição completa, propriedades de Fisher-Yates e teste estatístico simples. */
public final class DealAndShuffleTest {
    public static long run() {
        final int samples=8000;
        int[] firstSuit={0,0,0,0};
        Set<String> firstTen=new HashSet<>();
        for(int i=0;i<samples;i++){
            long seed=0x5EEDL+i*7919L;
            GameEngine g=TestSupport.game(seed,"normal");
            TestSupport.invariant(g,"distribuição semente "+seed);
            TestSupport.check(g.stock.size()==37,"37 cartas no monte");
            TestSupport.check(g.discardPile.size()==1,"1 carta no lixo");
            for(int p=0;p<4;p++)TestSupport.check(g.hands[p].size()==11,"11 na mão "+p);
            for(int t=0;t<2;t++)TestSupport.check(g.deads[t].size()==11,"11 no morto "+t);
            GameEngine.Card first=g.hands[0].get(0);
            firstSuit[first.suit]++;
            if(i<400){
                StringBuilder signature=new StringBuilder();
                for(int k=0;k<10;k++)signature.append(g.hands[0].get(k).id).append(',');
                firstTen.add(signature.toString());
                GameEngine copy=TestSupport.game(seed,"normal");
                TestSupport.check(copy.stateJson().equals(g.stateJson()),"seed fixa não reproduz distribuição");
            }
        }
        // Testa frequências grosseiras, sem confundir estatística com prova de aleatoriedade.
        for(int suit=0;suit<4;suit++)
            TestSupport.check(Math.abs(firstSuit[suit]-samples/4)<samples*0.05,
                "Viés suspeito na distribuição: naipe "+suit+" obteve "+firstSuit[suit]);
        TestSupport.check(firstTen.size()>350,"Embaralhamento não variou adequadamente");
        System.out.println("  Frequências 1ª carta por naipe: "+java.util.Arrays.toString(firstSuit));
        TestSupport.okay("Distribuição e embaralhamento",samples);
        return samples;
    }
    public static void main(String[] args){run();}
}
