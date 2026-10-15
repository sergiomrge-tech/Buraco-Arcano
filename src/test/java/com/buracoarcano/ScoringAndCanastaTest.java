package com.buracoarcano;

import java.util.ArrayList;
import java.util.List;

/** Variações de pontuação: canastra suja, limpa, A-Rei e A-A. */
public final class ScoringAndCanastaTest {
    private static List<GameEngine.Card> cards(int start,int size){
        List<GameEngine.Card> list=new ArrayList<>();
        for(int i=0;i<size;i++){
            int r=start+i;
            list.add(TestSupport.card(r==14?1:0,0,r==14?1:r));
        }
        return list;
    }
    public static long run(){
        long cases=0;
        for(int len=3;len<=14;len++)for(int start=1;start+len-1<=14;start++){
            List<GameEngine.Card> clean=cards(start,len);
            int expected=len<7?0:len>=14?1000:len>=13?500:200;
            TestSupport.check(GameEngine.validate(clean)!=null,"canastra limpa inválida "+start+"/"+len);
            TestSupport.check(GameEngine.meldBonus(new GameEngine.Meld(clean))==expected,
                "bônus limpo errado "+start+"/"+len);cases++;
            for(int replace=0;replace<len;replace++){
                List<GameEngine.Card> dirty=cards(start,len);
                dirty.set(replace,TestSupport.card(0,1,2));
                GameEngine.Run analyzed=GameEngine.validate(dirty);
                TestSupport.check(analyzed!=null&&!analyzed.clean,"canastra suja inválida "+start+"/"+len+"/"+replace);
                TestSupport.check(GameEngine.meldBonus(new GameEngine.Meld(dirty))==(len>=7?100:0),
                    "bônus sujo errado "+start+"/"+len+"/"+replace);cases++;
            }
        }
        for(int suit=0;suit<4;suit++){
            TestSupport.check(GameEngine.value(TestSupport.card(0,suit,1))==15,"A 15");
            TestSupport.check(GameEngine.value(TestSupport.card(0,suit,2))==10,"2 10");
            for(int r=3;r<=7;r++)TestSupport.check(GameEngine.value(TestSupport.card(0,suit,r))==5,"3 a 7 valem 5");
            for(int r=8;r<=13;r++)TestSupport.check(GameEngine.value(TestSupport.card(0,suit,r))==10,"8 a K valem 10");
            cases+=13;
        }
        TestSupport.okay("Pontuação, janelas naturais e sujas",cases);
        return cases;
    }
    public static void main(String[] args){run();}
}
