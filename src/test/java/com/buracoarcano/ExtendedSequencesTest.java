package com.buracoarcano;

import java.util.ArrayList;
import java.util.List;

/**
 * Varre os 4 naipes, todos os tamanhos de 3 a 14, as posições iniciais
 * possíveis e a substituição de CADA posição por um 2 curinga.
 * Inclui canastra real A-2-3-...-K-A (14 cartas, duas cópias do Ás).
 */
public final class ExtendedSequencesTest {
    private static List<GameEngine.Card> sequence(int suit,int start,int size) {
        List<GameEngine.Card> cards=new ArrayList<>();
        for(int k=0;k<size;k++) {
            int rank=start+k;
            cards.add(TestSupport.card(rank==14?1:0,suit,rank==14?1:rank));
        }
        return cards;
    }
    public static long run(){
        long tested=0,clean=0,dirty=0,invalid=0;
        for(int suit=0;suit<4;suit++)for(int length=3;length<=14;length++)
            for(int start=1;start+length-1<=14;start++){
                List<GameEngine.Card> full=sequence(suit,start,length);
                GameEngine.Run run=GameEngine.validate(full);
                TestSupport.check(run!=null&&run.clean&&run.wild==0,
                    "Sequência natural inválida: suit="+suit+" start="+start+" length="+length);
                clean++;tested++;

                // Em qualquer posição, um 2 de outro naipe pode substituir carta.
                for(int slot=0;slot<length;slot++){
                    List<GameEngine.Card> test=new ArrayList<>(full);
                    test.set(slot,TestSupport.card(1,(suit+1)%4,2));
                    run=GameEngine.validate(test);
                    TestSupport.check(run!=null&&!run.clean&&run.wild==1,
                        "Curinga isolado deve ser aceito: suit="+suit+" start="+start+
                        " length="+length+" slot="+slot);
                    dirty++;tested++;
                }

                // Dois curingas fora do naipe nunca podem formar uma sequência.
                for(int a=0;a<length;a++)for(int b=a+1;b<length;b++){
                    List<GameEngine.Card> test=new ArrayList<>(full);
                    test.set(a,TestSupport.card(0,(suit+1)%4,2));
                    test.set(b,TestSupport.card(1,(suit+2)%4,2));
                    TestSupport.check(GameEngine.validate(test)==null,
                        "Dois curingas aceitos indevidamente: suit="+suit+" length="+length);
                    invalid++;tested++;
                }
            }

        for(int suit=0;suit<4;suit++){
            List<GameEngine.Card> sameRank=new ArrayList<>();
            sameRank.add(TestSupport.card(0,suit,8));
            sameRank.add(TestSupport.card(1,suit,8));
            sameRank.add(TestSupport.card(0,suit,9));
            TestSupport.check(GameEngine.validate(sameRank)==null,"Duplicação de rank aceita");
            tested++;invalid++;

            List<GameEngine.Card> mix=sequence(suit,3,3);
            mix.set(1,TestSupport.card(0,(suit+1)%4,4));
            TestSupport.check(GameEngine.validate(mix)==null,"Naipes mistos aceitos");
            tested++;invalid++;

            for(int length=0;length<3;length++){
                TestSupport.check(GameEngine.validate(sequence(suit,3,length))==null,
                    "Jogo com menos de três cartas aceito");tested++;invalid++;
            }
            List<GameEngine.Card> oversized=sequence(suit,1,14);
            oversized.add(TestSupport.card(1,suit,13));
            TestSupport.check(GameEngine.validate(oversized)==null,"Jogo com 15 cartas aceito");
            tested++;invalid++;
        }
        System.out.println("  naturais="+clean+" | com curinga="+dirty+" | inválidas="+invalid);
        TestSupport.okay("Sequências completas por tamanho/naipe/coringa",tested);
        return tested;
    }
    public static void main(String[] args){run();}
}
