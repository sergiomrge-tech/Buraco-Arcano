package com.buracoarcano;

import java.util.ArrayList;
import java.util.List;

/**
 * Testa TODAS as combinações não ordenadas de 3 cartas físicas distintas
 * do baralho duplo: C(104,3)=182.104 casos.
 * Oráculo separado: atribuição exaustiva de slots com backtracking.
 */
public final class ThreeCardExhaustiveTest {
    private static boolean possible(List<GameEngine.Card> cards) {
        int inferred=-1;
        for(GameEngine.Card c:cards)if(c.rank!=2){
            if(inferred<0)inferred=c.suit;
            else if(inferred!=c.suit)return false;
        }
        if(inferred<0)return false;
        for(int start=1;start<=12;start++)
            if(place(cards,0,start,inferred,0,0))return true;
        return false;
    }
    private static boolean place(List<GameEngine.Card> cards,int at,int start,int suit,int used,int wilds){
        if(at==cards.size())return true;
        GameEngine.Card c=cards.get(at);
        for(int i=0;i<3;i++){
            if((used&(1<<i))!=0)continue;
            int slot=start+i;
            boolean natural=c.suit==suit&&(c.rank==slot||(c.rank==1&&slot==14));
            boolean wildcard=c.rank==2&&!natural;
            if(!natural&&!(wildcard&&wilds==0))continue;
            if(place(cards,at+1,start,suit,used|(1<<i),wilds+(wildcard?1:0)))return true;
        }
        return false;
    }
    public static long run() {
        List<GameEngine.Card> deck=TestSupport.deck();
        long cases=0,accepted=0;
        for(int a=0;a<102;a++)for(int b=a+1;b<103;b++)for(int c=b+1;c<104;c++){
            List<GameEngine.Card> selected=new ArrayList<>(3);
            selected.add(deck.get(a));selected.add(deck.get(b));selected.add(deck.get(c));
            boolean expected=possible(selected);
            GameEngine.Run actual=GameEngine.validate(selected);
            TestSupport.check((actual!=null)==expected,
                "Motor divergiu do oráculo em "+deck.get(a).id+","+deck.get(b).id+","+deck.get(c).id+
                " (esperado "+expected+", recebido "+(actual!=null)+")");
            if(actual!=null)accepted++;
            cases++;
        }
        TestSupport.check(cases==182104,"Número de combinações incorreto");
        System.out.println("  Sequências válidas encontradas: "+accepted);
        TestSupport.okay("Todas as trincas C(104,3)",cases);
        return cases;
    }
    public static void main(String[] args){run();}
}
