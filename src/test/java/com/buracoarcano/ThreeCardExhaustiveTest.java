package com.buracoarcano;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Testa TODAS as combinações não ordenadas de 3 cartas físicas distintas:
 * C(104,3) = 182.104. Oráculo independente por backtracking.
 * Emite linha por caso no CSV: dados de entrada, decisão esperada e obtida.
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
    private static boolean place(List<GameEngine.Card> cards,int at,int start,int suit,int used,int wilds) {
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
        long cases=0,accepted=0,failed=0;
        Path output=Path.of(System.getProperty("buraco.triplesReport","reports/qa-trincas-104.csv"));
        try {
            Path parent=output.getParent();
            if(parent!=null)Files.createDirectories(parent);
            try(BufferedWriter out=Files.newBufferedWriter(output,StandardCharsets.UTF_8)){
                out.write("caso,carta1,carta2,carta3,esperado,obtido,status\n");
                for(int a=0;a<102;a++)for(int b=a+1;b<103;b++)for(int c=b+1;c<104;c++){
                    List<GameEngine.Card> selection=new ArrayList<>(3);
                    selection.add(deck.get(a));selection.add(deck.get(b));selection.add(deck.get(c));
                    boolean expected=possible(selection);
                    GameEngine.Run actual=GameEngine.validate(selection);
                    boolean valid=actual!=null,pass=expected==valid;
                    cases++;
                    out.write("T3-"+String.format("%06d",cases)+","+deck.get(a).id+","+deck.get(b).id+","+deck.get(c).id+
                        ","+expected+","+valid+","+(pass?"PASS":"FAIL")+"\n");
                    if(!pass)failed++;
                    if(valid)accepted++;
                }
            }
        } catch(IOException e) {throw new IllegalStateException("Falha ao gerar relatório de casos",e);}
        TestSupport.check(cases==182104,"Total de casos incorreto");
        TestSupport.check(failed==0,failed+" de 182104 divergências; consultar CSV "+output);
        System.out.println("  Válidas="+accepted+" | divergentes="+failed+" | relatório="+output);
        TestSupport.okay("Todas as trincas C(104,3)",cases);
        return cases;
    }
    public static void main(String[] args){run();}
}
