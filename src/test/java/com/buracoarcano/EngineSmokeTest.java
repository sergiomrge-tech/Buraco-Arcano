package com.buracoarcano;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

public final class EngineSmokeTest {
    private static final SecureRandom random=new SecureRandom();
    private static final GameEngine.RandomSource rng=random::nextInt;
    private static void expect(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args){
        HashSet<String> startingHands=new HashSet<>();
        for(int i=0;i<30;i++) {
            GameEngine g=new GameEngine("normal",rng);
            expect(g.totalCards()==104,"baralho deve ter 104 cartas");
            expect(g.stock.size()==37,"monte precisa ter 37 cartas");
            expect(g.discardPile.size()==1,"lixo inicial 1 carta");
            for(int p=0;p<4;p++)expect(g.hands[p].size()==11,"11 por mao");
            startingHands.add(g.hands[0].get(0).id+"-"+g.hands[0].get(1).id);
        }
        expect(startingHands.size()>26,"distribuicao precisa ser aleatoria");
        List<GameEngine.Card> valid=new ArrayList<>();
        valid.add(new GameEngine.Card(0,0,3));
        valid.add(new GameEngine.Card(0,0,4));
        valid.add(new GameEngine.Card(0,0,5));
        expect(GameEngine.validate(valid)!=null,"sequencia valida");
        valid.set(2,new GameEngine.Card(0,1,5));
        expect(GameEngine.validate(valid)==null,"não misturar naipes");
        valid.set(2,new GameEngine.Card(0,1,2));
        expect(GameEngine.validate(valid)!=null,"2 curinga");
        for(String level:new String[]{"facil","normal","dificil","lendario"}) {
            GameEngine g=new GameEngine(level,rng);
            for(int i=0;i<80&& !g.stockEnded;i++) {
                int p=g.player;
                if(p==0) {
                    if(g.buyStock(p)&&!g.hands[p].isEmpty())g.discard(p,g.hands[p].get(0).id);
                } else g.aiPlay(p);
                expect(g.totalCards()==104,"cartas perdidas na rodada "+level+": "+g.totalCards());
            }
            expect(g.round==1,"rodada deve permanecer 1");
            String json=g.stateJson();
            expect(json.contains("\"hands\"")&&json.contains("\"melds\"")&&json.contains("\"totals\""),"JSON completo");
        }
        System.out.println("JAVA OK: aleatoriedade, distribuicao, canastras, conservacao, 4 bots, serializacao.");
    }
}
